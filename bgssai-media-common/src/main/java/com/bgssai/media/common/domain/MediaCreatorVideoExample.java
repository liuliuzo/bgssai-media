package com.bgssai.media.common.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MediaCreatorVideoExample {
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
        public Criteria andUserIdEqualTo(Long value) { addCriterion("user_id =", value, "userId"); return this; }
        public Criteria andUserIdIn(List<Long> values) { addCriterion("user_id in", values, "userId"); return this; }
        public Criteria andChannelIdEqualTo(Long value) { addCriterion("channel_id =", value, "channelId"); return this; }
        public Criteria andChannelIdIn(List<Long> values) { addCriterion("channel_id in", values, "channelId"); return this; }
        public Criteria andTitleEqualTo(String value) { addCriterion("title =", value, "title"); return this; }
        public Criteria andDescriptionEqualTo(String value) { addCriterion("description =", value, "description"); return this; }
        public Criteria andStatusEqualTo(String value) { addCriterion("status =", value, "status"); return this; }
        public Criteria andStatusNotEqualTo(String value) { addCriterion("status <>", value, "status"); return this; }
        public Criteria andOriginalFilenameEqualTo(String value) { addCriterion("original_filename =", value, "originalFilename"); return this; }
        public Criteria andOriginalPathEqualTo(String value) { addCriterion("original_path =", value, "originalPath"); return this; }
        public Criteria andContentTypeEqualTo(String value) { addCriterion("content_type =", value, "contentType"); return this; }
        public Criteria andSizeBytesEqualTo(Long value) { addCriterion("size_bytes =", value, "sizeBytes"); return this; }
        public Criteria andSizeBytesIn(List<Long> values) { addCriterion("size_bytes in", values, "sizeBytes"); return this; }
        public Criteria andCoverPathEqualTo(String value) { addCriterion("cover_path =", value, "coverPath"); return this; }
        public Criteria andCoverUrlEqualTo(String value) { addCriterion("cover_url =", value, "coverUrl"); return this; }
        public Criteria andLikeCountEqualTo(Integer value) { addCriterion("like_count =", value, "likeCount"); return this; }
        public Criteria andCommentCountEqualTo(Integer value) { addCriterion("comment_count =", value, "commentCount"); return this; }
        public Criteria andCreatedAtEqualTo(Date value) { addCriterion("created_at =", value, "createdAt"); return this; }
        public Criteria andUpdatedAtEqualTo(Date value) { addCriterion("updated_at =", value, "updatedAt"); return this; }
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
