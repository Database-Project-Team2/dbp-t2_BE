package com.dbpt2.server.member;

import static com.dbpt2.server.status.StatusRules.ACTUAL_PARTICIPANT;
import static com.dbpt2.server.status.StatusRules.MEMBER_ASSIGNED;
import static com.dbpt2.server.status.StatusRules.MEMBER_NOT_ENDED;
import static com.dbpt2.server.status.StatusRules.PROJECT_IN_PROGRESS;
import static com.dbpt2.server.status.StatusRules.PROJECT_NOT_CLOSED;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {

    boolean existsByProjectIdAndEmployeeId(Integer projectId, Integer employeeId);

    boolean existsByProjectIdAndEmployeeIdAndRole(Integer projectId, Integer employeeId, ProjectRole role);

    boolean existsByEmployeeIdAndRole(Integer employeeId, ProjectRole role);

    Optional<ProjectMember> findByProjectIdAndEmployeeId(Integer projectId, Integer employeeId);

    /** 실제 참여자: 투입 시작일이 있고, 종료된 프로젝트면 시작일이 실제 종료일 이전. */
    @Query("SELECT COUNT(m) > 0 FROM ProjectMember m JOIN m.project p"
            + " WHERE p.id = :projectId AND m.employee.id = :employeeId AND " + ACTUAL_PARTICIPANT)
    boolean isActualParticipant(@Param("projectId") int projectId, @Param("employeeId") int employeeId);

    /** 프로젝트 구성원 목록(직원 정보 함께 조회). */
    @Query("SELECT m FROM ProjectMember m JOIN FETCH m.employee JOIN FETCH m.project p"
            + " WHERE p.id = :projectId ORDER BY m.id")
    List<ProjectMember> findTeam(@Param("projectId") int projectId);

    /** [집계] 프로젝트별 전체 구성원 수와 기준일 투입 인원(QRY-002). 투입 전은 투입 인원에서 제외. */
    @Query("SELECT m.project.id, COUNT(m), SUM(CASE WHEN " + MEMBER_ASSIGNED + " THEN 1 ELSE 0 END)"
            + " FROM ProjectMember m WHERE m.project.id IN :projectIds GROUP BY m.project.id")
    List<Object[]> countMembersByProject(@Param("projectIds") Collection<Integer> projectIds, @Param("asOf") LocalDate asOf);

    /** [집계] 프로젝트별 실제 참여자 수(고객평가 대상 수). */
    @Query("SELECT p.id, COUNT(m) FROM ProjectMember m JOIN m.project p"
            + " WHERE p.id IN :projectIds AND " + ACTUAL_PARTICIPANT + " GROUP BY p.id")
    List<Object[]> countParticipantsByProject(@Param("projectIds") Collection<Integer> projectIds);

    /** [기간] 기준일에 진행 중인 프로젝트에 투입 중인 구성원(QRY-004 과거 시점 투입 현황). */
    @Query("SELECT m FROM ProjectMember m JOIN FETCH m.employee JOIN FETCH m.project p"
            + " WHERE " + PROJECT_IN_PROGRESS + " AND " + MEMBER_ASSIGNED + " ORDER BY p.id, m.id")
    List<ProjectMember> findAssignedInOngoingProjects(@Param("asOf") LocalDate asOf);

    /** [기간] 직원의 참여 중 배정: 종료되지 않은 프로젝트의 투입 중/투입 전 배정. */
    @Query("SELECT m FROM ProjectMember m JOIN FETCH m.project p JOIN FETCH p.client"
            + " WHERE m.employee.id = :employeeId AND " + PROJECT_NOT_CLOSED + " AND " + MEMBER_NOT_ENDED
            + " ORDER BY p.actualStartDate, p.id")
    List<ProjectMember> findOpenMemberships(@Param("employeeId") int employeeId, @Param("asOf") LocalDate asOf);

    /** 직원의 모든 배정(프로젝트 함께 조회). */
    @Query("SELECT m FROM ProjectMember m JOIN FETCH m.project p JOIN FETCH p.client"
            + " WHERE m.employee.id = :employeeId ORDER BY p.actualStartDate, p.id")
    List<ProjectMember> findByEmployee(@Param("employeeId") int employeeId);

    /** [집계] 직원별 기준일 투입 중인 배정 수(직원 검색의 current_assignment_count). */
    @Query("SELECT m.employee.id, COUNT(m) FROM ProjectMember m"
            + " WHERE m.employee.id IN :employeeIds AND " + MEMBER_ASSIGNED + " GROUP BY m.employee.id")
    List<Object[]> countAssignmentsByEmployee(@Param("employeeIds") Collection<Integer> employeeIds, @Param("asOf") LocalDate asOf);

    /** 직원이 PM인 프로젝트 ID(의견 공개 범위 판정). */
    @Query("SELECT m.project.id FROM ProjectMember m WHERE m.employee.id = :employeeId"
            + " AND m.role = com.dbpt2.server.member.ProjectRole.PM")
    List<Integer> findPmProjectIds(@Param("employeeId") int employeeId);

    /** 구성원의 프로젝트 ID만 읽는다(엔티티를 영속성 컨텍스트에 올리지 않아, 잠금 후 다시 읽을 때 최신 값을 본다). */
    @Query("SELECT m.project.id FROM ProjectMember m WHERE m.id = :memberId")
    Optional<Integer> findProjectId(@Param("memberId") int memberId);

    /** 구성원 변경(기간 수정·삭제) 시 프로젝트 잠금 다음에 구성원 행을 잠그고 최신 상태로 읽는다(SELECT ... FOR UPDATE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM ProjectMember m WHERE m.id = :memberId")
    Optional<ProjectMember> findByIdForUpdate(@Param("memberId") int memberId);
}
