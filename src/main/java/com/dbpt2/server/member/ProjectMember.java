package com.dbpt2.server.member;

import com.dbpt2.server.employee.Employee;
import com.dbpt2.server.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PROJECT_MEMBER — 직원의 프로젝트별 고정 역할과 한 번의 실제 투입기간.
 * 투입 전: 시작·종료 NULL / 투입 중: 종료 NULL.
 */
@Entity
@Table(
        name = "PROJECT_MEMBER",
        uniqueConstraints = @UniqueConstraint(name = "uq_project_member", columnNames = {"project_id", "employee_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    /** 최초 등록 후 변경 불가 — updatable = false로 UPDATE 문에서 아예 제외된다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "project_role", nullable = false, updatable = false, columnDefinition = "VARCHAR(30)")
    private ProjectRole role;

    @Column(name = "assignment_start_date")
    private LocalDate assignmentStartDate;

    @Column(name = "assignment_end_date")
    private LocalDate assignmentEndDate;

    public static ProjectMember create(Project project, Employee employee, ProjectRole role,
                                       LocalDate assignmentStartDate, LocalDate assignmentEndDate) {
        ProjectMember m = new ProjectMember();
        m.project = project;
        m.employee = employee;
        m.role = role;
        m.assignmentStartDate = assignmentStartDate;
        m.assignmentEndDate = assignmentEndDate;
        return m;
    }

    public void changeAssignment(LocalDate assignmentStartDate, LocalDate assignmentEndDate) {
        this.assignmentStartDate = assignmentStartDate;
        this.assignmentEndDate = assignmentEndDate;
    }

    /** 프로젝트 종료 시 아직 끝나지 않은 실제 투입을 함께 끝낸다. */
    public boolean endWithProject(LocalDate projectEndDate) {
        if (assignmentStartDate != null && assignmentEndDate == null) {
            assignmentEndDate = projectEndDate;
            return true;
        }
        return false;
    }

    public boolean isBeforeAssignment() {
        return assignmentStartDate == null && assignmentEndDate == null;
    }
}
