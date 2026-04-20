package com.example.stu_backend.repository;

import com.example.stu_backend.entity.ClassStudent;
import com.example.stu_backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 对应教程 {@code UserDAO}，表名与列名与 {@code report.sql} 中 {@code tb_User} 一致。
 */
@Repository
public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private static final String TABLE = "tb_User";

    private static final RowMapper<User> USER_ROW_MAPPER = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User u = new User();
            u.setUserName(rs.getString("user_name"));
            u.setUserPassword(rs.getString("user_password"));
            u.setFullName(rs.getString("full_name"));
            u.setEmail(rs.getString("email"));
            u.setRoleType(rs.getString("role_type"));
            int att = rs.getInt("attemps");
            u.setAttemps(rs.wasNull() ? null : att);
            return u;
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按用户名 + 口令查询（登录），存在则返回完整用户。
     */
    public Optional<User> findByUserNameAndPassword(String userName, String userPassword) {
        String sql = "SELECT user_name, user_password, full_name, email, role_type, attemps FROM "
                + TABLE + " WHERE user_name = ? AND user_password = ?";
        List<User> list = jdbcTemplate.query(sql, USER_ROW_MAPPER, userName, userPassword);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * 按用户名查询（如忘记密码场景），存在则返回用户（可能不含口令等业务需要时再补全）。
     */
    public Optional<User> findByUserName(String userName) {
        String sql = "SELECT user_name, user_password, full_name, email, role_type, attemps FROM "
                + TABLE + " WHERE user_name = ?";
        List<User> list = jdbcTemplate.query(sql, USER_ROW_MAPPER, userName);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * 更新口令与邮箱（与教程 {@code updateUser} 一致）。
     */
    public int updatePasswordAndEmail(String userName, String userPassword, String email) {
        String sql = "UPDATE " + TABLE + " SET user_password = ?, email = ? WHERE user_name = ?";
        return jdbcTemplate.update(sql, userPassword, email, userName);
    }

    /** 仅更新口令（忘记密码场景）。 */
    public int updatePassword(String userName, String userPassword) {
        String sql = "UPDATE " + TABLE + " SET user_password = ? WHERE user_name = ?";
        return jdbcTemplate.update(sql, userPassword, userName);
    }

    /**
     * 批量插入/更新学生账号，避免建课写入 tb_SC 时触发外键约束。
     */
    public void upsertStudents(Collection<ClassStudent> students) {
        logger.info("开始批量插入/更新学生账号，学生数量: {}", students.size());

        String sql = "INSERT INTO " + TABLE + " (user_name, user_password, full_name, role_type) "
                + "VALUES (?, MD5(?), ?, 'student') "
                + "ON DUPLICATE KEY UPDATE "
                + "full_name = VALUES(full_name), "
                + "role_type = VALUES(role_type)";

        for (ClassStudent student : students) {
            String sno = student.getSno();
            String sname = student.getStudentName();

            // 确保学号格式正确（去除非数字字符，确保长度符合 VARCHAR(15)）
            String normalizedSno = sno.replaceAll("[^0-9]", "");
            if (normalizedSno.length() > 15) {
                logger.warn("学号长度超过15位，将被截断: {} -> {}", sno, normalizedSno.substring(0, 15));
                normalizedSno = normalizedSno.substring(0, 15);
            }

            logger.debug("处理学生: sno={}, originalSno={}, sname={}", normalizedSno, sno, sname);

            try {
                int result = jdbcTemplate.update(sql, normalizedSno, normalizedSno, sname);
                logger.debug("学生 {} 插入/更新结果: {}", normalizedSno, result);
            } catch (Exception e) {
                logger.error("插入学生失败: sno={}, sname={}, 错误: {}", normalizedSno, sname, e.getMessage());
                throw e; // 重新抛出异常，确保事务回滚
            }
        }

        logger.info("学生账号批量插入/更新完成");
    }

    public List<User> findStudentsWithEmailByCourseId(String courseId) {
        String sql = "SELECT DISTINCT u.user_name, u.user_password, u.full_name, u.email, u.role_type, u.attemps "
                + "FROM tb_User u INNER JOIN tb_SC sc ON sc.SNO = u.user_name "
                + "WHERE sc.course_id = ? AND u.role_type = 'student' "
                + "AND u.email IS NOT NULL AND TRIM(u.email) <> ''";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, courseId);
    }
}
