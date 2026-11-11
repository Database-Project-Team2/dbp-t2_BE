package com.dbpt2.server.project;

import com.dbpt2.server.client.Client;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PROJECT — 실제 착수한 프로젝트만 등록한다.
 * 수행 중 = actualStartDate ≤ 기준일 AND actualEndDate IS NULL (예정 종료일 경과와 무관).
 */
@Entity
@Table(name = "PROJECT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Integer id;

    @Column(name = "project_name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "actual_start_date", nullable = false)
    private LocalDate actualStartDate;

    @Column(name = "planned_end_date", nullable = false)
    private LocalDate plannedEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    /** 고객평가 의뢰일 ≤ 회수일 ≤ 보고일 (DB CHECK). */
    @Column(name = "evaluation_requested_date")
    private LocalDate evaluationRequestedDate;

    @Column(name = "evaluation_received_date")
    private LocalDate evaluationReceivedDate;

    @Column(name = "evaluation_reported_date")
    private LocalDate evaluationReportedDate;
}
