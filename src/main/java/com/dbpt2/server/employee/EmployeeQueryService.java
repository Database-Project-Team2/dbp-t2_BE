package com.dbpt2.server.employee;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.crypto.ResidentNoCipher;
import com.dbpt2.server.member.ProjectMemberRepository;
import com.dbpt2.server.security.AuthUser;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeQueryService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeSearch employeeSearch;
    private final ProjectMemberRepository memberRepository;
    private final AccessPolicy accessPolicy;
    private final ResidentNoCipher cipher;
    private final Clock clock;

    /** 직원 검색 결과: 직원 응답 + 오늘 투입 중인 배정 수. */
    public record EmployeeListItem(@JsonUnwrapped EmployeeResponse employee, long currentAssignmentCount) {}

    public Page<EmployeeListItem> search(AuthUser user, EmployeeSearch.Criteria criteria, Pageable pageable) {
        accessPolicy.requireManagement(user);
        if (criteria.minCareerMonths() != null && criteria.minCareerMonths() < 0) {
            throw ApiException.invalid("min_career_months", "최소 경력은 0 이상이어야 해요.");
        }
        Page<Employee> page = employeeSearch.search(criteria, pageable);
        Map<Integer, Long> counts = assignmentCounts(page.map(Employee::getId).getContent(), criteria.asOf());
        return page.map(e -> new EmployeeListItem(EmployeeResponse.of(e, cipher), counts.getOrDefault(e.getId(), 0L)));
    }

    public LocalDate today() {
        return TimeConfig.today(clock);
    }

    /** 경영진 또는 본인. 다른 직원 정보는 존재 여부도 숨긴다(404). */
    public EmployeeResponse detail(AuthUser user, int employeeId) {
        if (!accessPolicy.isManagement(user) && user.employeeId() != employeeId) {
            throw ApiException.notFound();
        }
        return employeeRepository.findById(employeeId).map(e -> EmployeeResponse.of(e, cipher)).orElseThrow(ApiException::notFound);
    }

    Map<Integer, Long> assignmentCounts(List<Integer> ids, LocalDate asOf) {
        Map<Integer, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : memberRepository.countAssignmentsByEmployee(ids, asOf)) {
                counts.put((Integer) row[0], ((Number) row[1]).longValue());
            }
        }
        return counts;
    }
}
