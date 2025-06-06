package service.impl;

import model.Event;
import service.EventService;
import service.ReminderService;
import telegram.TelegramBotView; // Для отправки уведомлений
import util.DateTimeUtils;
import util.MarkdownFormatter;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReminderServiceImpl implements ReminderService {

    private final EventService eventService; // Для получения списка событий
    private TelegramBotView telegramBotView; // Для отправки сообщений (должен быть установлен)
    private ScheduledExecutorService scheduler;
    // private boolean running = false;

    // Как часто проверять напоминания (например, каждую минуту)
    private static final long CHECK_INTERVAL_MINUTES = 1;

    public ReminderServiceImpl(EventService eventService) {
        this.eventService = eventService;
    }

    // Метод для установки View (инъекция зависимости)
    public void setTelegramBotView(TelegramBotView telegramBotView) {
        this.telegramBotView = telegramBotView;
    }

    @Override
    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            System.out.println("[ReminderService] Already started.");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkForUpcomingReminders, 0, CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
        System.out.println("[ReminderService] Started. Checking every " + CHECK_INTERVAL_MINUTES + " minute(s).");
    }

    @Override
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("[ReminderService] Stopped.");
    }

    private void checkForUpcomingReminders() {
        if (telegramBotView == null) {
            // System.err.println("[ReminderService] TelegramBotView is not set. Cannot send reminders.");
            return; // Не отправляем, если View не установлен
        }
        // System.out.println("[ReminderService] Checking for upcoming reminders... (" + LocalDateTime.now() + ")");
        List<Event> allEvents = eventService.getAllEventsGlobally();
        LocalDateTime now = LocalDateTime.now();

        for (Event event : allEvents) {
            if (event.isRemindersEnabled() && event.getReminderTime() != null && !event.isReminderSent()) {
                if (now.isEqual(event.getReminderTime()) || now.isAfter(event.getReminderTime())) {
                    sendReminderNotification(event);
                    event.setReminderSent(true);
                    try {
                        // Обновляем событие в хранилище, чтобы пометить, что напоминание отправлено
                        // Важно: используем ownerChatId из события для корректного обновления
                        eventService.updateEvent(event, event.getOwnerChatId()); 
                    } catch (Exception e) {
                        System.err.println("[ReminderService] Failed to mark reminder as sent for event ID: " + event.getId() + " - " + e.getMessage());
                        // Можно откатить event.setReminderSent(false) если критично, но это усложнит логику
                    }
                }
            }
        }
    }

    private void sendReminderNotification(Event event) {
        if (telegramBotView == null) return;
        
        Long recipientChatId = event.getOwnerChatId();
        if (recipientChatId == null) {
            System.out.println("[ReminderService] Reminder for event '" + event.getTitle() + "' has no ownerChatId. Cannot send notification.");
            return;
        }

        // Экранируем все динамические части
        String eventTitle = MarkdownFormatter.escapeMarkdownV2(event.getTitle());
        String startTime = MarkdownFormatter.escapeMarkdownV2(DateTimeUtils.formatMedium(event.getStartTime()));
        String endTime = MarkdownFormatter.escapeMarkdownV2(DateTimeUtils.formatMedium(event.getEndTime()));
        
        String description = "";
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            description = "\n📝 Описание: _" + MarkdownFormatter.escapeMarkdownV2(event.getDescription()) + "_";
        }
        
        String location = "";
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            location = "\n📍 Место: _" + MarkdownFormatter.escapeMarkdownV2(event.getLocation()) + "_";
        }

        StringBuilder messageText = new StringBuilder();
        messageText.append("🔔 *Напоминание о событии\\!*\n\n");
        messageText.append("*").append(eventTitle).append("*\n\n");
        messageText.append("📅 Начало: ").append(startTime).append("\n");
        messageText.append("⌚️ Окончание: ").append(endTime);
        messageText.append(description); // Уже содержит \n, если есть
        messageText.append(location);    // Уже содержит \n, если есть

        SendMessage message = new SendMessage();
        message.setChatId(recipientChatId.toString());
        message.setParseMode("MarkdownV2");
        message.setText(messageText.toString());

        try {
            telegramBotView.executeSendMessage(message);
            System.out.println("[ReminderService] Reminder notification sent successfully to chatId: " + recipientChatId);
        } catch (Exception e) {
            System.err.println("[ReminderService] Failed to send reminder notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 