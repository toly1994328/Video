package com.toly1994.video;

import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作者：张风捷特烈<br/>
 * 时间：2019/1/8 0008:9:01<br/>
 * 邮箱：1981462002@qq.com<br/>
 * 说明：
 */
public class Svg2Xml {

    @Test
    public void svgDir() {
        String dirPath = "E:\\Material\\MyUI\\#svg\\factory";
        svg2xmlFromDir(dirPath);
    }

    @Test
    public void singleSvg() {
        File file = new File("E:\\Material\\MyUI\\#svg\\相机\\icon_switch.svg");
        svg2xml(file);
    }

    /**
     * 将一个文件夹里的所有svg转换为xml
     *
     * @param filePath
     */
    public static void svg2xmlFromDir(String filePath) {

        File file = new File(filePath);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.getName().endsWith(".svg")) {
                    System.out.println(f);
                    svg2xml(f);
                }
            }
        } else {
            svg2xml(file);
        }
    }

    /**
     * 将.svg文件转换为安卓可用的.xml
     *
     * @param file 文件路径
     */
    public static void svg2xml(File file) {
        if (!file.exists() && file.isDirectory()) {
            return;
        }

        FileWriter fw = null;
        FileReader fr = null;
        ArrayList<String> paths = new ArrayList<>();
        try {
            fr = new FileReader(file);
            //字符数组循环读取
            char[] buf = new char[1024];
            int len = 0;
            StringBuilder sb = new StringBuilder();
            while ((len = fr.read(buf)) != -1) {
                sb.append(new String(buf, 0, len));
            }
            //收集所有path
            collectPaths(sb.toString(), paths);

            //拼接字符串
            StringBuilder outSb = contactStr(paths);
            //写出到磁盘
            File outFile = new File(file.getParentFile(), file.getName().substring(0, file.getName().lastIndexOf(".")) + ".xml");
            fw = new FileWriter(outFile);
            fw.write(outSb.toString());

            System.out.println("转换成功：" + file.getName());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fw != null) {
                    fw.close();
                }
                if (fr != null) {
                    fr.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 拼接字符串
     *
     * @param paths
     * @return
     */
    private static StringBuilder contactStr(ArrayList<String> paths) {
        StringBuilder outSb = new StringBuilder();
        outSb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "        android:width=\"48dp\"\n" +
                "        android:height=\"48dp\"\n" +
                "        android:viewportWidth=\"1024\"\n" +
                "        android:viewportHeight=\"1024\">\n");
        for (String path : paths) {
            if (path.length() > 0) {
                outSb.append("    <path\n" +
                        "        android:fillColor=\"#FF7F47\"\nandroid:pathData=\"");
                outSb.append(path);
                outSb.append("\"/>");
            }
        }
        outSb.append("</vector>");
        return outSb;
    }

    /**
     * 收集所有path
     *
     * @param result
     * @param paths
     */
    private static void collectPaths(String result, ArrayList<String> paths) {
        String regex = " d=\"(?<res>(.)*?)\"";
        Matcher matcher = Pattern.compile(regex).matcher(result);
        while (matcher.find()) {
            String path = matcher.group("res");
            paths.add(path);
        }
    }
}
