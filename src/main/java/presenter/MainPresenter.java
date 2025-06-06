package presenter;

import model.Event;
import java.time.LocalDateTime;

// Интерфейс для Presenter главного окна
public interface MainPresenter {
    void loadEvents(Long ownerChatId); // Загрузить события для конкретного пользователя
    void addEventRequested(); // Пользователь нажал "Добавить событие"
    void editEventRequested(Event event); // Пользователь выбрал событие для редактирования
    void deleteEventRequested(String eventId, Long ownerChatId); // Пользователь выбрал событие для удаления, указывая ID и владельца
    void eventSelected(Event event); // Пользователь выбрал событие в списке/календаре
    void addEvent(Event event, Long ownerChatId); // Используется для создания локальных событий или обработки синхронизированных

    // Методы для управления напоминаниями
    void setEventReminderTimeRequested(String eventId, LocalDateTime reminderTime, Long ownerChatId);
    void toggleEventRemindersRequested(String eventId, boolean enable, Long ownerChatId);

    // Получение события по ID для конкретного пользователя
    Event getEventById(String eventId, Long ownerChatId);

    // Методы для управления доступом наблюдателя
    void grantViewAccess(Long granterChatId, Long observerChatId);
    void loadObservedEvents(Long currentObserverChatId, Long targetOwnerChatIdToView);

    // TODO: Добавить методы для анализа и синхронизации
    // void analysisRequested();
    // void syncRequested();
}
