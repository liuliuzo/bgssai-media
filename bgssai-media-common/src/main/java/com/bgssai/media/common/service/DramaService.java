package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaDramaExample;
import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.domain.MediaEpisodeExample;
import com.bgssai.media.common.mapper.MediaDramaMapper;
import com.bgssai.media.common.mapper.MediaEpisodeMapper;
import com.bgssai.media.common.web.BizException;
import com.bgssai.media.common.web.PageResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DramaService {

    private final MediaDramaMapper mediaDramaMapper;
    private final MediaEpisodeMapper mediaEpisodeMapper;

    public DramaService(MediaDramaMapper mediaDramaMapper, MediaEpisodeMapper mediaEpisodeMapper) {
        this.mediaDramaMapper = mediaDramaMapper;
        this.mediaEpisodeMapper = mediaEpisodeMapper;
    }

    public PageResult<MediaDrama> page(String status, String keyword, int pageNum, int pageSize) {
        MediaDramaExample example = new MediaDramaExample();
        MediaDramaExample.Criteria c = example.createCriteria();
        if (status != null && !status.isBlank()) {
            c.andStatusEqualTo(status);
        }
        if (keyword != null && !keyword.isBlank()) {
            c.andTitleLike("%" + keyword.trim() + "%");
        }
        example.setOrderByClause("id desc");
        PageHelper.startPage(pageNum, pageSize);
        List<MediaDrama> list = mediaDramaMapper.selectByExample(example);
        PageInfo<MediaDrama> info = new PageInfo<>(list);
        return new PageResult<>(info.getTotal(), pageNum, pageSize, list);
    }

    public MediaDrama get(Long id) {
        MediaDrama drama = mediaDramaMapper.selectByPrimaryKey(id);
        if (drama == null) {
            throw new BizException(404, "drama not found");
        }
        return drama;
    }

    public Map<String, Object> detail(Long id, boolean publishedOnly) {
        MediaDrama drama = get(id);
        if (publishedOnly && !"published".equals(drama.getStatus())) {
            throw new BizException(404, "drama not found");
        }
        MediaEpisodeExample example = new MediaEpisodeExample();
        MediaEpisodeExample.Criteria c = example.createCriteria().andDramaIdEqualTo(id);
        if (publishedOnly) {
            c.andStatusEqualTo("published");
        }
        example.setOrderByClause("ep_no asc");
        List<MediaEpisode> episodes = mediaEpisodeMapper.selectByExample(example);
        Map<String, Object> result = new HashMap<>();
        result.put("drama", drama);
        result.put("episodes", episodes);
        return result;
    }

    public MediaDrama create(MediaDrama drama) {
        if (drama.getStatus() == null) drama.setStatus("draft");
        if (drama.getSource() == null) drama.setSource("manual");
        mediaDramaMapper.insertSelective(drama);
        return drama;
    }

    public MediaDrama update(Long id, MediaDrama patch) {
        get(id);
        patch.setId(id);
        mediaDramaMapper.updateByPrimaryKeySelective(patch);
        return get(id);
    }

    public MediaDrama publish(Long id, boolean published) {
        MediaDrama patch = new MediaDrama();
        patch.setId(id);
        patch.setStatus(published ? "published" : "draft");
        mediaDramaMapper.updateByPrimaryKeySelective(patch);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        mediaEpisodeMapper.deleteByDramaId(id);
        mediaDramaMapper.deleteByPrimaryKey(id);
    }

    public List<MediaEpisode> listEpisodes(Long dramaId) {
        MediaEpisodeExample example = new MediaEpisodeExample();
        example.createCriteria().andDramaIdEqualTo(dramaId);
        example.setOrderByClause("ep_no asc");
        return mediaEpisodeMapper.selectByExample(example);
    }

    public MediaEpisode getEpisode(Long id) {
        MediaEpisode ep = mediaEpisodeMapper.selectByPrimaryKey(id);
        if (ep == null) {
            throw new BizException(404, "episode not found");
        }
        return ep;
    }

    public MediaEpisode createEpisode(MediaEpisode episode) {
        get(episode.getDramaId());
        if (episode.getStatus() == null) episode.setStatus("draft");
        mediaEpisodeMapper.insertSelective(episode);
        return episode;
    }

    public MediaEpisode updateEpisode(Long id, MediaEpisode patch) {
        getEpisode(id);
        patch.setId(id);
        mediaEpisodeMapper.updateByPrimaryKeySelective(patch);
        return getEpisode(id);
    }

    public void deleteEpisode(Long id) {
        getEpisode(id);
        mediaEpisodeMapper.deleteByPrimaryKey(id);
    }
}
