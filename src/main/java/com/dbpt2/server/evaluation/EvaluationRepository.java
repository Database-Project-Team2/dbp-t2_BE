package com.dbpt2.server.evaluation;

import static com.dbpt2.server.status.StatusRules.ACTUAL_PARTICIPANT;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

    /** 고객평가 중복 검사용 — DB UNIQUE로는 막히지 않는다. */
    boolean existsByTargetIdAndType(Integer targetMemberId, EvaluationType type);

    boolean existsByTargetIdAndTypeAndEvaluatorId(Integer targetMemberId, EvaluationType type, Integer evaluatorMemberId);

    /** [집계] 프로젝트별 고객평가를 받은 실제 참여자 수(고객평가 회수 진행률). */
    @Query("SELECT p.id, COUNT(DISTINCT m.id) FROM Evaluation ev JOIN ev.target m JOIN m.project p"
            + " WHERE p.id IN :projectIds AND ev.type = com.dbpt2.server.evaluation.EvaluationType.CLIENT"
            + " AND " + ACTUAL_PARTICIPANT + " GROUP BY p.id")
    List<Object[]> countClientEvaluatedByProject(@Param("projectIds") Collection<Integer> projectIds);

    /** 프로젝트의 평가 전체(대상·평가자 함께). 평가 가능 대상 계산용. */
    @Query("SELECT ev FROM Evaluation ev JOIN FETCH ev.target t LEFT JOIN FETCH ev.evaluator WHERE t.project.id IN :projectIds")
    List<Evaluation> findByProjects(@Param("projectIds") Collection<Integer> projectIds);
}
