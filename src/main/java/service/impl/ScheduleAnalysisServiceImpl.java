package service.impl;

import dao.EventDAO;
import model.Event;
import model.ScheduleAnalysis;
import service.ScheduleAnalysisService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class ScheduleAnalysisServiceImpl implements ScheduleAnalysisService {
    private final EventDAO eventDAO;

    public ScheduleAnalysisServiceImpl(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    @Override
    public ScheduleAnalysis analyzeCurrentWeek(Long ownerChatId) {
        LocalDate today = LocalDate.now();
        // Начало недели (понедельник)
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        // Конец недели (воскресенье, конец дня)
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekEnd.atTime(LocalTime.MAX);

        List<Event> events = eventDAO.findEventsBetweenForOwner(start, end, ownerChatId);
        int total = events.size();
        int withReminders = (int) events.stream().filter(Event::isRemindersEnabled).count();
        int withoutReminders = total - withReminders;

        // Группировка по дням недели
        Map<DayOfWeek, Long> countByDay = events.stream()
                .collect(Collectors.groupingBy(e -> e.getStartTime().getDayOfWeek(), Collectors.counting()));
        // Находим самые загруженные дни
        long max = countByDay.values().stream().max(Long::compareTo).orElse(0L);
        List<String> busiestDays = countByDay.entrySet().stream()
                .filter(e -> e.getValue() == max && max > 0)
                .map(e -> e.getKey().toString())
                .collect(Collectors.toList());

        return new ScheduleAnalysis(total, withReminders, withoutReminders, busiestDays);
    }
} 