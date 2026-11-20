package com.dbpt2.server.member;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.member.MemberDtos.AssignmentResponse;
import com.dbpt2.server.security.AuthUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentQueryService {

    private final AssignmentSearch assignmentSearch;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public record Filter(String employeeName, Integer employeeId, Integer projectId, ProjectRole projectRole,
                         LocalDate asOf, LocalDate from, LocalDate to, boolean ongoingProjectsOnly) {}

    public Page<AssignmentResponse> search(AuthUser user, Filter f, Pageable pageable) {
        accessPolicy.requireManagement(user);
        LocalDate today = TimeConfig.today(clock);
        return assignmentSearch.search(criteria(f, today, false), pageable).map(m -> AssignmentResponse.of(m, statusDate(f, today)));
    }

    public List<AssignmentResponse> searchAll(AuthUser user, Filter f) {
        accessPolicy.requireManagement(user);
        LocalDate today = TimeConfig.today(clock);
        return assignmentSearch.searchAll(criteria(f, today, false)).stream().map(m -> AssignmentResponse.of(m, statusDate(f, today))).toList();
    }

    static AssignmentSearch.Criteria criteria(Filter f, LocalDate today, boolean actualOnly) {
        if (f.from() != null && f.to() != null && f.from().isAfter(f.to())) {
            throw ApiException.invalid("to", "기간의 끝은 시작 이후여야 해요.");
        }
        return new AssignmentSearch.Criteria(f.employeeName(), f.employeeId(), f.projectId(), f.projectRole(), f.asOf(),
                f.from(), f.to(), f.ongoingProjectsOnly(), today, actualOnly);
    }

    static LocalDate statusDate(Filter f, LocalDate today) {
        return f.asOf() != null ? f.asOf() : today;
    }
}
