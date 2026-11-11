package com.dbpt2.server.employee;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    Optional<Employee> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    Optional<Employee> findByEmployeeNo(String employeeNo);
}
