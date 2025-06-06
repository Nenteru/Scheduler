package model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Event {
    private String id; // Может быть UUID или генерируемый БД
    private String googleId; // ID события из Google Calendar
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private LocalDateTime reminderTime; // Простое напоминание для начала
    private boolean remindersEnabled = true; // Напоминания включены по умолчанию
    private Long ownerChatId; // ID чата владельца события для отправки напоминаний
    private boolean reminderSent = false; // Флаг, что напоминание уже было отправлено

    // Конструкторы
    public Event() {
    }

    // Конструктор без googleId и ownerChatId (можно пометить как @Deprecated или оставить для совместимости)
    public Event(String id, String title, String description, LocalDateTime startTime, LocalDateTime endTime, String location, LocalDateTime reminderTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.reminderTime = reminderTime;
        this.remindersEnabled = true; // По умолчанию напоминания включены для нового события
    }

    // Основной конструктор, включающий все поля, включая googleId и ownerChatId
    public Event(String id, String googleId, String title, String description, LocalDateTime startTime, LocalDateTime endTime, String location, LocalDateTime reminderTime, Long ownerChatId) {
        this.id = id;
        this.googleId = googleId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.reminderTime = reminderTime;
        this.remindersEnabled = true;
        this.ownerChatId = ownerChatId;
        this.reminderSent = false; // Новое событие - напоминание не отправлено
    }
    
    // Старый конструктор с ownerChatId, но без googleId - можно обновить или оставить и использовать новый основной
    // Для обратной совместимости и меньших правок в существующих вызовах, можно обновить его, добавив null для googleId
    public Event(String id, String title, String description, LocalDateTime startTime, LocalDateTime endTime, String location, LocalDateTime reminderTime, Long ownerChatId) {
        this(id, null, title, description, startTime, endTime, location, reminderTime, ownerChatId);
    }

    // Геттеры и Сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGoogleId() { return googleId; } // Новый геттер
    public void setGoogleId(String googleId) { this.googleId = googleId; } // Новый сеттер
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalDateTime reminderTime) { this.reminderTime = reminderTime; }
    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }
    public void setRemindersEnabled(boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }
    public Long getOwnerChatId() { return ownerChatId; }
    public void setOwnerChatId(Long ownerChatId) { this.ownerChatId = ownerChatId; }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }

    // equals, hashCode, toString для удобства
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        // Сравниваем по ID, если он есть, иначе по другим полям для новых событий
        if (id != null && event.id != null) {
            return id.equals(event.id);
        }
        // Пример сравнения для новых событий (можно уточнить)
        return Objects.equals(title, event.title) &&
                Objects.equals(startTime, event.startTime) &&
                Objects.equals(endTime, event.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, startTime, endTime);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", googleId='" + googleId + '\'' +
                ", title='" + title + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", remindersEnabled=" + remindersEnabled +
                ", ownerChatId=" + ownerChatId +
                ", reminderSent=" + reminderSent +
                '}';
    }
}
