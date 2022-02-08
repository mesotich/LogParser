package com.javarush.task.task39.task3913;

import com.javarush.task.task39.task3913.query.IPQuery;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LogParser implements IPQuery {

    private final Path logDir;
    private TreeMap<Date, String> logs;

    public LogParser(Path logDir) {
        this.logDir = logDir;
        initialize();
    }

    @Override
    public int getNumberOfUniqueIPs(Date after, Date before) {
        if (after == null)
            after = logs.firstKey();
        if (before == null)
            before = logs.lastKey();
        return getUniqueIPs(after, before).size();
    }

    @Override
    public Set<String> getUniqueIPs(Date after, Date before) {
        if (after == null)
            after = logs.firstKey();
        if (before == null)
            before = logs.lastKey();
        return logs.subMap(after, before)
                .values()
                .stream()
                .map(this::getIp)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getIPsForUser(String user, Date after, Date before) {
        if (after == null)
            after = logs.firstKey();
        if (before == null)
            before = logs.lastKey();
        return logs.subMap(after, before)
                .values()
                .stream()
                .filter(l -> getUser(l).equals(user))
                .map(this::getIp)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getIPsForEvent(Event event, Date after, Date before) {
        if (after == null)
            after = logs.firstKey();
        if (before == null)
            before = logs.lastKey();
        return logs.subMap(after, before)
                .values()
                .stream()
                .filter(l -> getEvent(l).equals(event))
                .map(this::getIp)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getIPsForStatus(Status status, Date after, Date before) {
        if (after == null)
            after = logs.firstKey();
        if (before == null)
            before = logs.lastKey();
        return logs.subMap(after, before)
                .values()
                .stream()
                .filter(l -> getStatus(l).equals(status))
                .map(this::getIp)
                .collect(Collectors.toSet());
    }

    private void initialize() {
        if (logs != null)
            return;
        logs = new TreeMap<>(Comparator.comparingLong(Date::getTime));
        String log;
        Set<Path> paths = null;
        try {
            paths = Files.list(logDir)
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().toLowerCase().endsWith(".log"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (paths == null)
            return;
        for (Path path : paths
        ) {
            try (BufferedReader br = Files.newBufferedReader(path)) {
                while (br.ready()) {
                    log = br.readLine();
                    logs.put(getDate(log), log);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getIp(String log) {
        String[] string = log.split("\t");
        return string[0];
    }

    private String getUser(String log) {
        String[] string = log.split("\t");
        return string[1];
    }

    private Event getEvent(String log) {
        String[] string = log.split("\t");
        return Event.valueOf(string[3]);
    }

    private Status getStatus(String log) {
        String[] strings = log.split("\t");
        String string = strings.length == 5 ? strings[4] : strings[5];
        return Status.valueOf(string);
    }

    private Date getDate(String log) {
        Date date = null;
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String[] string = log.split("\t");
        try {
            date = df.parse(string[2]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
}