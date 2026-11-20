package com.dbpt2.server.member;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.ErrorCode;
import com.dbpt2.server.common.PatchBody;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.employee.Employee;
import com.dbpt2.server.employee.EmployeeRepository;
import com.dbpt2.server.evaluation.EvaluationRepository;
import com.dbpt2.server.member.MemberDtos.MemberResponse;
import com.dbpt2.server.project.Project;
import com.dbpt2.server.project.ProjectRepository;
import com.dbpt2.server.security.AuthUser;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구성원 추가·투입기간 수정·투입 전 삭제(경영진 전용, 명세 4장).
 * 같은 직원은 한 프로젝트에 한 번, 역할 고정, 재투입 없음. 실제 날짜는 입사일·착수일 이상, 오늘 이하.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final EmployeeRepository employeeRepository;
    private final EvaluationRepository evaluationRepository;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public record AddMemberRequest(
            @jakarta.validation.constraints.NotNull(message = "직원을 선택해 주세요.") Integer employeeId,
            @jakarta.validation.constraints.NotNull(message = "수행 역할을 선택해 주세요.") ProjectRole projectRole,
            LocalDate assignmentStartDate,
            LocalDate assignmentEndDate) {}

    public MemberResponse add(AuthUser user, int projectId, AddMemberRequest request) {
        accessPolicy.requireManagement(user);
        Project project = lockOpenProject(projectId);
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> ApiException.field(ErrorCode.NOT_FOUND, "employee_id", "직원을 찾을 수 없어요."));
        if (memberRepository.existsByProjectIdAndEmployeeId(projectId, employee.getId())) {
            throw new ApiException(ErrorCode.MEMBER_DUPLICATED);
        }
        requireActive(employee);
        validateDates(project, employee, request.assignmentStartDate(), request.assignmentEndDate());
        try {
            ProjectMember saved = memberRepository.saveAndFlush(ProjectMember.create(project, employee, request.projectRole(),
                    request.assignmentStartDate(), request.assignmentEndDate()));
            return MemberResponse.of(saved, today());
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("uq_project_member")) {
                throw new ApiException(ErrorCode.MEMBER_DUPLICATED);
            }
            throw e;
        }
    }

    public MemberResponse changeAssignment(AuthUser user, int memberId, PatchBody patch) {
        accessPolicy.requireManagement(user);
        ProjectMember member = memberRepository.findById(memberId).orElseThrow(ApiException::notFound);
        Project project = lockOpenProject(member.getProject().getId());
        if (member.getAssignmentEndDate() != null) {
            throw new ApiException(ErrorCode.ASSIGNMENT_ENDED, "투입이 종료된 구성원은 재투입하거나 기간을 변경할 수 없어요.");
        }
        LocalDate start = patch.has("assignment_start_date") ? patch.date("assignment_start_date", true) : member.getAssignmentStartDate();
        LocalDate end = patch.has("assignment_end_date") ? patch.date("assignment_end_date", true) : member.getAssignmentEndDate();
        if (member.getAssignmentStartDate() != null && start == null) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "assignment_start_date", "이미 투입한 구성원을 투입 전으로 되돌릴 수 없어요.");
        }
        requireActive(member.getEmployee());
        validateDates(project, member.getEmployee(), start, end);
        member.changeAssignment(start, end);
        return MemberResponse.of(member, today());
    }

    public void remove(AuthUser user, int memberId) {
        accessPolicy.requireManagement(user);
        ProjectMember member = memberRepository.findById(memberId).orElseThrow(ApiException::notFound);
        lockOpenProject(member.getProject().getId());
        if (!member.isBeforeAssignment()) {
            throw ApiException.rule("투입 전인 구성원만 삭제할 수 있어요. 실제 투입 이력은 유지해요.");
        }
        if (evaluationRepository.existsByTargetIdOrEvaluatorIdOrRegisteredById(memberId, memberId, memberId)) {
            throw new ApiException(ErrorCode.DATA_CONFLICT, "평가 기록이 있는 구성원은 삭제할 수 없어요.");
        }
        memberRepository.delete(member);
    }

    private Project lockOpenProject(int projectId) {
        Project project = projectRepository.findByIdForUpdate(projectId).orElseThrow(ApiException::notFound);
        if (project.isClosed()) {
            throw new ApiException(ErrorCode.PROJECT_CLOSED, "종료된 프로젝트의 구성원과 투입 이력은 변경할 수 없어요.");
        }
        return project;
    }

    private static void requireActive(Employee employee) {
        if (!employee.isActive()) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "employee_id", "현재 재직 중인 직원만 투입할 수 있어요.");
        }
    }

    private void validateDates(Project project, Employee employee, LocalDate start, LocalDate end) {
        LocalDate today = today();
        if (start != null) {
            if (start.isBefore(project.getActualStartDate()) || start.isBefore(employee.getHireDate())) {
                throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "assignment_start_date",
                        "투입은 프로젝트 착수일과 직원 입사일 이후여야 해요.");
            }
            if (start.isAfter(today)) {
                throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "assignment_start_date", "실제 투입 시작일은 오늘 이후일 수 없어요.");
            }
        }
        if (end != null) {
            if (start == null || end.isBefore(start)) {
                throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "assignment_end_date", "투입 종료일은 투입 시작일 이후여야 해요.");
            }
            if (end.isAfter(today)) {
                throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "assignment_end_date", "실제 투입 종료일은 오늘 이후일 수 없어요.");
            }
        }
    }

    private LocalDate today() {
        return TimeConfig.today(clock);
    }
}
