package dao;

import model.Event;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventDAO {
    /**
     * Создает новое событие. 
     * Объект Event должен содержать ownerChatId и, возможно, googleId.
     * @param event Событие для сохранения.
     * @return Сохраненное событие (может содержать сгенерированный ID).
     */
    Event createEvent(Event event);

    /**
     * Обновляет существующее событие.
     * Объект Event должен содержать ID существующего события и ownerChatId.
     * @param event Обновленное событие.
     * @return Обновленное событие.
     * @throws IllegalArgumentException если событие с таким ID не найдено или ownerChatId в событии не совпадает (в зависимости от реализации DAO).
     */
    Event updateEvent(Event event);

    /**
     * Находит событие по его локальному ID и ID владельца.
     * @param eventId Локальный ID события.
     * @param ownerChatId ID чата владельца.
     * @return Optional с событием, если найдено, иначе Optional.empty().
     */
    Optional<Event> findByIdAndOwnerChatId(String eventId, Long ownerChatId);

    /**
     * Находит событие по его Google Calendar ID и ID владельца.
     * @param googleId ID события из Google Calendar.
     * @param ownerChatId ID чата владельца.
     * @return Optional с событием, если найдено, иначе Optional.empty().
     */
    Optional<Event> findByGoogleIdAndOwnerChatId(String googleId, Long ownerChatId);

    /**
     * Возвращает список всех событий для указанного владельца.
     * @param ownerChatId ID чата владельца.
     * @return Список событий.
     */
    List<Event> findAllByOwnerChatId(Long ownerChatId);

    /**
     * Возвращает список событий для указанного владельца в заданном временном интервале.
     * @param start Начало интервала (включительно).
     * @param end Конец интервала (исключительно).
     * @param ownerChatId ID чата владельца.
     * @return Список событий.
     */
    List<Event> findEventsBetweenForOwner(LocalDateTime start, LocalDateTime end, Long ownerChatId);

    /**
     * Удаляет событие по его локальному ID и ID владельца.
     * @param eventId Локальный ID события для удаления.
     * @param ownerChatId ID чата владельца.
     */
    void deleteByIdAndOwnerChatId(String eventId, Long ownerChatId);

    /**
     * Возвращает список абсолютно всех событий из хранилища (использовать с осторожностью).
     * Этот метод предназначен для специальных случаев, таких как работа ReminderService,
     * который должен проверять все события всех пользователей.
     * @return Список всех событий в системе.
     */
    List<Event> getAllEventsGlobally();

    // Старые методы, которые были заменены или стали не нужны:
    // Optional<Event> getEventById(String eventId);
    // List<Event> getAllEvents();
    // List<Event> findEventsBetween(LocalDateTime start, LocalDateTime end);
    // boolean deleteEvent(String eventId);
    // boolean existsByGoogleId(String googleId);
    // Event findByGoogleId(String googleId);
}