package com.bgssai.media.common.sms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmsOtpPropertiesTest {

    @Test
    void defaultsToMagicAndCount3() {
        SmsOtpProperties props = new SmsOtpProperties("Magic", 3);
        assertEquals("Magic", props.getProductLabel());
        assertEquals(3, props.getTemplateParamCount());
    }

    @Test
    void blankProductFallsBackToMagic() {
        assertEquals("Magic", new SmsOtpProperties("  ", 2).getProductLabel());
    }

    @Test
    void rejectsInvalidParamCount() {
        assertThrows(IllegalArgumentException.class, () -> new SmsOtpProperties("Magic", 1));
    }
}
