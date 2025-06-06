package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties;

    static {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("Error loading config.properties file: " + e.getMessage());
            System.err.println("Please create config.properties file with required properties:");
            System.err.println("bot.token=YOUR_BOT_TOKEN");
            System.err.println("bot.username=YOUR_BOT_USERNAME");
            System.err.println("admin.secret=YOUR_ADMIN_SECRET");
        }
    }

    public static String getBotToken() {
        return properties.getProperty("bot.token");
    }

    public static String getBotUsername() {
        return properties.getProperty("bot.username");
    }

    public static String getAdminSecret() {
        return properties.getProperty("admin.secret");
    }
} 