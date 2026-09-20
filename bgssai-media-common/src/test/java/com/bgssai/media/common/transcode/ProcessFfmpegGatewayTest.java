package com.bgssai.media.common.transcode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ProcessFfmpegGatewayTest {

    @Test
    void missingBinaryIsUnavailable() {
        ProcessFfmpegGateway gateway = new ProcessFfmpegGateway("ffmpeg-not-installed-aud20-020");
        assertFalse(gateway.available());
    }
}
