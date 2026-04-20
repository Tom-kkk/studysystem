package com.example.stu_backend.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 验证码工具类：生成随机字符与 {@link BufferedImage}，逻辑与教程 CheckcodeUtil 一致。
 */
public final class CheckcodeUtil {

    private static final int WIDTH = 90;
    private static final int HEIGHT = 20;
    private static final int CODE_COUNT = 4;
    private static final Font FONT = new Font("宋体", Font.PLAIN, 16);
    private static final String CHARS = "OPASDFGHQWERTYKLZXCVUIJBNM7014368259cvbnmlkjzxhgfrtyuiopdsaqwe";

    private CheckcodeUtil() {}

    /**
     * @return key {@code checkCode} 为验证码明文；key {@code image} 为图片
     */
    public static Map<String, Object> getCheckCodeAndImage() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_BGR);
        Graphics g = image.getGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.BLUE);
        g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

        StringBuilder sb = new StringBuilder();
        Random ran = new Random();
        for (int i = 0; i < 10; i++) {
            int x1 = ran.nextInt(WIDTH);
            int x2 = ran.nextInt(WIDTH);
            int y1 = ran.nextInt(HEIGHT);
            int y2 = ran.nextInt(HEIGHT);
            g.setColor(new Color((int) (Math.random() * 0x1000000)));
            g.drawLine(x1, y1, x2, y2);
        }

        g.setColor(Color.BLACK);
        g.setFont(FONT);
        for (int i = 1; i <= CODE_COUNT; i++) {
            int index = ran.nextInt(CHARS.length());
            char ch = CHARS.charAt(index);
            sb.append(ch);
            g.drawString(String.valueOf(ch), WIDTH / (CODE_COUNT + 1) * i, HEIGHT / 2 + 5);
        }
        g.dispose();

        String checkCode = sb.toString();
        Map<String, Object> map = new HashMap<>(2);
        map.put("checkCode", checkCode);
        map.put("image", image);
        return map;
    }
}
