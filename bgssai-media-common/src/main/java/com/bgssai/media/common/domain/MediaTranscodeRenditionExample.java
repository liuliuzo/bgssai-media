package com.bgssai.media.common.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MediaTranscodeRenditionExample {
    protected String orderByClause;
    protected List<Criteria> oredCriteria = new ArrayList<>();

    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public String getOrderByClause() { return orderByClause; }
    public List<Criteria> getOredCriteria() { return oredCriteria; }

    public Criteria createCriteria() {
        Criteria criteria = new Criteria();
        if (oredCriteria.isEmpty()) { oredCriteria.add(criteria); }
        return criteria;
    }

    public static class Criteria {
        private final List<Criterion> criteria = new ArrayList<>();
        public boolean isValid() { return !criteria.isEmpty(); }
        public List<Criterion> getAllCriteria() { return criteria; }
        public List<Criterion> getCriteria() { return criteria; }
        protected void addCriterion(String condition) { criteria.add(new Criterion(condition)); }
        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) { throw new RuntimeException("Value for " + property + " cannot be null"); }
            criteria.add(new Criterion(condition, value));
        }

        public Criteria andIdEqualTo(Long value) { addCriterion("id =", value, "id"); return this; }
        public Criteria andIdIn(List<Long> values) { addCriterion("id in", values, "id"); return this; }
        public Criteria andJobIdEqualTo(Long value) { addCriterion("job_id =", value, "jobId"); return this; }
        public Criteria andJobIdIn(List<Long> values) { addCriterion("job_id in", values, "jobId"); return this; }
        public Criteria andVideoIdEqualTo(Long value) { addCriterion("video_id =", value, "videoId"); return this; }
        public Criteria andVideoIdIn(List<Long> values) { addCriterion("video_id in", values, "videoId"); return this; }
        public Criteria andBitrateLabelEqualTo(String value) { addCriterion("bitrate_label =", value, "bitrateLabel"); return this; }
        public Criteria andHeightEqualTo(Integer value) { addCriterion("height =", value, "height"); return this; }
        public Criteria andPlayUrlEqualTo(String value) { addCriterion("play_url =", value, "playUrl"); return this; }
        public Criteria andStoragePathEqualTo(String value) { addCriterion("storage_path =", value, "storagePath"); return this; }
        public Criteria andCreatedAtEqualTo(Date value) { addCriterion("created_at =", value, "createdAt"); return this; }
    }

    public static class Criterion {
        private final String condition;
        private final Object value;
        private final boolean singleValue;
        private final boolean listValue;
        private final boolean noValue;

        public Criterion(String condition) {
            this.condition = condition;
            this.value = null;
            this.singleValue = false;
            this.listValue = false;
            this.noValue = true;
        }

        public Criterion(String condition, Object value) {
            this.condition = condition;
            this.value = value;
            this.noValue = false;
            if (value instanceof List) {
                this.listValue = true;
                this.singleValue = false;
            } else {
                this.listValue = false;
                this.singleValue = true;
            }
        }

        public String getCondition() { return condition; }
        public Object getValue() { return value; }
        public boolean isSingleValue() { return singleValue; }
        public boolean isListValue() { return listValue; }
        public boolean isNoValue() { return noValue; }
    }
}
