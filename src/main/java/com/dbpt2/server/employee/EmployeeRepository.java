package com.dbpt2.server.employee;

import static com.dbpt2.server.status.StatusRules.MEMBER_NOT_ENDED;
import static com.dbpt2.server.status.StatusRules.PROJECT_NOT_CLOSED;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Integer>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    Optional<Employee> findByEmployeeNo(String employeeNo);

    /** 사번 발급: 입사연도로 시작하는 사번 중 가장 큰 값. */
    @Query(value = "SELECT MAX(employee_no) FROM EMPLOYEE WHERE employee_no LIKE CONCAT(:year, '%')", nativeQuery = true)
    Optional<String> findMaxEmployeeNoOfYear(@Param("year") String year);

    /**
     * [집계] 참여 프로젝트가 없는 재직 직원 수. 종료되지 않은 프로젝트의 투입 중·투입 전 배정이 하나도 없는 ACTIVE 직원.
     * 다른 프로젝트에 참여 중이어도 추가 배정은 가능하므로 이 값은 현황 지표로만 쓴다.
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.employmentStatus = com.dbpt2.server.employee.EmploymentStatus.ACTIVE"
            + " AND NOT EXISTS (SELECT 1 FROM ProjectMember m JOIN m.project p"
            + "   WHERE m.employee = e AND " + PROJECT_NOT_CLOSED + " AND " + MEMBER_NOT_ENDED + ")")
    long countActiveWithoutProjects(@Param("asOf") LocalDate asOf);
}
