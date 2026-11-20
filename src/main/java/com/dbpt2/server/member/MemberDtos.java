package com.dbpt2.server.member;

import com.dbpt2.server.employee.Employee;
import com.dbpt2.server.employee.EmploymentStatus;
import com.dbpt2.server.employee.JobCategory;
import com.dbpt2.server.project.Project;
import com.dbpt2.server.status.AssignmentStatus;
import com.dbpt2.server.status.ProjectStatus;
import com.dbpt2.server.status.StatusRules;
import java.time.LocalDate;

public final class MemberDtos {

    private MemberDtos() {}

    /** 구성원·이력 응답에 붙는 직원 요약. 연락처·주민번호·계정 정보는 넣지 않는다. */
    public record EmployeeSummary(int employeeId, String employeeNo, String employeeName, JobCategory jobCategory,
                                  EmploymentStatus employmentStatus) {
        public static EmployeeSummary of(Employee e) {
            return new EmployeeSummary(e.getId(), e.getEmployeeNo(), e.getName(), e.getJobCategory(), e.getEmploymentStatus());
        }
    }

    public record MemberResponse(int memberId, int projectId, int employeeId, ProjectRole projectRole,
                                 LocalDate assignmentStartDate, LocalDate assignmentEndDate,
                                 AssignmentStatus assignmentStatus, EmployeeSummary employee) {
        public static MemberResponse of(ProjectMember m, LocalDate asOf) {
            return new MemberResponse(m.getId(), m.getProject().getId(), m.getEmployee().getId(), m.getRole(),
                    m.getAssignmentStartDate(), m.getAssignmentEndDate(), StatusRules.assignmentStatus(m, asOf),
                    EmployeeSummary.of(m.getEmployee()));
        }
    }

    public record ProjectRef(int projectId, String projectName, ProjectStatus status, LocalDate actualStartDate,
                             LocalDate plannedEndDate, LocalDate actualEndDate) {
        public static ProjectRef of(Project p, LocalDate asOf) {
            return new ProjectRef(p.getId(), p.getName(), StatusRules.projectStatus(p, asOf), p.getActualStartDate(),
                    p.getPlannedEndDate(), p.getActualEndDate());
        }
    }

    /** 투입 이력 행: 구성원 + 직원 요약 + 프로젝트 요약. */
    public record AssignmentResponse(int memberId, int projectId, int employeeId, ProjectRole projectRole,
                                     LocalDate assignmentStartDate, LocalDate assignmentEndDate,
                                     AssignmentStatus assignmentStatus, EmployeeSummary employee, ProjectRef project) {
        public static AssignmentResponse of(ProjectMember m, LocalDate asOf) {
            return new AssignmentResponse(m.getId(), m.getProject().getId(), m.getEmployee().getId(), m.getRole(),
                    m.getAssignmentStartDate(), m.getAssignmentEndDate(), StatusRules.assignmentStatus(m, asOf),
                    EmployeeSummary.of(m.getEmployee()), ProjectRef.of(m.getProject(), asOf));
        }
    }
}
