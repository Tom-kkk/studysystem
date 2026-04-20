package com.example.stu_backend.web.course;

import com.example.stu_backend.service.CourseService;
import com.example.stu_backend.service.ProjectService;
import com.example.stu_backend.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final CourseService courseService;

    public ProjectController(ProjectService projectService, CourseService courseService) {
        this.projectService = projectService;
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<?> listByCourse(@RequestParam("courseId") String courseId, HttpSession session) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            String courseTeacherId = courseService.getTeacherIdByCourseId(courseId.trim());

            if (!courseTeacherId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能查看自己课程的项目"));
            }

            return ResponseEntity.ok(Map.of("success", true, "data", projectService.getAllProjects(courseId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查询项目失败：" + e.getMessage()));
        }
    }

    @PostMapping("/status")
    public ResponseEntity<?> updateStatus(
            @RequestParam("courseId") String courseId,
            @RequestParam("projectId") String projectId,
            @RequestParam("status") String status,
            @RequestParam(value = "deadlineAt", required = false) String deadlineAt,
            HttpSession session
    ) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            String courseTeacherId = courseService.getTeacherIdByCourseId(courseId.trim());

            if (!courseTeacherId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能修改自己课程的项目状态"));
            }

            projectService.updateProjectStatus(courseId, projectId, status, deadlineAt);
            return ResponseEntity.ok(Map.of("success", true, "message", "修改成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "修改失败：" + e.getMessage()));
        }
    }

    @PostMapping("/notify")
    public ResponseEntity<?> notifyStudents(
            @RequestParam("courseId") String courseId,
            @RequestParam("projectId") String projectId,
            HttpSession session
    ) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            String courseTeacherId = courseService.getTeacherIdByCourseId(courseId.trim());
            if (!courseTeacherId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能通知自己课程的学生"));
            }
            int sentCount = projectService.notifyProjectStudents(courseId, projectId);
            return ResponseEntity.ok(Map.of("success", true, "message", "通知发送完成", "sentCount", sentCount));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "通知发送失败：" + e.getMessage()));
        }
    }
}
