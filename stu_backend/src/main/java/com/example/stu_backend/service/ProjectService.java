package com.example.stu_backend.service;

import com.example.stu_backend.entity.Project;
import com.example.stu_backend.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ExperimentMailService experimentMailService;

    public ProjectService(ProjectRepository projectRepository, ExperimentMailService experimentMailService) {
        this.projectRepository = projectRepository;
        this.experimentMailService = experimentMailService;
    }

    public List<Project> getAllProjects(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("课程ID不能为空");
        }
        return projectRepository.getAllProjects(courseId.trim());
    }

    public List<Project> getProjects(String courseId, int status) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("课程ID不能为空");
        }
        if (status < 0 || status > 2) {
            throw new IllegalArgumentException("状态仅支持 0/1/2");
        }
        return projectRepository.getProjects(courseId.trim(), status);
    }

    public void updateProjectStatus(String courseId, String projectId, String status, String deadlineAt) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("课程ID不能为空");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        int value = mapStatus(status);
        LocalDateTime deadline = parseDeadline(deadlineAt);
        if (value == 1 && deadline == null) {
            throw new IllegalArgumentException("开放项目时必须设置截止时间");
        }
        if (value != 1) {
            deadline = null;
        }
        String normalizedCourseId = courseId.trim();
        String normalizedProjectId = projectId.trim();
        int updated = projectRepository.updateProjectStatus(normalizedCourseId, normalizedProjectId, value, deadline);
        if (updated <= 0) {
            throw new IllegalArgumentException("未找到要更新的项目");
        }
        if (value == 1) {
            Project project = projectRepository.findByCourseAndProjectId(normalizedCourseId, normalizedProjectId)
                    .orElseThrow(() -> new IllegalArgumentException("未找到要更新的项目"));
            experimentMailService.notifyProjectPublished(
                    normalizedCourseId,
                    normalizedProjectId,
                    project.getProjectName(),
                    deadline
            );
        }
    }

    public int notifyProjectStudents(String courseId, String projectId) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("课程ID不能为空");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        return experimentMailService.notifyProjectManual(courseId.trim(), projectId.trim());
    }

    private static int mapStatus(String status) {
        if ("open".equalsIgnoreCase(status)) return 1;
        if ("close".equalsIgnoreCase(status)) return 0;
        if ("deadline".equalsIgnoreCase(status)) return 2;
        throw new IllegalArgumentException("状态仅支持 open/close/deadline");
    }

    private static LocalDateTime parseDeadline(String deadlineAt) {
        if (deadlineAt == null || deadlineAt.isBlank()) {
            return null;
        }
        try {
            LocalDateTime deadline = LocalDateTime.parse(deadlineAt.trim());
            if (deadline.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("截止时间必须晚于当前时间");
            }
            return deadline;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("截止时间格式错误，需为 ISO-8601 格式");
        }
    }
}
