/**
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * You may not modify, use, reproduce, or distribute this software except in
 * compliance with the terms of the License at:
 * http://java.net/projects/javaeetutorial/pages/BerkeleyLicense
 */
package javaeetutorial.web.websocketbot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import javax.inject.Named;

@Named
public class BotBean {

    private static List<String> scene = new ArrayList<String>();
    private static Set<String> area = new HashSet<String>();
    private static int max_price = 0;
    private static int min_price = 100;

    static {
        try {
            int price = 0;
            String encoding = "GBK";
            File file = new File("旅游景点.txt");
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file), encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = "";
                while ((lineTxt = bufferedReader.readLine()) != null) {
                    scene.add(lineTxt);
                    area.add(lineTxt.split(" ")[1]);
                    price = Integer.parseInt(lineTxt.split(" ")[4].replace("￥", ""));
                    if (price > max_price) {
                        max_price = price;
                    }
                    if (price < min_price) {
                        min_price = price;
                    }
                }
                read.close();
            } else {
                System.out.println("找不到指定的文件");
            }
        } catch (Exception e) {
            System.out.println("读取文件内容出错");
            e.printStackTrace();
        }
    }

    public List<String> getArea(String str) {
        List<String> lst = new ArrayList<String>();
        for (int len = 2; len <= str.length(); len++) {
            for (int j = 0; j < str.length() - len + 1; j++) {
                String str2 = str.substring(j, j + len);
                if (str2.length() < 1) {
                    continue;
                }
                lst.add(str2);
            }
        }
        return lst;
    }

    public String retrieve(List<String> scene, String str) {
        String targetStr = "";
        if (str.equals("")) {
            return targetStr;
        }
        String temp = "";
        int i = 1;
        for (String string : scene) {
            if (string.contains(str)) {
                if (!temp.contains(string)) {
                    temp += string;
                    targetStr += string + "\n";
                }
            }
            i++;
        }
        return targetStr;
    }

    public boolean isNumeric(String str) {
        if (str.matches("[0-9]+")) {
            return true;
        }
        return false;
    }

    public static int findFirstNonNumPos(String str) {  //该函数用来找到">"或"<"后第一个非数字字符的位置
        int start = 0;
        if (str.contains(">")) {
            start = str.indexOf(">") + 1;
        } else if (str.contains("<")) {
            start = str.indexOf("<") + 1;
        }

        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) >= '0' && str.charAt(i) <= '9') {
                continue;
            } else {
                return i;
            }
        }
        return str.length();
    }

    public String onlyForRangeRetrieval(String str) //该函数用来将诸如“河南30~60的景点”中的"30~60提取出来并返回"
    {
        String rst = "";
        int left = 0;
        int right = str.length();
        int start = 0;
        start = str.indexOf("~");
        int i = 0;
        for (i = start - 1; i >= 0; i--) {
            if (str.charAt(i) < '0' || str.charAt(i) > '9') {
                left = i;
                break;
            }
        }
        if(i == -1)
            left = 0;
        else
            left++;
        for (i = start + 1; i < str.length(); i++) {
            if (str.charAt(i) < '0' || str.charAt(i) > '9') {
                right = i;
                break;
            }
        }
        return str.substring(left, right);
    }

    public String conditionalCheck(String msg, String response) //该函数用来检查用户输入是否包含条件查询（仅针对票价字段）
    {
        String rst = "";
        if (response.equals("")) {
            return response;
        }
        if (!msg.contains("<") && !msg.contains(">") && !msg.contains("~")) {
            return response;
        } else {
            String[] str = response.split("\n");
            for (int i = 0; i < str.length; i++) {
                String temp = "";
                if (msg.contains(">")) {
//                    temp = msg.replace(">", "");
                    temp = msg.substring(msg.indexOf(">") + 1, findFirstNonNumPos(msg));
//                    System.out.println("temp in > area: " + temp);
                    float threshold = 0.0f;
                    if (!isNumeric(temp)) {
                        return response;
                    } else {
                        threshold = Float.parseFloat(temp);
                        if (threshold > max_price) {
                            return "票价范围越界!(" + min_price + "~" + max_price + ")";
                        }
//                        System.out.println("threashold: " + threshold);
                        if (Float.parseFloat(str[i].split(" ")[4].replace("￥", "")) > threshold) {
                            rst += str[i] + "\n";
                        }
                    }
                } else if (msg.contains("<")) {
//                    System.out.println("inside < area");
//                    System.out.println("msg.indexOf(\"<\") + 1" + msg.indexOf("<") + 1);
//                    System.out.println("findFirstNonNumPos(msg)" + findFirstNonNumPos(msg));
                    temp = msg.substring(msg.indexOf("<") + 1, findFirstNonNumPos(msg));
                    System.out.println("temp in < area: " + temp);
                    float threshold = 0.0f;
                    if (!isNumeric(temp)) {
                        return response;
                    } else {
                        threshold = Float.parseFloat(temp);
                        System.out.println(threshold);
                        if (threshold < min_price) {
                            return "票价范围越界!(" + min_price + "~" + max_price + ")";
                        }
                        if (Float.parseFloat(str[i].split(" ")[4].replace("￥", "")) < threshold) {
                            rst += str[i] + "\n";
                        }
                    }
                } else if (msg.contains("~")) {
//                    System.out.println("inside < area");
//                    System.out.println("msg.indexOf(\"<\") + 1" + msg.indexOf("<") + 1);
//                    System.out.println("findFirstNonNumPos(msg)" + findFirstNonNumPos(msg));
                    temp = onlyForRangeRetrieval(msg);
                    String[] tst = temp.split("~");
                    System.out.println("temp in ~ area: " + temp);
                    float threshold1 = 0.0f;
                    float threshold2 = 0.0f;
                    if (!isNumeric(tst[0])||!isNumeric(tst[1])) {
                        return response;
                    } else {
                        threshold1 = Float.parseFloat(tst[0]);
                        threshold2 = Float.parseFloat(tst[1]);
//                        System.out.println(threshold);
//                        if (threshold1 < min_price || threshold2 > max_price) {
//                            return "票价范围越界!(" + min_price + "~" + max_price + ")";
//                        }
                        if (Float.parseFloat(str[i].split(" ")[4].replace("￥", "")) > threshold1 && Float.parseFloat(str[i].split(" ")[4].replace("￥", "")) < threshold2) {
                            rst += str[i] + "\n";
                        }
                    }
                }
            }
        }
        return rst;
    }

    /* Respond to a message from the chat */
    public String respond(String msg) {
        String response;

        /* Remove question marks */
        msg = msg.toLowerCase().replaceAll("\\?", "");//移除用户输入中的?
        msg = msg.replace(" ", ""); //移除用户输入中的空格

        response = retrieve(scene, msg);
        response = conditionalCheck(msg, response);
//        System.out.println("ouput in line 138: " + response);

        if (response.equals("")) {
            List<String> lst = getArea(msg);
            String region = "";
            for (int i = 0; i < lst.size(); i++) {
                response += retrieve(scene,lst.get(i).replace("景", "*"));
//                for (String str : area) {
//                    if (str.equals(lst.get(i).toString())) {
//                        region = str;
//                        break;
//                    }
//
//                }
            }
//            System.out.println("region: " + region);
            //response += retrieve(scene, region);
            response = conditionalCheck(msg, response);
//            System.out.println("ouput in line 154: " + response);
        }

        if (!response.equals("")) {
            String[] str = response.split("\n");
            if (!response.contains("越界")) {
                response = "共为您匹配到关于\"" + msg + "\"的" + str.length + "条记录（显示格式为景点&地区&级别&年份&门票）：\n";
                for (int i = 0; i < str.length; i++) {
                    response += (i + 1) + ". " + str[i] + "\n";
                }
                response = response.substring(0, response.length() - 1);  //删除最后一个换行符
            }else{
                response = "指定票价范围内没有相关景点信息！";
            }
        }
        if (response.equals("")) {
            if (msg.contains("谢谢")) {
                response = "不客气，欢迎下次使用！";
            }else if(msg.contains("帮助"))
            {
                response = "您可以直接输入景点名称，如\"长城\"进行查询，或者输入区域名称，如\"河南\"查询河南的所有景点；";
                response += "如欲查询票价在指定范围内的景点信息，可输入\"河南票价>[<]50的景点\"，或\"河南票价在50~60的景点\";";
                response += "完成查询后，您可以直接用鼠标选中景点名称，地图定位功能将为您实时显示景点地理位置的详细信息.";
            }
            else if ((msg.contains("所有景点") || msg.contains("序列号"))) {
                response = "当前数据中收录的景点信息总计" + scene.size() + "条，分别为：\n";
                for (int i = 0; i < scene.size(); i++) {
                    response += (i + 1) + ". " + scene.get(i).split(" ")[0] + "\n";
                }
            } else {
                response = "对不起，系统无法理解您的输入. ";
                response += "您可以直接输入景点名称进行查询. ";
            }
        }
        try {
            Thread.sleep(1200);
        } catch (InterruptedException ex) {
        }
        return response;
    }
}
