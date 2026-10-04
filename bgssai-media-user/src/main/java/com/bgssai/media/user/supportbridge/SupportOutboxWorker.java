package com.bgssai.media.user.supportbridge;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** 定时把客服队列推到统一客服。未配置时 flush 立即返回。不打开全局调度。 */
@Component
public class SupportOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(SupportOutboxWorker.class);
    private final SupportOutboxService outboxService;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "support-outbox");
        thread.setDaemon(true);
        return thread;
    });

    public SupportOutboxWorker(SupportOutboxService outboxService) {
        this.outboxService = outboxService;
        executor.scheduleWithFixedDelay(this::flush, 5, 5, TimeUnit.SECONDS);
    }

    void flush() {
        try {
            outboxService.flush();
        } catch (RuntimeException ex) {
            log.warn("统一客服投递循环失败 error={}", ex.getClass().getSimpleName());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
