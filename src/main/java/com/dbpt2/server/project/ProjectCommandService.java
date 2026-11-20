package com.dbpt2.server.project;

import com.dbpt2.server.access.AccessPolicy;
import com.dbpt2.server.client.Client;
import com.dbpt2.server.client.ClientRepository;
import com.dbpt2.server.common.ApiException;
import com.dbpt2.server.common.ErrorCode;
import com.dbpt2.server.common.PatchBody;
import com.dbpt2.server.common.TimeConfig;
import com.dbpt2.server.member.ProjectMember;
import com.dbpt2.server.member.ProjectMemberRepository;
import com.dbpt2.server.project.ProjectCommands.ClientOption;
import com.dbpt2.server.project.ProjectCommands.CloseProjectResponse;
import com.dbpt2.server.project.ProjectCommands.CreateProjectRequest;
import com.dbpt2.server.security.AuthUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프로젝트 등록·수정·종료(경영진 전용). 프로젝트 행을 잠근 뒤 검사하고 바꾼다. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ClientRepository clientRepository;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public int create(AuthUser user, CreateProjectRequest request) {
        accessPolicy.requireManagement(user);
        String name = validName(request.projectName());
        Client client = client(request.clientId());
        validatePeriod(request.actualStartDate(), request.plannedEndDate());
        return projectRepository.save(Project.create(name, client, request.actualStartDate(), request.plannedEndDate())).getId();
    }

    public void update(AuthUser user, int projectId, PatchBody patch) {
        accessPolicy.requireManagement(user);
        Project project = lock(projectId);
        if (project.isClosed()) {
            throw new ApiException(ErrorCode.PROJECT_CLOSED, "이미 종료한 프로젝트는 변경할 수 없어요.");
        }
        String name = patch.has("project_name") ? validName(patch.text("project_name", false)) : project.getName();
        Client client = patch.has("client_id") ? client(patch.integer("client_id")) : project.getClient();
        LocalDate start = patch.has("actual_start_date") ? patch.date("actual_start_date", false) : project.getActualStartDate();
        LocalDate plannedEnd = patch.has("planned_end_date") ? patch.date("planned_end_date", false) : project.getPlannedEndDate();
        validatePeriod(start, plannedEnd);
        for (ProjectMember m : memberRepository.findTeam(projectId)) {
            if (m.getAssignmentStartDate() != null && m.getAssignmentStartDate().isBefore(start)) {
                throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "actual_start_date",
                        "기존 구성원의 투입 시작일(" + m.getAssignmentStartDate() + ")보다 늦게 착수일을 변경할 수 없어요.");
            }
        }
        project.update(name, client, start, plannedEnd);
    }

    /** 종료: 한 트랜잭션에서 프로젝트 종료일과 투입 중 구성원의 종료일을 함께 기록한다. 투입 전 구성원은 그대로 둔다. */
    public List<Integer> close(AuthUser user, int projectId, LocalDate actualEndDate) {
        accessPolicy.requireManagement(user);
        Project project = lock(projectId);
        if (project.isClosed()) {
            throw new ApiException(ErrorCode.PROJECT_CLOSED, "이미 종료한 프로젝트예요.");
        }
        if (actualEndDate.isBefore(project.getActualStartDate())) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "actual_end_date", "실제 종료일은 착수일 이후여야 해요.");
        }
        if (actualEndDate.isAfter(today())) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "actual_end_date", "실제 종료일은 오늘 이후일 수 없어요.");
        }
        List<ProjectMember> team = memberRepository.findTeam(projectId);
        for (ProjectMember m : team) {
            boolean startsAfter = m.getAssignmentStartDate() != null && m.getAssignmentStartDate().isAfter(actualEndDate);
            boolean endsAfter = m.getAssignmentEndDate() != null && m.getAssignmentEndDate().isAfter(actualEndDate);
            if (startsAfter || endsAfter) {
                throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "actual_end_date",
                        "구성원의 투입기간보다 먼저 프로젝트를 종료할 수 없어요.");
            }
        }
        project.close(actualEndDate);
        List<Integer> closed = new ArrayList<>();
        for (ProjectMember m : team) {
            if (m.endWithProject(actualEndDate)) {
                closed.add(m.getId());
            }
        }
        return closed;
    }

    @Transactional(readOnly = true)
    public List<ClientOption> clientOptions(AuthUser user, int projectId, String clientName) {
        accessPolicy.requireManagement(user);
        if (!projectRepository.existsById(projectId)) {
            throw ApiException.notFound();
        }
        String term = clientName == null ? "" : clientName.trim().toLowerCase(Locale.ROOT);
        return clientRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase(Locale.ROOT).contains(term))
                .map(c -> new ClientOption(c.getId(), c.getName()))
                .toList();
    }

    private Project lock(int projectId) {
        return projectRepository.findByIdForUpdate(projectId).orElseThrow(ApiException::notFound);
    }

    private Client client(Integer clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> ApiException.field(ErrorCode.NOT_FOUND, "client_id", "발주처를 찾을 수 없어요."));
    }

    private static String validName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.length() > 200) {
            throw ApiException.invalid("project_name", "프로젝트명을 1~200자로 입력해 주세요.");
        }
        return name;
    }

    private void validatePeriod(LocalDate start, LocalDate plannedEnd) {
        if (start.isAfter(today())) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "actual_start_date", "실제 착수일은 오늘 이후일 수 없어요.");
        }
        if (plannedEnd.isBefore(start)) {
            throw ApiException.field(ErrorCode.BUSINESS_RULE_VIOLATION, "planned_end_date", "예정 종료일은 착수일 이후여야 해요.");
        }
    }

    private LocalDate today() {
        return TimeConfig.today(clock);
    }
}
