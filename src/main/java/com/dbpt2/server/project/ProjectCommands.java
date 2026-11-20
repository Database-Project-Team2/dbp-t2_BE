package com.dbpt2.server.project;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public final class ProjectCommands {

    private ProjectCommands() {}

    public record CreateProjectRequest(
            @NotNull(message = "프로젝트명을 입력해 주세요.") String projectName,
            @NotNull(message = "발주처를 선택해 주세요.") Integer clientId,
            @NotNull(message = "실제 착수일을 입력해 주세요.") LocalDate actualStartDate,
            @NotNull(message = "예정 종료일을 입력해 주세요.") LocalDate plannedEndDate) {}

    public record CloseProjectRequest(@NotNull(message = "실제 종료일을 입력해 주세요.") LocalDate actualEndDate) {}

    public record CloseProjectResponse(ProjectDtos.ProjectDetail project, List<Integer> closedMemberIds) {}

    public record ClientOption(int clientId, String clientName) {}
}
