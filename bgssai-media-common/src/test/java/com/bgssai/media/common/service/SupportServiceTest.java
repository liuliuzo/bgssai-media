package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaSupportMessage;
import com.bgssai.media.common.domain.MediaSupportSession;
import com.bgssai.media.common.mapper.MediaSupportMessageMapper;
import com.bgssai.media.common.mapper.MediaSupportSessionMapper;
import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupportServiceTest {

    @Mock MediaSupportSessionMapper sessionMapper;
    @Mock MediaSupportMessageMapper messageMapper;

    SupportService service;
    final AtomicLong sessionIds = new AtomicLong(1);
    final AtomicLong messageIds = new AtomicLong(1);
    final List<MediaSupportSession> sessions = new ArrayList<>();
    final List<MediaSupportMessage> messages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        sessions.clear();
        messages.clear();
        service = new SupportService(sessionMapper, messageMapper);
        service.clearRateBucketsForTest();

        when(sessionMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaSupportSession row = inv.getArgument(0);
            row.setId(sessionIds.getAndIncrement());
            sessions.add(row);
            return 1;
        });
        when(sessionMapper.selectByPrimaryKey(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return sessions.stream().filter(s -> id.equals(s.getId())).findFirst().orElse(null);
        });
        when(sessionMapper.updateByPrimaryKeySelective(any())).thenAnswer(inv -> {
            MediaSupportSession patch = inv.getArgument(0);
            MediaSupportSession existing = sessions.stream()
                    .filter(s -> patch.getId().equals(s.getId())).findFirst().orElse(null);
            if (existing == null) {
                return 0;
            }
            if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
            if (patch.getLastMessageAt() != null) existing.setLastMessageAt(patch.getLastMessageAt());
            if (patch.getUserUnread() != null) existing.setUserUnread(patch.getUserUnread());
            if (patch.getAdminUnread() != null) existing.setAdminUnread(patch.getAdminUnread());
            if (patch.getUserId() != null) existing.setUserId(patch.getUserId());
            return 1;
        });
        when(sessionMapper.selectByExample(any())).thenAnswer(inv -> {
            // token lookup: return matching sessions
            return new ArrayList<>(sessions);
        });
        when(messageMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaSupportMessage row = inv.getArgument(0);
            row.setId(messageIds.getAndIncrement());
            messages.add(row);
            return 1;
        });
        when(messageMapper.selectByExample(any())).thenAnswer(inv -> new ArrayList<>(messages));
    }

    @Test
    void createSessionStoresPendingAndFirstMessage() {
        Map<String, Object> result = service.createSession(
                9L, "Alice", "a@example.com", "13800000000",
                null, "播放失败怎么办", "ip:1.1.1.1");

        MediaSupportSession session = (MediaSupportSession) result.get("session");
        assertNotNull(session.getId());
        assertNotNull(session.getSessionToken());
        assertEquals(SupportService.STATUS_PENDING, session.getStatus());
        assertEquals(1, session.getAdminUnread());
        assertEquals(1, messages.size());
        assertEquals(SupportService.SENDER_USER, messages.get(0).getSenderType());
        assertEquals("播放失败怎么办", messages.get(0).getContent());
    }

    @Test
    void adminReplyIncrementsUserUnreadAndOpensSession() {
        service.createSession(9L, null, null, null, null, "hello", "ip:2.2.2.2");
        MediaSupportSession created = sessions.get(0);

        Map<String, Object> detail = service.adminReply(created.getId(), 1L, "已收到，请说明机型");
        MediaSupportSession session = (MediaSupportSession) detail.get("session");
        assertEquals(SupportService.STATUS_OPEN, session.getStatus());
        assertEquals(1, session.getUserUnread());
        assertEquals(0, session.getAdminUnread());
        assertEquals(2, messages.size());
        assertEquals(SupportService.SENDER_ADMIN, messages.get(1).getSenderType());
    }

    @Test
    void closedSessionRejectsUserMessage() {
        service.createSession(9L, null, null, null, null, "hello", "ip:3.3.3.3");
        MediaSupportSession created = sessions.get(0);
        service.adminClose(created.getId(), 1L);

        BizException ex = assertThrows(BizException.class, () ->
                service.sendUserMessage(created.getId(), created.getSessionToken(), 9L, "再问一句", "ip:3.3.3.3"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createRateLimited() {
        for (int i = 0; i < 5; i++) {
            service.createSession(null, null, null, null, null, "msg-" + i, "ip:rate");
        }
        BizException ex = assertThrows(BizException.class, () ->
                service.createSession(null, null, null, null, null, "msg-overflow", "ip:rate"));
        assertEquals(429, ex.getCode());
    }
}
