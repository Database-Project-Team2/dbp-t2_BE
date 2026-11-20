package com.dbpt2.server.member;

import com.dbpt2.server.employee.Employee;
import com.dbpt2.server.employee.JobCategory;
import java.time.LocalDate;

/** 구성원 후보: 선택에 필요한 정보만. 주민번호·계정 정보는 넣지 않는다. */
public record MemberCandidate(int employeeId, String employeeNo, String employeeName, JobCategory jobCategory,
                              String skillDescription, LocalDate hireDate, int totalCareerMonths) {

    public static MemberCandidate of(Employee e) {
        return new MemberCandidate(e.getId(), e.getEmployeeNo(), e.getName(), e.getJobCategory(), e.getSkillDescription(),
                e.getHireDate(), e.getTotalCareerMonths());
    }
}
