package com.javarush.task.task39.task3913;

import java.nio.file.Paths;
import java.util.Date;

public class Solution {

    public static void main(String[] args) {
        LogParser logParser = new LogParser(Paths.get("C:\\Users\\Дмитрий\\IdeaProjects\\JavaRush\\LogParser\\src\\com\\javarush\\task\\task39\\task3913\\logs"));
        System.out.println(logParser.getIPsForStatus(Status.OK,null, null));
    }
}