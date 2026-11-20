package com.dbpt2.server.member;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.Csv;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.employee.Employee;
import com.dbpt2.server.employee.EmployeeRepository;
import com.dbpt2.server.member.MemberDtos.AssignmentResponse;
import com.dbpt2.server.security.AuthUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인 경력(RPT-001)과 투입 이력 CSV. 개인 경력은 경영진 또는 본인.
 * 본인은 실제 투입 이력만, 경영진은 투입 전 배정까지 본다(기존 화면 규칙).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerService {

    private static final Map<ProjectRole, String> ROLE_LABELS = Map.of(ProjectRole.PM, "PM", ProjectRole.PL, "PL",
            ProjectRole.ANALYST, "분석가", ProjectRole.DESIGNER, "설계자", ProjectRole.PROGRAMMER, "프로그래머", ProjectRole.TESTER, "테스터");

    private final AssignmentSearch assignmentSearch;
    private final EmployeeRepository employeeRepository;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public List<AssignmentResponse> career(AuthUser user, int employeeId, AssignmentQueryService.Filter f) {
        boolean manager = accessPolicy.isManagement(user);
        if (!manager && user.employeeId() != employeeId) {
            throw ApiException.notFound();
        }
        if (!employeeRepository.existsById(employeeId)) {
            throw ApiException.notFound();
        }
        LocalDate today = TimeConfig.today(clock);
        AssignmentQueryService.Filter scoped = new AssignmentQueryService.Filter(null, employeeId, f.projectId(), f.projectRole(),
                f.asOf(), f.from(), f.to(), f.ongoingProjectsOnly());
        return assignmentSearch.searchAll(AssignmentQueryService.criteria(scoped, today, !manager)).stream()
                .map(m -> AssignmentResponse.of(m, AssignmentQueryService.statusDate(scoped, today))).toList();
    }

    public String careerCsv(AuthUser user, int employeeId, AssignmentQueryService.Filter f) {
        List<AssignmentResponse> rows = career(user, employeeId, f);
        Employee e = employeeRepository.findById(employeeId).orElseThrow(ApiException::notFound);
        List<List<Object>> lines = new ArrayList<>();
        for (AssignmentResponse r : rows) {
            lines.add(List.of(e.getEmployeeNo(), e.getName(), e.getTotalCareerMonths(), r.project().projectName(),
                    ROLE_LABELS.get(r.projectRole()), r.assignmentStartDate() == null ? "투입 전" : r.assignmentStartDate(),
                    r.assignmentEndDate() == null ? "" : r.assignmentEndDate()));
        }
        return Csv.content(List.of("사번", "직원명", "총 경력(개월)", "프로젝트", "역할", "투입 시작일", "투입 종료일"), lines);
    }

    public String careerFilename(int employeeId) {
        return employeeRepository.findById(employeeId).map(e -> e.getName() + "_경력기초자료.csv").orElse("경력기초자료.csv");
    }

    /** 투입 이력 CSV: 이력 검색과 같은 조건·권한(경영진), page/size 없이 전체. */
    public String assignmentsCsv(List<AssignmentResponse> rows) {
        List<List<Object>> lines = new ArrayList<>();
        for (AssignmentResponse r : rows) {
            lines.add(List.of(r.employee().employeeName(), r.employee().employeeNo(), r.project().projectName(),
                    ROLE_LABELS.get(r.projectRole()), r.assignmentStartDate() == null ? "" : r.assignmentStartDate(),
                    r.assignmentEndDate() == null ? "" : r.assignmentEndDate()));
        }
        return Csv.content(List.of("직원", "사번", "프로젝트", "역할", "투입 시작일", "투입 종료일"), lines);
    }
}
