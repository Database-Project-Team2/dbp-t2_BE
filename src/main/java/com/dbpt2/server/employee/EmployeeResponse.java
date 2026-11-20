package com.dbpt2.server.employee;

import com.dbpt2.server.crypto.ResidentNoCipher;
import java.time.LocalDate;

/** 직원 응답. 비밀번호 해시·주민번호 원문/암호문은 넣지 않는다. */
public record EmployeeResponse(
        int employeeId,
        String employeeNo,
        String employeeName,
        String education,
        EmploymentStatus employmentStatus,
        LocalDate hireDate,
        LocalDate resignationDate,
        int totalCareerMonths,
        String skillDescription,
        JobCategory jobCategory,
        String loginId,
        SystemRole systemRole,
        String residentNoMasked) {

    public static EmployeeResponse of(Employee e, ResidentNoCipher cipher) {
        return new EmployeeResponse(e.getId(), e.getEmployeeNo(), e.getName(), e.getEducation(), e.getEmploymentStatus(),
                e.getHireDate(), e.getResignationDate(), e.getTotalCareerMonths(), e.getSkillDescription(),
                e.getJobCategory(), e.getLoginId(), e.getSystemRole(), cipher.mask(e.getResidentNoEncrypted()));
    }
}
