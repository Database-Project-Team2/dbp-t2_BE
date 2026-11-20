package com.dbpt2.server.project;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.employee.EmployeeSearch;
import com.dbpt2.server.member.MemberCandidate;
import com.dbpt2.server.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberCandidateService {

    private final ProjectRepository projectRepository;
    private final EmployeeSearch employeeSearch;
    private final AccessPolicy accessPolicy;

    public Page<MemberCandidate> candidates(AuthUser user, int projectId, EmployeeSearch.Criteria criteria, Pageable pageable) {
        accessPolicy.requireManagement(user);
        if (!projectRepository.existsById(projectId)) {
            throw ApiException.notFound();
        }
        return employeeSearch.search(criteria, pageable).map(MemberCandidate::of);
    }
}
