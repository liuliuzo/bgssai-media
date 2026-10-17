package com.bgssai.media.common.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Annotation mappers for OPC 2026-10-17 additive tables.
 * Keeps new surfaces cohesive without regenerating every Example XML.
 */
@Mapper
public interface OpcExtMapper {

    // ---- identity bind/unbind ----
    @Select("SELECT id, user_id AS userId, provider, open_id AS openId, nickname, created_at AS createdAt "
            + "FROM user_identity WHERE user_id = #{userId} ORDER BY id ASC")
    List<Map<String, Object>> listIdentities(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_identity WHERE user_id = #{userId}")
    int countIdentities(@Param("userId") Long userId);

    @Delete("DELETE FROM user_identity WHERE user_id = #{userId} AND provider = #{provider}")
    int deleteIdentity(@Param("userId") Long userId, @Param("provider") String provider);

    @Select("SELECT id FROM user_identity WHERE provider = #{provider} AND open_id = #{openId} LIMIT 1")
    Long findIdentityId(@Param("provider") String provider, @Param("openId") String openId);

    // ---- account deletion ----
    @Update("UPDATE sys_user SET status = 'deleted', deleted_at = #{deletedAt}, "
            + "email = NULL, phone = NULL, password_hash = #{passwordHash}, "
            + "username = #{username}, current_session_id = NULL WHERE id = #{id} AND status = 'active'")
    int softDeleteUser(@Param("id") Long id,
                       @Param("username") String username,
                       @Param("passwordHash") String passwordHash,
                       @Param("deletedAt") Date deletedAt);

    @Delete("DELETE FROM user_identity WHERE user_id = #{userId}")
    int deleteAllIdentities(@Param("userId") Long userId);

    @Update("UPDATE sys_user SET email = NULL WHERE id = #{id}")
    int clearEmail(@Param("id") Long id);

    @Update("UPDATE sys_user SET phone = NULL WHERE id = #{id}")
    int clearPhone(@Param("id") Long id);

    // ---- channel extras ----
    @Update("UPDATE media_user_channel SET avatar_url = #{avatarUrl}, display_name = COALESCE(#{displayName}, display_name), "
            + "bio = COALESCE(#{bio}, bio), handle = COALESCE(#{handle}, handle) WHERE id = #{id}")
    int updateChannelProfile(@Param("id") Long id,
                             @Param("avatarUrl") String avatarUrl,
                             @Param("displayName") String displayName,
                             @Param("bio") String bio,
                             @Param("handle") String handle);

    @Select("SELECT status FROM media_user_channel WHERE id = #{id}")
    String channelStatus(@Param("id") Long id);

    @Update("UPDATE media_user_channel SET status = #{status} WHERE id = #{id}")
    int setChannelStatus(@Param("id") Long id, @Param("status") String status);

    // ---- subscription feed ----
    @Select("SELECT v.id, v.user_id AS userId, v.channel_id AS channelId, v.title, v.description, v.tags, "
            + "v.cover_url AS coverUrl, v.like_count AS likeCount, v.comment_count AS commentCount, "
            + "v.created_at AS createdAt, c.handle AS channelHandle, c.display_name AS channelName, "
            + "c.avatar_url AS channelAvatar "
            + "FROM media_creator_video v "
            + "INNER JOIN media_channel_follow f ON f.channel_id = v.channel_id AND f.follower_user_id = #{userId} "
            + "INNER JOIN media_user_channel c ON c.id = v.channel_id "
            + "WHERE v.status = 'READY' AND v.moderation_status = 'approved' AND v.visibility = 'public' "
            + "AND c.status = 'active' "
            + "ORDER BY v.created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> subscriptionFeed(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT id, user_id AS userId, channel_id AS channelId, title, description, tags, cover_url AS coverUrl, "
            + "like_count AS likeCount, comment_count AS commentCount, status, moderation_status AS moderationStatus, "
            + "visibility, created_at AS createdAt "
            + "FROM media_creator_video WHERE channel_id = #{channelId} AND status = 'READY' "
            + "AND moderation_status = 'approved' AND visibility = 'public' ORDER BY created_at DESC")
    List<Map<String, Object>> channelPublicVideos(@Param("channelId") Long channelId);

    // ---- search ----
    @Select("SELECT v.id, v.channel_id AS channelId, v.title, v.tags, v.cover_url AS coverUrl, "
            + "c.handle AS channelHandle, c.display_name AS channelName "
            + "FROM media_creator_video v "
            + "INNER JOIN media_user_channel c ON c.id = v.channel_id "
            + "WHERE v.status = 'READY' AND v.moderation_status = 'approved' AND v.visibility = 'public' "
            + "AND c.status = 'active' "
            + "AND (v.title LIKE CONCAT('%', #{q}, '%') OR IFNULL(v.tags,'') LIKE CONCAT('%', #{q}, '%') "
            + "OR c.handle LIKE CONCAT('%', #{q}, '%') OR c.display_name LIKE CONCAT('%', #{q}, '%')) "
            + "ORDER BY v.created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> searchVideos(@Param("q") String q, @Param("limit") int limit);

    @Select("SELECT id, handle, display_name AS displayName, bio, avatar_url AS avatarUrl, status "
            + "FROM media_user_channel WHERE status = 'active' "
            + "AND (handle LIKE CONCAT('%', #{q}, '%') OR display_name LIKE CONCAT('%', #{q}, '%')) "
            + "ORDER BY id DESC LIMIT #{limit}")
    List<Map<String, Object>> searchChannels(@Param("q") String q, @Param("limit") int limit);

    // ---- not interested ----
    @Insert("INSERT IGNORE INTO media_not_interested(user_id, video_id, reason, created_at) "
            + "VALUES(#{userId}, #{videoId}, #{reason}, NOW())")
    int insertNotInterested(@Param("userId") Long userId,
                            @Param("videoId") Long videoId,
                            @Param("reason") String reason);

    @Select("SELECT video_id FROM media_not_interested WHERE user_id = #{userId}")
    List<Long> listNotInterestedVideoIds(@Param("userId") Long userId);

    // ---- watch history / later / playlist ----
    @Select("SELECT pause_watch_history FROM media_user_pref WHERE user_id = #{userId}")
    Integer getPauseWatchHistory(@Param("userId") Long userId);

    @Insert("INSERT INTO media_user_pref(user_id, pause_watch_history) VALUES(#{userId}, #{pause}) "
            + "ON DUPLICATE KEY UPDATE pause_watch_history = #{pause}")
    int upsertPauseWatchHistory(@Param("userId") Long userId, @Param("pause") int pause);

    @Insert("INSERT INTO media_watch_history(user_id, video_id, content_id, title, watched_sec, playable, last_watched_at) "
            + "VALUES(#{userId}, #{videoId}, #{contentId}, #{title}, #{watchedSec}, #{playable}, NOW()) "
            + "ON DUPLICATE KEY UPDATE watched_sec = GREATEST(watched_sec, VALUES(watched_sec)), "
            + "title = VALUES(title), playable = VALUES(playable), last_watched_at = NOW()")
    int upsertWatchHistory(@Param("userId") Long userId,
                           @Param("videoId") Long videoId,
                           @Param("contentId") String contentId,
                           @Param("title") String title,
                           @Param("watchedSec") int watchedSec,
                           @Param("playable") int playable);

    @Select("SELECT id, video_id AS videoId, content_id AS contentId, title, watched_sec AS watchedSec, "
            + "playable, last_watched_at AS lastWatchedAt FROM media_watch_history "
            + "WHERE user_id = #{userId} ORDER BY last_watched_at DESC LIMIT #{limit}")
    List<Map<String, Object>> listWatchHistory(@Param("userId") Long userId, @Param("limit") int limit);

    @Delete("DELETE FROM media_watch_history WHERE user_id = #{userId} AND video_id = #{videoId}")
    int deleteWatchHistoryItem(@Param("userId") Long userId, @Param("videoId") Long videoId);

    @Insert("INSERT IGNORE INTO media_watch_later(user_id, video_id, sort_order, created_at) "
            + "VALUES(#{userId}, #{videoId}, #{sortOrder}, NOW())")
    int insertWatchLater(@Param("userId") Long userId,
                         @Param("videoId") Long videoId,
                         @Param("sortOrder") int sortOrder);

    @Delete("DELETE FROM media_watch_later WHERE user_id = #{userId} AND video_id = #{videoId}")
    int deleteWatchLater(@Param("userId") Long userId, @Param("videoId") Long videoId);

    @Select("SELECT id, video_id AS videoId, sort_order AS sortOrder, created_at AS createdAt "
            + "FROM media_watch_later WHERE user_id = #{userId} ORDER BY sort_order ASC, id ASC")
    List<Map<String, Object>> listWatchLater(@Param("userId") Long userId);

    @Insert("INSERT INTO media_playlist(user_id, title, visibility, created_at) "
            + "VALUES(#{userId}, #{title}, #{visibility}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPlaylist(Map<String, Object> row);

    @Select("SELECT id, user_id AS userId, title, visibility, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM media_playlist WHERE id = #{id}")
    Map<String, Object> getPlaylist(@Param("id") Long id);

    @Select("SELECT id, user_id AS userId, title, visibility, created_at AS createdAt "
            + "FROM media_playlist WHERE user_id = #{userId} ORDER BY id DESC")
    List<Map<String, Object>> listPlaylists(@Param("userId") Long userId);

    @Insert("INSERT IGNORE INTO media_playlist_item(playlist_id, video_id, sort_order, created_at) "
            + "VALUES(#{playlistId}, #{videoId}, #{sortOrder}, NOW())")
    int insertPlaylistItem(@Param("playlistId") Long playlistId,
                           @Param("videoId") Long videoId,
                           @Param("sortOrder") int sortOrder);

    @Delete("DELETE FROM media_playlist_item WHERE playlist_id = #{playlistId} AND video_id = #{videoId}")
    int deletePlaylistItem(@Param("playlistId") Long playlistId, @Param("videoId") Long videoId);

    @Select("SELECT video_id AS videoId, sort_order AS sortOrder FROM media_playlist_item "
            + "WHERE playlist_id = #{playlistId} ORDER BY sort_order ASC, id ASC")
    List<Map<String, Object>> listPlaylistItems(@Param("playlistId") Long playlistId);

    // ---- analytics ----
    @Insert("INSERT INTO media_view_event(user_id, video_id, channel_id, watched_sec, completed, event_day, created_at) "
            + "VALUES(#{userId}, #{videoId}, #{channelId}, #{watchedSec}, #{completed}, #{eventDay}, NOW())")
    int insertViewEvent(@Param("userId") Long userId,
                        @Param("videoId") Long videoId,
                        @Param("channelId") Long channelId,
                        @Param("watchedSec") int watchedSec,
                        @Param("completed") int completed,
                        @Param("eventDay") java.sql.Date eventDay);

    @Insert("INSERT INTO media_subscribe_event(channel_id, follower_user_id, action, event_day, created_at) "
            + "VALUES(#{channelId}, #{followerUserId}, #{action}, #{eventDay}, NOW())")
    int insertSubscribeEvent(@Param("channelId") Long channelId,
                             @Param("followerUserId") Long followerUserId,
                             @Param("action") String action,
                             @Param("eventDay") java.sql.Date eventDay);

    @Select("SELECT event_day AS eventDay, "
            + "COUNT(*) AS play_count, "
            + "COALESCE(SUM(watched_sec),0) AS watched_sec_sum, "
            + "COALESCE(SUM(completed),0) AS completed_count "
            + "FROM media_view_event WHERE video_id = #{videoId} "
            + "AND event_day >= #{fromDay} AND event_day <= #{toDay} "
            + "GROUP BY event_day ORDER BY event_day ASC")
    List<Map<String, Object>> videoDailyStats(@Param("videoId") Long videoId,
                                              @Param("fromDay") java.sql.Date fromDay,
                                              @Param("toDay") java.sql.Date toDay);

    @Select("SELECT event_day AS eventDay, "
            + "SUM(CASE WHEN action='follow' THEN 1 ELSE 0 END) AS follow_count, "
            + "SUM(CASE WHEN action='unfollow' THEN 1 ELSE 0 END) AS unfollow_count "
            + "FROM media_subscribe_event WHERE channel_id = #{channelId} "
            + "AND event_day >= #{fromDay} AND event_day <= #{toDay} "
            + "GROUP BY event_day ORDER BY event_day ASC")
    List<Map<String, Object>> channelSubscribeDaily(@Param("channelId") Long channelId,
                                                    @Param("fromDay") java.sql.Date fromDay,
                                                    @Param("toDay") java.sql.Date toDay);

    // ---- reports / notices ----
    @Insert("INSERT INTO media_content_report(reporter_user_id, target_type, target_id, reason, status, merge_key, created_at) "
            + "VALUES(#{reporterUserId}, #{targetType}, #{targetId}, #{reason}, 'open', #{mergeKey}, NOW()) "
            + "ON DUPLICATE KEY UPDATE reason = IF(status='open', VALUES(reason), reason)")
    int upsertReport(@Param("reporterUserId") Long reporterUserId,
                     @Param("targetType") String targetType,
                     @Param("targetId") Long targetId,
                     @Param("reason") String reason,
                     @Param("mergeKey") String mergeKey);

    @Select("SELECT id, reporter_user_id AS reporterUserId, target_type AS targetType, target_id AS targetId, "
            + "reason, status, handler_note AS handlerNote, handled_at AS handledAt, notify_sent AS notifySent "
            + "FROM media_content_report WHERE id = #{id}")
    Map<String, Object> getReport(@Param("id") Long id);

    @Select("SELECT id, reporter_user_id AS reporterUserId, target_type AS targetType, target_id AS targetId, "
            + "reason, status, created_at AS createdAt FROM media_content_report "
            + "WHERE status = 'open' ORDER BY id ASC LIMIT #{limit}")
    List<Map<String, Object>> listOpenReports(@Param("limit") int limit);

    @Update("UPDATE media_content_report SET status = #{status}, handler_note = #{note}, handled_at = NOW(), "
            + "notify_sent = #{notifySent} WHERE id = #{id} AND status = 'open'")
    int resolveReport(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("note") String note,
                      @Param("notifySent") int notifySent);

    @Insert("INSERT INTO media_user_notice(user_id, kind, title, body, ref_type, ref_id, created_at) "
            + "VALUES(#{userId}, #{kind}, #{title}, #{body}, #{refType}, #{refId}, NOW())")
    int insertNotice(@Param("userId") Long userId,
                     @Param("kind") String kind,
                     @Param("title") String title,
                     @Param("body") String body,
                     @Param("refType") String refType,
                     @Param("refId") Long refId);

    @Select("SELECT id, kind, title, body, ref_type AS refType, ref_id AS refId, read_flag AS readFlag, "
            + "created_at AS createdAt FROM media_user_notice WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{limit}")
    List<Map<String, Object>> listNotices(@Param("userId") Long userId, @Param("limit") int limit);

    // ---- comment pin / soft delete helpers ----
    @Update("UPDATE media_video_comment SET pinned = #{pinned} WHERE id = #{id}")
    int setCommentPinned(@Param("id") Long id, @Param("pinned") int pinned);

    @Update("UPDATE media_video_comment SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int softDeleteComment(@Param("id") Long id);

    @Update("UPDATE media_creator_video SET moderation_status = #{status}, moderated_at = NOW() WHERE id = #{id}")
    int setModeration(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE media_creator_video SET tags = #{tags} WHERE id = #{id}")
    int setVideoTags(@Param("id") Long id, @Param("tags") String tags);

    @Update("UPDATE media_creator_video SET visibility = #{visibility} WHERE id = #{id}")
    int setVideoVisibility(@Param("id") Long id, @Param("visibility") String visibility);
}
