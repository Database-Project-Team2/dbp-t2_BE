package com.dbpt2.server.employee;

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
 * 직원 검색(SRH-001). 이름·사번 부분 일치, 직군·재직 상태, 현재 투입 여부, 최소 경력, 기술 키워드 여러 개 AND(부분 일치).
 * skill_description은 자유 문장이라 표준 기술 토큰이 아니며, 키워드마다 LIKE 조건을 하나씩 더한다.
 */
@Repository
@RequiredArgsConstructor
public class EmployeeSearch {

    private final EntityManager em;

    public record Criteria(String employeeName, String employeeNo, JobCategory jobCategory, EmploymentStatus employmentStatus,
                           Boolean assigned, Integer minCareerMonths, List<String> skills, LocalDate asOf,
                           Integer excludeProjectId, boolean activeOnly) {}

    public Page<Employee> search(Criteria c, Pageable pageable) {
        return build(c).page(em, "SELECT e FROM Employee e", "SELECT COUNT(e) FROM Employee e", "ORDER BY e.employeeNo", Employee.class, pageable);
    }

    JpqlSearch build(Criteria c) {
        JpqlSearch q = new JpqlSearch();
        if (has(c.employeeName())) {
            q.where("LOWER(e.name) LIKE :employeeName ESCAPE '\\'", "employeeName", contains(c.employeeName()));
        }
        if (has(c.employeeNo())) {
            q.where("e.employeeNo LIKE :employeeNo ESCAPE '\\'", "employeeNo", contains(c.employeeNo()));
        }
        if (c.jobCategory() != null) {
            q.where("e.jobCategory = :jobCategory", "jobCategory", c.jobCategory());
        }
        if (c.activeOnly()) {
            q.where("e.employmentStatus = com.dbpt2.server.employee.EmploymentStatus.ACTIVE");
        } else if (c.employmentStatus() != null) {
            q.where("e.employmentStatus = :employmentStatus", "employmentStatus", c.employmentStatus());
        }
        if (c.minCareerMonths() != null) {
            q.where("e.totalCareerMonths >= :minCareer", "minCareer", c.minCareerMonths());
        }
        if (c.assigned() != null) {
            String exists = "EXISTS (SELECT 1 FROM ProjectMember m WHERE m.employee = e AND " + StatusRules.MEMBER_ASSIGNED + ")";
            q.where(c.assigned() ? exists : "NOT " + exists, "asOf", c.asOf());
        }
        if (c.excludeProjectId() != null) {
            q.where("NOT EXISTS (SELECT 1 FROM ProjectMember x WHERE x.employee = e AND x.project.id = :excludeProjectId)",
                    "excludeProjectId", c.excludeProjectId());
        }
        List<String> skills = c.skills() == null ? List.of() : c.skills().stream().filter(JpqlSearch::has).toList();
        for (int i = 0; i < skills.size(); i++) {
            q.where("LOWER(e.skillDescription) LIKE :skill" + i + " ESCAPE '\\'", "skill" + i, contains(skills.get(i)));
        }
        return q;
    }
}
