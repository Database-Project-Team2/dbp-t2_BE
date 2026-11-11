package com.dbpt2.server.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {

    boolean existsByProjectIdAndEmployeeId(Integer projectId, Integer employeeId);
}
