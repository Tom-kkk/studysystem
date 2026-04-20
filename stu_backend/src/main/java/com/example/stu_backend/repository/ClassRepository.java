package com.example.stu_backend.repository;

import com.example.stu_backend.entity.ClassStudent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 教学班数据访问层，对齐文档中的 ClassDAO.createClass 逻辑。
 */
@Repository
public class ClassRepository {

    private final JdbcTemplate jdbcTemplate;

    public ClassRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createClass(String teacherId, String className, List<String> prjList, List<ClassStudent> students) {
        String quotedTable = quoteIdentifier(className);
        // 处理“课程已删但教学班表残留”场景，避免重复创建时报错。
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + quotedTable);

        StringBuilder ddl = new StringBuilder("CREATE TABLE `")
                .append(className)
                .append("` (")
                .append("`ID` INT PRIMARY KEY,")
                .append("`SNO` VARCHAR(15) NULL,")
                .append("`Sname` VARCHAR(255) NULL");
        for (String project : prjList) {
            ddl.append(",`").append(project).append("` VARCHAR(255) NULL");
        }
        ddl.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        jdbcTemplate.execute(ddl.toString());

        String insertSql = "INSERT INTO " + quotedTable + "(`ID`,`SNO`,`Sname`) VALUES(?,?,?)";
        for (ClassStudent student : students) {
            jdbcTemplate.update(insertSql, student.getId(), student.getSno(), student.getStudentName());
        }

        // 课程元数据由业务层在同一事务中写入，避免依赖数据库存储过程。
    }

    public String getFilePath(String classTableName, String projectColumn, String sno) {
        String table = quoteIdentifier(strictIdentifier(classTableName));
        String column = quoteIdentifier(strictIdentifier(projectColumn));
        String sql = "SELECT " + column + " FROM " + table + " WHERE REGEXP_REPLACE(TRIM(SNO), '[^0-9]', '') = ?";
        List<String> result = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), normalizeSno(sno));
        return result.isEmpty() ? null : result.get(0);
    }

    public int setFilePath(String classTableName, String projectColumn, String sno, String filePath) {
        String table = quoteIdentifier(strictIdentifier(classTableName));
        String column = quoteIdentifier(strictIdentifier(projectColumn));
        String sql = "UPDATE " + table + " SET " + column + " = ? WHERE REGEXP_REPLACE(TRIM(SNO), '[^0-9]', '') = ?";
        return jdbcTemplate.update(sql, filePath, normalizeSno(sno));
    }

    public boolean existsStudent(String classTableName, String sno) {
        String table = quoteIdentifier(strictIdentifier(classTableName));
        String sql = "SELECT COUNT(1) FROM " + table + " WHERE REGEXP_REPLACE(TRIM(SNO), '[^0-9]', '') = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, normalizeSno(sno));
        return count != null && count > 0;
    }

    public boolean tableExists(String tableName) {
        String sql = "SHOW TABLES LIKE ?";
        List<String> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), tableName);
        return !list.isEmpty();
    }

    public List<String> listClassTables() {
        List<String> allTables = jdbcTemplate.query("SHOW TABLES", (rs, rowNum) -> rs.getString(1));
        List<String> classTables = new ArrayList<>();
        for (String table : allTables) {
            if (table == null) {
                continue;
            }
            String lower = table.toLowerCase();
            if (lower.equals("tb_course") || lower.equals("tb_project") || lower.equals("tb_sc") || lower.equals("tb_user")) {
                continue;
            }
            classTables.add(table);
        }
        return classTables;
    }

    public List<String> listProjectColumns(String classTableName) {
        String table = quoteIdentifier(strictIdentifier(classTableName));
        String sql = "SHOW COLUMNS FROM " + table;
        List<String> cols = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("Field"));
        List<String> projects = new ArrayList<>();
        for (String col : cols) {
            if (col == null) {
                continue;
            }
            if (col.equalsIgnoreCase("ID") || col.equalsIgnoreCase("SNO") || col.equalsIgnoreCase("Sname")) {
                continue;
            }
            projects.add(col);
        }
        return projects;
    }

    public List<ClassStudent> listStudents(String classTableName) {
        String table = quoteIdentifier(strictIdentifier(classTableName));
        String sql = "SELECT ID, SNO, Sname FROM " + table + " ORDER BY ID";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ClassStudent student = new ClassStudent();
            student.setId(rs.getInt("ID"));
            student.setSno(rs.getString("SNO"));
            student.setStudentName(rs.getString("Sname"));
            return student;
        });
    }

    private static String strictIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("非法标识符：" + identifier);
        }
        return identifier.trim();
    }

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private static String normalizeSno(String sno) {
        if (sno == null) {
            return "";
        }
        return sno.replaceAll("[^0-9]", "");
    }
}
