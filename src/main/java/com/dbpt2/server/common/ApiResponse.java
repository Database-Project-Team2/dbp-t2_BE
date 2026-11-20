package com.dbpt2.server.common;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** 성공 응답 `{data, meta}` / 목록 `{data, page, meta}`. */
public record ApiResponse<T>(T data, Meta meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, Meta.current());
    }

    public static <T> PageBody<T> page(Page<T> page) {
        return new PageBody<>(page.getContent(), PageInfo.from(page), Meta.current());
    }

    public record PageBody<T>(List<T> data, PageInfo page, Meta meta) {}

    public record PageInfo(int page, int size, long totalElements, int totalPages, boolean hasNext) {
        static PageInfo from(Page<?> page) {
            return new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
        }
    }

    public record Meta(String requestId, OffsetDateTime timestamp) {
        public static final String REQUEST_ID_ATTRIBUTE = "dbp.requestId";

        public static Meta current() {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            Object id = attributes == null ? null : attributes.getAttribute(REQUEST_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            return new Meta(id == null ? UUID.randomUUID().toString() : id.toString(), OffsetDateTime.now(TimeConfig.ZONE));
        }
    }
}
