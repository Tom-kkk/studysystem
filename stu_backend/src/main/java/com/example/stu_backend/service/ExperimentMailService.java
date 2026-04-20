package com.example.stu_backend.service;

import com.example.stu_backend.entity.Project;
import com.example.stu_backend.entity.User;
import com.example.stu_backend.repository.CourseRepository;
import com.example.stu_backend.repository.ProjectRepository;
import com.example.stu_backend.repository.UserRepository;
import com.example.stu_backend.util.EmailUtil;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ExperimentMailService {

    private static final Logger logger = LoggerFactory.getLogger(ExperimentMailService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ProjectRepository projectRepository;
    private final EmailUtil emailUtil;

    @Value("${app.mail.deadline-reminder-hours:24}")
    private long reminderHours;

    public ExperimentMailService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            ProjectRepository projectRepository,
            EmailUtil emailUtil
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.projectRepository = projectRepository;
        this.emailUtil = emailUtil;
    }

    public void notifyProjectPublished(String courseId, String projectId, String projectName, LocalDateTime deadline) {
        List<User> students = userRepository.findStudentsWithEmailByCourseId(courseId);
        if (students.isEmpty()) {
            return;
        }
        String courseName = courseRepository.getCourseNameByCourseId(courseId).orElse(courseId);
        String title = "【实验发布通知】" + courseName + " - " + projectName;
        String deadlineText = deadline == null
                ? "本实验暂未设置截止时间，请及时关注课程通知。"
                : "截止时间：" + deadline.format(TIME_FORMATTER);
        String content = "同学你好，\n\n"
                + "课程《" + courseName + "》已发布新实验：\n"
                + "实验编号：" + projectId + "\n"
                + "实验名称：" + projectName + "\n"
                + deadlineText + "\n\n"
                + "请尽快登录系统完成报告提交。";
        sendToUsers(students, title, content);
    }

    public int notifyProjectManual(String courseId, String projectId) {
        Optional<Project> projectOpt = projectRepository.findByCourseAndProjectId(courseId, projectId);
        if (projectOpt.isEmpty()) {
            throw new IllegalArgumentException("项目不存在");
        }
        Project project = projectOpt.get();
        List<User> students = userRepository.findStudentsWithEmailByCourseId(courseId);
        if (students.isEmpty()) {
            return 0;
        }
        String courseName = courseRepository.getCourseNameByCourseId(courseId).orElse(courseId);
        String statusText = project.getProjectOpen() != null && project.getProjectOpen() == 1 ? "开放中"
                : project.getProjectOpen() != null && project.getProjectOpen() == 2 ? "已截止"
                : "未开放";
        String deadlineText = project.getProjectDeadline() == null
                ? "截止时间：暂未设置"
                : "截止时间：" + project.getProjectDeadline().format(TIME_FORMATTER);
        String title = "【教师通知】" + courseName + " - " + project.getProjectName();
        String content = "同学你好，\n\n"
                + "教师对实验项目发起了通知：\n"
                + "课程：" + courseName + "\n"
                + "实验编号：" + project.getProjectId() + "\n"
                + "实验名称：" + project.getProjectName() + "\n"
                + "项目状态：" + statusText + "\n"
                + deadlineText + "\n\n"
                + "请及时登录系统查看并完成提交。";
        sendToUsers(students, title, content);
        return students.size();
    }

    public void sendDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime to = now.plusHours(Math.max(reminderHours, 1));
        List<Project> projects = projectRepository.findOpenProjectsForReminder(now, to);
        if (projects.isEmpty()) {
            return;
        }

        for (Project project : projects) {
            LocalDateTime deadline = project.getProjectDeadline();
            if (deadline == null) {
                continue;
            }
            List<User> students = userRepository.findStudentsWithEmailByCourseId(project.getCourseId());
            if (!students.isEmpty()) {
                String courseName = courseRepository.getCourseNameByCourseId(project.getCourseId())
                        .orElse(project.getCourseId());
                long leftHours = Math.max(1, Duration.between(now, deadline).toHours());
                String title = "【截止提醒】" + courseName + " - " + project.getProjectName();
                String content = "同学你好，\n\n"
                        + "你有一个实验即将截止：\n"
                        + "课程：" + courseName + "\n"
                        + "实验编号：" + project.getProjectId() + "\n"
                        + "实验名称：" + project.getProjectName() + "\n"
                        + "截止时间：" + deadline.format(TIME_FORMATTER) + "\n"
                        + "预计剩余时间约 " + leftHours + " 小时。\n\n"
                        + "请尽快提交实验报告。";
                sendToUsers(students, title, content);
            }
            projectRepository.markReminderSent(project.getCourseId(), project.getProjectId());
        }
    }

    private void sendToUsers(List<User> users, String title, String content) {
        for (User user : users) {
            String to = user.getEmail();
            if (to == null || to.isBlank()) {
                continue;
            }
            try {
                emailUtil.sendPlainEmail(to.trim(), title, content);
            } catch (MessagingException | UnsupportedEncodingException e) {
                logger.warn("邮件发送失败, user={}, email={}, reason={}",
                        user.getUserName(), to, e.getMessage());
            }
        }
    }
}
