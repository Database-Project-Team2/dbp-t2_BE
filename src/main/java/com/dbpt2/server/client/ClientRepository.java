package com.dbpt2.server.client;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Integer> {

    /** 직원이 볼 수 있는 발주처: 본인이 구성원(투입 전 포함)인 프로젝트의 발주처. */
    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE p.client.id = :clientId"
            + " AND EXISTS (SELECT 1 FROM ProjectMember m WHERE m.project = p AND m.employee.id = :employeeId)")
    boolean isVisibleTo(@Param("clientId") int clientId, @Param("employeeId") int employeeId);

    /** [집계] 발주처별 프로젝트 수 — 경영진은 전체, 직원은 본인 소속 프로젝트만 센다(viewerId가 null이면 전체). */
    @Query("SELECT p.client.id, COUNT(p) FROM Project p WHERE p.client.id IN :clientIds"
            + " AND (:viewerId IS NULL OR EXISTS (SELECT 1 FROM ProjectMember m WHERE m.project = p AND m.employee.id = :viewerId))"
            + " GROUP BY p.client.id")
    List<Object[]> countProjectsByClient(@Param("clientIds") List<Integer> clientIds, @Param("viewerId") Integer viewerId);
}
