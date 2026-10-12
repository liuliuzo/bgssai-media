package com.bgssai.media.user.controller;

import com.bgssai.media.common.format.FormatMatrix;
import com.bgssai.media.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public format-matrix table. Hosts stay empty until an inventory allocates them. */
@RestController
public class MediaFormatsController {

    @GetMapping("/bgssai/user/media/formats")
    public ApiResponse<Map<String, Object>> formats() {
        return ApiResponse.ok(FormatMatrix.document());
    }
}
