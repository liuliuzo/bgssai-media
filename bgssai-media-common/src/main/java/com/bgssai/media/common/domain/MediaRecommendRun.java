package com.bgssai.media.common.domain;

import java.util.Date;

public class MediaRecommendRun {
    private Long id;
    private Long userId;
    private Integer candidateCount;
    private Integer keptCount;
    private String algorithm;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getCandidateCount() { return candidateCount; }
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }

    public Integer getKeptCount() { return keptCount; }
    public void setKeptCount(Integer keptCount) { this.keptCount = keptCount; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

}
