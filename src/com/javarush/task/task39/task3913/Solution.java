package com.javarush.task.task39.task3913;

import java.nio.file.Paths;
import java.util.Date;

public class Solution {

    public static void main(String[] args) {
        LogParser logParser = new LogParser(Paths.get("C:\\Users\\Дмитрий\\IdeaProjects\\JavaRush\\LogParser\\src\\com\\javarush\\task\\task39\\task3913\\logs"));
        System.out.println(logParser.execute("get date for user = \"Eduard Petrovich Morozko\" and date between \"11.12.2013 0:00:00\" and \"03.01.2014 23:59:59\"."));


    }
}