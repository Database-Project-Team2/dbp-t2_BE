package com.dbpt2.server.project;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.member.MemberDtos.MemberResponse;
import com.dbpt2.server.member.ProjectMember;
import com.dbpt2.server.member.ProjectMemberRepository;
import com.dbpt2.server.member.ProjectRole;
import com.dbpt2.server.project.ProjectDtos.ClientContact;
import com.dbpt2.server.project.ProjectDtos.ClientRef;
import com.dbpt2.server.project.ProjectDtos.CustomerEvaluationProgress;
import com.dbpt2.server.project.ProjectDtos.MyMembership;
import com.dbpt2.server.project.ProjectDtos.Permissions;
import com.dbpt2.server.project.ProjectDtos.ProjectDetail;
import com.dbpt2.server.project.ProjectDtos.ProjectListItem;
import com.dbpt2.server.security.AuthUser;
import com.dbpt2.server.status.ProjectStatus;
import com.dbpt2.server.status.StatusRules;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectSearch projectSearch;
    private final ProjectStats stats;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public record ListResult(Page<ProjectListItem> page, Map<ProjectStatus, Long> statusCounts) {}

    public ListResult list(AuthUser user, String projectName, String clientName, ProjectStatus status, LocalDate asOf,
                           LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.invalid("to", "기간의 끝은 시작 이후여야 해요.");
        }
        LocalDate statusDate = asOf != null ? asOf : TimeConfig.today(clock);
        ProjectSearch.Criteria criteria = new ProjectSearch.Criteria(projectName, clientName, status, asOf, from, to,
                accessPolicy.isManagement(user) ? null : user.employeeId(), statusDate);
        Page<Project> page = projectSearch.search(criteria, pageable);
        Map<Integer, ProjectStats.Counts> counts = stats.of(page.map(Project::getId).getContent(), statusDate);
        Map<ProjectStatus, Long> statusCounts = new EnumMap<>(ProjectStatus.class);
        for (ProjectStatus s : ProjectStatus.values()) {
            statusCounts.put(s, 0L);
        }
        projectSearch.searchWithoutStatus(criteria).forEach(p -> statusCounts.merge(StatusRules.projectStatus(p, statusDate), 1L, Long::sum));
        return new ListResult(page.map(p -> toListItem(p, counts.get(p.getId()), statusDate)), statusCounts);
    }

    public ProjectDetail detail(AuthUser user, int projectId) {
        accessPolicy.requireProjectVisible(user, projectId);
        Project p = projectRepository.findWithClient(projectId).orElseThrow(ApiException::notFound);
        LocalDate today = TimeConfig.today(clock);
        ProjectStats.Counts c = stats.of(List.of(projectId), today).get(projectId);
        ProjectMember mine = memberRepository.findByProjectIdAndEmployeeId(projectId, user.employeeId()).orElse(null);
        boolean participant = mine != null && StatusRules.isActualParticipant(mine);
        boolean pm = mine != null && mine.getRole() == ProjectRole.PM;
        MyMembership membership = mine == null ? null : new MyMembership(mine.getId(), mine.getRole(), mine.getAssignmentStartDate(),
                mine.getAssignmentEndDate(), StatusRules.assignmentStatus(mine, today), participant);
        Permissions permissions = new Permissions(accessPolicy.isManagement(user), pm, accessPolicy.isManagement(user) || pm,
                participant && p.getActualEndDate() != null);
        return new ProjectDetail(p.getId(), p.getName(), p.getClient().getId(), p.getActualStartDate(), p.getPlannedEndDate(),
                p.getActualEndDate(), p.getEvaluationRequestedDate(), p.getEvaluationReceivedDate(), p.getEvaluationReportedDate(),
                StatusRules.projectStatus(p, today), StatusRules.workflowStatus(p), ClientContact.of(p.getClient()),
                c.members(), c.current(), membership, permissions, new CustomerEvaluationProgress(c.participants(), c.clientEvaluated()));
    }

    public List<MemberResponse> members(AuthUser user, int projectId, LocalDate asOf) {
        accessPolicy.requireProjectVisible(user, projectId);
        if (!projectRepository.existsById(projectId)) {
            throw ApiException.notFound();
        }
        LocalDate date = asOf != null ? asOf : TimeConfig.today(clock);
        return memberRepository.findTeam(projectId).stream().map(m -> MemberResponse.of(m, date)).toList();
    }

    static ProjectListItem toListItem(Project p, ProjectStats.Counts c, LocalDate asOf) {
        return new ProjectListItem(p.getId(), p.getName(), p.getClient().getId(), p.getActualStartDate(), p.getPlannedEndDate(),
                p.getActualEndDate(), p.getEvaluationRequestedDate(), p.getEvaluationReceivedDate(), p.getEvaluationReportedDate(),
                StatusRules.projectStatus(p, asOf), StatusRules.workflowStatus(p), ClientRef.of(p.getClient()),
                c == null ? 0 : c.members(), c == null ? 0 : c.current());
    }
}
