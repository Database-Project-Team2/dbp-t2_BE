package com.dbpt2.server.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * 선택 조건이 많은 검색용 JPQL 조립기. 조건마다 WHERE 절 조각과 파라미터를 더하고, 완성된 JPQL을 그대로 실행한다.
 * 조립 결과는 {@link #jpql(String)}로 확인할 수 있다(보고서 인용·테스트용).
 */
public final class JpqlSearch {

    private final List<String> conditions = new ArrayList<>();
    private final Map<String, Object> params = new LinkedHashMap<>();

    public JpqlSearch where(String condition) {
        conditions.add(condition);
        return this;
    }

    public JpqlSearch where(String condition, String name, Object value) {
        conditions.add(condition);
        params.put(name, value);
        return this;
    }

    public JpqlSearch param(String name, Object value) {
        params.put(name, value);
        return this;
    }

    /** 대소문자 무시 부분 일치용 LIKE 값. %, _는 글자 그대로 찾는다. */
    public static String contains(String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public static boolean has(String value) {
        return value != null && !value.isBlank();
    }

    public String whereClause() {
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    public String jpql(String selectFrom) {
        return selectFrom + whereClause();
    }

    public <T> List<T> list(EntityManager em, String selectFrom, String orderBy, Class<T> type) {
        TypedQuery<T> query = em.createQuery(jpql(selectFrom) + " " + orderBy, type);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    public <T> Page<T> page(EntityManager em, String selectFrom, String countFrom, String orderBy, Class<T> type, Pageable pageable) {
        TypedQuery<T> query = em.createQuery(jpql(selectFrom) + " " + orderBy, type);
        params.forEach(query::setParameter);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        TypedQuery<Long> count = em.createQuery(jpql(countFrom), Long.class);
        params.forEach(count::setParameter);
        return new PageImpl<>(query.getResultList(), pageable, count.getSingleResult());
    }
}
