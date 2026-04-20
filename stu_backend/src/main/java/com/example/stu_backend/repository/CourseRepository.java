package com.example.stu_backend.repository;

import com.example.stu_backend.entity.Course;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 对齐文档中的 CourseDAO。
 */
@Repository
public class CourseRepository {

    private static final String TABLE = "tb_Course";

    private static final RowMapper<Course> COURSE_ROW_MAPPER = (rs, rowNum) -> {
        Course course = new Course();
        course.setCourseId(rs.getString("course_id"));
        course.setCourseName(rs.getString("course_name"));
        int num = rs.getInt("num_of_project");
        course.setNumOfProject(rs.wasNull() ? null : num);
        course.setTableName(rs.getString("table_name"));
        course.setTeacherId(rs.getString("teacher_id"));
        return course;
    };

    private final JdbcTemplate jdbcTemplate;

    public CourseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取课程对应班级表名。
     */
    public Optional<String> getTableName(String courseId) {
        String sql = "SELECT table_name FROM " + TABLE + " WHERE course_id = ?";
        List<String> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("table_name"), courseId);
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.get(0));
    }

    /**
     * 根据教师账号获取课程 ID 列表。
     */
    public List<String> getCourseIds(String teacherId) {
        String sql = "SELECT course_id FROM " + TABLE + " WHERE teacher_id = ? ORDER BY course_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("course_id"), teacherId);
    }

    /**
     * 获取教师课程详情列表。
     */
    public List<Course> getCourseList(String teacherId) {
        String sql = "SELECT course_id, course_name, num_of_project, table_name, teacher_id FROM "
                + TABLE + " WHERE teacher_id = ? ORDER BY course_id";
        return jdbcTemplate.query(sql, COURSE_ROW_MAPPER, teacherId);
    }

    /**
     * 根据课程ID获取教师ID。
     */
    public Optional<String> getTeacherIdByCourseId(String courseId) {
        String sql = "SELECT teacher_id FROM " + TABLE + " WHERE course_id = ?";
        List<String> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("teacher_id"), courseId);
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.get(0));
    }

    public Optional<String> getCourseNameByCourseId(String courseId) {
        String sql = "SELECT course_name FROM " + TABLE + " WHERE course_id = ?";
        List<String> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("course_name"), courseId);
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.get(0));
    }

    public List<Course> getAllCourses() {
        String sql = "SELECT course_id, course_name, num_of_project, table_name, teacher_id FROM "
                + TABLE + " ORDER BY course_id";
        return jdbcTemplate.query(sql, COURSE_ROW_MAPPER);
    }

    public void upsertCourse(String courseId, String courseName, int numOfProject, String tableName, String teacherId) {
        jdbcTemplate.update("DELETE FROM " + TABLE + " WHERE course_id = ?", courseId);
        String sql = "INSERT INTO " + TABLE
                + "(course_id, course_name, num_of_project, table_name, teacher_id) VALUES(?,?,?,?,?)";
        jdbcTemplate.update(sql, courseId, courseName, numOfProject, tableName, teacherId);
    }
}
