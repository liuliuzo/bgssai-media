package com.bgssai.media.common.domain;

import java.util.ArrayList;
import java.util.List;

public class SysUserExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria = new ArrayList<>();

    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public String getOrderByClause() { return orderByClause; }
    public void setDistinct(boolean distinct) { this.distinct = distinct; }
    public boolean isDistinct() { return distinct; }
    public List<Criteria> getOredCriteria() { return oredCriteria; }

    public Criteria createCriteria() {
        Criteria criteria = new Criteria();
        if (oredCriteria.isEmpty()) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    public void or(Criteria criteria) { oredCriteria.add(criteria); }

    public static class Criteria {
        private final List<Criterion> criteria = new ArrayList<>();
        public boolean isValid() { return !criteria.isEmpty(); }
        public List<Criterion> getAllCriteria() { return criteria; }
        public List<Criterion> getCriteria() { return criteria; }
        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) throw new RuntimeException("Value for " + property + " cannot be null");
            criteria.add(new Criterion(condition, value));
        }
        protected void addCriterion(String condition) {
            criteria.add(new Criterion(condition));
        }
        public Criteria andUsernameEqualTo(String value) { addCriterion("username =", value, "username"); return this; }
        public Criteria andEmailEqualTo(String value) { addCriterion("email =", value, "email"); return this; }
        public Criteria andPhoneEqualTo(String value) { addCriterion("phone =", value, "phone"); return this; }
        public Criteria andRoleCodeEqualTo(String value) { addCriterion("role_code =", value, "roleCode"); return this; }
        public Criteria andStatusEqualTo(String value) { addCriterion("status =", value, "status"); return this; }
        public Criteria andIdEqualTo(Long value) { addCriterion("id =", value, "id"); return this; }
    }

    public static class Criterion {
        private final String condition;
        private final Object value;
        private final boolean singleValue;
        private final boolean noValue;
        public Criterion(String condition) { this.condition = condition; this.value = null; this.noValue = true; this.singleValue = false; }
        public Criterion(String condition, Object value) { this.condition = condition; this.value = value; this.singleValue = true; this.noValue = false; }
        public String getCondition() { return condition; }
        public Object getValue() { return value; }
        public boolean isSingleValue() { return singleValue; }
        public boolean isNoValue() { return noValue; }
    }
}
