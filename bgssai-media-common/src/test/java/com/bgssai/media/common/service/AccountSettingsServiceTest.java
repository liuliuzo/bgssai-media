package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.SysUser;
import com.bgssai.media.common.domain.UserIdentity;
import com.bgssai.media.common.mapper.OpcExtMapper;
import com.bgssai.media.common.mapper.SysUserMapper;
import com.bgssai.media.common.mapper.UserIdentityMapper;
import com.bgssai.media.common.security.AccountPassword;
import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountSettingsServiceTest {

    SysUserMapper users = mock(SysUserMapper.class);
    UserIdentityMapper identities = mock(UserIdentityMapper.class);
    OpcExtMapper opc = mock(OpcExtMapper.class);
    AccountSettingsService service;
    SysUser user;
    List<Map<String, Object>> idRows = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new AccountSettingsService(users, identities, opc);
        user = new SysUser();
        user.setId(9L);
        user.setUsername("alice");
        user.setPasswordHash(new AccountPassword().encode("secret"));
        user.setEmail("a@example.com");
        user.setStatus("active");
        when(users.selectByPrimaryKey(9L)).thenReturn(user);
        when(opc.listIdentities(9L)).thenReturn(idRows);
        when(opc.countIdentities(9L)).thenAnswer(inv -> idRows.size());
    }

    @Test
    void refusesUnbindLastMethod() {
        user.setPasswordHash(AccountPassword.unusable());
        user.setEmail("a@example.com");
        user.setPhone(null);
        idRows.clear();
        BizException ex = assertThrows(BizException.class, () -> service.unbindEmail(9L));
        assertTrue(ex.getMessage().contains("至少保留"), () -> "msg=" + ex.getMessage());
    }

    @Test
    void unbindAllowedWhileAnotherMethodRemains() {
        user.setPasswordHash(AccountPassword.unusable());
        user.setPhone(null);
        idRows.add(Map.of("provider", "WECHAT", "nickname", "wx"));
        service.unbindEmail(9L);
        verify(users).clearEmail(9L);

        user.setEmail(null);
        BizException last = assertThrows(BizException.class, () -> service.unbindIdentity(9L, "WECHAT"));
        assertEquals(409, last.getCode());
        verify(opc, never()).deleteIdentity(anyLong(), anyString());
    }

    @Test
    void bindRejectsIdentityOwnedByAnotherUser() {
        UserIdentity other = new UserIdentity();
        other.setUserId(77L);
        other.setProvider("WECHAT");
        other.setOpenId("wx-open-1");
        when(identities.findByProviderOpenId("WECHAT", "wx-open-1")).thenReturn(other);
        BizException ex = assertThrows(BizException.class,
                () -> service.bindIdentity(9L, "wechat", "wx-open-1", "nick"));
        assertEquals(409, ex.getCode());
        verify(identities, never()).insert(any());
    }

    @Test
    void deleteSoftDeletesAndRevokesSession() {
        when(opc.softDeleteUser(eq(9L), anyString(), anyString(), any(Date.class))).thenReturn(1);
        Map<String, Object> out = service.deleteAccount(9L);
        assertEquals("deleted", out.get("status"));
        assertEquals(Boolean.TRUE, out.get("session_revoked"));
        verify(opc).softDeleteUser(eq(9L), eq("deleted_9"),
                argThat(h -> h.startsWith(AccountPassword.UNUSABLE_PREFIX)), any(Date.class));
        verify(opc).deleteAllIdentities(9L);
    }

    @Test
    void deletedAccountCannotBeDeletedOrManagedAgain() {
        user.setStatus("deleted");
        assertEquals(401, assertThrows(BizException.class, () -> service.deleteAccount(9L)).getCode());
        assertEquals(401, assertThrows(BizException.class, () -> service.listLoginMethods(9L)).getCode());
        verify(opc, never()).softDeleteUser(anyLong(), anyString(), anyString(), any(Date.class));
    }
}
