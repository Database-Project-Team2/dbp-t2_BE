package com.dbpt2.server.evaluation;

import static com.dbpt2.server.common.JpqlSearch.contains;
import static com.dbpt2.server.common.JpqlSearch.has;

import com.dbpt2.server.common.JpqlSearch;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** 평가 결과 검색(SRH-004). 피평가 직원·프로젝트·유형·평가일 기간. 직원(viewerId)은 본인 소속 프로젝트(투입 전 포함)만. */
@Repository
@RequiredArgsConstructor
public class EvaluationSearch {

    private final EntityManager em;

    public record Criteria(String targetEmployeeName, Integer targetEmployeeId, Integer projectId, EvaluationType type,
                           LocalDate from, LocalDate to, Integer viewerId) {}

    private static final String SELECT = "SELECT ev FROM Evaluation ev JOIN FETCH ev.target t JOIN FETCH t.employee te"
            + " JOIN FETCH t.project p JOIN FETCH p.client LEFT JOIN FETCH ev.evaluator ee LEFT JOIN FETCH ee.employee"
            + " JOIN FETCH ev.registeredBy rb JOIN FETCH rb.employee";
    private static final String COUNT = "SELECT COUNT(ev) FROM Evaluation ev JOIN ev.target t JOIN t.employee te JOIN t.project p";
    private static final String ORDER = "ORDER BY ev.evaluatedDate DESC, ev.id DESC";

    public Page<Evaluation> search(Criteria c, Pageable pageable) {
        return build(c).page(em, SELECT, COUNT, ORDER, Evaluation.class, pageable);
    }

    public List<Evaluation> searchAll(Criteria c) {
        return build(c).list(em, SELECT, ORDER, Evaluation.class);
    }

    JpqlSearch build(Criteria c) {
        JpqlSearch q = new JpqlSearch();
        if (c.viewerId() != null) {
            q.where("EXISTS (SELECT 1 FROM ProjectMember vm WHERE vm.project = p AND vm.employee.id = :viewerId)", "viewerId", c.viewerId());
        }
        if (has(c.targetEmployeeName())) {
            q.where("LOWER(te.name) LIKE :targetName ESCAPE '\\'", "targetName", contains(c.targetEmployeeName()));
        }
        if (c.targetEmployeeId() != null) {
            q.where("te.id = :targetEmployeeId", "targetEmployeeId", c.targetEmployeeId());
        }
        if (c.projectId() != null) {
            q.where("p.id = :projectId", "projectId", c.projectId());
        }
        if (c.type() != null) {
            q.where("ev.type = :type", "type", c.type());
        }
        if (c.from() != null) {
            q.where("ev.evaluatedDate >= :from", "from", c.from());
        }
        if (c.to() != null) {
            q.where("ev.evaluatedDate <= :to", "to", c.to());
        }
        return q;
    }
}
