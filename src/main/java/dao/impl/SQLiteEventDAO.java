package dao.impl;

import dao.EventDAO;
import model.Event;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SQLiteEventDAO implements EventDAO {
    private final String dbPath;

    public SQLiteEventDAO(String dbPath) {
        this.dbPath = dbPath;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS events (
                    id TEXT PRIMARY KEY,
                    google_id TEXT, 
                    title TEXT NOT NULL,
                    description TEXT,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    location TEXT,
                    reminder_time TEXT,
                    reminders_enabled INTEGER DEFAULT 1,
                    owner_chat_id INTEGER,
                    reminder_sent INTEGER DEFAULT 0
                )
            """;
            // Примечание по UNIQUE (google_id, owner_chat_id):
            // Решено пока не добавлять, чтобы избежать проблем с NULL google_id для локальных событий.
            // Уникальность google_id для конкретного пользователя должна обеспечиваться логикой сервиса.
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSQL);
            }
        } catch (SQLException e) {
            System.err.println("[SQLiteEventDAO] Error initializing database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    @Override
    public Event createEvent(Event event) {
        String sql = """
            INSERT INTO events (id, google_id, title, description, start_time, end_time, location, reminder_time, reminders_enabled, owner_chat_id, reminder_sent)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String id = event.getId();
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            event.setId(id); // Устанавливаем сгенерированный ID обратно в объект
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, event.getId());
            pstmt.setString(2, event.getGoogleId());
            pstmt.setString(3, event.getTitle());
            pstmt.setString(4, event.getDescription());
            pstmt.setString(5, event.getStartTime().toString());
            pstmt.setString(6, event.getEndTime().toString());
            pstmt.setString(7, event.getLocation());
            pstmt.setString(8, event.getReminderTime() != null ? event.getReminderTime().toString() : null);
            pstmt.setInt(9, event.isRemindersEnabled() ? 1 : 0);
            pstmt.setObject(10, event.getOwnerChatId());
            pstmt.setInt(11, event.isReminderSent() ? 1 : 0);

            pstmt.executeUpdate();
            return event;
        } catch (SQLException e) {
            System.err.println("[SQLiteEventDAO] Error creating event: " + e.getMessage());
            throw new RuntimeException("Failed to create event", e);
        }
    }
    
    @Override
    public Event updateEvent(Event event) {
        String sql = """
            UPDATE events 
            SET google_id = ?, title = ?, description = ?, start_time = ?, end_time = ?, location = ?,
                reminder_time = ?, reminders_enabled = ?, owner_chat_id = ?, reminder_sent = ?
            WHERE id = ? AND owner_chat_id = ? 
        """;

        if (event.getId() == null || event.getId().isEmpty()) {
            throw new IllegalArgumentException("Event ID must be provided for an update.");
        }
        if (event.getOwnerChatId() == null) {
            // Это критично для WHERE clause, сервис должен был это обеспечить.
            throw new IllegalArgumentException("OwnerChatId must be provided in Event for an update.");
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, event.getGoogleId());
            pstmt.setString(2, event.getTitle());
            pstmt.setString(3, event.getDescription());
            pstmt.setString(4, event.getStartTime().toString());
            pstmt.setString(5, event.getEndTime().toString());
            pstmt.setString(6, event.getLocation());
            pstmt.setString(7, event.getReminderTime() != null ? event.getReminderTime().toString() : null);
            pstmt.setInt(8, event.isRemindersEnabled() ? 1 : 0);
            pstmt.setObject(9, event.getOwnerChatId()); 
            pstmt.setInt(10, event.isReminderSent() ? 1 : 0);
            pstmt.setString(11, event.getId());
            pstmt.setObject(12, event.getOwnerChatId()); // Для WHERE clause

            int updatedRows = pstmt.executeUpdate();
            if (updatedRows == 0) {
                throw new IllegalArgumentException("Event with ID " + event.getId() + " not found for owner " + event.getOwnerChatId() + " or no changes needed.");
            }
            return event;
        } catch (SQLException e) {
            System.err.println("[SQLiteEventDAO] Error updating event: " + e.getMessage());
            throw new RuntimeException("Failed to update event", e);
        }
    }

    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event event = new Event(
            rs.getString("id"),
            rs.getString("google_id"), 
            rs.getString("title"),
            rs.getString("description"),
            LocalDateTime.parse(rs.getString("start_time")),
            LocalDateTime.parse(rs.getString("end_time")),
            rs.getString("location"),
            rs.getString("reminder_time") != null ? LocalDateTime.parse(rs.getString("reminder_time")) : null,
            rs.getObject("owner_chat_id") != null ? rs.getLong("owner_chat_id") : null
        );
        event.setRemindersEnabled(rs.getInt("reminders_enabled") == 1);
        event.setReminderSent(rs.getInt("reminder_sent") == 1);
        return event;
    }

    @Override
    public Optional<Event> findByIdAndOwnerChatId(String eventId, Long ownerChatId) {
        String sql = "SELECT * FROM events WHERE id = ? AND owner_chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, eventId);
            pstmt.setObject(2, ownerChatId); // setObject для Long, т.к. owner_chat_id INTEGER
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToEvent(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            System.err.println("[SQLiteEventDAO] Error in findByIdAndOwnerChatId: " + e.getMessage());
            throw new RuntimeException("Failed to find event by id and owner", e);
        }
    }

    @Override
    public Optional<Event> findByGoogleIdAndOwnerChatId(String googleId, Long ownerChatId) {
        String sql = "SELECT * FROM events WHERE google_id = ? AND owner_chat_id = ?";
        if (googleId == null || googleId.trim().isEmpty()) { // Google ID не может быть пустым для поиска
             return Optional.empty();
        }
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, googleId);
            pstmt.setObject(2, ownerChatId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToEvent(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            System.err.println("[SQLiteEventDAO] Error in findByGoogleIdAndOwnerChatId: " + e.getMessage());
            throw new RuntimeException("Failed to find event by google_id and owner", e);
        }
    }

    @Override
    public List<Event> findAllByOwnerChatId(Long ownerChatId) {
        String sql = "SELECT * FROM events WHERE owner_chat_id = ? ORDER BY start_time";
        List<Event> events = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, ownerChatId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        } catch (SQLException e) {
            System.err.println("[SQLiteEventDAO] Error in findAllByOwnerChatId: " + e.getMessage());
            throw new RuntimeException("Failed to find events by owner", e);
        }
        return events;
    }

    @Override
    public List<Event> findEventsBetweenForOwner(LocalDateTime start, LocalDateTime end, Long ownerChatId) {
        String sql = "SELECT * FROM events WHERE owner_chat_id = ? AND end_time > ? AND start_time < ? ORDER BY start_time";
        List<Event> events = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, ownerChatId);
            pstmt.setString(2, start.toString());
            pstmt.setString(3, end.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        } catch (SQLException e) {
            System.err.println("[SQLiteEventDAO] Error in findEventsBetweenForOwner: " + e.getMessage());
            throw new RuntimeException("Failed to find events in period for owner", e);
        }
        return events;
    }

    @Override
    public void deleteByIdAndOwnerChatId(String eventId, Long ownerChatId) {
        String sql = "DELETE FROM events WHERE id = ? AND owner_chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, eventId);
            pstmt.setObject(2, ownerChatId);
            int deletedRows = pstmt.executeUpdate();
            if (deletedRows == 0) {
                System.out.println("[SQLiteEventDAO] No event found with ID " + eventId + " for owner " + ownerChatId + " to delete (or already deleted).");
            } else {
                System.out.println("[SQLiteEventDAO] Successfully deleted event with ID " + eventId + " for owner " + ownerChatId);
            }
        } catch (SQLException e) {
            System.err.println("[SQLiteEventDAO] Error in deleteByIdAndOwnerChatId: " + e.getMessage());
            throw new RuntimeException("Failed to delete event", e);
        }
    }

    @Override
    public List<Event> getAllEventsGlobally() {
        String sql = "SELECT * FROM events ORDER BY start_time"; // Можно добавить сортировку, если нужно
        List<Event> events = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        } catch (SQLException e) {
            System.err.println("[SQLiteEventDAO] Error in getAllEventsGlobally: " + e.getMessage());
            throw new RuntimeException("Failed to get all events globally", e);
        }
        System.out.println("[SQLiteEventDAO] Fetched all events globally. Count: " + events.size());
        return events;
    }
} 