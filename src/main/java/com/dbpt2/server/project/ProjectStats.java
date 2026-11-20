package com.dbpt2.server.project;

import com.dbpt2.server.evaluation.EvaluationRepository;
import com.dbpt2.server.member.ProjectMemberRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 프로젝트 목록·상세에 붙는 집계(구성원 수, 투입 인원, 고객평가 진행)를 프로젝트 묶음 단위로 한 번에 조회한다. */
@Component
@RequiredArgsConstructor
public class ProjectStats {

    private final ProjectMemberRepository memberRepository;
    private final EvaluationRepository evaluationRepository;

    public record Counts(long members, long current, long participants, long clientEvaluated) {}

    public Map<Integer, Counts> of(Collection<Integer> projectIds, LocalDate asOf) {
        Map<Integer, Counts> result = new HashMap<>();
        if (projectIds.isEmpty()) {
            return result;
        }
        Map<Integer, long[]> raw = new HashMap<>();
        for (Integer id : projectIds) {
            raw.put(id, new long[4]);
        }
        for (Object[] row : memberRepository.countMembersByProject(projectIds, asOf)) {
            long[] c = raw.get((Integer) row[0]);
            c[0] = ((Number) row[1]).longValue();
            c[1] = row[2] == null ? 0 : ((Number) row[2]).longValue();
        }
        for (Object[] row : memberRepository.countParticipantsByProject(projectIds)) {
            raw.get((Integer) row[0])[2] = ((Number) row[1]).longValue();
        }
        for (Object[] row : evaluationRepository.countClientEvaluatedByProject(projectIds)) {
            raw.get((Integer) row[0])[3] = ((Number) row[1]).longValue();
        }
        raw.forEach((id, c) -> result.put(id, new Counts(c[0], c[1], c[2], c[3])));
        return result;
    }
}
