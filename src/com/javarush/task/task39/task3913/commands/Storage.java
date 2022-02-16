package com.javarush.task.task39.task3913.commands;

import com.javarush.task.task39.task3913.commands.Command;

public interface Storage {

    void register(String query, Command command);

    void execute(String query);

    boolean containsQuery(String query);
}
