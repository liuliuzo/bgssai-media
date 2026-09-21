package com.bgssai.media.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class McpTokenPathAliasTest {

    @Test
    void controller_registers_canonical_and_legacy_prefix() {
        RequestMapping mapping = McpTokenController.class.getAnnotation(RequestMapping.class);
        List<String> paths = Arrays.asList(mapping.value());
        assertTrue(paths.contains("/api/mcp/tokens"));
        assertTrue(paths.contains("/api/mcp-tokens"));
    }
}
