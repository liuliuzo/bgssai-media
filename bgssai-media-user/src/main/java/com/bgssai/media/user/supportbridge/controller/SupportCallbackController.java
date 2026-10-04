package com.bgssai.media.user.supportbridge.controller;

import com.bgssai.media.user.supportbridge.SupportCallbackReceiver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
public class SupportCallbackController {

    private final SupportCallbackReceiver receiver;

    public SupportCallbackController(SupportCallbackReceiver receiver) {
        this.receiver = receiver;
    }

    @PostMapping("/api/cs/support/callback")
    public ResponseEntity<Map<String, Object>> handle(HttpServletRequest request) throws IOException {
        return receiver.receive(request.getInputStream().readNBytes(65537),
                request.getHeader("X-Support-Signature"), request.getHeader("Idempotency-Key"));
    }
}
