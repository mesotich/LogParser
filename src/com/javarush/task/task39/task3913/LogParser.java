package com.javarush.task.task39.task3913;

import com.javarush.task.task39.task3913.query.IPQuery;
import com.javarush.task.task39.task3913.query.UserQuery;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LogParser implements IPQuery, UserQuery {

    private final Path logDir;
    private final Set<LogEntry> logs;
    private final DateFormat df;

    public LogParser(Path logDir) {
        this.logDir = logDir;
        logs = new HashSet<>();
        df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        loadLogs();
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

    private class LogEntry {

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

        private boolean betweenDates(Date after, Date before) {
            if (after == null)
                after = new Date(0L);
            if (before == null)
                before = new Date(Long.MAX_VALUE);
            return date.after(after) && date.before(before);
        }
    }
}