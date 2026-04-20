package com.example.stu_backend.web.course;

import com.example.stu_backend.service.CourseService;
import com.example.stu_backend.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestParam("teacherId") String teacherId,
            @RequestParam("courseName") String courseName,
            @RequestParam("className") String className,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            if (!teacherId.trim().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能创建自己的课程"));
            }

            if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "仅支持 .xlsx 文件"));
            }
            if (file.getSize() > 5L * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "文件大小不能超过 5MB"));
            }
            courseService.createCourseByExcel(teacherId, courseName, className, file);
            return ResponseEntity.ok(Map.of("success", true, "message", "课程新建成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Excel 解析失败，请检查模板"));
        } catch (Exception e) {
            Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
            String rootMsg = (root != null && root.getMessage() != null) ? root.getMessage() : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "新建课程失败：" + rootMsg));
        }
    }

    @GetMapping
    public ResponseEntity<?> listByTeacher(@RequestParam("teacherId") String teacherId, HttpSession session) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            if (!teacherId.trim().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能查看自己的课程"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", courseService.getTeacherCourses(teacherId)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查询课程失败：" + e.getMessage()));
        }
    }

    @GetMapping("/ids")
    public ResponseEntity<?> listCourseIdsByTeacher(@RequestParam("teacherId") String teacherId, HttpSession session) {
        try {
            String currentUserId = SessionUtils.getCurrentUserId(session);
            if (!teacherId.trim().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "只能查看自己的课程"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", courseService.getTeacherCourseIds(teacherId)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查询课程失败：" + e.getMessage()));
        }
    }

    @GetMapping("/table-name")
    public ResponseEntity<?> getCourseTableName(@RequestParam("courseId") String courseId) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", courseService.getCourseTableName(courseId)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查询表名失败：" + e.getMessage()));
        }
    }
}
