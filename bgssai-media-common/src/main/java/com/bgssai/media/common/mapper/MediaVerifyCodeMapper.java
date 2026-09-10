package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaVerifyCode;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface MediaVerifyCodeMapper {
    int insertSelective(MediaVerifyCode row);

    /** 取该 target+scene 最近一条未消费、未过期的码。 */
    MediaVerifyCode selectLatestUsable(@Param("target") String target,
                                       @Param("scene") String scene,
                                       @Param("now") Date now);

    /**
     * 条件更新兼作并发闸门：两个请求拿同一个码时只有一个能把 used 从 0 改成 1，
     * 另一个拿到 0 行受影响，按「验证码不对」处理。
     */
    int markUsed(@Param("id") Long id, @Param("now") Date now);

    /** 校验失败计数；到达上限后该码在 selectLatestUsable 里被排除。 */
    int increaseAttempts(@Param("id") Long id);

    /** 指定时间窗内该 target 已发码条数，用于重发间隔与每日上限。 */
    int countSentSince(@Param("target") String target, @Param("since") Date since);
}
