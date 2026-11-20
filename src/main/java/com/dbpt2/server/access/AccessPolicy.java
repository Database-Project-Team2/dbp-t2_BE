package com.dbpt2.server.access;

import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.member.ProjectMemberRepository;
import com.dbpt2.server.member.ProjectRole;
import com.dbpt2.server.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 요청별 권한 판정을 한 곳에 모은다.
 * <ul>
 *   <li>경영진: system_role = MANAGEMENT</li>
 *   <li>구성원: 해당 프로젝트 PROJECT_MEMBER 행이 있다(투입 전 포함)</li>
 *   <li>PM: 해당 프로젝트의 project_role = PM (다른 프로젝트 PM 여부와 무관)</li>
 *   <li>실제 참여자: 투입 시작일이 있고, 종료된 프로젝트면 시작일 ≤ 실제 종료일</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AccessPolicy {

    private final ProjectMemberRepository memberRepository;

    public boolean isManagement(AuthUser user) {
        return user.isManagement();
    }

    public boolean isMemberOf(AuthUser user, int projectId) {
        return memberRepository.existsByProjectIdAndEmployeeId(projectId, user.employeeId());
    }

    public boolean isPmOf(AuthUser user, int projectId) {
        return memberRepository.existsByProjectIdAndEmployeeIdAndRole(projectId, user.employeeId(), ProjectRole.PM);
    }

    public boolean isActualParticipantOf(AuthUser user, int projectId) {
        return memberRepository.isActualParticipant(projectId, user.employeeId());
    }

    public boolean hasPmAssignment(AuthUser user) {
        return memberRepository.existsByEmployeeIdAndRole(user.employeeId(), ProjectRole.PM);
    }

    /** 경영진이 아니면 403. */
    public void requireManagement(AuthUser user) {
        if (!isManagement(user)) {
            throw ApiException.forbidden();
        }
    }

    /** 경영진 또는 구성원만 조회. 범위 밖이면 존재 여부도 숨기도록 404. */
    public void requireProjectVisible(AuthUser user, int projectId) {
        if (!isManagement(user) && !isMemberOf(user, projectId)) {
            throw ApiException.notFound();
        }
    }
}
