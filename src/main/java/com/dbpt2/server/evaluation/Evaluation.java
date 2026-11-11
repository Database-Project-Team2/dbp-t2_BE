package com.dbpt2.server.evaluation;

import com.dbpt2.server.member.ProjectMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * EVALUATION — 고객·PM·동료평가. PROJECT_MEMBER를 세 번 참조한다.
 * <ul>
 *   <li>target: 피평가 구성원</li>
 *   <li>evaluator: 내부 평가자(고객평가는 NULL)</li>
 *   <li>registeredBy: 등록자(고객평가는 해당 프로젝트 PM, 내부평가는 평가자 본인)</li>
 * </ul>
 * 고객평가의 대상당 1건 제한은 UNIQUE로 막히지 않으므로(evaluator NULL) 서버에서 검증해야 한다.
 */
@Entity
@Table(
        name = "EVALUATION",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_internal_evaluation",
                columnNames = {"target_member_id", "evaluation_type", "evaluator_member_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_member_id", nullable = false, updatable = false)
    private ProjectMember target;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false, updatable = false, columnDefinition = "VARCHAR(20)")
    private EvaluationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_member_id", updatable = false)
    private ProjectMember evaluator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by_member_id", nullable = false, updatable = false)
    private ProjectMember registeredBy;

    @Column(name = "evaluated_date", nullable = false)
    private LocalDate evaluatedDate;

    @Column(name = "work_performance_score", nullable = false)
    private int workPerformanceScore;

    @Column(name = "work_performance_comment", nullable = false, columnDefinition = "TEXT")
    private String workPerformanceComment;

    @Column(name = "communication_score", nullable = false)
    private int communicationScore;

    @Column(name = "communication_comment", nullable = false, columnDefinition = "TEXT")
    private String communicationComment;
}
