package com.javarush.task.task39.task3913.commands;

import java.util.HashMap;
import java.util.Map;

public class CommandStorage implements Storage {

    private Map<String, Command> storage;

    @Override
    public void register(String query, Command command) {
        if (storage == null)
            storage = new HashMap<>();
        if (!storage.containsKey(query))
            storage.put(query, command);
    }

    @Override
    public void execute(String query) {
        if (containsQuery(query))
            storage.get(query).execute();
    }

    @Override
    public boolean containsQuery(String query) {
        return storage.containsKey(query);
    }
}
