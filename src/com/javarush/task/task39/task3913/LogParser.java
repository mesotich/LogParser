package com.javarush.task.task39.task3913;

import com.javarush.task.task39.task3913.commands.Command;
import com.javarush.task.task39.task3913.commands.CommandStorage;
import com.javarush.task.task39.task3913.commands.Storage;
import com.javarush.task.task39.task3913.query.QLQuery;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogParser implements QLQuery {

    private final Path logDir;
    private final Set<LogEntry> logs;
    private final DateFormat df;
    private Storage storage;
    private Set<Object> currentObjects;
    private Set<LogEntry> currentLogs;
    private LogEntry logEntry;
    private Object field;

    public LogParser(Path logDir) {
        this.logDir = logDir;
        logs = new HashSet<>();
        df = new SimpleDateFormat("d.M.yyyy H:m:s");
        loadLogs();
    }

    private void initStorage() {
        currentLogs = logs;
        currentObjects = null;
        field = null;
        logEntry = currentLogs.iterator().next();
        if (storage != null)
            return;
        storage = new CommandStorage();
        storage.register("get ip", createGetCommand(l -> l.ip));
        storage.register("get user", createGetCommand(l -> l.name));
        storage.register("get date", createGetCommand(l -> l.date));
        storage.register("get event", createGetCommand(l -> l.event));
        storage.register("get status", createGetCommand(l -> l.status));
        storage.register("ip", createCommand(l -> l.ip));
        storage.register("user", createCommand(l -> l.name));
        storage.register("date", createCommand(l -> l.date));
        storage.register("event", createCommand(l -> l.event));
        storage.register("status", createCommand(l -> l.status));
    }

    private Command createGetCommand(Function<LogEntry, Object> function) {
        return () -> this.currentObjects = currentLogs.stream()
                .map(function)
                .collect(Collectors.toSet());
    }

    private Command createCommand(Function<LogEntry, Object> function) {
        return () -> this.field = function.apply(logEntry);
    }

    private Object getValue(Class<?> clazz, String value) {
        if (clazz.equals(String.class))
            return value;
        if (clazz.equals(Date.class)) {
            try {
                return df.parse(value);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (clazz.equals(Event.class))
            return Event.valueOf(value);
        if (clazz.equals(Status.class))
            return Status.valueOf(value);
        return "";
    }

    @Override
    public Set<Object> execute(String query) throws IllegalArgumentException {
        int size;
        Set<LogEntry> set;
        initStorage();
        if (storage.containsQuery(query)) {
            storage.execute(query);
            return currentObjects;
        }
        currentLogs = new HashSet<>();
        String[] fields = query.split(" ", 5);
        if (fields.length != 5)
            throw new IllegalArgumentException("Неравильный формат запроса");
        List<String> listValues = listValues(query);
        logs.forEach(l -> {
            logEntry = l;
            storage.execute(fields[3]);
            if (field.equals(getValue(field.getClass(), listValues.get(0)))) {
                currentLogs.add(l);
            }
        });
        size = listValues.size();
        if (size == 2 || size == 4) {
            String field3 = field3(query);
            set = currentLogs;
            currentLogs = new HashSet<>();
            set.forEach(l -> {
                logEntry = l;
                storage.execute(field3);
                if (field.equals(getValue(field.getClass(), listValues.get(1)))) {
                    currentLogs.add(l);
                }
            });
        }
        if (size == 3 || size == 4) {
            String dateBefore = size == 3 ? listValues.get(1) : listValues.get(2);
            String dateAfter = size == 3 ? listValues.get(2) : listValues.get(3);
            Date before = (Date) getValue(Date.class, dateBefore);
            Date after = (Date) getValue(Date.class, dateAfter);
            currentLogs = currentLogs.stream()
                    .filter(l -> l.betweenDates(before, after))
                    .collect(Collectors.toSet());
        }
        String getString = fields[0] + " " + fields[1];
        storage.execute(getString);
        return currentObjects;
    }

    private void loadLogs() {
        Set<Path> set = getLogsFiles();
        LogEntry logEntry;
        String[] parts;
        for (Path path : set
        ) {
            try (BufferedReader br = Files.newBufferedReader(path)) {
                while (br.ready()) {
                    parts = br.readLine().split("\t");
                    if (parts.length != 5)
                        continue;
                    logEntry = new LogEntry(parts[0], parts[1], df.parse(parts[2]), getEvent(parts[3])
                            , getTask(parts[3]), Status.valueOf(parts[4]));
                    logs.add(logEntry);
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private Set<Path> getLogsFiles() {
        Set<Path> result = new HashSet<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(logDir)) {
            for (Path path : ds
            ) {
                if (path.toString().toLowerCase().endsWith(".log"))
                    result.add(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Event getEvent(String eventString) {
        for (Event event : Event.values()
        ) {
            if (eventString.contains(event.toString()))
                return event;
        }
        return null;
    }

    private int getTask(String eventString) {
        Event event = getEvent(eventString);
        String string = "";
        if (event == null)
            return -1;
        if (event.equals(Event.DONE_TASK) || event.equals(Event.SOLVE_TASK))
            string = eventString.replaceAll(event.toString(), "").trim();
        else return -1;
        return Integer.parseInt(string);
    }

    private List<String> listValues(String query) {
        List<String> result = new ArrayList<>();
        Pattern pattern1 = Pattern.compile("\".+?\"");
        Matcher matcher = pattern1.matcher(query);
        while (matcher.find()) {
            result.add(query.substring(matcher.start() + 1, matcher.end() - 1));
        }
        return result;
    }

    private String field3(String query) {
        String result = "";
        Pattern pattern1 = Pattern.compile("(?<=and\s)\\w+(?=\s=)");
        Matcher matcher = pattern1.matcher(query);
        while (matcher.find()) {
            result = query.substring(matcher.start(), matcher.end());
        }
        return result;
    }

    public class LogEntry {

        private final String ip;
        private final String name;
        private final Date date;
        private final Event event;
        private final int task;
        private final Status status;

        public LogEntry(String ip, String name, Date date, Event event, int task, Status status) {
            this.ip = ip;
            this.name = name;
            this.date = date;
            this.event = event;
            this.task = task;
            this.status = status;
        }

        public boolean betweenDates(Date after, Date before) {
            if (after == null)
                after = new Date(0L);
            if (before == null)
                before = new Date(Long.MAX_VALUE);
            return date.after(after) && date.before(before);
        }
    }
}