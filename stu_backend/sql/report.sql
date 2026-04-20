-- =============================================================================
-- report 数据库初始化脚本（MySQL 8.x）
-- 依据：docs/feishu_md/6 数据库设计.md
-- 字符集：utf8mb4（替代文档中的 utf8，以完整支持中文与 emoji）
-- =============================================================================

-- 可选：延长连接超时（需 SUPER 或 SYSTEM_VARIABLES_ADMIN，失败可忽略）
-- SET GLOBAL wait_timeout = 28800000;
-- SET GLOBAL interactive_timeout = 28800000;

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `report`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `report`;

-- ---------------------------------------------------------------------------
-- 表（先删子表再删父表）
-- ---------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `tb_SC`;
DROP TABLE IF EXISTS `tb_Project`;
DROP TABLE IF EXISTS `tb_Course`;
DROP TABLE IF EXISTS `tb_User`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `tb_User` (
  `user_name`   VARCHAR(15)  NOT NULL COMMENT '用户名/学号',
  `user_password` VARCHAR(50) NULL COMMENT '口令，默认学号经 MD5',
  `full_name`   VARCHAR(50) NULL COMMENT '姓名',
  `email`       VARCHAR(50) NULL,
  `role_type`   VARCHAR(10) NOT NULL DEFAULT 'student',
  `attemps`     INT NULL,
  PRIMARY KEY (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_Course` (
  `course_id`     VARCHAR(50) NOT NULL COMMENT '课程+班级，如：数据库原理与应用实验191',
  `course_name`   VARCHAR(50) NULL COMMENT '课程名',
  `num_of_project` INT NULL COMMENT '实验项目数量',
  `table_name`    VARCHAR(50) NULL COMMENT '班级明细表名（Excel 导入）',
  `teacher_id`    VARCHAR(50) NULL,
  PRIMARY KEY (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_SC` (
  `SNO`       VARCHAR(15) NOT NULL COMMENT '学号',
  `course_id` VARCHAR(50) NOT NULL,
  `grade`     VARCHAR(50) NULL COMMENT '成绩',
  PRIMARY KEY (`SNO`, `course_id`),
  CONSTRAINT `fk_sc_user` FOREIGN KEY (`SNO`) REFERENCES `tb_User` (`user_name`),
  CONSTRAINT `fk_sc_course` FOREIGN KEY (`course_id`) REFERENCES `tb_Course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_Project` (
  `id`            INT NOT NULL AUTO_INCREMENT,
  `project_id`    VARCHAR(50) NOT NULL COMMENT '如：191数据库编程实验',
  `project_name`  VARCHAR(50) NULL,
  `course_id`     VARCHAR(50) NULL,
  `project_open`  INT NULL,
  `project_deadline` DATETIME NULL COMMENT '实验截止时间',
  `deadline_reminder_sent` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '临期提醒是否已发送（1=已发）',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_project_course` FOREIGN KEY (`course_id`) REFERENCES `tb_Course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 存储过程：从班级明细表导入课程、用户、选课、实验项目（逻辑同教材）
-- 使用前需先建好班级表并从 Excel 导入数据；列顺序与教材一致（前若干列为固定列，其后为各实验项目列）
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `add_data`;

DELIMITER $$

CREATE PROCEDURE `add_data`(
  IN `p_course_id` VARCHAR(200) CHARACTER SET utf8mb4,
  IN `p_table_name` VARCHAR(200) CHARACTER SET utf8mb4,
  IN `p_teacher_id` VARCHAR(50) CHARACTER SET utf8mb4
)
BEGIN
  DECLARE `prj` VARCHAR(128);
  DECLARE `prjnum` INT DEFAULT 0;
  DECLARE `i` INT DEFAULT 0;
  DECLARE `done` INT DEFAULT 0;
  DECLARE `prj_Cursor` CURSOR FOR
    SELECT `COLUMN_NAME`
    FROM `information_schema`.`COLUMNS`
    WHERE BINARY `TABLE_SCHEMA` = BINARY DATABASE()
      AND CAST(`TABLE_NAME` AS BINARY) = CAST(`p_table_name` AS BINARY)
    ORDER BY `ORDINAL_POSITION`;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET `done` = 1;

  SELECT COUNT(*) - 3 INTO `prjnum`
  FROM `information_schema`.`COLUMNS`
  WHERE BINARY `TABLE_SCHEMA` = BINARY DATABASE()
    AND CAST(`TABLE_NAME` AS BINARY) = CAST(`p_table_name` AS BINARY);

  SET @t_tbl = REPLACE(`p_table_name`, '`', '');
  SET @str_sql = CONCAT(
    'INSERT INTO tb_Course(course_id,course_name,num_of_project,table_name,teacher_id) VALUES(',
    QUOTE(`p_course_id`), ',',
    QUOTE(LEFT(`p_course_id`, CHAR_LENGTH(`p_course_id`) - 11)), ',',
    `prjnum`, ',',
    QUOTE(`p_table_name`), ',',
    QUOTE(`p_teacher_id`),
    ')'
  );
  PREPARE `stmt` FROM @str_sql;
  EXECUTE `stmt`;
  DEALLOCATE PREPARE `stmt`;

  SET @str_sql = CONCAT(
    'INSERT INTO tb_User(user_name,user_password,full_name,role_type) ',
    'SELECT CAST(TRIM(CAST(tb2.SNO AS CHAR(32))) AS CHAR(15)), MD5(CAST(TRIM(CAST(tb2.SNO AS CHAR(32))) AS CHAR(32))), tb2.Sname, ''student'' ',
    'FROM `', @t_tbl, '` AS tb2 ',
    'WHERE BINARY CAST(TRIM(CAST(tb2.SNO AS CHAR(32))) AS CHAR(15)) NOT IN (SELECT BINARY tb3.user_name FROM tb_User AS tb3)'
  );
  PREPARE `stmt` FROM @str_sql;
  EXECUTE `stmt`;
  DEALLOCATE PREPARE `stmt`;

  SET @str_sql = CONCAT(
    'INSERT INTO tb_SC(SNO,course_id) ',
    'SELECT CAST(TRIM(CAST(tb4.SNO AS CHAR(32))) AS CHAR(15)), ',
    QUOTE(`p_course_id`),
    ' FROM `', @t_tbl, '` AS tb4'
  );
  PREPARE `stmt` FROM @str_sql;
  EXECUTE `stmt`;
  DEALLOCATE PREPARE `stmt`;

  OPEN `prj_Cursor`;
  SET `i` = 1;
  WHILE `i` <= 3 DO
    FETCH `prj_Cursor` INTO `prj`;
    SET `i` = `i` + 1;
  END WHILE;

  SET `i` = 1;
  SET `done` = 0;
  WHILE `i` <= `prjnum` DO
    IF `done` = 1 THEN
      SET `i` = `prjnum` + 1;
    ELSE
      SET @str_sql = CONCAT(
        'INSERT INTO tb_Project(project_id,project_name,course_id,project_open,project_deadline,deadline_reminder_sent) VALUES(',
        QUOTE(CONCAT(RIGHT(`p_course_id`, 3), `prj`)), ',',
        QUOTE(`prj`), ',',
        QUOTE(`p_course_id`), ', 0, NULL, 1)'
      );

      SET @str_s = CONCAT(
        'UPDATE `', @t_tbl, '` SET `', REPLACE(`prj`, '`', ''), '` = NULL'
      );
      PREPARE `stmt` FROM @str_s;
      EXECUTE `stmt`;
      DEALLOCATE PREPARE `stmt`;

      PREPARE `stmt` FROM @str_sql;
      EXECUTE `stmt`;
      DEALLOCATE PREPARE `stmt`;

      FETCH `prj_Cursor` INTO `prj`;
      SET `i` = `i` + 1;
    END IF;
  END WHILE;

  CLOSE `prj_Cursor`;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------------
-- 存储过程：按课程移除选课、项目、课程及仅属于该课的学生账号
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `delete_data`;

DELIMITER $$

CREATE PROCEDURE `delete_data`(
  IN `p_course_id` VARCHAR(200),
  IN `p_table_name` VARCHAR(200)
)
BEGIN
  SET @t_tbl = REPLACE(`p_table_name`, '`', '');

  SET @str_sql = CONCAT(
    'DELETE FROM tb_SC WHERE BINARY course_id = BINARY ', QUOTE(`p_course_id`),
    ' AND SNO IN (SELECT CAST(TRIM(CAST(SNO AS CHAR(32))) AS CHAR(15)) FROM `', @t_tbl, '`)'
  );
  PREPARE `stmt` FROM @str_sql;
  EXECUTE `stmt`;
  DEALLOCATE PREPARE `stmt`;

  SET @str_sql = CONCAT('DELETE FROM tb_Project WHERE BINARY course_id = BINARY ', QUOTE(`p_course_id`));
  PREPARE `stmt` FROM @str_sql;
  EXECUTE `stmt`;
  DEALLOCATE PREPARE `stmt`;

  SET @str_sql = CONCAT('DELETE FROM tb_Course WHERE BINARY course_id = BINARY ', QUOTE(`p_course_id`));
  PREPARE `stmt` FROM @str_sql;
  EXECUTE `stmt`;
  DEALLOCATE PREPARE `stmt`;

  SET @str_sql = CONCAT(
    'DELETE FROM tb_User WHERE user_name IN (SELECT CAST(TRIM(CAST(SNO AS CHAR(32))) AS CHAR(15)) FROM `', @t_tbl, '`)',
    ' AND user_name NOT IN (SELECT SNO FROM tb_SC)'
  );
  PREPARE `stmt` FROM @str_sql;
  EXECUTE `stmt`;
  DEALLOCATE PREPARE `stmt`;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------------
-- 示例：教师账号（口令与用户名相同，经 MD5 存储）
-- ---------------------------------------------------------------------------
INSERT INTO `tb_User` (`user_name`, `user_password`, `full_name`, `email`, `role_type`)
VALUES ('101001', MD5('101001'), '赵老师', NULL, 'teacher')
ON DUPLICATE KEY UPDATE
  `role_type` = VALUES(`role_type`),
  `full_name` = VALUES(`full_name`);

-- ---------------------------------------------------------------------------
-- 示例：学生账号（口令与用户名相同，经 MD5 存储）
-- ---------------------------------------------------------------------------
INSERT INTO `tb_User` (`user_name`, `user_password`, `full_name`, `email`, `role_type`)
VALUES
  ('20230001', MD5('20230001'), '张三', 'zhangsan@example.com', 'student'),
  ('20230002', MD5('20230002'), '李四', 'lisi@example.com', 'student')
ON DUPLICATE KEY UPDATE
  `role_type` = VALUES(`role_type`),
  `full_name` = VALUES(`full_name`),
  `email` = VALUES(`email`);
