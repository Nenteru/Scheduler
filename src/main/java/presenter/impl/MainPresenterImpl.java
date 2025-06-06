package presenter.impl;

import model.Event;
import presenter.MainPresenter;
import service.EventService;
// import service.AnalysisService; // Закомментировано, т.к. не используется в текущей задаче
// import service.SyncService;     // Закомментировано, т.к. не используется в текущей задаче
import view.MainView;
import dao.ObserverPermissionDAO; // Импорт DAO для разрешений
import dao.impl.InMemoryObserverPermissionDAO; // Конкретная реализация

import java.time.LocalDateTime;
import java.util.List;

public class MainPresenterImpl implements MainPresenter {

    private MainView view; // View интерфейс
    private final EventService eventService; // Service интерфейс
    // private AnalysisService analysisService; // WIP
    // private SyncService syncService;         // WIP
    private ObserverPermissionDAO observerPermissionDAO; // DAO для разрешений

    // Инъекция зависимостей через конструктор
    public MainPresenterImpl(EventService eventService, /* другие сервисы */ MainView view) {
        this.eventService = eventService;
        this.view = view;
        this.observerPermissionDAO = new InMemoryObserverPermissionDAO(); // Инициализация
        // this.analysisService = analysisService;
        // this.syncService = syncService;
    }

    // Метод для установки View (обычно вызывается после создания Presenter и View)
    public void setView(MainView view) {
        this.view = view;
        this.view.setPresenter(this); // Даем View ссылку на Presenter
    }

    @Override
    public void loadEvents(Long ownerChatId) {
        if (view == null) return;
        try {
            System.out.println("[Presenter] Loading events for owner: " + ownerChatId);
            List<Event> events = eventService.getEventsForOwner(ownerChatId);
            view.displayEvents(events, false, ownerChatId);
            System.out.println("[Presenter] Events loaded for owner " + ownerChatId + " and displayed: " + events.size());
        } catch (Exception e) {
            System.err.println("[Presenter] Error loading events for owner " + ownerChatId + ": " + e.getMessage());
            view.showErrorMessage("Ошибка загрузки", "Не удалось загрузить события: " + e.getMessage());
        }
    }

    @Override
    public void addEventRequested() {
        if (view == null) return;
        System.out.println("[Presenter] Add event requested.");
        view.showEventEditor(null); // Передаем null для создания нового события
    }

    @Override
    public void editEventRequested(Event event) {
        if (view == null || event == null) return;
        System.out.println("[Presenter] Edit event requested for: " + event.getTitle());
        view.showEventEditor(event); // Передаем событие для редактирования
    }

    @Override
    public void deleteEventRequested(String eventId, Long ownerChatId) {
        if (view == null || eventId == null || ownerChatId == null) return;
        
        Event eventToDelete = null;
        try {
            eventToDelete = this.getEventById(eventId, ownerChatId);
            if (eventToDelete == null) {
                view.showErrorMessage("Ошибка удаления", "Событие с ID " + eventId + " не найдено или не принадлежит вам.");
                return;
            }
        } catch (Exception e) {
            view.showErrorMessage("Ошибка удаления", "Не удалось получить событие для удаления: " + e.getMessage());
            return;
        }
        
        System.out.println("[Presenter] Delete event requested for ID: " + eventId + " by owner: " + ownerChatId);
        boolean confirmed = view.showConfirmationDialog("Удаление события", "Вы уверены, что хотите удалить событие \"" + eventToDelete.getTitle() + "\"?");
        if (confirmed) {
            try {
                eventService.deleteEvent(eventId, ownerChatId);
                view.showInfoMessage("Успех", "Событие \"" + eventToDelete.getTitle() + "\" удалено.");
                loadEvents(ownerChatId);
            } catch (Exception e) {
                System.err.println("[Presenter] Error deleting event ID " + eventId + ": " + e.getMessage());
                view.showErrorMessage("Ошибка удаления", "Не удалось удалить событие: " + e.getMessage());
            }
        }
    }

    @Override
    public void eventSelected(Event event) {
        if (view == null || event == null) return;
        System.out.println("[Presenter] Event selected: " + event.getTitle());
        view.displayEventDetails(event); // Показываем детали в View
    }

    @Override
    public void addEvent(Event event, Long ownerChatId) {
        if (event == null) return;
        if (view == null) {
            System.err.println("[Presenter] View is not set. Cannot add event for chatId: " + ownerChatId);
            return;
        }
        try {
            System.out.println("[Presenter] Adding new local event: " + event.getTitle() + " for owner: " + ownerChatId);
            Event addedEvent = eventService.addEvent(event, ownerChatId);
            if (addedEvent != null) {
                view.showInfoMessage("Создание события", "Событие \"" + addedEvent.getTitle() + "\" успешно создано.");
                loadEvents(ownerChatId);
            } else {
                view.showErrorMessage("Ошибка создания", "Не удалось создать событие \"" + event.getTitle() + "\". Событие не было возвращено сервисом.");
            }
        } catch (Exception e) {
            System.err.println("[Presenter] Error adding new local event: " + e.getMessage());
            view.showErrorMessage("Ошибка создания события", "Не удалось создать событие \"" + event.getTitle() + "\": " + e.getMessage());
        }
    }

    // Методы для управления напоминаниями
    @Override
    public void setEventReminderTimeRequested(String eventId, LocalDateTime reminderTime, Long ownerChatId) {
        if (view == null) return;
        try {
            Event event = this.getEventById(eventId, ownerChatId);
            
            if (event == null) {
                view.showErrorMessage("Ошибка установки напоминания", "Событие с ID " + eventId + " не найдено или не принадлежит вам.");
                return;
            }
            
            Event updatedEvent = eventService.setEventReminderTime(eventId, reminderTime, ownerChatId);
            view.showInfoMessage("Напоминание обновлено", 
                "Время напоминания для события '" + updatedEvent.getTitle() + "' установлено на " + 
                (reminderTime != null ? util.DateTimeUtils.formatMedium(reminderTime) : "не установлено (отключено)") + ".");
            view.displayEventDetails(updatedEvent);
        } catch (Exception e) {
            System.err.println("[Presenter] Error setting reminder time: " + e.getMessage());
            view.showErrorMessage("Ошибка установки напоминания", e.getMessage());
        }
    }

    @Override
    public void toggleEventRemindersRequested(String eventId, boolean enable, Long ownerChatId) {
        if (view == null) return;
        try {
            Event event = this.getEventById(eventId, ownerChatId);

            if (event == null) {
                view.showErrorMessage("Ошибка изменения статуса напоминаний", "Событие с ID " + eventId + " не найдено или не принадлежит вам.");
                return;
            }

            Event updatedEvent = eventService.toggleEventReminders(eventId, enable, ownerChatId);
            view.showInfoMessage("Напоминания обновлены", 
                "Напоминания для события '" + updatedEvent.getTitle() + "' теперь " + (enable ? "ВКЛЮЧЕНЫ" : "ОТКЛЮЧЕНЫ") + ".");
            view.displayEventDetails(updatedEvent);
        } catch (Exception e) {
            System.err.println("[Presenter] Error toggling reminders: " + e.getMessage());
            view.showErrorMessage("Ошибка изменения статуса напоминаний", e.getMessage());
        }
    }

    @Override
    public Event getEventById(String eventId, Long ownerChatId) {
        try {
            return eventService.getEventByIdAndOwner(eventId, ownerChatId)
                .orElseThrow(() -> new IllegalArgumentException("Событие с ID " + eventId + " не найдено или не принадлежит пользователю " + ownerChatId + "."));
        } catch (Exception e) {
            System.err.println("[Presenter] Error getting event by ID " + eventId + " for owner " + ownerChatId + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void grantViewAccess(Long granterChatId, Long observerChatId) {
        if (view == null) return;
        if (granterChatId.equals(observerChatId)) {
            view.showErrorMessage("Ошибка доступа", "Нельзя предоставить доступ самому себе.");
            return;
        }
        observerPermissionDAO.addPermission(observerChatId, granterChatId);
        view.showInfoMessage("Доступ предоставлен", "Пользователь " + observerChatId + " теперь может просматривать ваши события.");
        // Можно также отправить сообщение наблюдателю, если это необходимо
        // view.showInfoMessageToUser(observerChatId, "Доступ получен", "Пользователь " + granterChatId + " предоставил вам доступ к просмотру его событий.");
    }

    @Override
    public void loadObservedEvents(Long currentObserverChatId, Long targetOwnerChatIdToView) {
        if (view == null) return;
        if (!observerPermissionDAO.hasPermission(currentObserverChatId, targetOwnerChatIdToView)) {
            view.showErrorMessage("Нет доступа", "У вас нет разрешения на просмотр событий пользователя " + targetOwnerChatIdToView + ".");
            return;
        }
        List<Event> events = eventService.getEventsForOwner(targetOwnerChatIdToView);
        if (events.isEmpty()) {
            view.showInfoMessage("События не найдены", "У пользователя " + targetOwnerChatIdToView + " нет событий или вы не имеете к ним доступа.");
        } else {
            view.displayEvents(events, true, targetOwnerChatIdToView);
        }
    }

    // TODO: Реализовать методы для analysisRequested() и syncRequested(), когда будут готовы сервисы
}
