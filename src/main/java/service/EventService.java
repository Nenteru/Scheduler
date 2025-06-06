package service;

import model.Event;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventService {
    /**
     * Добавляет новое событие или обновляет существующее (если из Google Sync).
     * @param event Событие для добавления/обновления.
     * @param ownerChatId ID чата пользователя, создающего событие или для которого синхронизируется событие.
     * @return Добавленное или обновленное событие.
     * @throws IllegalArgumentException если время события пересекается с существующим (для новых).
     */
    Event addEvent(Event event, Long ownerChatId);

    /**
     * Обновляет событие, принадлежащее указанному пользователю.
     * @param event Обновляемое событие (должно содержать ID существующего события).
     * @param ownerChatId ID чата пользователя, которому принадлежит событие.
     * @return Обновленное событие.
     * @throws IllegalArgumentException если время события пересекается с другим существующим событием или событие не найдено/не принадлежит пользователю.
     */
    Event updateEvent(Event event, Long ownerChatId);

    /**
     * Удаляет событие по ID, если оно принадлежит указанному пользователю.
     * @param eventId ID события.
     * @param ownerChatId ID чата пользователя, которому принадлежит событие.
     */
    void deleteEvent(String eventId, Long ownerChatId);

    /**
     * Получает событие по ID, если оно принадлежит указанному пользователю.
     * @param eventId ID события.
     * @param ownerChatId ID чата пользователя, которому принадлежит событие.
     * @return Optional с событием.
     */
    Optional<Event> getEventByIdAndOwner(String eventId, Long ownerChatId);

    /**
     * Получает все события для указанного пользователя.
     * @param ownerChatId ID чата пользователя.
     * @return Список событий пользователя.
     */
    List<Event> getEventsForOwner(Long ownerChatId);

    /**
     * Получает события за указанный период для указанного пользователя.
     * @param start Начало периода.
     * @param end Конец периода.
     * @param ownerChatId ID чата пользователя.
     * @return Список событий в периоде для пользователя.
     */
    List<Event> getEventsForPeriodForOwner(LocalDateTime start, LocalDateTime end, Long ownerChatId);

    /**
     * Устанавливает время напоминания для события, если оно принадлежит указанному пользователю.
     * @param eventId ID события.
     * @param reminderTime Время напоминания (null для удаления).
     * @param ownerChatId ID чата пользователя.
     * @return Обновленное событие.
     */
    Event setEventReminderTime(String eventId, LocalDateTime reminderTime, Long ownerChatId);

    /**
     * Включает/выключает напоминания для события, если оно принадлежит указанному пользователю.
     * @param eventId ID события.
     * @param enable true для включения, false для выключения.
     * @param ownerChatId ID чата пользователя.
     * @return Обновленное событие.
     */
    Event toggleEventReminders(String eventId, boolean enable, Long ownerChatId);

    /**
     * Возвращает список абсолютно всех событий из хранилища (использовать с осторожностью).
     * Предназначен для ReminderService.
     * @return Список всех событий в системе.
     */
    List<Event> getAllEventsGlobally();
}
