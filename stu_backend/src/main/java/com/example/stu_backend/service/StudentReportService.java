package com.example.stu_backend.service;

import com.example.stu_backend.dto.response.StudentCourseView;
import com.example.stu_backend.dto.response.StudentProjectView;
import com.example.stu_backend.dto.response.TeacherStudentReportView;
import com.example.stu_backend.entity.ClassStudent;
import com.example.stu_backend.entity.Course;
import com.example.stu_backend.entity.Project;
import com.example.stu_backend.repository.ClassRepository;
import com.example.stu_backend.repository.CourseRepository;
import com.example.stu_backend.repository.ProjectRepository;
import com.example.stu_backend.repository.ScRepository;
import com.example.stu_backend.util.PropertiesUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class StudentReportService {

    private static final long MAX_SIZE = 8L * 1024 * 1024;

    private final ScRepository scRepository;
    private final ProjectRepository projectRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;

    public StudentReportService(
            ScRepository scRepository,
            ProjectRepository projectRepository,
            CourseRepository courseRepository,
            ClassRepository classRepository
    ) {
        this.scRepository = scRepository;
        this.projectRepository = projectRepository;
        this.courseRepository = courseRepository;
        this.classRepository = classRepository;
    }

    public List<StudentCourseView> getStudentCourseProjects(String sno) {
        validateSno(sno);
        List<String> courseIds = scRepository.getCourseIdsByStudent(sno);
        if (courseIds.isEmpty()) {
            // 兜底：某些环境未维护 tb_SC 选课关系时，从课程绑定班级表反查学生归属课程。
            for (Course course : courseRepository.getAllCourses()) {
                String tableName = course.getTableName();
                if (tableName != null && !tableName.isBlank() && classRepository.existsStudent(tableName, sno)) {
                    courseIds.add(course.getCourseId());
                }
            }
        }
        if (courseIds.isEmpty()) {
            // 再兜底：历史数据可能只创建了班级表，未写入 tb_course/tb_sc。
            for (String classTable : classRepository.listClassTables()) {
                if (classRepository.existsStudent(classTable, sno)) {
                    courseIds.add(classTable);
                }
            }
        }
        if (courseIds.isEmpty()) {
            // 保持空列表，避免把不属于该学生的课程展示出来，导致上传时被“未选课”校验拦截。
            return new ArrayList<>();
        }
        List<StudentCourseView> result = new ArrayList<>();
        for (String courseId : courseIds) {
            String tableName = courseRepository.getTableName(courseId)
                    .orElseGet(() -> classRepository.tableExists(courseId) ? courseId : null);
            if (tableName == null) {
                throw new IllegalArgumentException("课程未绑定班级表：" + courseId);
            }
            if (!classRepository.existsStudent(tableName, sno)) {
                // 只返回当前学生真实在课名单内的课程，避免前端展示后上传失败。
                continue;
            }
            StudentCourseView course = new StudentCourseView();
            course.setCourseId(courseId);
            List<StudentProjectView> projects = new ArrayList<>();
            List<Project> courseProjects = projectRepository.getAllProjects(courseId);
            if (courseProjects.isEmpty()) {
                List<String> projectColumns = classRepository.listProjectColumns(tableName);
                for (int i = 0; i < projectColumns.size(); i++) {
                    String projectName = projectColumns.get(i);
                    String projectId = "P" + (i + 1);
                    StudentProjectView item = new StudentProjectView();
                    item.setProjectId(projectId);
                    item.setProjectName(projectName);
                    item.setProjectOpen(1);
                    String filePath = classRepository.getFilePath(tableName, projectName, sno);
                    boolean uploaded = filePath != null && !filePath.isBlank();
                    item.setUploaded(uploaded);
                    item.setFileName(extractFileName(filePath));
                    item.setCanCheck(uploaded);
                    item.setCanUpload(true);
                    item.setCheckText(uploaded ? "检查上传" : "");
                    item.setUploadStatus(resolveUploadStatus(1, uploaded));
                    item.setUploadStatusText(resolveUploadStatusText(1, uploaded));
                    item.setCheckUrl("/api/student/reports/file?sno=" + sno + "&courseId=" + courseId + "&projectId=" + projectId);
                    item.setProjectDeadline(null);
                    projects.add(item);
                }
            }
            for (Project project : courseProjects) {
                StudentProjectView item = new StudentProjectView();
                item.setProjectId(project.getProjectId());
                item.setProjectName(project.getProjectName());
                item.setProjectOpen(project.getProjectOpen());
                String filePath = classRepository.getFilePath(tableName, project.getProjectName(), sno);
                boolean uploaded = filePath != null && !filePath.isBlank();
                item.setUploaded(uploaded);
                item.setFileName(extractFileName(filePath));
                item.setCanCheck(uploaded);
                item.setCanUpload(project.getProjectOpen() != null && project.getProjectOpen() == 1);
                item.setCheckText(uploaded ? "检查上传" : "");
                item.setUploadStatus(resolveUploadStatus(project.getProjectOpen(), uploaded));
                item.setUploadStatusText(resolveUploadStatusText(project.getProjectOpen(), uploaded));
                item.setCheckUrl("/api/student/reports/file?sno=" + sno + "&courseId=" + courseId + "&projectId=" + project.getProjectId());
                item.setProjectDeadline(project.getProjectDeadline());
                projects.add(item);
            }
            course.setProjects(projects);
            result.add(course);
        }
        return result;
    }

    public String upload(String sno, String courseId, String projectId, MultipartFile file) throws IOException {
        validateSno(sno);
        validateParam(courseId, "课程ID不能为空");
        validateParam(projectId, "项目ID不能为空");
        if (!isStudentInCourse(sno, courseId)) {
            throw new IllegalArgumentException("当前学生未选该课程");
        }
        String tableName = resolveCourseTableName(courseId);
        if (tableName == null) {
            throw new IllegalArgumentException("课程未绑定班级表");
        }
        ResolvedProject resolvedProject = resolveProject(courseId, tableName, projectId);
        if (resolvedProject.projectOpen() != null && resolvedProject.projectOpen() != 1) {
            throw new IllegalArgumentException("该项目未开放上传");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!originalName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("文件类型错误，必须是 PDF");
        }
        if (file.getSize() <= 0 || file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("文件大小需在 0~8MB");
        }
        String saveName = sno + "-" + resolvedProject.projectName() + ".pdf";
        Path saveDir = Path.of(resolveBasePath(), courseId, projectId);
        Files.createDirectories(saveDir);
        Path target = saveDir.resolve(saveName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        int updated = classRepository.setFilePath(tableName, resolvedProject.projectName(), sno, target.toString());
        if (updated <= 0) {
            throw new IllegalArgumentException("当前学生不在该课程名单，无法上传");
        }
        return target.toString();
    }

    public File resolveUploadedFile(String sno, String courseId, String projectId) {
        validateSno(sno);
        validateParam(courseId, "课程ID不能为空");
        validateParam(projectId, "项目ID不能为空");
        if (!isStudentInCourse(sno, courseId)) {
            throw new IllegalArgumentException("当前学生未选该课程");
        }
        String tableName = resolveCourseTableName(courseId);
        if (tableName == null) {
            throw new IllegalArgumentException("课程未绑定班级表");
        }
        ResolvedProject resolvedProject = resolveProject(courseId, tableName, projectId);
        // 按实验文档约束：关闭/截止项目不允许学生查看已上传报告。
        if (resolvedProject.projectOpen() == null || resolvedProject.projectOpen() != 1) {
            throw new IllegalArgumentException("下载关闭的项目");
        }
        String filePath = classRepository.getFilePath(tableName, resolvedProject.projectName(), sno);
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("未找到已上传报告");
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("报告文件不存在");
        }
        return file;
    }

    public List<TeacherStudentReportView> getTeacherProjectReports(String teacherId, String courseId, String projectId) {
        validateParam(teacherId, "教师账号不能为空");
        validateParam(courseId, "课程ID不能为空");
        validateParam(projectId, "项目ID不能为空");
        verifyTeacherOwnsCourse(teacherId, courseId);

        String tableName = resolveCourseTableName(courseId);
        if (tableName == null) {
            throw new IllegalArgumentException("课程未绑定班级表");
        }
        ResolvedProject resolvedProject = resolveProject(courseId, tableName, projectId);
        List<ClassStudent> students = classRepository.listStudents(tableName);
        List<TeacherStudentReportView> result = new ArrayList<>();
        for (ClassStudent student : students) {
            String sno = normalizeSno(student.getSno());
            String filePath = classRepository.getFilePath(tableName, resolvedProject.projectName(), sno);
            boolean uploaded = filePath != null && !filePath.isBlank();

            TeacherStudentReportView row = new TeacherStudentReportView();
            row.setSno(sno);
            row.setStudentName(student.getStudentName());
            row.setUploaded(uploaded);
            row.setFileName(extractFileName(filePath));
            row.setGrade(scRepository.getGrade(sno, courseId).orElse(null));
            row.setCheckUrl(uploaded
                    ? "/api/teacher/reports/file?teacherId=" + teacherId + "&courseId=" + courseId
                    + "&projectId=" + projectId + "&sno=" + sno
                    : null);
            result.add(row);
        }
        return result;
    }

    public File resolveTeacherUploadedFile(String teacherId, String courseId, String projectId, String sno) {
        validateParam(teacherId, "教师账号不能为空");
        validateParam(courseId, "课程ID不能为空");
        validateParam(projectId, "项目ID不能为空");
        validateSno(sno);
        verifyTeacherOwnsCourse(teacherId, courseId);
        if (!isStudentInCourse(sno, courseId)) {
            throw new IllegalArgumentException("学生不在当前课程名单");
        }

        String tableName = resolveCourseTableName(courseId);
        if (tableName == null) {
            throw new IllegalArgumentException("课程未绑定班级表");
        }
        ResolvedProject resolvedProject = resolveProject(courseId, tableName, projectId);
        String filePath = classRepository.getFilePath(tableName, resolvedProject.projectName(), sno);
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("未找到该学生上传的报告");
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("报告文件不存在");
        }
        return file;
    }

    public String buildTeacherProjectReportsCsv(String teacherId, String courseId, String projectId) {
        List<TeacherStudentReportView> rows = getTeacherProjectReports(teacherId, courseId, projectId);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("学号,姓名,上传状态,文件名,成绩").append('\n');
        for (TeacherStudentReportView row : rows) {
            csv.append(escapeCsv(row.getSno())).append(',');
            csv.append(escapeCsv(row.getStudentName())).append(',');
            csv.append(escapeCsv(row.isUploaded() ? "已上传" : "未上传")).append(',');
            csv.append(escapeCsv(row.getFileName())).append(',');
            csv.append(escapeCsv(row.getGrade())).append('\n');
        }
        return csv.toString();
    }

    public void updateTeacherCourseGrade(String teacherId, String courseId, String sno, String grade) {
        validateParam(teacherId, "教师账号不能为空");
        validateParam(courseId, "课程ID不能为空");
        validateSno(sno);
        verifyTeacherOwnsCourse(teacherId, courseId);
        if (!isStudentInCourse(sno, courseId)) {
            throw new IllegalArgumentException("学生不在当前课程名单");
        }
        String normalizedGrade = grade == null ? "" : grade.trim();
        if (normalizedGrade.length() > 50) {
            throw new IllegalArgumentException("成绩长度不能超过50个字符");
        }
        int updated = scRepository.updateGrade(sno, courseId, normalizedGrade.isBlank() ? null : normalizedGrade);
        if (updated <= 0) {
            throw new IllegalArgumentException("更新成绩失败，未找到选课记录");
        }
    }

    private static String resolveBasePath() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String path;
        if (os.startsWith("windows")) {
            path = PropertiesUtil.pro.getProperty("Win_path");
        } else {
            path = PropertiesUtil.pro.getProperty("Linux_path");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("报告存储路径未配置");
        }
        return path;
    }

    private static void validateSno(String sno) {
        validateParam(sno, "学号不能为空");
    }

    private static void validateParam(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String extractFileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        return Path.of(filePath).getFileName().toString();
    }

    private static String normalizeSno(String sno) {
        if (sno == null) {
            return "";
        }
        return sno.replaceAll("[^0-9]", "");
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private boolean isStudentInCourse(String sno, String courseId) {
        if (scRepository.existsEnrollment(sno, courseId)) {
            return true;
        }
        Optional<String> tableName = courseRepository.getTableName(courseId);
        if (tableName.isPresent()) {
            return classRepository.existsStudent(tableName.get(), sno);
        }
        return classRepository.tableExists(courseId) && classRepository.existsStudent(courseId, sno);
    }

    private void verifyTeacherOwnsCourse(String teacherId, String courseId) {
        String ownerTeacherId = courseRepository.getTeacherIdByCourseId(courseId)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在"));
        if (!ownerTeacherId.equals(teacherId)) {
            throw new IllegalArgumentException("只能查看自己课程的实验报告");
        }
    }

    private String resolveCourseTableName(String courseId) {
        return courseRepository.getTableName(courseId)
                .orElseGet(() -> classRepository.tableExists(courseId) ? courseId : null);
    }

    private ResolvedProject resolveProject(String courseId, String tableName, String projectId) {
        Optional<Project> projectOpt = projectRepository.findByCourseAndProjectId(courseId, projectId);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            return new ResolvedProject(project.getProjectName(), project.getProjectOpen());
        }

        List<String> columns = classRepository.listProjectColumns(tableName);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("项目不存在");
        }
        String normalized = projectId == null ? "" : projectId.trim();
        if (normalized.matches("(?i)^P\\d+$")) {
            int index = Integer.parseInt(normalized.substring(1)) - 1;
            if (index >= 0 && index < columns.size()) {
                return new ResolvedProject(columns.get(index), 1);
            }
        }
        throw new IllegalArgumentException("项目不存在");
    }

    private record ResolvedProject(String projectName, Integer projectOpen) {
    }

    private static String resolveUploadStatus(Integer projectOpen, boolean uploaded) {
        if (uploaded) {
            return "uploaded";
        }
        if (projectOpen == null) {
            return "unknown";
        }
        if (projectOpen == 1) {
            return "not_uploaded";
        }
        if (projectOpen == 0) {
            return "closed";
        }
        if (projectOpen == 2) {
            return "deadline";
        }
        return "unknown";
    }

    private static String resolveUploadStatusText(Integer projectOpen, boolean uploaded) {
        if (uploaded) {
            return "已上传";
        }
        if (projectOpen == null) {
            return "状态未知";
        }
        if (projectOpen == 1) {
            return "未上传";
        }
        if (projectOpen == 0) {
            return "未开放";
        }
        if (projectOpen == 2) {
            return "已截止";
        }
        return "状态未知";
    }
}
