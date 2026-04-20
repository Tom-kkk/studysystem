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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/student/reports")
public class StudentReportController {

    private final StudentReportService studentReportService;

    public StudentReportController(StudentReportService studentReportService) {
        this.studentReportService = studentReportService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam("sno") String sno, HttpSession session) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            if (!sno.trim().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能查看自己的报告"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", studentReportService.getStudentCourseProjects(sno.trim())
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查询失败：" + e.getMessage()));
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("sno") String sno,
            @RequestParam("courseId") String courseId,
            @RequestParam("projectId") String projectId,
            @RequestParam("file") MultipartFile file,
            HttpSession session
    ) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            if (!sno.trim().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能上传自己的报告"));
            }

            studentReportService.upload(sno.trim(), courseId.trim(), projectId.trim(), file);
            return ResponseEntity.ok(Map.of("success", true, "message", "文件上传成功！"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "上传失败：" + e.getMessage()));
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> viewFile(
            @RequestParam("sno") String sno,
            @RequestParam("courseId") String courseId,
            @RequestParam("projectId") String projectId,
            HttpSession session
    ) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            if (!sno.trim().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能查看自己的报告"));
            }

            File file = studentReportService.resolveUploadedFile(sno.trim(), courseId.trim(), projectId.trim());
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
}
