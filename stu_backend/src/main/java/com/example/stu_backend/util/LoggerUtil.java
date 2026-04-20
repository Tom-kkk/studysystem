package com.example.stu_backend.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 日志工具类：基于 JDK {@link java.util.logging}，与教程 LoggerUtil 一致；
 * 在 {@link PropertiesUtil} 指定的 Win/Linux 根目录下的 {@code logger_file} 子目录中写入文件日志。
 * 不依赖与 FileHandler 冲突的全局 {@code LogManager.readConfiguration}，避免重复 Handler。
 */
public final class LoggerUtil {

    public static final Logger logger = Logger.getLogger("MyLog");

    static {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            // ignore
        }

        String rawRoot = System.getProperty("os.name", "").startsWith("Windows")
                ? PropertiesUtil.pro.getProperty("Win_path")
                : PropertiesUtil.pro.getProperty("Linux_path");
        String sub = PropertiesUtil.pro.getProperty("logger_file");
        Path dirPath = Paths.get(rawRoot == null ? "" : rawRoot.trim(), sub == null ? "report_log" : sub.trim());
        File dir = dirPath.toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            Logger.getLogger(LoggerUtil.class.getName()).warning("无法创建日志目录: " + dirPath);
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Path logFile = dirPath.resolve(df.format(new Date()) + ".log");

        try {
            FileHandler fh = new FileHandler(logFile.toString(), true);
            fh.setFormatter(new SimpleFormatter());
            fh.setEncoding(StandardCharsets.UTF_8.name());
            fh.setLevel(Level.ALL);
            logger.addHandler(fh);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }

        try {
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setEncoding(StandardCharsets.UTF_8.name());
            consoleHandler.setFormatter(new SimpleFormatter());
            consoleHandler.setLevel(Level.INFO);
            logger.addHandler(consoleHandler);

            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LoggerUtil() {}
}
