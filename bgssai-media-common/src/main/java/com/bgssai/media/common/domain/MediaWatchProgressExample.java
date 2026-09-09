package com.bgssai.media.common.domain;

import java.util.ArrayList;
import java.util.List;

public class MediaWatchProgressExample {
    protected String orderByClause;
    protected List<Criteria> oredCriteria = new ArrayList<>();
    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public String getOrderByClause() { return orderByClause; }
    public List<Criteria> getOredCriteria() { return oredCriteria; }
    public Criteria createCriteria() {
        Criteria criteria = new Criteria();
        if (oredCriteria.isEmpty()) oredCriteria.add(criteria);
        return criteria;
    }
    public static class Criteria {
        private final List<Criterion> criteria = new ArrayList<>();
        public boolean isValid() { return !criteria.isEmpty(); }
        public List<Criterion> getAllCriteria() { return criteria; }
        public List<Criterion> getCriteria() { return criteria; }
        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) throw new RuntimeException("Value for " + property + " cannot be null");
            criteria.add(new Criterion(condition, value));
        }
        public Criteria andUserIdEqualTo(Long value) { addCriterion("user_id =", value, "userId"); return this; }
        public Criteria andDramaIdEqualTo(Long value) { addCriterion("drama_id =", value, "dramaId"); return this; }
    }
    public static class Criterion {
        private final String condition;
        private final Object value;
        private final boolean singleValue;
        public Criterion(String condition, Object value) { this.condition = condition; this.value = value; this.singleValue = true; }
        public String getCondition() { return condition; }
        public Object getValue() { return value; }
        public boolean isSingleValue() { return singleValue; }
        public boolean isNoValue() { return false; }
    }
}
