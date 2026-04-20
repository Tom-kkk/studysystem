package com.example.stu_backend.service;

import com.example.stu_backend.entity.ClassStudent;
import com.example.stu_backend.repository.ClassRepository;
import com.example.stu_backend.entity.Course;
import com.example.stu_backend.repository.CourseRepository;
import com.example.stu_backend.repository.ProjectRepository;
import com.example.stu_backend.repository.ScRepository;
import com.example.stu_backend.repository.UserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final ProjectRepository projectRepository;
    private final ScRepository scRepository;
    private final UserRepository userRepository;

    public CourseService(
            ClassRepository classRepository,
            CourseRepository courseRepository,
            ProjectRepository projectRepository,
            ScRepository scRepository,
            UserRepository userRepository
    ) {
        this.classRepository = classRepository;
        this.courseRepository = courseRepository;
        this.projectRepository = projectRepository;
        this.scRepository = scRepository;
        this.userRepository = userRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createCourseByExcel(String teacherId, String courseName, String className, MultipartFile file) throws IOException {
        if (teacherId == null || teacherId.isBlank()) {
            throw new IllegalArgumentException("教师账号不能为空");
        }
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("课程名不能为空");
        }
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("班级不能为空");
        }
        String courseId = normalizeIdentifier(courseName.trim() + className.trim());
        if (!courseId.equals(courseName.trim() + className.trim())) {
            throw new IllegalArgumentException("课程名和班级仅支持中文、英文、数字、下划线");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 Excel 文件");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 内容为空");
            }
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new IllegalArgumentException("Excel 缺少表头");
            }

            List<String> projects = new ArrayList<>();
            for (int i = 3; i < header.getLastCellNum(); i++) {
                String projectName = formatter.formatCellValue(header.getCell(i)).trim();
                if (!projectName.isEmpty()) {
                    projects.add(normalizeIdentifier(projectName));
                }
            }
            List<ClassStudent> students = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String idText = formatter.formatCellValue(row.getCell(0)).trim();
                String sno = normalizeSno(formatter.formatCellValue(row.getCell(1)).trim());
                String sname = formatter.formatCellValue(row.getCell(2)).trim();
                if (idText.isEmpty() && sno.isEmpty() && sname.isEmpty()) {
                    continue;
                }
                if (idText.isEmpty() || sno.isEmpty() || sname.isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 1) + " 行学生数据不完整");
                }
                ClassStudent student = new ClassStudent();
                student.setId(parseIntCell(row.getCell(0), formatter, i));
                student.setSno(sno);
                student.setStudentName(sname);
                students.add(student);
            }
            if (students.isEmpty()) {
                throw new IllegalArgumentException("Excel 未读取到学生名单");
            }

            String normalizedTeacherId = teacherId.trim();
            classRepository.createClass(normalizedTeacherId, courseId, projects, students);

            // 统一由 Java 代码写入主数据，确保教师建课后学生端立即可查。
            // 顺序很重要：必须先插入学生账号到 tb_User，再插入选课关系到 tb_SC
            courseRepository.upsertCourse(courseId, courseName.trim(), projects.size(), courseId, normalizedTeacherId);
            projectRepository.replaceProjects(courseId, projects);

            // 先插入学生账号到 tb_User，确保外键约束不会失败
            // 对学号进行标准化处理，确保格式一致
            for (ClassStudent student : students) {
                String originalSno = student.getSno();
                String normalizedSno = normalizeSno(originalSno);
                // 截断过长的学号
                if (normalizedSno.length() > 15) {
                    normalizedSno = normalizedSno.substring(0, 15);
                }
                student.setSno(normalizedSno);
                if (!originalSno.equals(normalizedSno)) {
                    System.out.println("学号已标准化: " + originalSno + " -> " + normalizedSno);
                }
            }

            List<String> normalizedSnoList = students.stream()
                    .map(ClassStudent::getSno)
                    .distinct()
                    .collect(Collectors.toList());

            userRepository.upsertStudents(students);

            // 再插入选课关系到 tb_SC
            scRepository.replaceEnrollments(courseId, normalizedSnoList);
        }
    }

    public List<Course> getTeacherCourses(String teacherId) {
        if (teacherId == null || teacherId.isBlank()) {
            throw new IllegalArgumentException("教师账号不能为空");
        }
        return courseRepository.getCourseList(teacherId.trim());
    }

    public List<String> getTeacherCourseIds(String teacherId) {
        if (teacherId == null || teacherId.isBlank()) {
            throw new IllegalArgumentException("教师账号不能为空");
        }
        return courseRepository.getCourseIds(teacherId.trim());
    }

    public String getCourseTableName(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("课程ID不能为空");
        }
        return courseRepository.getTableName(courseId.trim())
                .orElseThrow(() -> new IllegalArgumentException("课程不存在或未绑定教学班表"));
    }

    public String getTeacherIdByCourseId(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("课程ID不能为空");
        }
        return courseRepository.getTeacherIdByCourseId(courseId.trim())
                .orElseThrow(() -> new IllegalArgumentException("课程不存在"));
    }

    private static Integer parseIntCell(Cell cell, DataFormatter formatter, int rowIndex) {
        try {
            String text = formatter.formatCellValue(cell).trim();
            return Integer.parseInt(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("第 " + (rowIndex + 1) + " 行序号不是整数");
        }
    }

    private static String normalizeIdentifier(String raw) {
        String value = raw == null ? "" : raw.trim();
        return value.replaceAll("[^\\p{IsHan}A-Za-z0-9_]", "");
    }

    private static String normalizeSno(String raw) {
        String value = raw == null ? "" : raw.trim();
        return value.replaceAll("[^0-9]", "");
    }
}
