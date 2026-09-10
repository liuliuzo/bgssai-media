package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.McpToken;
import com.bgssai.media.common.domain.SysUser;
import com.bgssai.media.common.mapper.McpTokenMapper;
import com.bgssai.media.common.mapper.SysUserMapper;
import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class McpTokenServiceTest {

    @Mock McpTokenMapper mcpTokenMapper;
    @Mock SysUserMapper sysUserMapper;

    McpTokenService service;
    final AtomicLong idSeq = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        service = new McpTokenService(mcpTokenMapper, sysUserMapper);
        when(mcpTokenMapper.insertSelective(any(McpToken.class))).thenAnswer(inv -> {
            McpToken row = inv.getArgument(0);
            row.setId(idSeq.getAndIncrement());
            return 1;
        });
    }

    @Test
    void createReturnsPlainTokenOnceAndStoresHashOnly() {
        Map<String, Object> created = service.create(2L, "Claude Desktop");
        assertTrue(String.valueOf(created.get("token")).startsWith(McpTokenService.TOKEN_PREFIX));
        assertEquals("Claude Desktop", created.get("name"));
        assertNotNull(created.get("token_masked"));

        ArgumentCaptor<McpToken> captor = ArgumentCaptor.forClass(McpToken.class);
        verify(mcpTokenMapper).insertSelective(captor.capture());
        McpToken saved = captor.getValue();
        assertEquals(McpTokenService.sha256(String.valueOf(created.get("token"))), saved.getTokenHash());
        assertFalse(saved.getTokenHash().contains(McpTokenService.TOKEN_PREFIX));
    }

    @Test
    void createRejectsBlankName() {
        assertThrows(BizException.class, () -> service.create(2L, "  "));
    }

    @Test
    void authenticateAcceptsBearerAndTouchesLastUsed() {
        String plain = McpTokenService.generatePlainToken();
        McpToken row = new McpToken();
        row.setId(9L);
        row.setUserId(2L);
        row.setStatus("active");
        row.setTokenHash(McpTokenService.sha256(plain));
        when(mcpTokenMapper.selectActiveByHash(row.getTokenHash())).thenReturn(row);

        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("demo");
        user.setStatus("active");
        when(sysUserMapper.selectByPrimaryKey(2L)).thenReturn(user);

        SysUser got = service.authenticate("Bearer " + plain);
        assertNotNull(got);
        assertEquals(2L, got.getId());
        verify(mcpTokenMapper).touchLastUsed(eq(9L), any());
    }

    @Test
    void listReturnsMaskedViewsOnly() {
        McpToken row = new McpToken();
        row.setId(1L);
        row.setName("Cursor");
        row.setTokenPrefix("bm_mcp_abcd");
        row.setTokenSuffix("ef12");
        row.setStatus("active");
        when(mcpTokenMapper.selectActiveByUserId(2L)).thenReturn(List.of(row));

        List<Map<String, Object>> list = service.listForUser(2L);
        assertEquals(1, list.size());
        assertEquals("bm_mcp_abcd...ef12", list.get(0).get("token_masked"));
        assertFalse(list.get(0).containsKey("token"));
        assertFalse(list.get(0).containsKey("token_hash"));
    }
}
