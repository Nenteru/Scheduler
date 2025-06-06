package model;

import java.util.List;

public class ScheduleAnalysis {
    private int totalEvents;
    private int eventsWithReminders;
    private int eventsWithoutReminders;
    private List<String> busiestDays;

    public ScheduleAnalysis() {}

    public ScheduleAnalysis(int totalEvents, int eventsWithReminders, int eventsWithoutReminders, List<String> busiestDays) {
        this.totalEvents = totalEvents;
        this.eventsWithReminders = eventsWithReminders;
        this.eventsWithoutReminders = eventsWithoutReminders;
        this.busiestDays = busiestDays;
    }

    public int getTotalEvents() { return totalEvents; }
    public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }
    public int getEventsWithReminders() { return eventsWithReminders; }
    public void setEventsWithReminders(int eventsWithReminders) { this.eventsWithReminders = eventsWithReminders; }
    public int getEventsWithoutReminders() { return eventsWithoutReminders; }
    public void setEventsWithoutReminders(int eventsWithoutReminders) { this.eventsWithoutReminders = eventsWithoutReminders; }
    public List<String> getBusiestDays() { return busiestDays; }
    public void setBusiestDays(List<String> busiestDays) { this.busiestDays = busiestDays; }
} 