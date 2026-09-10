package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaVerifyCode;
import com.bgssai.media.common.mail.PlatformMailSender;
import com.bgssai.media.common.mapper.MediaVerifyCodeMapper;
import com.bgssai.media.common.sms.PlatformSmsSender;
import com.bgssai.media.common.sms.SmsPurpose;
import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 锁住验证码登录的四条硬约束：真实投递、码不回显、一次性消费、失败计数。
 * 这些是「任何人无需持有手机号就能登录」那个洞的直接反面，回归掉一条洞就重开。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerifyCodeServiceTest {

    @Mock MediaVerifyCodeMapper verifyCodeMapper;
    @Mock PlatformMailSender mailSender;
    @Mock PlatformSmsSender smsSender;

    VerifyCodeService service;
    final AtomicReference<MediaVerifyCode> saved = new AtomicReference<>();
    final AtomicReference<String> deliveredCode = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        service = new VerifyCodeService(verifyCodeMapper, mailSender, smsSender);
        when(smsSender.codeExpireSeconds()).thenReturn(300);
        when(verifyCodeMapper.countSentSince(anyString(), any(Date.class))).thenReturn(0);
        when(verifyCodeMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaVerifyCode row = inv.getArgument(0);
            row.setId(1L);
            saved.set(row);
            return 1;
        });
        when(smsSender.sendCode(anyString(), any(SmsPurpose.class), anyString())).thenAnswer(inv -> {
            deliveredCode.set(inv.getArgument(2));
            return "req-1";
        });
    }

    @Test
    void sendPhoneCodeDeliversRealCodeAndStoresOnlyItsDigest() {
        service.sendPhoneCode("13800000000", "LOGIN");

        verify(smsSender).sendCode(eq("13800000000"), eq(SmsPurpose.LOGIN), anyString());
        String code = deliveredCode.get();
        assertNotNull(code);
        assertTrue(code.matches("\\d{6}"), "验证码应为 6 位数字");
        assertNotEquals("123456", code, "固定演示码必须已经绝迹");
        // 落库的是摘要不是明文：库被读到时明文码等同于可直接登录的凭证。
        assertEquals(sha256(code), saved.get().getCodeHash());
        assertNull(saved.get().getUsed());
        assertTrue(saved.get().getExpiresAt().after(new Date()));
    }

    @Test
    void sendPhoneCodeRejectsWhenChannelUnconfigured() {
        when(smsSender.sendCode(anyString(), any(SmsPurpose.class), anyString()))
                .thenThrow(new BizException(503, "短信服务尚未配置，请联系管理员"));

        assertThrows(BizException.class, () -> service.sendPhoneCode("13800000000", "LOGIN"));
        // 投递失败不能留下一条可用的码，否则「发送失败」的用户仍能登录。
        verify(verifyCodeMapper, never()).insertSelective(any());
    }

    @Test
    void resendWithinIntervalIsRejected() {
        when(verifyCodeMapper.countSentSince(anyString(), any(Date.class))).thenReturn(1);

        BizException ex = assertThrows(BizException.class, () -> service.sendPhoneCode("13800000000", "LOGIN"));
        assertEquals(429, ex.getCode());
        verify(smsSender, never()).sendCode(anyString(), any(), anyString());
    }

    @Test
    void consumeMarksCodeUsedExactlyOnce() {
        MediaVerifyCode row = usableRow("654321");
        when(verifyCodeMapper.selectLatestUsable(eq("13800000000"), eq("LOGIN"), any(Date.class))).thenReturn(row);
        when(verifyCodeMapper.markUsed(eq(1L), any(Date.class))).thenReturn(1).thenReturn(0);

        service.consume("13800000000", "654321", "LOGIN");

        // 第二次用同一个码：markUsed 返回 0（已被消费），必须失败。
        assertThrows(BizException.class, () -> service.consume("13800000000", "654321", "LOGIN"));
    }

    @Test
    void wrongCodeCountsAnAttempt() {
        MediaVerifyCode row = usableRow("654321");
        when(verifyCodeMapper.selectLatestUsable(anyString(), anyString(), any(Date.class))).thenReturn(row);

        assertThrows(BizException.class, () -> service.consume("13800000000", "000000", "LOGIN"));
        // 不计数的话，六位码用几千次请求就能穷举。
        verify(verifyCodeMapper).increaseAttempts(1L);
        verify(verifyCodeMapper, never()).markUsed(anyLong(), any(Date.class));
    }

    @Test
    void consumeFailsWhenNoUsableCodeExists() {
        when(verifyCodeMapper.selectLatestUsable(anyString(), anyString(), any(Date.class))).thenReturn(null);

        assertThrows(BizException.class, () -> service.consume("13800000000", "654321", "LOGIN"));
    }

    private MediaVerifyCode usableRow(String code) {
        MediaVerifyCode row = new MediaVerifyCode();
        row.setId(1L);
        row.setTarget("13800000000");
        row.setScene("LOGIN");
        row.setCodeHash(sha256(code));
        row.setAttempts(0);
        row.setUsed(false);
        row.setExpiresAt(new Date(System.currentTimeMillis() + 60_000));
        return row;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
