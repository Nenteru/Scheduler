package dao.impl;

import model.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteEventDAOTest {

    private SQLiteEventDAO eventDAO;
    private String dbPath;
    private Long ownerChatId;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws SQLException {
        File dbFile = tempDir.resolve("test_events.db").toFile();
        dbPath = dbFile.getAbsolutePath();
        eventDAO = new SQLiteEventDAO(dbPath);
        ownerChatId = 1L;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM events");
        }
    }

    @AfterEach
    void tearDown() {
        // @TempDir cleanup
    }

    private Event createSampleEvent(String id, String title, LocalDateTime startTime, LocalDateTime endTime) {
        Event event = new Event(id, null, title, "Description for " + title, startTime, endTime, "Location", startTime.plusHours(1), ownerChatId);
        event.setRemindersEnabled(true);
        return event;
    }

    @Test
    void createAndFindByIdAndOwnerChatId() {
        LocalDateTime now = LocalDateTime.now();
        String eventId = UUID.randomUUID().toString();
        Event newEvent = createSampleEvent(eventId, "Test Event 1", now, now.plusHours(2));
        eventDAO.createEvent(newEvent);
        Optional<Event> retrievedEventOpt = eventDAO.findByIdAndOwnerChatId(eventId, ownerChatId);
        assertTrue(retrievedEventOpt.isPresent(), "Event should be found by ID and owner");
        Event retrievedEvent = retrievedEventOpt.get();
        assertEquals(eventId, retrievedEvent.getId());
        assertEquals(newEvent.getTitle(), retrievedEvent.getTitle());
        assertEquals(newEvent.getStartTime(), retrievedEvent.getStartTime());
        assertEquals(newEvent.getOwnerChatId(), retrievedEvent.getOwnerChatId());
        assertTrue(retrievedEvent.isRemindersEnabled());
    }

    @Test
    void createEvent_generatesId_ifNotProvided() {
        LocalDateTime now = LocalDateTime.now();
        Event newEvent = createSampleEvent(null, "Event With Generated ID", now, now.plusHours(1));
        Event createdEvent = eventDAO.createEvent(newEvent);
        assertNotNull(createdEvent.getId(), "ID should be generated");
        assertFalse(createdEvent.getId().isEmpty(), "Generated ID should not be empty");
        Optional<Event> retrievedEventOpt = eventDAO.findByIdAndOwnerChatId(createdEvent.getId(), ownerChatId);
        assertTrue(retrievedEventOpt.isPresent(), "Event with generated ID should be retrievable");
    }

    @Test
    void findByIdAndOwnerChatId_nonExistent_returnsEmpty() {
        Optional<Event> event = eventDAO.findByIdAndOwnerChatId("non-existent-id", ownerChatId);
        assertFalse(event.isPresent(), "Should not find a non-existent event");
    }

    @Test
    void findAllByOwnerChatId() {
        LocalDateTime now = LocalDateTime.now();
        Event event1 = createSampleEvent(UUID.randomUUID().toString(), "Event Alpha", now, now.plusHours(1));
        Event event2 = createSampleEvent(UUID.randomUUID().toString(), "Event Beta", now.plusDays(1), now.plusDays(1).plusHours(1));
        eventDAO.createEvent(event1);
        eventDAO.createEvent(event2);
        List<Event> allEvents = eventDAO.findAllByOwnerChatId(ownerChatId);
        assertEquals(2, allEvents.size(), "Should retrieve all created events for owner");
        assertTrue(allEvents.stream().anyMatch(e -> e.getId().equals(event1.getId())));
        assertTrue(allEvents.stream().anyMatch(e -> e.getId().equals(event2.getId())));
    }

    @Test
    void findEventsBetweenForOwner() {
        LocalDateTime baseTime = LocalDateTime.of(2024, 6, 15, 10, 0);
        Event event1 = createSampleEvent(UUID.randomUUID().toString(), "Event In Range 1", baseTime, baseTime.plusHours(2));
        Event event2 = createSampleEvent(UUID.randomUUID().toString(), "Event In Range 2", baseTime.plusHours(1), baseTime.plusHours(3));
        Event event3 = createSampleEvent(UUID.randomUUID().toString(), "Event Before Range", baseTime.minusHours(3), baseTime.minusHours(1));
        Event event4 = createSampleEvent(UUID.randomUUID().toString(), "Event After Range", baseTime.plusHours(5), baseTime.plusHours(6));
        eventDAO.createEvent(event1);
        eventDAO.createEvent(event2);
        eventDAO.createEvent(event3);
        eventDAO.createEvent(event4);
        List<Event> foundEvents = eventDAO.findEventsBetweenForOwner(baseTime, baseTime.plusHours(4), ownerChatId);
        assertEquals(2, foundEvents.size(), "Should find 2 events in the specified range for owner");
        assertTrue(foundEvents.stream().anyMatch(e -> e.getId().equals(event1.getId())));
        assertTrue(foundEvents.stream().anyMatch(e -> e.getId().equals(event2.getId())));
        assertFalse(foundEvents.stream().anyMatch(e -> e.getId().equals(event3.getId())));
        assertFalse(foundEvents.stream().anyMatch(e -> e.getId().equals(event4.getId())));
    }

    @Test
    void updateEvent() {
        LocalDateTime now = LocalDateTime.now();
        String eventId = UUID.randomUUID().toString();
        Event originalEvent = createSampleEvent(eventId, "Original Title", now, now.plusHours(1));
        eventDAO.createEvent(originalEvent);
        Event eventToUpdate = eventDAO.findByIdAndOwnerChatId(eventId, ownerChatId).orElseThrow();
        eventToUpdate.setTitle("Updated Title");
        eventToUpdate.setLocation("New Location");
        eventToUpdate.setRemindersEnabled(false);
        Event updatedEvent = eventDAO.updateEvent(eventToUpdate);
        assertEquals("Updated Title", updatedEvent.getTitle());
        assertEquals("New Location", updatedEvent.getLocation());
        assertFalse(updatedEvent.isRemindersEnabled());
        Event retrievedAfterUpdate = eventDAO.findByIdAndOwnerChatId(eventId, ownerChatId).orElseThrow();
        assertEquals("Updated Title", retrievedAfterUpdate.getTitle());
        assertFalse(retrievedAfterUpdate.isRemindersEnabled());
    }

    @Test
    void updateEvent_nonExistent_throwsException() {
        LocalDateTime now = LocalDateTime.now();
        Event nonExistentEvent = createSampleEvent("non-existent-id", "Non Existent", now, now.plusHours(1));
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            eventDAO.updateEvent(nonExistentEvent);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void deleteByIdAndOwnerChatId() {
        LocalDateTime now = LocalDateTime.now();
        String eventId = UUID.randomUUID().toString();
        Event eventToDelete = createSampleEvent(eventId, "To Be Deleted", now, now.plusHours(1));
        eventDAO.createEvent(eventToDelete);
        assertTrue(eventDAO.findByIdAndOwnerChatId(eventId, ownerChatId).isPresent(), "Event should exist before deletion");
        eventDAO.deleteByIdAndOwnerChatId(eventId, ownerChatId);
        assertFalse(eventDAO.findByIdAndOwnerChatId(eventId, ownerChatId).isPresent(), "Event should not exist after deletion");
    }

    @Test
    void findByGoogleIdAndOwnerChatId() {
        LocalDateTime now = LocalDateTime.now();
        String googleId = "google-event-123";
        Event event = createSampleEvent(UUID.randomUUID().toString(), "Google Synced Event", now, now.plusHours(1));
        event.setGoogleId(googleId);
        eventDAO.createEvent(event);
        Optional<Event> found = eventDAO.findByGoogleIdAndOwnerChatId(googleId, ownerChatId);
        assertTrue(found.isPresent());
        assertEquals(googleId, found.get().getGoogleId());
    }

    @Test
    void getAllEventsGlobally() {
        LocalDateTime now = LocalDateTime.now();
        Event event1 = createSampleEvent(UUID.randomUUID().toString(), "Event1", now, now.plusHours(1));
        Event event2 = createSampleEvent(UUID.randomUUID().toString(), "Event2", now.plusDays(1), now.plusDays(1).plusHours(1));
        eventDAO.createEvent(event1);
        eventDAO.createEvent(event2);
        List<Event> allEvents = eventDAO.getAllEventsGlobally();
        assertTrue(allEvents.size() >= 2, "Should retrieve all events globally");
        assertTrue(allEvents.stream().anyMatch(e -> e.getId().equals(event1.getId())));
        assertTrue(allEvents.stream().anyMatch(e -> e.getId().equals(event2.getId())));
    }
} 