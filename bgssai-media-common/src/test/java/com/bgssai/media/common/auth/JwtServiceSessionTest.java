package com.bgssai.media.common.auth;
import com.bgssai.media.common.mapper.SysUserMapper;
import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class JwtServiceSessionTest {
    @Test void laterLoginRevokesEarlierTokenAndDisabledAccountCannotReuseToken() {
        SysUserMapper users = mock(SysUserMapper.class);
        AtomicReference<String> current = new AtomicReference<>();
        when(users.bindSession(eq(1L), eq("USER"), anyString())).thenAnswer(i -> { current.set(i.getArgument(2)); return 1; });
        when(users.currentSession(1L, "USER")).thenAnswer(i -> current.get());
        JwtService service = new JwtService("a-test-secret-with-at-least-32-bytes", 3600000, users);
        AuthUser user = new AuthUser(); user.setId(1L); user.setUsername("alice"); user.setRole_code("USER");
        String first = service.createToken(user);
        assertEquals(1L, service.parse(first).getId());
        String second = service.createToken(user);
        assertNotEquals(first, second);
        assertEquals(2003, assertThrows(BizException.class, () -> service.parse(first)).getCode());
        assertEquals(1L, service.parse(second).getId());
        current.set(null);
        assertThrows(BizException.class, () -> service.parse(second));
    }
}
