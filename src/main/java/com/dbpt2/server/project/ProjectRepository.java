package com.dbpt2.server.project;

import static com.dbpt2.server.status.StatusRules.PROJECT_IN_PROGRESS;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    @Query("SELECT p FROM Project p JOIN FETCH p.client WHERE p.id = :projectId")
    Optional<Project> findWithClient(@Param("projectId") int projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :projectId")
    Optional<Project> findByIdForUpdate(@Param("projectId") int projectId);

    /** [집계] 기준일에 진행 중인 프로젝트 수(QRY-001). 예정 종료일이 지나도 실제 종료 전이면 진행 중. */
    @Query("SELECT COUNT(p) FROM Project p WHERE " + PROJECT_IN_PROGRESS)
    long countInProgress(@Param("asOf") LocalDate asOf);

    /** [기간] 기준일에 진행 중인 프로젝트. */
    @Query("SELECT p FROM Project p JOIN FETCH p.client WHERE " + PROJECT_IN_PROGRESS + " ORDER BY p.actualStartDate, p.id")
    List<Project> findInProgress(@Param("asOf") LocalDate asOf);

    /** [집계] 고객평가 보고 대기: 실제 종료됐지만 회사 보고 전인 프로젝트. */
    @Query("SELECT p FROM Project p JOIN FETCH p.client"
            + " WHERE p.actualEndDate IS NOT NULL AND p.evaluationReportedDate IS NULL ORDER BY p.actualEndDate, p.id")
    List<Project> findPendingCustomerEvaluation();

    @Query("SELECT p FROM Project p JOIN FETCH p.client ORDER BY p.actualStartDate, p.id")
    List<Project> findAllWithClient();
}
