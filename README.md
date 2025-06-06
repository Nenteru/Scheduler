# Telegram Schedule Bot

Telegram бот для управления расписанием с интеграцией Google Calendar.

## Функциональность

- Просмотр событий из Google Calendar
- Предоставление доступа другим пользователям для просмотра вашего расписания
- Административный интерфейс для управления шаблонами сообщений
- Поддержка Markdown форматирования

## Настройка проекта

### Предварительные требования

- Java 11 или выше
- Maven
- Telegram Bot Token
- Google Cloud Project с включенным Calendar API

### Настройка Google Calendar API

1. Создайте проект в [Google Cloud Console](https://console.cloud.google.com)
2. Включите Google Calendar API для вашего проекта
3. Создайте учетные данные OAuth 2.0
4. Скачайте файл credentials.json и поместите его в `src/main/resources/`

### Настройка Telegram бота

1. Создайте нового бота через [@BotFather](https://t.me/botfather)
2. Получите токен бота
3. Скопируйте файл `config.properties.example` в `config.properties`
4. Заполните следующие параметры в `config.properties`:
   ```properties
   bot.token=YOUR_BOT_TOKEN
   bot.username=YOUR_BOT_USERNAME
   admin.secret=YOUR_ADMIN_SECRET
   ```