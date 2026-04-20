package com.example.stu_backend.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 对齐文档中的 SCDAO，负责学生选课关系查询。
 */
@Repository
public class ScRepository {

    private static final Logger logger = LoggerFactory.getLogger(ScRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public ScRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> getCourseIdsByStudent(String sno) {
        String sql = "SELECT course_id FROM tb_SC WHERE SNO = ? ORDER BY course_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("course_id"), sno);
    }

    public boolean existsEnrollment(String sno, String courseId) {
        String sql = "SELECT COUNT(1) FROM tb_SC WHERE SNO = ? AND course_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sno, courseId);
        return count != null && count > 0;
    }

    public Optional<String> getGrade(String sno, String courseId) {
        String sql = "SELECT grade FROM tb_SC WHERE SNO = ? AND course_id = ?";
        List<String> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("grade"), sno, courseId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rows.get(0));
    }

    public int updateGrade(String sno, String courseId, String grade) {
        String sql = "UPDATE tb_SC SET grade = ? WHERE SNO = ? AND course_id = ?";
        return jdbcTemplate.update(sql, grade, sno, courseId);
    }

    public void replaceEnrollments(String courseId, Collection<String> snoList) {
        logger.info("开始替换选课记录, courseId={}, 学生数量: {}", courseId, snoList.size());

        // 先删除该课程的旧选课记录
        int deletedRows = jdbcTemplate.update("DELETE FROM tb_SC WHERE course_id = ?", courseId);
        logger.info("删除旧选课记录数: {}", deletedRows);

        String sql = "INSERT INTO tb_SC(SNO, course_id) VALUES(?,?)";

        for (String sno : snoList) {
            // 确保学号格式正确
            String normalizedSno = sno.replaceAll("[^0-9]", "");
            if (normalizedSno.length() > 15) {
                logger.warn("选课记录学号长度超过15位，将被截断: {} -> {}", sno, normalizedSno.substring(0, 15));
                normalizedSno = normalizedSno.substring(0, 15);
            }

            logger.debug("插入选课记录: courseId={}, sno={}", courseId, normalizedSno);

            try {
                int result = jdbcTemplate.update(sql, normalizedSno, courseId);
                logger.debug("选课记录插入结果: courseId={}, sno={}, result={}", courseId, normalizedSno, result);
            } catch (Exception e) {
                logger.error("插入选课记录失败: courseId={}, sno={}, 错误: {}", courseId, normalizedSno, e.getMessage());
                throw e; // 重新抛出异常，确保事务回滚
            }
        }

        logger.info("选课记录替换完成");
    }
}
