package com.dbpt2.server.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {

    boolean existsByProjectIdAndEmployeeId(Integer projectId, Integer employeeId);

    boolean existsByProjectIdAndEmployeeIdAndRole(Integer projectId, Integer employeeId, ProjectRole role);

    boolean existsByEmployeeIdAndRole(Integer employeeId, ProjectRole role);

    /** 실제 참여자: 투입 시작일이 있고, 종료된 프로젝트면 시작일이 실제 종료일 이전. */
    @Query("""
            SELECT COUNT(m) > 0 FROM ProjectMember m JOIN m.project p
            WHERE p.id = :projectId AND m.employee.id = :employeeId
              AND m.assignmentStartDate IS NOT NULL
              AND (p.actualEndDate IS NULL OR m.assignmentStartDate <= p.actualEndDate)
            """)
    boolean isActualParticipant(@Param("projectId") int projectId, @Param("employeeId") int employeeId);
}
