package com.example.stu_backend.web.course;

import com.example.stu_backend.service.StudentReportService;
import com.example.stu_backend.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/reports")
public class TeacherReportController {

    private final StudentReportService studentReportService;

    public TeacherReportController(StudentReportService studentReportService) {
        this.studentReportService = studentReportService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam("teacherId") String teacherId,
            @RequestParam("courseId") String courseId,
            @RequestParam("projectId") String projectId,
            HttpSession session
    ) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            if (!teacherId.trim().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能查看自己的课程报告"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", studentReportService.getTeacherProjectReports(
                            teacherId.trim(),
                            courseId.trim(),
                            projectId.trim()
                    )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查询报告失败：" + e.getMessage()));
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> viewFile(
            @RequestParam("teacherId") String teacherId,
            @RequestParam("courseId") String courseId,
            @RequestParam("projectId") String projectId,
            @RequestParam("sno") String sno,
            HttpSession session
    ) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            if (!teacherId.trim().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能查看自己的课程报告"));
            }

            File file = studentReportService.resolveTeacherUploadedFile(
                    teacherId.trim(),
                    courseId.trim(),
                    projectId.trim(),
                    sno.trim()
            );
            Resource resource = new FileSystemResource(file);
            String encodedName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(file.length())
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "读取文件失败：" + e.getMessage()));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<?> export(
            @RequestParam("teacherId") String teacherId,
            @RequestParam("courseId") String courseId,
            @RequestParam("projectId") String projectId,
            HttpSession session
    ) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            if (!teacherId.trim().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能导出自己的课程报告"));
            }

            String csvContent = studentReportService.buildTeacherProjectReportsCsv(
                    teacherId.trim(),
                    courseId.trim(),
                    projectId.trim()
            );
            String fileName = teacherId.trim() + "-" + courseId.trim() + "-" + projectId.trim() + "-名单.csv";
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvContent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "导出名单失败：" + e.getMessage()));
        }
    }

    @PostMapping("/grade")
    public ResponseEntity<?> updateGrade(
            @RequestParam("teacherId") String teacherId,
            @RequestParam("courseId") String courseId,
            @RequestParam("sno") String sno,
            @RequestParam(value = "grade", required = false) String grade,
            HttpSession session
    ) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            if (!teacherId.trim().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能提交自己课程的成绩"));
            }
            studentReportService.updateTeacherCourseGrade(
                    teacherId.trim(),
                    courseId.trim(),
                    sno.trim(),
                    grade
            );
            return ResponseEntity.ok(Map.of("success", true, "message", "成绩保存成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "成绩保存失败：" + e.getMessage()));
        }
    }
}
