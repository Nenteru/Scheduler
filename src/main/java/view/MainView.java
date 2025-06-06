package view;

import model.Event;
import presenter.MainPresenter;

import java.util.List;

// Интерфейс для главного окна приложения
public interface MainView {
    void displayEvents(List<Event> events, boolean isObservedView, Long targetOwnerIdIfObserved);
    void displayEventDetails(Event event); // Показать детали выбранного события
    void showEventEditor(Event event); // Открыть редактор для нового (null) или существующего события
    void showErrorMessage(String title, String message);
    void showInfoMessage(String title, String message);
    boolean showConfirmationDialog(String title, String message); // Для подтверждения удаления
    void setPresenter(MainPresenter presenter); // Связать View с Presenter

    // Возможно, потребуется метод для отправки сообщения конкретному пользователю, 
    // если бот должен уведомлять наблюдателя о предоставлении доступа.
    // void showInfoMessageToUser(Long chatId, String title, String message);
}