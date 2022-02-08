package com.javarush.task.task39.task3913;

import com.javarush.task.task39.task3913.query.IPQuery;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogParser implements IPQuery {

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
    public int getNumberOfUniqueIPs(Date after, Date before) {
        return getUniqueIPs(after, before).size();
    }

    @Override
    public Set<String> getUniqueIPs(Date after, Date before) {
        Set<String> result = new HashSet<>();
        for (LogEntry le : logs
        ) {
            if (le.betweenDates(after, before))
                result.add(le.ip);
        }
        return result;
    }

    @Override
    public Set<String> getIPsForUser(String user, Date after, Date before) {
        Set<String> result = new HashSet<>();
        for (LogEntry le : logs
        ) {
            if (le.betweenDates(after, before) && le.name.equals(user))
                result.add(le.ip);
        }
        return result;
    }

    @Override
    public Set<String> getIPsForEvent(Event event, Date after, Date before) {
        Set<String> result = new HashSet<>();
        for (LogEntry le : logs
        ) {
            if (le.betweenDates(after, before) && le.event.equals(event))
                result.add(le.ip);
        }
        return result;
    }

    @Override
    public Set<String> getIPsForStatus(Status status, Date after, Date before) {
        Set<String> result = new HashSet<>();
        for (LogEntry le : logs
        ) {
            if (le.betweenDates(after, before) && le.status.equals(status))
                result.add(le.ip);
        }
        return result;
    }

    private void loadLogs() {
        Set<Path> set = getLogsFiles();
        LogEntry logEntry;
        String status;
        String[] parts;
        int task = 0;
        for (Path path : set
        ) {
            try (BufferedReader br = Files.newBufferedReader(path)) {
                while (br.ready()) {
                    parts = br.readLine().split("\t");
                    switch (parts.length) {
                        case 5:
                            status = parts[4];
                            break;
                        case 6:
                            status = parts[5];
                            task = Integer.parseInt(parts[4]);
                            break;
                        default:
                            continue;
                    }
                    logEntry = new LogEntry(parts[0], parts[1], df.parse(parts[2]), Event.valueOf(parts[3])
                            , task, Status.valueOf(status));
                    logs.add(logEntry);
                    task = 0;
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