package com.javarush.task.task39.task3913;

import java.nio.file.Paths;
import java.util.Date;

public class Solution {

    public static void main(String[] args) {
        LogParser logParser = new LogParser(Paths.get("C:\\Users\\Дмитрий\\IdeaProjects\\JavaRush\\LogParser\\src\\com\\javarush\\task\\task39\\task3913\\logs"));
        System.out.println(logParser.execute("get event for date = \"5.1.2021 20:22:55\""));
    }
}