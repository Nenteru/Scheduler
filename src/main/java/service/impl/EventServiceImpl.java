package service.impl;

import dao.EventDAO;
import model.Event;
import service.EventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class EventServiceImpl implements EventService {

    private final EventDAO eventDAO;

    public EventServiceImpl(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    @Override
    public Event addEvent(Event newEvent, Long ownerChatId) throws IllegalArgumentException {
        // Устанавливаем ownerChatId для события сразу. Это важно для корректной привязки.
        newEvent.setOwnerChatId(ownerChatId);

        System.out.println("[Service] Processing event: " + newEvent.getTitle() +
                           " (ID: " + newEvent.getId() + ", GoogleID: " + newEvent.getGoogleId() + 
                           ") for ownerChatId: " + ownerChatId);

        // Логика для событий, пришедших из Google Calendar (имеют Google ID)
        if (newEvent.getGoogleId() != null && !newEvent.getGoogleId().isEmpty()) {
            // Ищем существующее локальное событие по Google ID И ID владельца
            Event existingLocalEvent = eventDAO.findByGoogleIdAndOwnerChatId(newEvent.getGoogleId(), ownerChatId).orElse(null);

            if (existingLocalEvent != null) {
                System.out.println("[Service] Event with Google ID '" + newEvent.getGoogleId() + 
                                   "' for owner '" + ownerChatId + "' already exists locally. Checking for updates.");
                
                // Сохраняем локальный ID существующего события, чтобы обновить его, а не создавать дубликат
                newEvent.setId(existingLocalEvent.getId()); 

                boolean needsUpdate = !newEvent.getTitle().equals(existingLocalEvent.getTitle()) ||
                                      (newEvent.getDescription() != null ? !newEvent.getDescription().equals(existingLocalEvent.getDescription()) : existingLocalEvent.getDescription() != null) ||
                                      !newEvent.getStartTime().equals(existingLocalEvent.getStartTime()) ||
                                      !newEvent.getEndTime().equals(existingLocalEvent.getEndTime()) ||
                                      (newEvent.getLocation() != null ? !newEvent.getLocation().equals(existingLocalEvent.getLocation()) : existingLocalEvent.getLocation() != null) ||
                                      newEvent.isRemindersEnabled() != existingLocalEvent.isRemindersEnabled() ||
                                      (newEvent.getReminderTime() != null ? !newEvent.getReminderTime().equals(existingLocalEvent.getReminderTime()) : existingLocalEvent.getReminderTime() != null);
                
                if (needsUpdate) {
                    System.out.println("[Service] Changes detected for event '" + newEvent.getTitle() + "'. Attempting to update.");
                    // ownerChatId уже установлен в newEvent
                    Event updatedEvent = eventDAO.updateEvent(newEvent); // DAO должен обновить по newEvent.getId()
                    System.out.println("[Service] Successfully updated event '" + updatedEvent.getTitle() + 
                                       "' (ID: " + updatedEvent.getId() + ") OwnerChatID: " + updatedEvent.getOwnerChatId());
                    return updatedEvent;
                } else {
                    System.out.println("[Service] No changes detected for event '" + newEvent.getTitle() + "'. Skipping update.");
                    return existingLocalEvent; // Возвращаем существующее без изменений
                }
            } else {
                // Событие из Google, но его нет локально у этого пользователя. Добавляем как новое.
                // ownerChatId уже установлен в newEvent. Локальный ID будет сгенерирован в DAO.
                newEvent.setId(null); // Явно указываем, что ID должен быть сгенерирован (если DAO это делает)
                System.out.println("[Service] New event from Google (GoogleID: "+ newEvent.getGoogleId() +
                                   ") for owner " + ownerChatId + ". Adding as new local event.");
            }
        } else {
            // Это новое локальное событие (Google ID отсутствует)
            // ownerChatId уже установлен. Локальный ID будет сгенерирован в DAO.
            newEvent.setId(null); // Явно указываем, что ID должен быть сгенерирован
            System.out.println("[Service] New local event. OwnerChatId: " + ownerChatId);
        }

        // Для всех новых событий (локальных или из Google, которых не было)
        System.out.println("[Service] Attempting to create as a new event: " + newEvent.getTitle() + 
                           " with ownerChatId: " + newEvent.getOwnerChatId());
        Event createdEvent = eventDAO.createEvent(newEvent);
        System.out.println("[Service] Successfully created new event: " + createdEvent.getTitle() + 
                           " (ID: " + createdEvent.getId() + ") OwnerChatID: " + createdEvent.getOwnerChatId());
        return createdEvent;
    }

    @Override
    public Event updateEvent(Event event, Long ownerChatId) {
        if (event.getId() == null || event.getId().isEmpty()) {
            throw new IllegalArgumentException("Event ID must be provided for an update.");
        }
        if (event.getStartTime() == null || event.getEndTime() == null || event.getEndTime().isBefore(event.getStartTime())) {
            throw new IllegalArgumentException("Invalid event time: start time must be before end time.");
        }
        
        // Проверяем, что событие действительно принадлежит пользователю перед обновлением
        Event existingEvent = eventDAO.findByIdAndOwnerChatId(event.getId(), ownerChatId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + event.getId() + " for owner: " + ownerChatId));
        
        // Устанавливаем ownerChatId из параметра, чтобы гарантировать корректность
        event.setOwnerChatId(ownerChatId);
        // Также копируем Google ID из существующего события, если он был, чтобы не потерять его при обновлении.
        // Особенно если 'event' пришел из UI и не содержит googleId.
        if (existingEvent.getGoogleId() != null && event.getGoogleId() == null) {
            event.setGoogleId(existingEvent.getGoogleId());
        }

        System.out.println("[Service] Updating event: " + event.getTitle() + " for owner: " + ownerChatId);
        return eventDAO.updateEvent(event);
    }

    @Override
    public void deleteEvent(String eventId, Long ownerChatId) {
        System.out.println("[Service] Deleting event ID: " + eventId + " for owner: " + ownerChatId);
        // DAO должен проверить принадлежность и удалить, или бросить исключение если не найдено/не принадлежит
        eventDAO.deleteByIdAndOwnerChatId(eventId, ownerChatId);
    }

    @Override
    public Optional<Event> getEventByIdAndOwner(String eventId, Long ownerChatId) {
        System.out.println("[Service] Getting event by ID: " + eventId + " for owner: " + ownerChatId);
        return eventDAO.findByIdAndOwnerChatId(eventId, ownerChatId);
    }

    @Override
    public List<Event> getEventsForOwner(Long ownerChatId) {
        System.out.println("[Service] Getting all events for owner: " + ownerChatId);
        return eventDAO.findAllByOwnerChatId(ownerChatId);
    }

    @Override
    public List<Event> getEventsForPeriodForOwner(LocalDateTime start, LocalDateTime end, Long ownerChatId) {
        System.out.println("[Service] Getting events from " + start + " to " + end + " for owner: " + ownerChatId);
        return eventDAO.findEventsBetweenForOwner(start, end, ownerChatId);
    }

    @Override
    public Event setEventReminderTime(String eventId, LocalDateTime reminderTime, Long ownerChatId) {
        Event event = eventDAO.findByIdAndOwnerChatId(eventId, ownerChatId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId + " for owner: " + ownerChatId));
        
        event.setReminderTime(reminderTime);
        event.setReminderSent(false); // Сбрасываем флаг отправки при изменении времени
        System.out.println("[Service] Reminder time for event '" + event.getTitle() + 
                           "' (ID: " + eventId + ") set to: " + reminderTime + ". Owner: " + ownerChatId);
        return eventDAO.updateEvent(event); // DAO обновит событие по его ID, ownerChatId в объекте event уже корректен
    }

    @Override
    public Event toggleEventReminders(String eventId, boolean enable, Long ownerChatId) {
        Event event = eventDAO.findByIdAndOwnerChatId(eventId, ownerChatId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId + " for owner: " + ownerChatId));
        
        event.setRemindersEnabled(enable);
        System.out.println("[Service] Reminders for event '" + event.getTitle() + 
                           "' (ID: " + eventId + ") " + (enable ? "ENABLED" : "DISABLED") + ". Owner: " + ownerChatId);
        return eventDAO.updateEvent(event); // Аналогично setEventReminderTime
    }

    @Override
    public List<Event> getAllEventsGlobally() {
        System.out.println("[Service] Getting all events globally for reminder service.");
        return eventDAO.getAllEventsGlobally();
    }
}