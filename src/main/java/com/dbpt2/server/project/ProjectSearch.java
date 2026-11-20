package com.dbpt2.server.project;

import static com.dbpt2.server.common.JpqlSearch.contains;
import static com.dbpt2.server.common.JpqlSearch.has;

import com.dbpt2.server.common.JpqlSearch;
import com.dbpt2.server.status.ProjectStatus;
import com.dbpt2.server.status.StatusRules;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 프로젝트 검색(SRH-002). 프로젝트명·발주처명 부분 일치 AND, 상태(기준일 기준), 기준일 포함, 수행기간 겹침.
 * 직원(viewerId)은 본인이 구성원인 프로젝트만(투입 전 포함).
 */
@Repository
@RequiredArgsConstructor
public class ProjectSearch {

    private final EntityManager em;

    public record Criteria(String projectName, String clientName, ProjectStatus status, LocalDate asOf,
                           LocalDate from, LocalDate to, Integer viewerId, LocalDate statusDate) {}

    public Page<Project> search(Criteria c, Pageable pageable) {
        return build(c).page(em, "SELECT p FROM Project p JOIN FETCH p.client c", "SELECT COUNT(p) FROM Project p JOIN p.client c",
                "ORDER BY p.id", Project.class, pageable);
    }

    /** 상태 탭의 건수: 상태 조건만 빼고 같은 조건으로 상태별로 센다. */
    public List<Project> searchWithoutStatus(Criteria c) {
        Criteria withoutStatus = new Criteria(c.projectName(), c.clientName(), null, c.asOf(), c.from(), c.to(), c.viewerId(), c.statusDate());
        return build(withoutStatus).list(em, "SELECT p FROM Project p JOIN FETCH p.client c", "ORDER BY p.id", Project.class);
    }

    JpqlSearch build(Criteria c) {
        JpqlSearch q = new JpqlSearch();
        if (c.viewerId() != null) {
            q.where("EXISTS (SELECT 1 FROM ProjectMember vm WHERE vm.project = p AND vm.employee.id = :viewerId)", "viewerId", c.viewerId());
        }
        if (has(c.projectName())) {
            q.where("LOWER(p.name) LIKE :projectName ESCAPE '\\'", "projectName", contains(c.projectName()));
        }
        if (has(c.clientName())) {
            q.where("LOWER(c.name) LIKE :clientName ESCAPE '\\'", "clientName", contains(c.clientName()));
        }
        if (c.status() != null) {
            String rule = switch (c.status()) {
                case NOT_STARTED -> StatusRules.PROJECT_NOT_STARTED;
                case IN_PROGRESS -> StatusRules.PROJECT_IN_PROGRESS;
                case CLOSED -> StatusRules.PROJECT_CLOSED;
            };
            // 상태 판정 기준일은 :asOf 이름을 공유하므로 별도 이름으로 바꿔 쓴다.
            q.where(rule.replace(":asOf", ":statusDate"), "statusDate", c.statusDate());
        }
        if (c.asOf() != null) {
            q.where(StatusRules.PROJECT_PERIOD_CONTAINS, "asOf", c.asOf());
        }
        if (c.from() != null) {
            q.where("(p.actualEndDate IS NULL OR p.actualEndDate >= :from)", "from", c.from());
        }
        if (c.to() != null) {
            q.where("p.actualStartDate <= :to", "to", c.to());
        }
        return q;
    }
}
