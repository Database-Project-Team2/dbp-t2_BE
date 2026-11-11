package com.dbpt2.server.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

    /** 고객평가 중복 검사용 — DB UNIQUE로는 막히지 않는다. */
    boolean existsByTargetIdAndType(Integer targetMemberId, EvaluationType type);
}
