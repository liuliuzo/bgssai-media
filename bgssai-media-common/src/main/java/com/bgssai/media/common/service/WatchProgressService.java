package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaWatchProgress;
import com.bgssai.media.common.domain.MediaWatchProgressExample;
import com.bgssai.media.common.mapper.MediaWatchProgressMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WatchProgressService {

    private final MediaWatchProgressMapper mapper;

    public WatchProgressService(MediaWatchProgressMapper mapper) {
        this.mapper = mapper;
    }

    public List<MediaWatchProgress> listByUser(Long userId) {
        MediaWatchProgressExample example = new MediaWatchProgressExample();
        example.createCriteria().andUserIdEqualTo(userId);
        example.setOrderByClause("updated_at desc");
        return mapper.selectByExample(example);
    }

    public MediaWatchProgress save(Long userId, Long dramaId, Long episodeId, Integer positionSec) {
        MediaWatchProgressExample example = new MediaWatchProgressExample();
        example.createCriteria().andUserIdEqualTo(userId).andDramaIdEqualTo(dramaId);
        List<MediaWatchProgress> list = mapper.selectByExample(example);
        if (list.isEmpty()) {
            MediaWatchProgress row = new MediaWatchProgress();
            row.setUserId(userId);
            row.setDramaId(dramaId);
            row.setEpisodeId(episodeId);
            row.setPositionSec(positionSec == null ? 0 : positionSec);
            mapper.insertSelective(row);
            return row;
        }
        MediaWatchProgress row = list.get(0);
        row.setEpisodeId(episodeId);
        row.setPositionSec(positionSec == null ? 0 : positionSec);
        mapper.updateByPrimaryKeySelective(row);
        return row;
    }
}
