// telegram/TelegramBotView.java
package telegram;


import model.Event;
import presenter.MainPresenter;
import util.DateTimeUtils;
import view.MainView;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import dao.ScheduleDAO;
import dao.impl.GoogleCalendarDAO;
import dao.impl.UserNotAuthenticatedException;
import dao.EventDAO;
import dao.impl.SQLiteEventDAO;
import dao.AnalysisExportDAO;
import dao.impl.JsonAnalysisExportDAO;
import model.ScheduleAnalysis;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

import service.TemplateService;
import util.MarkdownFormatter;
import config.AppConfig;
import service.ScheduleAnalysisService;
import service.impl.ScheduleAnalysisServiceImpl;

/**
 * Реализация MainView для Telegram Bot.
 * Обрабатывает входящие сообщения от Telegram и отправляет ответы.
 */
public class TelegramBotView extends TelegramLongPollingBot implements MainView {

    private String botToken; // Токен, полученный от BotFather
    private String botUsername; // Username бота
    private MainPresenter presenter; // Ссылка на Presenter
    private final TemplateService templateService;

    // ID чата пользователя, для которого будем выводить информацию.
    private Long currentChatId;
    
    // Используем интерфейс ScheduleDAO
    private ScheduleDAO scheduleDAO;

    // --- Управление состоянием пользователей и администраторов ---
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Set<Long> adminChatIds = ConcurrentHashMap.newKeySet();

    private enum UserState {
        AWAITING_GOOGLE_CODE,
        AWAITING_TEMPLATE_TEXT
        // Другие состояния по мере необходимости
    }

    private static final String AVAILABLE_COMMANDS_HELP =
            "*Доступные команды*\n\n" +
            "*Основные команды:*\n" +
            "🗓 /list\\_events \\- показать локальное расписание\n" +
            "➕ /add\\_event \\- добавить событие\n" +
            "   Формат: `<название>;<гггг\\-мм\\-ддTчч:мм>;<гггг\\-мм\\-ддTчч:мм>;<описание>;<место>`\n" +
            "⏰ /set\\_reminder\\_time \\- установить время напоминания\n" +
            "   Формат: `<ID события>;<гггг\\-мм\\-ддTчч:мм>`\n" +
            "🔔 /toggle\\_reminders \\- вкл/выкл напоминания для события\n" +
            "   Формат: `<ID события> <on\\|off>`\n" +
            "🆔 /get\\_my\\_id \\- показать ваш Telegram ID \\(для предоставления доступа другим\\)\n\n" +
            "*Google Calendar:*\n" +
            "🔗 /connect\\_google\\_calendar \\- подключить ваш Google Calendar\n" +
            "❌ /disconnect\\_google\\_calendar \\- отключить ваш Google Calendar\n" +
            "🔄 /sync\\_google \\- синхронизировать события из Google Calendar\n" +
            "📋 /list\\_google \\- показать события из Google Calendar на текущую неделю\n\n" +
            "*Доступ для наблюдателей:*\n" +
            "🤝 /grant\\_view\\_access \\<ID пользователя\\> \\- предоставить другому пользователю доступ к просмотру ваших событий\n" +
            "👀 /list\\_observed\\_events \\<ID пользователя\\> \\- показать события пользователя, к которым у вас есть доступ\n\n" +
            "*Команды администратора:*\n" +
            "🔑 /admin\\_login `\\<code\\>` \\- войти как администратор\n" +
            "📋 /list\\_templates \\- \\(Админ\\) Показать все шаблоны\n" +
            "✏️ /set\\_template `\\<key\\> \\<text\\>` \\- \\(Админ\\) Установить шаблон\n" +
            "🔄 /reset\\_template `\\<key\\>` \\- \\(Админ\\) Сбросить шаблон\n" +
            "🔄 /admin\\_logout \\- выйти из режима администратора\n" +
            "📊 /get\\_analysis \\- получить анализ текущей недели";


    public TelegramBotView(String botToken, String botUsername, TemplateService templateService) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.templateService = templateService;
        try {
            this.scheduleDAO = new GoogleCalendarDAO();
            setupBotCommands();
        } catch (Exception e) {
            System.err.println("[TelegramBotView] Error initializing Google Calendar DAO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupBotCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("start", "Начать работу с ботом и показать помощь"));
        commands.add(new BotCommand("help", "Показать список доступных команд"));
        commands.add(new BotCommand("list_events", "Показать локальное расписание"));
        commands.add(new BotCommand("add_event", "Добавить событие"));
        commands.add(new BotCommand("set_reminder_time", "Установить время напоминания"));
        commands.add(new BotCommand("toggle_reminders", "Вкл/выкл напоминания для события"));
        commands.add(new BotCommand("get_my_id", "Показать ваш Telegram ID"));
        commands.add(new BotCommand("connect_google_calendar", "Подключить Google Calendar"));
        commands.add(new BotCommand("disconnect_google_calendar", "Отключить Google Calendar"));
        commands.add(new BotCommand("sync_google", "Синхронизировать с Google Calendar"));
        commands.add(new BotCommand("list_google", "Показать события из Google Calendar"));
        commands.add(new BotCommand("grant_view_access", "Разрешить другому пользователю просмотр ваших событий"));
        commands.add(new BotCommand("list_observed_events", "Показать события пользователя, за которым вы наблюдаете"));
        commands.add(new BotCommand("admin_login", "Войти как администратор"));
        commands.add(new BotCommand("list_templates", "(Админ) Показать шаблоны"));
        commands.add(new BotCommand("set_template", "(Админ) Установить шаблон"));
        commands.add(new BotCommand("reset_template", "(Админ) Сбросить шаблон"));
        commands.add(new BotCommand("admin_logout", "Выйти из режима администратора"));
        commands.add(new BotCommand("get_analysis", "Получить анализ текущей недели"));

        try {
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
            System.out.println("[TelegramBotView] Bot commands set successfully");
        } catch (TelegramApiException e) {
            System.err.println("[TelegramBotView] Error setting bot commands: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            currentChatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();
            System.out.println("[TelegramBotView] Received message from " + currentChatId + ": " + messageText);

            if (userStates.get(currentChatId) == UserState.AWAITING_GOOGLE_CODE) {
                handleGoogleAuthCode(messageText);
                return;
            }

            if (messageText.startsWith("/")) {
                handleCommand(messageText);
            } else {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2("Вы сказали: " + messageText + "\\nПопробуйте /help для просмотра доступных команд."));
            }
        } else if (update.hasCallbackQuery()) {
            currentChatId = update.getCallbackQuery().getMessage().getChatId();
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleCommand(String commandText) {
        if (presenter == null) {
            sendMessage(currentChatId, "Бот не инициализирован\\. Пожалуйста, сообщите администратору\\.");
            return;
        }

        String command = commandText.split(" ")[0];
        String args = "";
        if (commandText.contains(" ")) {
            args = commandText.substring(commandText.indexOf(" ") + 1).trim();
        }

        switch (command) {
            case "/start":
                String welcomeText = templateService.getTemplate("start_welcome", "Привет\\! Я бот\\-планировщик\\.");
                String additionalInfo = templateService.getTemplate("start_get_id_info", 
                    "\nЧтобы узнать свой Telegram ID \\(например, для предоставления доступа другому пользователю\\), используйте команду /get\\_my\\_id\\.");
                String welcomeMessage = String.format("*%s* 👋\n%s\n\nДля списка команд, используйте /help", 
                    welcomeText, additionalInfo);
                sendMessage(currentChatId, welcomeMessage);
                break;
            case "/help":
                String helpInfo = templateService.getTemplate("help", AVAILABLE_COMMANDS_HELP);
                sendMessage(currentChatId, helpInfo);
                break;
            case "/list_events":
                presenter.loadEvents(currentChatId);
                break;
            case "/connect_google_calendar":
                handleConnectGoogleCalendarCommand();
                break;
            case "/disconnect_google_calendar":
                handleDisconnectGoogleCalendarCommand();
                break;
            case "/sync_google":
                handleGoogleCalendarSync();
                break;
            case "/list_google":
                handleGoogleCalendarList();
                break;
            case "/add_event":
                handleAddEventCommand(args);
                break;
            case "/set_reminder_time":
                handleSetReminderTimeCommand(args);
                break;
            case "/toggle_reminders":
                handleToggleRemindersCommand(args);
                break;
            case "/grant_view_access":
                handleGrantViewAccessCommand(args);
                break;
            case "/list_observed_events":
                handleListObservedEventsCommand(args);
                break;
            case "/get_my_id":
                handleGetMyIdCommand();
                break;
            case "/admin_login":
                handleAdminLogin(args);
                break;
            case "/admin_logout":
                handleAdminLogout();
                break;
            case "/list_templates":
                handleListTemplates();
                break;
            case "/set_template":
                handleSetTemplateCommand(args);
                break;
            case "/reset_template":
                handleResetTemplateCommand(args);
                break;
            case "/get_analysis":
                handleGetAnalysisCommand();
                break;
            default:
                String unknownCommandText = templateService.getTemplate("unknown_command", "Неизвестная команда: {command}\nДоступные команды:\n");
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(unknownCommandText.replace("{command}", command)) + AVAILABLE_COMMANDS_HELP);
                break;
        }
    }

    private void handleAddEventCommand(String args) {
        if (args == null || args.trim().isEmpty()) {
            String helpText = "Неверный формат. Используйте: " +
                "<название>;<гггг-мм-ддTчч:мм>;<гггг-мм-ддTчч:мм>;<описание>;<место>";
            sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(helpText));
            return;
        }
        if (presenter == null || currentChatId == null) return;
        try {
            String[] params = args.split(";", -1);
            if (params.length < 3) {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("add_event_invalid_format", "Неверный формат. Используйте: <название>;<гггг-мм-ддTчч:мм>;<гггг-мм-ддTчч:мм>;<описание>;<место>")));
                return;
            }

            String title = params[0].trim();
            if (title.isEmpty()) {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("add_event_title_empty", "Название события не может быть пустым.")));
                return;
            }

            LocalDateTime startTime = LocalDateTime.parse(params[1].trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime endTime = LocalDateTime.parse(params[2].trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            if (endTime.isBefore(startTime)) {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("add_event_end_before_start", "Время окончания события не может быть раньше времени начала.")));
                return;
            }

            String description = params.length > 3 ? params[3].trim() : "";
            String location = params.length > 4 ? params[4].trim() : "";

            Event newEvent = new Event(null, title, description, startTime, endTime, location, null, currentChatId);

            presenter.addEvent(newEvent, currentChatId);

        } catch (DateTimeParseException e) {
            sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("add_event_invalid_date", "Ошибка в формате даты/времени. Пожалуйста, используйте гггг-мм-ддTчч:мм. Пример: 2023-12-25T15:30")));
        } catch (Exception e) {
            sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("add_event_error", "Произошла ошибка при добавлении события: ") + e.getMessage()));
        }
    }

    private void handleSetReminderTimeCommand(String paramsString) {
        if (presenter == null || currentChatId == null) return;
        try {
            String[] params = paramsString.split(";", -1);
            if (params.length != 2) {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("set_reminder_time_invalid_format", "Неверный формат. Используйте: /set_reminder_time <ID события>;<гггг-мм-ддTчч:мм> или <ID события>;null")));
                return;
            }

            String eventId = params[0].trim();
            if (eventId.isEmpty()) {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("set_reminder_time_event_id_empty", "ID события не может быть пустым.")));
                return;
            }

            String timeString = params[1].trim();
            LocalDateTime reminderTime = "null".equalsIgnoreCase(timeString) ? null : LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            presenter.setEventReminderTimeRequested(eventId, reminderTime, currentChatId);
        } catch (DateTimeParseException e) {
            sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("set_reminder_time_invalid_date", "Неверный формат даты/времени для напоминания. Используйте гггг-мм-ддTчч:мм или 'null'.") + " Ошибка: " + e.getMessage()));
        } catch (Exception e) {
            sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("set_reminder_time_error", "Ошибка установки времени напоминания: ") + e.getMessage()));
        }
    }

    private void handleToggleRemindersCommand(String paramsString) {
        if (presenter == null || currentChatId == null) return;
        try {
            String[] params = paramsString.split(" ", 2);
            if (params.length != 2) {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("toggle_reminders_invalid_format", "Неверный формат. Используйте: /toggle_reminders <ID события> <on|off>")));
                return;
            }

            String eventId = params[0].trim();
            if (eventId.isEmpty()) {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("toggle_reminders_event_id_empty", "ID события не может быть пустым.")));
                return;
            }

            String toggleValue = params[1].trim();
            boolean enable;
            if ("on".equalsIgnoreCase(toggleValue)) {
                enable = true;
            } else if ("off".equalsIgnoreCase(toggleValue)) {
                enable = false;
            } else {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("toggle_reminders_invalid_value", "Неверное значение для вкл/выкл. Используйте 'on' или 'off'.")));
                return;
            }
            presenter.toggleEventRemindersRequested(eventId, enable, currentChatId);
        } catch (Exception e) {
            sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2(templateService.getTemplate("toggle_reminders_error", "Ошибка изменения статуса напоминаний: ") + e.getMessage()));
        }
    }

    private void handleConnectGoogleCalendarCommand() {
        if (currentChatId == null) return;
        if (scheduleDAO instanceof GoogleCalendarDAO) {
            GoogleCalendarDAO googleDAO = (GoogleCalendarDAO) scheduleDAO;
            try {
                String authUrl = googleDAO.getAuthorizationUrl(currentChatId, currentChatId.toString());
                userStates.put(currentChatId, UserState.AWAITING_GOOGLE_CODE);
                
                SendMessage message = new SendMessage();
                message.setChatId(currentChatId.toString());
                message.setParseMode(null);
                message.enableWebPagePreview();

                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append("Подключение Google Calendar\n\n");
                messageBuilder.append("Для подключения, пожалуйста, выполните следующие шаги:\n\n");
                messageBuilder.append("1. Перейдите по ссылке ниже:\n");
                messageBuilder.append(authUrl).append("\n\n");
                messageBuilder.append("2. После авторизации Google покажет вам код\n");
                messageBuilder.append("3. Скопируйте этот код и отправьте его мне в следующем сообщении");
                
                message.setText(messageBuilder.toString());
                executeSendMessage(message);

            } catch (IOException e) {
                String errorMsg = String.format(
                    "❌ Не удалось сгенерировать URL для подключения Google Calendar: %s",
                    MarkdownFormatter.escapeMarkdownV2(e.getMessage())
                );
                sendMessage(currentChatId, errorMsg);
                System.err.println("[TelegramBotView] Error generating Google auth URL for user " + currentChatId + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            sendMessage(currentChatId, "❌ Ошибка: Функционал Google Calendar не настроен корректно");
            System.err.println("[TelegramBotView] scheduleDAO is not an instance of GoogleCalendarDAO in handleConnectGoogleCalendarCommand.");
        }
    }

    private void handleGoogleAuthCode(String code) {
        if (currentChatId == null) return;
        userStates.remove(currentChatId);

        if (scheduleDAO instanceof GoogleCalendarDAO) {
            GoogleCalendarDAO googleDAO = (GoogleCalendarDAO) scheduleDAO;
            try {
                googleDAO.exchangeCodeForTokens(currentChatId, code);
                sendMessage(currentChatId, "*✅ Google Calendar успешно подключен*");
            } catch (IOException e) {
                String errorMessage = String.format(
                    "❌ Ошибка подключения Google Calendar: %s\n\n" +
                    "Попробуйте /connect\\_google\\_calendar снова",
                    MarkdownFormatter.escapeMarkdownV2(e.getMessage())
                );
                sendMessage(currentChatId, errorMessage);
                System.err.println("[TelegramBotView] Error exchanging Google auth code for user " + currentChatId + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            sendMessage(currentChatId, "❌ Ошибка: Функционал Google Calendar не настроен корректно");
            System.err.println("[TelegramBotView] scheduleDAO is not an instance of GoogleCalendarDAO in handleGoogleAuthCode.");
        }
    }

    private void handleDisconnectGoogleCalendarCommand() {
        if (currentChatId == null) return;
        if (scheduleDAO instanceof GoogleCalendarDAO) {
            GoogleCalendarDAO googleDAO = (GoogleCalendarDAO) scheduleDAO;
            try {
                googleDAO.deleteTokens(currentChatId);
                sendMessage(currentChatId, "*✅ Google Calendar отключен*");
            } catch (IOException e) {
                String errorMsg = String.format(
                    "❌ Ошибка при отключении Google Calendar: %s",
                    MarkdownFormatter.escapeMarkdownV2(e.getMessage())
                );
                sendMessage(currentChatId, errorMsg);
                System.err.println("[TelegramBotView] Error disconnecting Google Calendar for user " + currentChatId + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            sendMessage(currentChatId, "❌ Ошибка: Функционал Google Calendar не настроен корректно");
            System.err.println("[TelegramBotView] scheduleDAO is not an instance of GoogleCalendarDAO in handleDisconnectGoogleCalendarCommand.");
        }
    }

    private void handleGoogleCalendarSync() {
        if (scheduleDAO == null || currentChatId == null) {
            SendMessage message = new SendMessage();
            message.setChatId(currentChatId.toString());
            message.setParseMode(null);
            message.setText("Функция Google Calendar недоступна. Сервис не инициализирован.");
            executeSendMessage(message);
            System.err.println("[TelegramBotView] scheduleDAO is null in handleGoogleCalendarSync.");
            return;
        }
        if (!(scheduleDAO instanceof GoogleCalendarDAO)) {
            SendMessage message = new SendMessage();
            message.setChatId(currentChatId.toString());
            message.setParseMode(null);
            message.setText("Ошибка: Функционал Google Calendar настроен некорректно (неверный тип DAO).");
            executeSendMessage(message);
            System.err.println("[TelegramBotView] scheduleDAO is not an instance of GoogleCalendarDAO in handleGoogleCalendarSync.");
            return;
        }

        SendMessage startMessage = new SendMessage();
        startMessage.setChatId(currentChatId.toString());
        startMessage.setParseMode(null);
        startMessage.setText("⏳ Начинаю синхронизацию с Google Calendar...");
        executeSendMessage(startMessage);

        try {
            LocalDate today = LocalDate.now();
            List<Event> googleEvents = scheduleDAO.getEvents(today, today.plusYears(1), currentChatId);

            if (googleEvents.isEmpty()) {
                SendMessage emptyMessage = new SendMessage();
                emptyMessage.setChatId(currentChatId.toString());
                emptyMessage.setParseMode(null);
                emptyMessage.setText("ℹ️ В вашем Google Calendar нет предстоящих событий для синхронизации (в диапазоне 1 год от текущей даты).");
                executeSendMessage(emptyMessage);
                return;
            }

            int syncedCount = 0;
            int skippedCount = 0;
            for (Event googleEvent : googleEvents) {
                googleEvent.setOwnerChatId(currentChatId); 
                try {
                    presenter.addEvent(googleEvent, currentChatId); 
                    syncedCount++;
                } catch (Exception e) { 
                    System.err.println("[TelegramBotView] Error adding synced event from Google: " + e.getMessage() + " for event: " + googleEvent.getTitle());
                    skippedCount++;
                }
            }

            SendMessage resultMessage = new SendMessage();
            resultMessage.setChatId(currentChatId.toString());
            resultMessage.setParseMode(null);
            resultMessage.setText(String.format("✅ Синхронизация с Google Calendar завершена.\n" +
                                             "Импортировано событий: %d\n" +
                                             "Пропущено (возможно, уже существуют или ошибка): %d", 
                                             syncedCount, skippedCount));
            executeSendMessage(resultMessage);

        } catch (UserNotAuthenticatedException e) {
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(currentChatId.toString());
            errorMessage.setParseMode(null);
            errorMessage.setText("⚠️ Вы не авторизованы в Google Calendar. " +
                               "Пожалуйста, используйте команду /connect_google_calendar для подключения.");
            executeSendMessage(errorMessage);
        } catch (IOException | GeneralSecurityException e) {
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(currentChatId.toString());
            errorMessage.setParseMode(null);
            errorMessage.setText("❌ Ошибка при получении событий из Google Calendar: " + e.getMessage());
            executeSendMessage(errorMessage);
            System.err.println("[TelegramBotView] Error fetching events from Google Calendar for user " + currentChatId + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(currentChatId.toString());
            errorMessage.setParseMode(null);
            errorMessage.setText("❌ Произошла неожиданная ошибка при синхронизации с Google Calendar: " + e.getMessage());
            executeSendMessage(errorMessage);
            System.err.println("[TelegramBotView] Unexpected error during Google Calendar sync for user " + currentChatId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGoogleCalendarList() {
        if (presenter == null || currentChatId == null) return;
        try {
            if (!(scheduleDAO instanceof GoogleCalendarDAO)) {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2("Вы не подключили Google Calendar. Используйте /connect_google_calendar"));
                return;
            }
            List<Event> googleEvents = scheduleDAO.getEvents(LocalDate.now(), LocalDate.now().plusDays(7), currentChatId);
            if (googleEvents.isEmpty()) {
                sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2("В вашем Google Calendar нет событий на ближайшую неделю."));
            } else {
                displayEvents(googleEvents, false, null);
            }
        } catch (UserNotAuthenticatedException e) {
            sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2("Вы не подключили Google Calendar. Используйте /connect_google_calendar"));
        } catch (Exception e) {
            System.err.println("[TelegramBotView] Error listing Google Calendar events: " + e.getMessage());
            e.printStackTrace();
            showErrorMessage("Ошибка Google Calendar", "Не удалось получить события из Google Calendar. Попробуйте /disconnect_google_calendar и /connect_google_calendar снова. " + e.getMessage());
        }
    }

    public void executeSendMessage(SendMessage message) {
        try {
            if (message.getParseMode() == null) {
            }
            execute(message);
            System.out.println("[TelegramBotView] Message sent to " + message.getChatId() + ": " + message.getText().lines().findFirst().orElse(""));
        } catch (TelegramApiException e) {
            System.err.println("[TelegramBotView] Error sending message: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("parse")) {
                System.out.println("[TelegramBotView] Attempting to send fallback message (plain text).");
                try {
                    message.setParseMode(null);
                    message.setText(message.getText() + "\n\n[Сообщение было упрощено из-за ошибки форматирования]");
                    execute(message);
                } catch (TelegramApiException ex) {
                    System.err.println("[TelegramBotView] Error sending fallback message: " + ex.getMessage());
                }
            }
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("MarkdownV2");
        executeSendMessage(message);
    }

    private void sendMessageWithEntities(Long chatId, String text, List<MessageEntity> entities) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setEntities(entities);
        executeSendMessage(message);
    }

    private void sendMessageWithKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("MarkdownV2");
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }
        executeSendMessage(message);
    }

    @Override
    public void displayEvents(List<Event> events, boolean isObservedView, Long targetOwnerIdIfObserved) {
        if (events.isEmpty()) {
            String message = isObservedView ? 
                templateService.getTemplate("observed_events_empty", 
                    "У пользователя пока нет событий\\.") :
                templateService.getTemplate("event_list_empty", 
                    "У вас пока нет событий\\.");
            sendMessage(currentChatId, message);
            return;
        }

        String header = isObservedView ?
            templateService.getTemplate("observed_events_header", 
                "События пользователя с ID " + MarkdownFormatter.code(targetOwnerIdIfObserved.toString()) + ":") :
            templateService.getTemplate("event_list_header", 
                "Ваше расписание:\n\n_Используйте кнопки под каждым событием для управления_");
        sendMessage(currentChatId, header);

        for (Event event : events) {
            String eventText = formatEventText(event, isObservedView);
            InlineKeyboardMarkup keyboard = createEventKeyboard(event, isObservedView);
            sendMessageWithKeyboard(currentChatId, eventText, keyboard);
        }
    }

    String formatEventText(Event event, boolean isObservedView) {
        StringBuilder sb = new StringBuilder();
        
        // Название события
        sb.append(MarkdownFormatter.bold(event.getTitle())).append("\n");
        
        // Дата и время начала и окончания
        String startTime = DateTimeUtils.formatDateTime(event.getStartTime());
        String endTime = DateTimeUtils.formatDateTime(event.getEndTime());
        sb.append("📅 Начало: ").append(MarkdownFormatter.code(startTime)).append("\n");
        sb.append("⌚️ Окончание: ").append(MarkdownFormatter.code(endTime)).append("\n");
        
        // Описание (если есть)
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            sb.append("📝 ").append(MarkdownFormatter.escapeMarkdownV2(event.getDescription())).append("\n");
        }
        
        // Место (если есть)
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            sb.append("📍 ").append(MarkdownFormatter.escapeMarkdownV2(event.getLocation())).append("\n");
        }
        
        // Статус напоминаний
        String reminderStatus = event.isRemindersEnabled() ? "ВКЛ" : "ОТКЛ";
        sb.append("🔔 Напоминания: ").append(MarkdownFormatter.code(reminderStatus));
        
        // Время напоминания (если есть и включено)
        if (event.getReminderTime() != null && event.isRemindersEnabled()) {
            String reminderTime = DateTimeUtils.formatDateTime(event.getReminderTime());
            sb.append(", ").append(MarkdownFormatter.code(reminderTime));
            if (event.isReminderSent()) {
                sb.append(" ").append(MarkdownFormatter.escapeMarkdownV2("(отправлено)"));
            }
        }
        sb.append("\n");
        
        // ID события
        sb.append("🆔 ").append(MarkdownFormatter.code(event.getId().toString()));
        
        return sb.toString();
    }

    private InlineKeyboardMarkup createEventKeyboard(Event event, boolean isObservedView) {
        if (event == null || event.getId() == null) {
            System.err.println("[TelegramBotView] Cannot create keyboard for null event or event with null ID");
            return null;
        }
        if (isObservedView) {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton detailsButton = new InlineKeyboardButton();
            detailsButton.setText("\uD83D\uDCD1 Детали");
            String safeEventId = createSafeCallbackId(event.getId(), isObservedView);
            detailsButton.setCallbackData("d:" + safeEventId);
            row1.add(detailsButton);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            return markup;
        }

        try {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            String safeEventId = createSafeCallbackId(event.getId(), false);

            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton detailsButton = new InlineKeyboardButton();
            detailsButton.setText("\uD83D\uDCD1 Детали"); 
            detailsButton.setCallbackData("d:" + safeEventId);
            row1.add(detailsButton);

            InlineKeyboardButton reminderTimeButton = new InlineKeyboardButton();
            reminderTimeButton.setText("⏰ Время напом.");
            reminderTimeButton.setCallbackData("t:" + safeEventId);
            row1.add(reminderTimeButton);

            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton toggleRemindersButton = new InlineKeyboardButton();
            toggleRemindersButton.setText((event.isRemindersEnabled() ? "🔕 Отключить" : "🔔 Включить") + " напом.");
            toggleRemindersButton.setCallbackData("r:" + safeEventId + ":" + (event.isRemindersEnabled() ? "0" : "1"));
            row2.add(toggleRemindersButton);

            keyboard.add(row1);
            keyboard.add(row2);
            markup.setKeyboard(keyboard);

            return markup;
        } catch (Exception e) {
            System.err.println("[TelegramBotView] Error creating keyboard for event " + event.getId() + ": " + e.getMessage());
            return null;
        }
    }

    private final Map<String, String> callbackIdMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> callbackObservedFlagMap = new ConcurrentHashMap<>();

    private String createSafeCallbackId(String fullId, boolean isObservedView) {
        String shortId = fullId.substring(0, Math.min(12, fullId.length())) +
                         "_" + Long.toHexString(currentChatId != null ? currentChatId : 0L) +
                         "_" + (isObservedView ? "1" : "0"); 
        shortId = shortId.replaceAll("[^a-zA-Z0-9_]", "");
        if (shortId.length() > 60) {
            shortId = shortId.substring(0, 60);
        }
        callbackIdMap.put(shortId, fullId);
        callbackObservedFlagMap.put(shortId, isObservedView);
        return shortId;
    }

    private String getFullEventIdFromCallback(String shortId) {
        String fullId = callbackIdMap.get(shortId);
        if (fullId == null) {
            System.err.println("[TelegramBotView] Unknown short callback ID: " + shortId);
            throw new IllegalArgumentException("Unknown callback ID: " + shortId);
        }
        return fullId;
    }

    // private boolean isObservedFromCallback(String shortId) {
    //     return callbackObservedFlagMap.getOrDefault(shortId, false);
    // }

    @Override
    public void displayEventDetails(Event event) {
        if (currentChatId == null) {
            System.err.println("[TelegramBotView] No currentChatId to display event details.");
            return;
        }
        if (event == null) {
            sendDirectMessage(currentChatId, "Событие не найдено.");
            return;
        }

        boolean isObserved = !currentChatId.equals(event.getOwnerChatId());

        String title = MarkdownFormatter.escapeMarkdownV2(event.getTitle());
        String description = MarkdownFormatter.escapeMarkdownV2(event.getDescription() != null && !event.getDescription().isEmpty() ? event.getDescription() : "Нет");
        String startTime = MarkdownFormatter.escapeMarkdownV2(DateTimeUtils.formatMedium(event.getStartTime()));
        String endTime = MarkdownFormatter.escapeMarkdownV2(DateTimeUtils.formatMedium(event.getEndTime()));
        String location = MarkdownFormatter.escapeMarkdownV2(event.getLocation() != null && !event.getLocation().isEmpty() ? event.getLocation() : "Не указано");
        String eventId = MarkdownFormatter.escapeMarkdownV2(event.getId());
        String fmtdReminderStatus = MarkdownFormatter.escapeMarkdownV2(event.isRemindersEnabled() ? "ВКЛ" : "ОТКЛ");
        String fmtdReminderTime;
        String fmtdReminderSentInfo = "";

        if (event.isRemindersEnabled() && event.getReminderTime() != null) {
            fmtdReminderTime = MarkdownFormatter.escapeMarkdownV2(DateTimeUtils.formatMedium(event.getReminderTime()));
            String sentStatusText = event.isReminderSent() ? " (Отправлено)" : " (Ожидается)";
            fmtdReminderSentInfo = MarkdownFormatter.escapeMarkdownV2(sentStatusText);
        } else if (event.isRemindersEnabled()) {
            fmtdReminderTime = MarkdownFormatter.escapeMarkdownV2("Не установлено");
            fmtdReminderSentInfo = MarkdownFormatter.escapeMarkdownV2(" (Ожидается)");
        } else {
            fmtdReminderTime = MarkdownFormatter.escapeMarkdownV2("Не установлено (отключены)");
        }

        String ownerInfoSegment = "";
        if (isObserved) {
            ownerInfoSegment = String.format("\nВладелец: `%s`", MarkdownFormatter.escapeMarkdownV2(event.getOwnerChatId().toString()));
        }

        String details = String.format("*Детали события:*\n\n" +
                "📝 Название: *%s*\n" +
                "📄 Описание: `%s`\n" +
                "📅 Начало: `%s`\n" +
                "⌚️ Окончание: `%s`\n" +
                "📍 Место: `%s`\n" +
                "🔔 Напоминания: `%s`\n" +
                "⏰ Время напоминания: `%s`%s" +
                "%s\n" +
                "🆔 `%s`",
            title,
            description,
            startTime,
            endTime,
            location,
            fmtdReminderStatus,
            fmtdReminderTime,
            fmtdReminderSentInfo,
            ownerInfoSegment,
            eventId
        );

        sendMessage(currentChatId, details);
    }

    @Override
    public void showEventEditor(Event event) {
        if (currentChatId == null) return;
        sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2("Функция редактирования/добавления событий пока не реализована в Telegram."));
    }

    @Override
    public void showErrorMessage(String title, String message) {
        System.err.println("[View] Displaying Error: " + title + " - " + message);
        String formattedMessage = String.format("*%s*\n\n%s",
            MarkdownFormatter.escapeMarkdownV2(title),
            MarkdownFormatter.escapeMarkdownV2(message));
        sendMessage(currentChatId, formattedMessage);
    }

    @Override
    public void showInfoMessage(String title, String message) {
        System.out.println("[View] Displaying Info: " + title + " - " + message);
        String formattedMessage = String.format("*%s*\n\n%s",
            MarkdownFormatter.escapeMarkdownV2(title),
            MarkdownFormatter.escapeMarkdownV2(message));
        sendMessage(currentChatId, formattedMessage);
    }

    @Override
    public boolean showConfirmationDialog(String title, String message) {
        System.out.println("[TelegramBotView] Confirmation requested: " + title + " - " + message + ". Auto-confirming for now for bot simplicity.");
        return true;
    }

    @Override
    public void setPresenter(MainPresenter presenter) {
        this.presenter = presenter;
        System.out.println("[TelegramBotView] Presenter set.");
    }

    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            System.out.println("[TelegramBotView] Bot registered successfully!");
        } catch (TelegramApiException e) {
            System.err.println("[TelegramBotView] Error registering bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendDirectMessage(Long chatId, String text) {
        if (chatId == null) {
            System.err.println("[TelegramBotView] Attempted to send direct message with null chatId.");
            return;
        }
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(MarkdownFormatter.escapeMarkdownV2(text));
        message.setParseMode("MarkdownV2");
        executeSendMessage(message);
    }

    private void handleCallbackQuery(org.telegram.telegrambots.meta.api.objects.CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        // String messageId = callbackQuery.getMessage().getMessageId().toString();

        System.out.println("[TelegramBotView] Received callback query: " + callbackData);

        try {
            String[] parts = callbackData.split(":", 2);
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid callback data format");
            }

            String action = parts[0];
            String shortId = parts[1];
            
            boolean enableReminders = false;
            if (action.equals("r")) {
                String[] reminderParts = parts[1].split(":");
                if (reminderParts.length != 2) {
                    throw new IllegalArgumentException("Invalid reminder toggle data");
                }
                shortId = reminderParts[0];
                enableReminders = reminderParts[1].equals("1");
            }

            String fullEventId = getFullEventIdFromCallback(shortId);
            Event event = presenter.getEventById(fullEventId, chatId);

            if (event == null) {
                sendDirectMessage(chatId, MarkdownFormatter.escapeMarkdownV2("❌ Событие не найдено."));
                answerCallbackQuery(callbackQuery.getId(), "Событие не найдено");
                return;
            }

            switch (action) {
                case "d": // details
                    displayEventDetails(event);
                    answerCallbackQuery(callbackQuery.getId(), "Показаны детали события");
                    break;

                case "t": // reminder time
                    String eventTitleForDisplay = event.getTitle(); 
                    String eventIdForCommand = event.getId(); 
                    String exampleTimeForCommand = event.getStartTime().minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    String command1Text = String.format("/set_reminder_time %s;YYYY-MM-DDThh:mm", eventIdForCommand);
                    String command2Text = String.format("/set_reminder_time %s;%s", eventIdForCommand, exampleTimeForCommand);
                    String command3Text = String.format("/set_reminder_time %s;null", eventIdForCommand);

                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("Установка времени напоминания для события '%s'.\n\n", eventTitleForDisplay));
                    
                    sb.append("Используйте команду (нажмите для копирования):\n");
                    int cmd1Offset = sb.length();
                    sb.append(command1Text).append("\n\n");

                    sb.append("Например (нажмите для копирования):\n");
                    int cmd2Offset = sb.length();
                    sb.append(command2Text).append("\n\n");

                    sb.append("Для отключения напоминания (нажмите для копирования):\n");
                    int cmd3Offset = sb.length();
                    sb.append(command3Text);

                    String fullHelpText = sb.toString();
                    List<MessageEntity> entities = new ArrayList<>();

                    MessageEntity entity1 = new MessageEntity();
                    entity1.setType("bot_command");
                    entity1.setOffset(cmd1Offset);
                    entity1.setLength(command1Text.length());
                    entities.add(entity1);

                    MessageEntity entity2 = new MessageEntity();
                    entity2.setType("bot_command");
                    entity2.setOffset(cmd2Offset);
                    entity2.setLength(command2Text.length());
                    entities.add(entity2);

                    MessageEntity entity3 = new MessageEntity();
                    entity3.setType("bot_command");
                    entity3.setOffset(cmd3Offset);
                    entity3.setLength(command3Text.length());
                    entities.add(entity3);
                    
                    sendMessageWithEntities(chatId, fullHelpText, entities);
                    answerCallbackQuery(callbackQuery.getId(), "Инструкция по установке времени отправлена");
                    break;

                case "r": // toggle reminders
                    presenter.toggleEventRemindersRequested(fullEventId, enableReminders, chatId);
                    Event updatedEvent = presenter.getEventById(fullEventId, chatId);
                    if (updatedEvent != null) {
                        answerCallbackQuery(callbackQuery.getId(), 
                            "Напоминания " + (enableReminders ? "включены" : "отключены") + ". Обновите список /list_events для просмотра изменений в кнопках.");
                    } else {
                         answerCallbackQuery(callbackQuery.getId(), 
                            "Напоминания " + (enableReminders ? "включены" : "отключены") + ", но не удалось обновить кнопки.");
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }
        } catch (Exception e) {
            System.err.println("[TelegramBotView] Error handling callback query: " + e.getMessage());
            e.printStackTrace();
            sendDirectMessage(chatId, MarkdownFormatter.escapeMarkdownV2("❌ Произошла ошибка при обработке запроса."));
            answerCallbackQuery(callbackQuery.getId(), "Ошибка: " + e.getMessage());
        }
    }

    private void answerCallbackQuery(String callbackQueryId, String text) {
        org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery answer = 
            new org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        answer.setText(text);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            System.err.println("[TelegramBotView] Error answering callback query: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGrantViewAccessCommand(String args) {
        if (presenter == null || currentChatId == null) return;
        try {
            Long observerChatId = Long.parseLong(args.trim());
            presenter.grantViewAccess(currentChatId, observerChatId);
        } catch (NumberFormatException e) {
            sendDirectMessage(currentChatId, "❌ Неверный формат ID пользователя. Введите числовой ID.");
        } catch (Exception e) {
            sendDirectMessage(currentChatId, "❌ Ошибка при предоставлении доступа: " + e.getMessage());
        }
    }

    private void handleListObservedEventsCommand(String args) {
        if (presenter == null || currentChatId == null) return;
        try {
            long targetOwnerId = Long.parseLong(args);
            presenter.loadObservedEvents(currentChatId, targetOwnerId);
        } catch (NumberFormatException e) {
            sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2("❌ Неверный ID пользователя. ID должен быть числом."));
        } catch (Exception e) {
            sendMessage(currentChatId, MarkdownFormatter.escapeMarkdownV2("❌ Ошибка при загрузке событий: " + e.getMessage()));
        }
    }

    private void handleGetMyIdCommand() {
        String message = templateService.getTemplate("get_my_id", 
            "Ваш Telegram ID: {id}\\. Используйте его, чтобы другие могли предоставить вам доступ к своему расписанию\\.");
        sendMessage(currentChatId, message.replace("{id}", MarkdownFormatter.code(currentChatId.toString())));
    }

    private void handleAdminLogin(String code) {
        if (code == null || !code.equals(AppConfig.getAdminSecret())) {
            sendMessage(currentChatId, templateService.getTemplate("admin_login_fail", 
                "Неверный код администратора\\."));
            return;
        }
        adminChatIds.add(currentChatId);
        sendMessage(currentChatId, templateService.getTemplate("admin_login_success", 
            "Вы успешно вошли как администратор\\."));
    }

    private void handleAdminLogout() {
        if (!isAdmin(currentChatId)) {
            sendMessage(currentChatId, templateService.getTemplate("admin_no_access", 
                "У вас нет доступа к этой команде\\."));
            return;
        }
        adminChatIds.remove(currentChatId);
        sendMessage(currentChatId, templateService.getTemplate("admin_logout_success", 
            "✅ Вы вышли из режима администратора\\."));
    }

    private boolean isAdmin(Long chatId) {
        if (chatId == null) return false;
        return adminChatIds.contains(chatId);
    }

    private void handleListTemplates() {
        if (!isAdmin(currentChatId)) {
            sendMessage(currentChatId, templateService.getTemplate("admin_no_access", 
                "У вас нет доступа к этой команде\\."));
            return;
        }

        Map<String, String> templates = templateService.getAllAvailableTemplates();
        if (templates.isEmpty()) {
            sendMessage(currentChatId, templateService.getTemplate("admin_list_templates_empty", 
                "Шаблоны не найдены\\."));
            return;
        }

        StringBuilder message = new StringBuilder(MarkdownFormatter.bold("Доступные шаблоны для редактирования:") + "\n");
        message.append(MarkdownFormatter.italic("(Кастомные значения переопределяют дефолтные)") + "\n\n");

        for (Map.Entry<String, String> entry : templates.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            boolean isCustom = templateService.isCustomTemplate(key);

            message.append(MarkdownFormatter.code(key))
                  .append(" ")
                  .append(isCustom ? MarkdownFormatter.bold("(кастомный)") : "")
                  .append("\n")
                  .append(MarkdownFormatter.codeBlock(value))
                  .append("\n\n");
        }

        sendMessage(currentChatId, message.toString());
    }

    private void handleSetTemplateCommand(String args) {
        if (!isAdmin(currentChatId)) {
            sendMessage(currentChatId, templateService.getTemplate("admin_no_access", 
                "У вас нет доступа к этой команде\\."));
            return;
        }
        String[] parts = args.split(" ", 2);
        if (parts.length < 2) {
            sendMessage(currentChatId, templateService.getTemplate("admin_set_template_invalid_format", 
                "Неверный формат\\. Используйте: /set\\_template \\<key\\> \\<text\\>"));
            return;
        }
        String key = parts[0];
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        String text = parts[1];
        templateService.setTemplate(key, text);
        String successMessage = templateService.getTemplate("admin_set_template_success", 
            "Шаблон для ключа {key} успешно обновлен\\.");
        sendMessage(currentChatId, successMessage.replace("{key}", MarkdownFormatter.code(parts[0])));
    }

    private void handleResetTemplateCommand(String args) {
        if (!isAdmin(currentChatId)) {
            sendMessage(currentChatId, templateService.getTemplate("admin_no_access", 
                "У вас нет доступа к этой команде\\."));
            return;
        }
        if (args.trim().isEmpty()) {
            sendMessage(currentChatId, templateService.getTemplate("admin_reset_template_invalid_format", 
                "Неверный формат\\. Используйте: /reset\\_template \\<key\\>"));
            return;
        }
        String key = args.trim();
        String originalKey = key;
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        templateService.resetTemplate(key);
        String successMessage = templateService.getTemplate("admin_reset_template_success", 
            "Шаблон для ключа {key} сброшен к значению по умолчанию\\.");
        sendMessage(currentChatId, successMessage.replace("{key}", MarkdownFormatter.code(originalKey)));
    }

    private void handleGetAnalysisCommand() {
        try {
            // Используем те же параметры, что и в демо
            String dbPath = "data/events.db";
            Long ownerChatId = currentChatId;
            String outputPath = "analysis_" + ownerChatId + ".json";

            EventDAO eventDAO = new SQLiteEventDAO(dbPath);
            ScheduleAnalysisService analysisService = new ScheduleAnalysisServiceImpl(eventDAO);
            ScheduleAnalysis analysis = analysisService.analyzeCurrentWeek(ownerChatId);

            AnalysisExportDAO exportDAO = new JsonAnalysisExportDAO();
            exportDAO.exportAnalysis(analysis, outputPath);

            sendDocument(currentChatId, outputPath, "Ваш анализ расписания за текущую неделю (JSON):");
        } catch (Exception e) {
            sendMessage(currentChatId, "Произошла ошибка при формировании анализа: " + e.getMessage());
        }
    }

    private void sendDocument(Long chatId, String filePath, String caption) {
        try {
            SendDocument sendDocumentRequest = new SendDocument();
            sendDocumentRequest.setChatId(chatId.toString());
            sendDocumentRequest.setDocument(new InputFile(new java.io.File(filePath)));
            sendDocumentRequest.setCaption(caption);
            execute(sendDocumentRequest);
        } catch (TelegramApiException e) {
            sendMessage(chatId, "Ошибка при отправке файла: " + e.getMessage());
        }
    }
}