package com.dbpt2.server.employee;

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
}
