package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaSupportMessage;
import com.bgssai.media.common.domain.MediaSupportMessageExample;
import com.bgssai.media.common.domain.MediaSupportSession;
import com.bgssai.media.common.domain.MediaSupportSessionExample;
import com.bgssai.media.common.mapper.MediaSupportMessageMapper;
import com.bgssai.media.common.mapper.MediaSupportSessionMapper;
import com.bgssai.media.common.web.BizException;
import com.bgssai.media.common.web.PageResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 站内在线客服（留言 / 会话）。
 * 状态：open（进行中）/ pending（待客服处理）/ closed（已关闭）。
 */
@Service
public class SupportService {

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CLOSED = "closed";

    public static final String SENDER_USER = "user";
    public static final String SENDER_ADMIN = "admin";
    public static final String SENDER_SYSTEM = "system";

    private static final int MAX_CONTENT_LEN = 4000;
    private static final int CREATE_LIMIT_PER_MINUTE = 5;
    private static final int MESSAGE_LIMIT_PER_MINUTE = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MediaSupportSessionMapper sessionMapper;
    private final MediaSupportMessageMapper messageMapper;

    /** 简易进程内限流：key -> {windowStartMs, count} */
    private final ConcurrentHashMap<String, long[]> rateBuckets = new ConcurrentHashMap<>();

    public SupportService(MediaSupportSessionMapper sessionMapper,
                          MediaSupportMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional
    public Map<String, Object> createSession(Long userId,
                                             String contactName,
                                             String contactEmail,
                                             String contactPhone,
                                             String subject,
                                             String firstMessage,
                                             String clientKey) {
        guardRate("create:" + nullToEmpty(clientKey), CREATE_LIMIT_PER_MINUTE);

        String content = requireText(firstMessage, "留言内容不能为空");
        String subj = trimToNull(subject);
        if (subj == null) {
            subj = content.length() > 40 ? content.substring(0, 40) : content;
        }

        Date now = new Date();
        MediaSupportSession session = new MediaSupportSession();
        session.setSessionToken(newToken());
        session.setUserId(userId);
        session.setContactName(trimToNull(contactName));
        session.setContactEmail(trimToNull(contactEmail));
        session.setContactPhone(trimToNull(contactPhone));
        session.setSubject(subj);
        session.setStatus(STATUS_PENDING);
        session.setLastMessageAt(now);
        session.setUserUnread(0);
        session.setAdminUnread(1);
        sessionMapper.insertSelective(session);

        MediaSupportMessage msg = new MediaSupportMessage();
        msg.setSessionId(session.getId());
        msg.setSenderType(SENDER_USER);
        msg.setSenderUserId(userId);
        msg.setContent(content);
        messageMapper.insertSelective(msg);

        return detailForUser(session, true);
    }

    public List<MediaSupportSession> listMine(Long userId) {
        if (userId == null) {
            throw new BizException(401, "login required");
        }
        MediaSupportSessionExample example = new MediaSupportSessionExample();
        example.createCriteria().andUserIdEqualTo(userId);
        example.setOrderByClause("updated_at desc");
        return sessionMapper.selectByExample(example);
    }

    public Map<String, Object> getForUser(Long sessionId, String sessionToken, Long userId) {
        MediaSupportSession session = requireAccessible(sessionId, sessionToken, userId);
        return detailForUser(session, true);
    }

    @Transactional
    public Map<String, Object> sendUserMessage(Long sessionId,
                                               String sessionToken,
                                               Long userId,
                                               String content,
                                               String clientKey) {
        guardRate("msg:" + nullToEmpty(clientKey), MESSAGE_LIMIT_PER_MINUTE);
        MediaSupportSession session = requireAccessible(sessionId, sessionToken, userId);
        if (STATUS_CLOSED.equals(session.getStatus())) {
            throw new BizException(400, "会话已关闭，请新开会话");
        }
        String text = requireText(content, "消息不能为空");
        Date now = new Date();

        MediaSupportMessage msg = new MediaSupportMessage();
        msg.setSessionId(session.getId());
        msg.setSenderType(SENDER_USER);
        msg.setSenderUserId(userId);
        msg.setContent(text);
        messageMapper.insertSelective(msg);

        MediaSupportSession patch = new MediaSupportSession();
        patch.setId(session.getId());
        patch.setStatus(STATUS_PENDING);
        patch.setLastMessageAt(now);
        patch.setAdminUnread((session.getAdminUnread() == null ? 0 : session.getAdminUnread()) + 1);
        if (userId != null && session.getUserId() == null) {
            patch.setUserId(userId);
        }
        sessionMapper.updateByPrimaryKeySelective(patch);

        return detailForUser(sessionMapper.selectByPrimaryKey(session.getId()), true);
    }

    @Transactional
    public Map<String, Object> markUserRead(Long sessionId, String sessionToken, Long userId) {
        MediaSupportSession session = requireAccessible(sessionId, sessionToken, userId);
        MediaSupportSession patch = new MediaSupportSession();
        patch.setId(session.getId());
        patch.setUserUnread(0);
        sessionMapper.updateByPrimaryKeySelective(patch);
        return detailForUser(sessionMapper.selectByPrimaryKey(session.getId()), true);
    }

    public PageResult<MediaSupportSession> adminPage(String status, String keyword, int pageNum, int pageSize) {
        MediaSupportSessionExample example = new MediaSupportSessionExample();
        MediaSupportSessionExample.Criteria c = example.createCriteria();
        if (status != null && !status.isBlank()) {
            c.andStatusEqualTo(status.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            // Example AND 语义：主题或联系方式模糊匹配走多 criteria OR 较繁琐，这里用 subject like
            c.andSubjectLike(like);
        }
        example.setOrderByClause("updated_at desc");
        PageHelper.startPage(pageNum, pageSize);
        List<MediaSupportSession> list = sessionMapper.selectByExample(example);
        PageInfo<MediaSupportSession> info = new PageInfo<>(list);
        return new PageResult<>(info.getTotal(), pageNum, pageSize, list);
    }

    public Map<String, Object> adminDetail(Long sessionId) {
        MediaSupportSession session = requireSession(sessionId);
        return detailForAdmin(session, true);
    }

    @Transactional
    public Map<String, Object> adminReply(Long sessionId, Long adminUserId, String content) {
        MediaSupportSession session = requireSession(sessionId);
        if (STATUS_CLOSED.equals(session.getStatus())) {
            throw new BizException(400, "会话已关闭");
        }
        String text = requireText(content, "回复不能为空");
        Date now = new Date();

        MediaSupportMessage msg = new MediaSupportMessage();
        msg.setSessionId(session.getId());
        msg.setSenderType(SENDER_ADMIN);
        msg.setSenderUserId(adminUserId);
        msg.setContent(text);
        messageMapper.insertSelective(msg);

        MediaSupportSession patch = new MediaSupportSession();
        patch.setId(session.getId());
        patch.setStatus(STATUS_OPEN);
        patch.setLastMessageAt(now);
        patch.setUserUnread((session.getUserUnread() == null ? 0 : session.getUserUnread()) + 1);
        patch.setAdminUnread(0);
        sessionMapper.updateByPrimaryKeySelective(patch);

        return detailForAdmin(sessionMapper.selectByPrimaryKey(session.getId()), true);
    }

    @Transactional
    public Map<String, Object> adminClose(Long sessionId, Long adminUserId) {
        MediaSupportSession session = requireSession(sessionId);
        if (STATUS_CLOSED.equals(session.getStatus())) {
            return detailForAdmin(session, true);
        }
        Date now = new Date();
        MediaSupportMessage msg = new MediaSupportMessage();
        msg.setSessionId(session.getId());
        msg.setSenderType(SENDER_SYSTEM);
        msg.setSenderUserId(adminUserId);
        msg.setContent("客服已关闭本会话");
        messageMapper.insertSelective(msg);

        MediaSupportSession patch = new MediaSupportSession();
        patch.setId(session.getId());
        patch.setStatus(STATUS_CLOSED);
        patch.setLastMessageAt(now);
        patch.setAdminUnread(0);
        patch.setUserUnread((session.getUserUnread() == null ? 0 : session.getUserUnread()) + 1);
        sessionMapper.updateByPrimaryKeySelective(patch);
        return detailForAdmin(sessionMapper.selectByPrimaryKey(session.getId()), true);
    }

    @Transactional
    public Map<String, Object> adminMarkRead(Long sessionId) {
        MediaSupportSession session = requireSession(sessionId);
        MediaSupportSession patch = new MediaSupportSession();
        patch.setId(session.getId());
        patch.setAdminUnread(0);
        sessionMapper.updateByPrimaryKeySelective(patch);
        return detailForAdmin(sessionMapper.selectByPrimaryKey(session.getId()), true);
    }

    private Map<String, Object> detailForUser(MediaSupportSession session, boolean withMessages) {
        Map<String, Object> result = new HashMap<>();
        result.put("session", session);
        if (withMessages) {
            result.put("messages", listMessages(session.getId()));
        }
        return result;
    }

    private Map<String, Object> detailForAdmin(MediaSupportSession session, boolean withMessages) {
        return detailForUser(session, withMessages);
    }

    private List<MediaSupportMessage> listMessages(Long sessionId) {
        MediaSupportMessageExample example = new MediaSupportMessageExample();
        example.createCriteria().andSessionIdEqualTo(sessionId);
        example.setOrderByClause("id asc");
        return messageMapper.selectByExample(example);
    }

    private MediaSupportSession requireSession(Long sessionId) {
        if (sessionId == null) {
            throw new BizException(400, "session_id required");
        }
        MediaSupportSession session = sessionMapper.selectByPrimaryKey(sessionId);
        if (session == null) {
            throw new BizException(404, "session not found");
        }
        return session;
    }

    private MediaSupportSession requireAccessible(Long sessionId, String sessionToken, Long userId) {
        MediaSupportSession session;
        if (sessionId != null) {
            session = requireSession(sessionId);
        } else if (sessionToken != null && !sessionToken.isBlank()) {
            MediaSupportSessionExample example = new MediaSupportSessionExample();
            example.createCriteria().andSessionTokenEqualTo(sessionToken.trim());
            List<MediaSupportSession> list = sessionMapper.selectByExample(example);
            if (list.isEmpty()) {
                throw new BizException(404, "session not found");
            }
            session = list.get(0);
        } else {
            throw new BizException(400, "session_id or session_token required");
        }

        boolean byToken = sessionToken != null
                && !sessionToken.isBlank()
                && sessionToken.trim().equals(session.getSessionToken());
        boolean byOwner = userId != null && userId.equals(session.getUserId());
        if (!byToken && !byOwner) {
            throw new BizException(403, "forbidden");
        }
        return session;
    }

    private String requireText(String raw, String emptyMsg) {
        if (raw == null || raw.isBlank()) {
            throw new BizException(400, emptyMsg);
        }
        String text = raw.trim();
        if (text.length() > MAX_CONTENT_LEN) {
            throw new BizException(400, "消息过长");
        }
        return text;
    }

    private void guardRate(String key, int limitPerMinute) {
        long now = System.currentTimeMillis();
        long window = 60_000L;
        long[] bucket = rateBuckets.compute(key, (k, prev) -> {
            if (prev == null || now - prev[0] >= window) {
                return new long[]{now, 0};
            }
            return prev;
        });
        synchronized (bucket) {
            if (now - bucket[0] >= window) {
                bucket[0] = now;
                bucket[1] = 0;
            }
            if (bucket[1] >= limitPerMinute) {
                throw new BizException(429, "操作过于频繁，请稍后再试");
            }
            bucket[1] = bucket[1] + 1;
        }
    }

    private static String newToken() {
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        StringBuilder sb = new StringBuilder(buf.length * 2);
        for (byte b : buf) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** 测试辅助：清空限流桶。 */
    void clearRateBucketsForTest() {
        rateBuckets.clear();
    }

    /** 测试辅助：当前桶数量。 */
    int rateBucketSizeForTest() {
        return rateBuckets.size();
    }
}
