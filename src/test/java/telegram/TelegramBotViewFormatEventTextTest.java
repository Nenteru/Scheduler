package telegram;

import model.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelegramBotViewFormatEventTextTest {

    @Test
    void testFormatEventText_basicFields() {
        // Arrange
        Event event = new Event();
        event.setTitle("Тестовое событие");
        event.setStartTime(LocalDateTime.of(2025, 12, 31, 10, 0, 0));
        event.setEndTime(LocalDateTime.of(2025, 12, 31, 11, 0, 0));
        event.setDescription("Описание");
        event.setLocation("Офис");
        event.setRemindersEnabled(false);
        event.setId("8030e194-9034-49b2-bed1-c34e6be27a1e");

        TelegramBotView view = new TelegramBotView("token", "username", null);

        // Act
        String result = view.formatEventText(event, false);

        // Assert
        String expected =
                "*Тестовое событие*\n" +
                "📅 Начало: `31 дек\\. 2025 10\\:00\\:00`\n" +
                "⌚️ Окончание: `31 дек\\. 2025 11\\:00\\:00`\n" +
                "📝 Описание\n" +
                "📍 Офис\n" +
                "🔔 Напоминания: `ОТКЛ`\n" +
                "🆔 `8030e194\\-9034\\-49b2\\-bed1\\-c34e6be27a1e`";
        assertEquals(expected, result.trim());
    }
}