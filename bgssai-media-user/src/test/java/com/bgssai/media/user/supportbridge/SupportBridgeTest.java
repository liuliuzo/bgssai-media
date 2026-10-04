package com.bgssai.media.user.supportbridge;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportBridgeTest {

    @Test
    void unconfigured_bridge_does_not_call_out_or_touch_storage() {
        CsSupportOutboxStore mapper = new CsSupportOutboxStore() {
            @Override
            public int insert(CsSupportOutbox row) {
                throw new AssertionError("未配置不应写队列");
            }

            @Override
            public List<CsSupportOutbox> selectByExample(CsSupportOutboxExample example) {
                throw new AssertionError("未配置不应查询队列");
            }

            @Override
            public int updateByExampleSelective(CsSupportOutbox record, CsSupportOutboxExample example) {
                throw new AssertionError("未配置不应更新队列");
            }
        };
        SupportOutboxService service = new SupportOutboxService(new SupportBridgeProperties(), mapper);
        service.enqueueSession("7", "张三", "在线客服", "hello", "11");
        service.enqueueMessage("7", "11", "hello");
        service.enqueueTicket("7", "3", "工单");
        service.flush();
    }

    @Test
    void bad_signature_is_rejected_before_applying() {
        SupportBridgeProperties properties = ready("test-secret");
        AtomicInteger applied = new AtomicInteger();
        SupportCallbackService callback = new SupportCallbackService(null, null) {
            @Override
            public void apply(SupportCallbackEvent event) {
                applied.incrementAndGet();
            }
        };
        SupportCallbackReceiver receiver = new SupportCallbackReceiver(properties, callback);
        byte[] body = """
                {"productCode":"media","event":"agent.reply","deliveryId":"d1","externalSessionId":"7","message":{"body":"hello"}}
                """.getBytes(StandardCharsets.UTF_8);
        assertEquals(401, receiver.receive(body, "sha256=bad", "d1").getStatusCode().value());
        assertEquals(503, new SupportCallbackReceiver(new SupportBridgeProperties(), callback)
                .receive(body, "sha256=" + SupportSignatures.sign("test-secret", body), "d1").getStatusCode().value());
        assertEquals(0, applied.get());
    }

    @Test
    void configured_flush_signs_the_request_and_keeps_retryable_failures() throws Exception {
        List<String> paths = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] raw = exchange.getRequestBody().readAllBytes();
            boolean signed = SupportSignatures.matches("test-secret", raw,
                    exchange.getRequestHeaders().getFirst("X-Support-Signature"));
            paths.add(exchange.getRequestURI().getPath() + (signed ? "" : "!unsigned"));
            int status = paths.size() == 1 ? 503 : 200;
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        try {
            SupportBridgeProperties properties = ready("test-secret");
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            List<CsSupportOutbox> due = new ArrayList<>();
            due.add(pendingRow());
            List<CsSupportOutbox> updates = new ArrayList<>();
            CsSupportOutboxStore mapper = new CsSupportOutboxStore() {
                @Override
                public int insert(CsSupportOutbox row) {
                    return 1;
                }

                @Override
                public List<CsSupportOutbox> selectByExample(CsSupportOutboxExample example) {
                    return List.copyOf(due);
                }

                @Override
                public int updateByExampleSelective(CsSupportOutbox record, CsSupportOutboxExample example) {
                    updates.add(record);
                    due.clear();
                    return 1;
                }
            };
            SupportOutboxService service = new SupportOutboxService(properties, mapper);
            service.flush();
            assertEquals("PENDING", updates.get(0).getStatus());
            assertTrue(updates.get(0).getNextAttemptAt() != null);
            due.add(pendingRow());
            service.flush();
            assertEquals("SENT", updates.get(1).getStatus());
            assertTrue(updates.get(1).getSentAt() != null);
            assertEquals(List.of(
                    "/api/ingest/media/sessions/7/messages",
                    "/api/ingest/media/sessions/7/messages"), paths);
        } finally {
            server.stop(0);
        }
        assertFalse(paths.get(0).contains("unsigned"));
    }

    private static CsSupportOutbox pendingRow() {
        CsSupportOutbox row = new CsSupportOutbox();
        row.setId(1L);
        row.setAttemptCount(0);
        row.setRequestPath("/sessions/7/messages");
        row.setPayloadJson("{\"externalId\":\"11\",\"visitorId\":\"7\",\"body\":\"hello\"}");
        return row;
    }

    private static SupportBridgeProperties ready(String secret) {
        SupportBridgeProperties properties = new SupportBridgeProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://127.0.0.1:9");
        properties.setProductCode("media");
        properties.setIngestSecret(secret);
        return properties;
    }
}
