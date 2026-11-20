package com.dbpt2.server.client;

import static com.dbpt2.server.common.JpqlSearch.contains;
import static com.dbpt2.server.common.JpqlSearch.has;

import com.dbpt2.server.common.JpqlSearch;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** 발주처 검색: 회사명 또는 담당자명 부분 일치. 직원은 본인 소속 프로젝트(투입 전 포함)의 발주처만. */
@Repository
@RequiredArgsConstructor
public class ClientSearch {

    private final EntityManager em;

    public Page<Client> search(String query, Integer viewerId, Pageable pageable) {
        JpqlSearch q = new JpqlSearch();
        if (viewerId != null) {
            q.where("EXISTS (SELECT 1 FROM ProjectMember m JOIN m.project p WHERE p.client = c AND m.employee.id = :viewerId)",
                    "viewerId", viewerId);
        }
        if (has(query)) {
            q.where("(LOWER(c.name) LIKE :query ESCAPE '\\' OR LOWER(c.contactName) LIKE :query ESCAPE '\\')", "query", contains(query));
        }
        return q.page(em, "SELECT c FROM Client c", "SELECT COUNT(c) FROM Client c", "ORDER BY c.id", Client.class, pageable);
    }
}
