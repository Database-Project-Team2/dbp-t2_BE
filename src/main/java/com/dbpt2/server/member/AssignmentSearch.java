package com.dbpt2.server.member;

import static com.dbpt2.server.common.JpqlSearch.contains;
import static com.dbpt2.server.common.JpqlSearch.has;

import com.dbpt2.server.common.JpqlSearch;
import com.dbpt2.server.status.StatusRules;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 투입 이력 검색(SRH-003, QRY-003·004). 기준일 검색은 시작·종료일 당일 포함, 기간 검색은 겹치는 구간,
 * 종료일 null은 열린 구간, 투입 전(시작일 null)은 기준일·기간 검색에서 제외.
 */
@Repository
@RequiredArgsConstructor
public class AssignmentSearch {

    private final EntityManager em;

    public record Criteria(String employeeName, Integer employeeId, Integer projectId, ProjectRole projectRole,
                           LocalDate asOf, LocalDate from, LocalDate to, boolean ongoingProjectsOnly, LocalDate today,
                           boolean actualOnly) {}

    private static final String SELECT = "SELECT m FROM ProjectMember m JOIN FETCH m.employee e JOIN FETCH m.project p";
    private static final String COUNT = "SELECT COUNT(m) FROM ProjectMember m JOIN m.employee e JOIN m.project p";
    private static final String ORDER = "ORDER BY p.actualStartDate DESC, p.id DESC, m.id";

    public Page<ProjectMember> search(Criteria c, Pageable pageable) {
        return build(c).page(em, SELECT, COUNT, ORDER, ProjectMember.class, pageable);
    }

    public List<ProjectMember> searchAll(Criteria c) {
        return build(c).list(em, SELECT, ORDER, ProjectMember.class);
    }

    JpqlSearch build(Criteria c) {
        JpqlSearch q = new JpqlSearch();
        if (has(c.employeeName())) {
            q.where("LOWER(e.name) LIKE :employeeName ESCAPE '\\'", "employeeName", contains(c.employeeName()));
        }
        if (c.employeeId() != null) {
            q.where("e.id = :employeeId", "employeeId", c.employeeId());
        }
        if (c.projectId() != null) {
            q.where("p.id = :projectId", "projectId", c.projectId());
        }
        if (c.projectRole() != null) {
            q.where("m.role = :projectRole", "projectRole", c.projectRole());
        }
        if (c.actualOnly()) {
            q.where("m.assignmentStartDate IS NOT NULL");
        }
        if (c.asOf() != null) {
            q.where(StatusRules.MEMBER_ASSIGNED, "asOf", c.asOf());
        }
        if (c.from() != null || c.to() != null) {
            q.where("m.assignmentStartDate IS NOT NULL");
        }
        if (c.from() != null) {
            q.where("(m.assignmentEndDate IS NULL OR m.assignmentEndDate >= :from)", "from", c.from());
        }
        if (c.to() != null) {
            q.where("m.assignmentStartDate <= :to", "to", c.to());
        }
        if (c.ongoingProjectsOnly()) {
            q.where(StatusRules.PROJECT_IN_PROGRESS.replace(":asOf", ":ongoingDate"), "ongoingDate",
                    c.asOf() != null ? c.asOf() : c.today());
        }
        return q;
    }
}
