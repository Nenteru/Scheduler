// AppLauncher.java

import dao.EventDAO;
import dao.ResponseTemplateDAO;
// import dao.impl.InMemoryEventDAO;
import dao.impl.InMemoryResponseTemplateDAO;
import dao.impl.SQLiteEventDAO;
// import model.Event;
import presenter.impl.MainPresenterImpl;
import service.EventService;
// import service.ReminderService;
import service.TemplateService;
import service.impl.EventServiceImpl;
import service.impl.ReminderServiceImpl;
import service.impl.TemplateServiceImpl;
import telegram.TelegramBotView;
import config.AppConfig;

//import javafx.application.Application;
//import javafx.stage.Stage;

// import java.time.LocalDateTime;

/**
 * Основной класс для запуска приложения.
 * Собирает вместе компоненты MVP.
 */
public class AppLauncher {

    public static void main(String[] args) {
        try {
            AppLauncher app = new AppLauncher();
            app.startBot();
        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startBot() throws Exception {
        System.out.println("Application starting (Telegram Bot Mode)...");

        // Создаем директорию для данных, если её нет
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get("data"));

        // 1. Создание зависимостей (DAO, Service)
        EventDAO eventDAO = new SQLiteEventDAO("data/events.db");
        ResponseTemplateDAO responseTemplateDAO = new InMemoryResponseTemplateDAO();
        
        EventService eventService = new EventServiceImpl(eventDAO);
        TemplateService templateService = new TemplateServiceImpl(responseTemplateDAO);

        // 2. Создание Telegram View (сначала, т.к. Presenter его требует)
        TelegramBotView telegramBotView = new TelegramBotView(
            AppConfig.getBotToken(),
            AppConfig.getBotUsername(),
            templateService
        );

        // 3. Создание Presenter и связывание с View
        MainPresenterImpl mainPresenter = new MainPresenterImpl(eventService, telegramBotView);
        mainPresenter.setView(telegramBotView);

        // 4. Создание и запуск ReminderService
        ReminderServiceImpl reminderService = new ReminderServiceImpl(eventService);
        reminderService.setTelegramBotView(telegramBotView);
        reminderService.start();

        // 5. Регистрация бота (после инициализации всех сервисов)
        telegramBotView.registerBot();

        System.out.println("Telegram Bot registered. Waiting for messages...");

        // Добавляем graceful shutdown для ReminderService
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down ReminderService...");
            reminderService.stop();
            System.out.println("ReminderService stopped.");
        }));
    }
}