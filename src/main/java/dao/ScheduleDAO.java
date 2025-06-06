// src/main/java/dao/ScheduleDAO.java
package dao;

import model.Event;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleDAO {
    List<Event> getEvents(LocalDate from, LocalDate to, Long ownerChatId) throws Exception;
}