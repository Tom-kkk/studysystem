package com.example.stu_backend.repository;

import com.example.stu_backend.entity.Project;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 对齐文档中的 ProjectDAO。
 */
@Repository
public class ProjectRepository {

    private static final String TABLE = "tb_Project";

    private static final RowMapper<Project> PROJECT_ROW_MAPPER = (rs, rowNum) -> {
        Project project = new Project();
        project.setCourseId(rs.getString("course_id"));
        project.setProjectId(rs.getString("project_id"));
        project.setProjectName(rs.getString("project_name"));
        int open = rs.getInt("project_open");
        project.setProjectOpen(rs.wasNull() ? null : open);
        project.setProjectDeadline(rs.getTimestamp("project_deadline") == null
                ? null
                : rs.getTimestamp("project_deadline").toLocalDateTime());
        int reminded = rs.getInt("deadline_reminder_sent");
        project.setDeadlineReminderSent(!rs.wasNull() && reminded == 1);
        return project;
    };

    private final JdbcTemplate jdbcTemplate;

    public ProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Project> getProjects(String courseId, int status) {
        String sql = "SELECT project_id, project_name, course_id, project_open, project_deadline, deadline_reminder_sent FROM "
                + TABLE + " WHERE course_id = ? AND project_open = ? ORDER BY id";
        return jdbcTemplate.query(sql, PROJECT_ROW_MAPPER, courseId, status);
    }

    public List<Project> getAllProjects(String courseId) {
        String sql = "SELECT project_id, project_name, course_id, project_open, project_deadline, deadline_reminder_sent FROM "
                + TABLE + " WHERE course_id = ? ORDER BY id";
        return jdbcTemplate.query(sql, PROJECT_ROW_MAPPER, courseId);
    }

    public int updateProjectStatus(String courseId, String projectId, int status, LocalDateTime projectDeadline) {
        String sql = "UPDATE " + TABLE + " SET project_open = ?, project_deadline = ?, deadline_reminder_sent = ? "
                + "WHERE course_id = ? AND project_id = ?";
        int reminderSent = projectDeadline == null ? 1 : 0;
        return jdbcTemplate.update(sql, status, projectDeadline, reminderSent, courseId, projectId);
    }

    public Optional<Project> findByCourseAndProjectId(String courseId, String projectId) {
        String sql = "SELECT project_id, project_name, course_id, project_open, project_deadline, deadline_reminder_sent FROM "
                + TABLE + " WHERE course_id = ? AND project_id = ?";
        List<Project> list = jdbcTemplate.query(sql, PROJECT_ROW_MAPPER, courseId, projectId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public void replaceProjects(String courseId, List<String> projectNames) {
        jdbcTemplate.update("DELETE FROM " + TABLE + " WHERE course_id = ?", courseId);
        String sql = "INSERT INTO " + TABLE
                + "(project_id, project_name, course_id, project_open, project_deadline, deadline_reminder_sent) "
                + "VALUES(?,?,?,?,?,?)";
        for (int i = 0; i < projectNames.size(); i++) {
            String projectId = "P" + (i + 1);
            jdbcTemplate.update(sql, projectId, projectNames.get(i), courseId, 0, null, 1);
        }
    }

    public List<Project> findOpenProjectsForReminder(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT project_id, project_name, course_id, project_open, project_deadline, deadline_reminder_sent FROM "
                + TABLE + " WHERE project_open = 1 AND project_deadline IS NOT NULL "
                + "AND deadline_reminder_sent = 0 AND project_deadline > ? AND project_deadline <= ?";
        return jdbcTemplate.query(sql, PROJECT_ROW_MAPPER, from, to);
    }

    public int markReminderSent(String courseId, String projectId) {
        String sql = "UPDATE " + TABLE + " SET deadline_reminder_sent = 1 WHERE course_id = ? AND project_id = ?";
        return jdbcTemplate.update(sql, courseId, projectId);
    }
}
