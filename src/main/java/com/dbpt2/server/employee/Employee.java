package com.dbpt2.server.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * EMPLOYEE — 직원 기본정보 + 로그인 계정 + 시스템 권한 (ERD v1.5).
 *
 * <p>매핑 규칙(모든 엔티티 공통)
 * <ul>
 *   <li>테이블·컬럼 이름을 명시한다. 이름 변환 없이 스키마 그대로 쓴다(application.yml naming 참고).</li>
 *   <li>enum 컬럼은 columnDefinition으로 VARCHAR를 명시한다. Hibernate 6은 MySQL에서 enum을 네이티브 ENUM으로 기대해
 *       VARCHAR 컬럼과 validate가 실패하기 때문이다.</li>
 *   <li>TEXT 컬럼도 같은 이유로 columnDefinition = "TEXT"를 명시한다.</li>
 * </ul>
 */
@Entity
@Table(name = "EMPLOYEE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer id;

    /** 사번: 입사연도 4자리 + 연도별 순번 4자리. 서버 발급, 수정 불가. */
    @Column(name = "employee_no", nullable = false, unique = true, length = 8, updatable = false)
    private String employeeNo;

    @Column(name = "employee_name", nullable = false, length = 100)
    private String name;

    /** 암호화 저장, 응답에서는 마스킹. */
    @Column(name = "resident_no_encrypted", nullable = false)
    private String residentNoEncrypted;

    @Column(name = "education", nullable = false, length = 100)
    private String education;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, columnDefinition = "VARCHAR(20)")
    private EmploymentStatus employmentStatus;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    /** RESIGNED 상태에서만 값이 있다. */
    @Column(name = "resignation_date")
    private LocalDate resignationDate;

    @Column(name = "total_career_months", nullable = false)
    private int totalCareerMonths;

    @Column(name = "skill_description", columnDefinition = "TEXT")
    private String skillDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_category", nullable = false, columnDefinition = "VARCHAR(30)")
    private JobCategory jobCategory;

    /** 로그인 ID: 중복 불가, 수정 불가. */
    @Column(name = "login_id", nullable = false, unique = true, length = 100, updatable = false)
    private String loginId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** 가입 시 선택 불가. 초기 데이터 또는 운영 관리자만 변경. */
    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false, columnDefinition = "VARCHAR(30)")
    private SystemRole systemRole;
}
