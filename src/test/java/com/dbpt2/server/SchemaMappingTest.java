package com.dbpt2.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbpt2.server.client.ClientRepository;
import com.dbpt2.server.employee.Employee;
import com.dbpt2.server.employee.EmployeeRepository;
import com.dbpt2.server.employee.EmploymentStatus;
import com.dbpt2.server.evaluation.Evaluation;
import com.dbpt2.server.evaluation.EvaluationRepository;
import com.dbpt2.server.evaluation.EvaluationType;
import com.dbpt2.server.member.ProjectMemberRepository;
import com.dbpt2.server.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * docker compose로 띄운 MySQL(시드 적재 상태)에 붙어 실행한다.
 * 컨텍스트가 뜨면 ddl-auto=validate를 통과한 것이고, 아래 검사로 시드가 엔티티로 읽히는지 확인한다.
 */
@SpringBootTest
@Transactional
class SchemaMappingTest {

    @Autowired EmployeeRepository employeeRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectMemberRepository projectMemberRepository;
    @Autowired EvaluationRepository evaluationRepository;

    @Test
    void 시드_데이터_건수가_스크립트와_같다() {
        assertThat(employeeRepository.count()).isEqualTo(10);
        assertThat(clientRepository.count()).isEqualTo(3);
        assertThat(projectRepository.count()).isEqualTo(4);
        assertThat(projectMemberRepository.count()).isEqualTo(20);
        assertThat(evaluationRepository.count()).isEqualTo(34);
    }

    @Test
    void enum과_날짜_컬럼이_읽힌다() {
        Employee resigned = employeeRepository.findByLoginId("demo10").orElseThrow();
        assertThat(resigned.getEmploymentStatus()).isEqualTo(EmploymentStatus.RESIGNED);
        assertThat(resigned.getResignationDate()).isNotNull();
    }

    @Test
    void 평가에서_구성원_프로젝트_발주처까지_따라간다() {
        Evaluation clientEvaluation = evaluationRepository.findById(1).orElseThrow();
        assertThat(clientEvaluation.getType()).isEqualTo(EvaluationType.CLIENT);
        assertThat(clientEvaluation.getEvaluator()).isNull();
        assertThat(clientEvaluation.getTarget().getProject().getClient().getContactName()).isEqualTo("김담당");
    }
}
