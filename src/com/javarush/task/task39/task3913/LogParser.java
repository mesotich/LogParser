package com.javarush.task.task39.task3913;

import com.javarush.task.task39.task3913.commands.Command;
import com.javarush.task.task39.task3913.commands.CommandStorage;
import com.javarush.task.task39.task3913.commands.Storage;
import com.javarush.task.task39.task3913.query.*;

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

public class LogParser implements IPQuery, UserQuery, DateQuery, EventQuery, QLQuery {

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

    public void initStorage() {
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
    public Set<Object> execute(String query) {
        initStorage();
        if (storage.containsQuery(query)) {
            storage.execute(query);
            return currentObjects;
        }
        currentLogs = new HashSet<>();
        String[] fields = query.split(" ", 5);
        if (fields.length != 5)
            return new HashSet<>();
        List<String> listValues = listValues(query);
        logs.forEach(l -> {
            logEntry = l;
            storage.execute(fields[3]);
            if (field.equals(getValue(field.getClass(), listValues.get(0)))) {
                currentLogs.add(l);
            }
        });
        if (listValues.size() == 3) {
            Date before = (Date) getValue(Date.class, listValues.get(1));
            Date after = (Date) getValue(Date.class, listValues.get(2));
            currentLogs = currentLogs.stream()
                    .filter(l -> l.betweenDates(before, after))
                    .collect(Collectors.toSet());
        }
        String getString = fields[0] + " " + fields[1];
        storage.execute(getString);
        return currentObjects;
    }

    @Override
    public int getNumberOfAllEvents(Date after, Date before) {
        return getAllEvents(after, before).size();
    }

    @Override
    public Set<Event> getAllEvents(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .map(l -> l.event)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Event> getEventsForIP(String ip, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.ip.equals(ip))
                .map(l -> l.event)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Event> getEventsForUser(String user, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.name.equals(user))
                .map(l -> l.event)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Event> getFailedEvents(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.status.equals(Status.FAILED))
                .map(l -> l.event)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Event> getErrorEvents(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.status.equals(Status.ERROR))
                .map(l -> l.event)
                .collect(Collectors.toSet());
    }

    @Override
    public int getNumberOfAttemptToSolveTask(int task, Date after, Date before) {
        return (int) logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.SOLVE_TASK))
                .filter(l -> l.task == task)
                .count();
    }

    @Override
    public int getNumberOfSuccessfulAttemptToSolveTask(int task, Date after, Date before) {
        return (int) logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.DONE_TASK))
                .filter(l -> l.task == task)
                .count();
    }

    @Override
    public Map<Integer, Integer> getAllSolvedTasksAndTheirNumber(Date after, Date before) {
        Map<Integer, Integer> result = new HashMap<>();
        logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.SOLVE_TASK))
                .forEach(l -> {
                    int delta = (result.containsKey(l.task))
                            ? result.get(l.task) + 1 : 1;
                    result.put(l.task, delta);
                });
        return result;
    }

    @Override
    public Map<Integer, Integer> getAllDoneTasksAndTheirNumber(Date after, Date before) {
        Map<Integer, Integer> result = new HashMap<>();
        logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.DONE_TASK))
                .forEach(l -> {
                    int delta = (result.containsKey(l.task))
                            ? result.get(l.task) + 1 : 1;
                    result.put(l.task, delta);
                });
        return result;
    }

    @Override
    public Set<Date> getDatesForUserAndEvent(String user, Event event, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.name.equals(user))
                .filter(l -> l.event.equals(event))
                .map(l -> l.date)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Date> getDatesWhenSomethingFailed(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.status.equals(Status.FAILED))
                .map(l -> l.date)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Date> getDatesWhenErrorHappened(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.status.equals(Status.ERROR))
                .map(l -> l.date)
                .collect(Collectors.toSet());
    }

    @Override
    public Date getDateWhenUserLoggedFirstTime(String user, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.name.equals(user))
                .filter(l -> l.status.equals(Status.OK))
                .map(l -> l.date)
                .collect(Collectors.toCollection(TreeSet::new))
                .pollFirst();
    }

    @Override
    public Date getDateWhenUserSolvedTask(String user, int task, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.name.equals(user))
                .filter(l -> l.event.equals(Event.SOLVE_TASK))
                .filter(l -> l.task == task)
                .map(l -> l.date)
                .collect(Collectors.toCollection(TreeSet::new))
                .pollFirst();
    }

    @Override
    public Date getDateWhenUserDoneTask(String user, int task, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.name.equals(user))
                .filter(l -> l.event.equals(Event.DONE_TASK))
                .filter(l -> l.task == task)
                .map(l -> l.date)
                .collect(Collectors.toCollection(TreeSet::new))
                .pollFirst();
    }

    @Override
    public Set<Date> getDatesWhenUserWroteMessage(String user, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.name.equals(user))
                .filter(l -> l.event.equals(Event.WRITE_MESSAGE))
                .map(l -> l.date)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Date> getDatesWhenUserDownloadedPlugin(String user, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.name.equals(user))
                .filter(l -> l.event.equals(Event.DOWNLOAD_PLUGIN))
                .map(l -> l.date)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getAllUsers() {
        return logs.stream()
                .map(l -> l.name)
                .collect(Collectors.toSet());
    }

    @Override
    public int getNumberOfUsers(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .map(l -> l.name)
                .collect(Collectors.toSet())
                .size();
    }

    @Override
    public int getNumberOfUserEvents(String user, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.name.equals(user))
                .map(l -> l.event)
                .collect(Collectors.toSet())
                .size();
    }

    @Override
    public Set<String> getUsersForIP(String ip, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.ip.equals(ip))
                .map(l -> l.name)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getLoggedUsers(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.LOGIN))
                .map(l -> l.name)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getDownloadedPluginUsers(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.DOWNLOAD_PLUGIN))
                .map(l -> l.name)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getWroteMessageUsers(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.WRITE_MESSAGE))
                .map(l -> l.name)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getSolvedTaskUsers(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.SOLVE_TASK))
                .map(l -> l.name)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getSolvedTaskUsers(Date after, Date before, int task) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.SOLVE_TASK))
                .filter(l -> l.task == task)
                .map(l -> l.name)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getDoneTaskUsers(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.DONE_TASK))
                .map(l -> l.name)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getDoneTaskUsers(Date after, Date before, int task) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(Event.DONE_TASK))
                .filter(l -> l.task == task)
                .map(l -> l.name)
                .collect(Collectors.toSet());
    }

    @Override
    public int getNumberOfUniqueIPs(Date after, Date before) {
        return getUniqueIPs(after, before).size();
    }

    @Override
    public Set<String> getUniqueIPs(Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .map(l -> l.ip)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getIPsForUser(String user, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.name.equals(user))
                .map(l -> l.ip)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getIPsForEvent(Event event, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.event.equals(event))
                .map(l -> l.ip)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getIPsForStatus(Status status, Date after, Date before) {
        return logs.stream()
                .filter(l -> l.betweenDates(after, before))
                .filter(l -> l.status.equals(status))
                .map(l -> l.ip)
                .collect(Collectors.toSet());
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

    private Object getRecord(LogEntry logEntry, String command) {
        switch (command) {
            case "ip":
                return logEntry.ip;
            case "user":
                return logEntry.name;
            case "date":
                return logEntry.date;
            case "event":
                return logEntry.event;
            case "status":
                return logEntry.status;
        }
        return new Object();
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

    public static class LogEntry {

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

        public int getTask() {
            return task;
        }
    }
}