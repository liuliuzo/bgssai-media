package com.bgssai.media.common.sms;

import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmsTemplateParamsTest {

    @Test
    void count2IsCodeAndMinutes() {
        assertArrayEquals(
                new String[] { "123456", "5" },
                SmsTemplateParams.build(2, "Magic", 7L, "123456", 5));
    }

    @Test
    void count3JoinsProductAndSeq() {
        assertArrayEquals(
                new String[] { "Magic#7", "123456", "5" },
                SmsTemplateParams.build(3, "Magic", 7L, "123456", 5));
    }

    @Test
    void count4SplitsProductAndSeq() {
        assertArrayEquals(
                new String[] { "Magic", "7", "123456", "5" },
                SmsTemplateParams.build(4, "Magic", 7L, "123456", 5));
    }

    @Test
    void invalidCountRejected() {
        assertThrows(BizException.class,
                () -> SmsTemplateParams.build(5, "Magic", 1L, "123456", 5));
    }
}
