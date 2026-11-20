package com.dbpt2.server.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** 목록 공통 page/size: 기본 0/20, size는 1~100. 범위를 벗어나면 400. */
public final class Paging {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Paging() {}

    public static Pageable of(Integer page, Integer size, Sort sort) {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 0) {
            throw ApiException.invalid("page", "page는 0 이상이어야 해요.");
        }
        if (s < 1 || s > MAX_SIZE) {
            throw ApiException.invalid("size", "size는 1~100 사이여야 해요.");
        }
        return PageRequest.of(p, s, sort);
    }
}
