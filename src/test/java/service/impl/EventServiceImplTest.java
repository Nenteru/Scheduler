package service.impl;

import dao.EventDAO;
import model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventDAO mockEventDAO;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event sampleEvent1;
    private Long ownerChatId;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        ownerChatId = 100L;
        sampleEvent1 = new Event("local-1", "Local Event 1", "Description 1", now, now.plusHours(1), "Location 1", now.minusMinutes(30), ownerChatId);
    }

    @Test
    void addEvent_newLocalEvent_createsSuccessfully() {
        Event newLocalEvent = new Event(null, "New Local", "Desc", LocalDateTime.now(), LocalDateTime.now().plusHours(1), "Local", null, null);
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        when(mockEventDAO.createEvent(eventArgumentCaptor.capture())).thenAnswer(invocation -> {
            Event eventToCreate = invocation.getArgument(0);
            if (eventToCreate.getId() == null || eventToCreate.getId().isEmpty()) {
                eventToCreate.setId(UUID.randomUUID().toString());
            }
            return eventToCreate;
        });
        Event result = eventService.addEvent(newLocalEvent, ownerChatId);
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(ownerChatId, result.getOwnerChatId());
        assertEquals("New Local", result.getTitle());
        verify(mockEventDAO).createEvent(any(Event.class));
        Event capturedEvent = eventArgumentCaptor.getValue();
        assertEquals(ownerChatId, capturedEvent.getOwnerChatId());
    }

    @Test
    void addEvent_newEventFromGoogle_notExistingLocally_createsSuccessfully() {
        String googleId = "google-123";
        Event googleEvent = new Event();
        googleEvent.setGoogleId(googleId);
        when(mockEventDAO.findByGoogleIdAndOwnerChatId(googleId, ownerChatId)).thenReturn(Optional.empty());
        when(mockEventDAO.createEvent(any(Event.class))).thenReturn(googleEvent);
        Event result = eventService.addEvent(googleEvent, ownerChatId);
        assertNotNull(result);
        assertEquals(googleId, result.getGoogleId());
        verify(mockEventDAO).findByGoogleIdAndOwnerChatId(googleId, ownerChatId);
        verify(mockEventDAO).createEvent(googleEvent);
    }

    @Test
    void addEvent_eventFromGoogle_existsLocally_noChanges_returnsExisting() {
        String googleId = "google-123";
        Event googleEvent = new Event();
        googleEvent.setGoogleId(googleId);
        googleEvent.setTitle("Title");
        googleEvent.setDescription("Desc");
        googleEvent.setStartTime(LocalDateTime.now());
        googleEvent.setEndTime(LocalDateTime.now().plusHours(1));
        googleEvent.setLocation("Loc");
        googleEvent.setRemindersEnabled(false);
        Event existingLocalEvent = new Event();
        existingLocalEvent.setId("existing-id");
        existingLocalEvent.setGoogleId(googleId);
        existingLocalEvent.setTitle("Title");
        existingLocalEvent.setDescription("Desc");
        existingLocalEvent.setStartTime(googleEvent.getStartTime());
        existingLocalEvent.setEndTime(googleEvent.getEndTime());
        existingLocalEvent.setLocation("Loc");
        existingLocalEvent.setRemindersEnabled(false);
        existingLocalEvent.setOwnerChatId(ownerChatId);
        when(mockEventDAO.findByGoogleIdAndOwnerChatId(googleId, ownerChatId)).thenReturn(Optional.of(existingLocalEvent));
        Event result = eventService.addEvent(googleEvent, ownerChatId);
        assertNotNull(result);
        assertEquals(existingLocalEvent.getId(), result.getId());
        assertEquals(ownerChatId, result.getOwnerChatId());
        verify(mockEventDAO).findByGoogleIdAndOwnerChatId(googleId, ownerChatId);
        verify(mockEventDAO, never()).createEvent(any(Event.class));
        verify(mockEventDAO, never()).updateEvent(any(Event.class));
    }

    @Test
    void addEvent_eventFromGoogle_existsLocally_withChanges_updatesSuccessfully() {
        String googleId = "google-123";
        Event googleEvent = new Event();
        googleEvent.setGoogleId(googleId);
        googleEvent.setTitle("New Title");
        googleEvent.setDescription("New Desc");
        googleEvent.setStartTime(LocalDateTime.now());
        googleEvent.setEndTime(LocalDateTime.now().plusHours(1));
        googleEvent.setLocation("New Loc");
        googleEvent.setRemindersEnabled(true);
        Event existingLocalEvent = new Event();
        existingLocalEvent.setId("existing-id");
        existingLocalEvent.setGoogleId(googleId);
        existingLocalEvent.setTitle("Old Title");
        existingLocalEvent.setDescription("Old Desc");
        existingLocalEvent.setStartTime(googleEvent.getStartTime().minusHours(1));
        existingLocalEvent.setEndTime(googleEvent.getEndTime().minusHours(1));
        existingLocalEvent.setLocation("Old Loc");
        existingLocalEvent.setRemindersEnabled(false);
        existingLocalEvent.setOwnerChatId(ownerChatId);
        when(mockEventDAO.findByGoogleIdAndOwnerChatId(googleId, ownerChatId)).thenReturn(Optional.of(existingLocalEvent));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        when(mockEventDAO.updateEvent(eventCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        Event result = eventService.addEvent(googleEvent, ownerChatId);
        assertNotNull(result);
        assertEquals(existingLocalEvent.getId(), result.getId());
        assertEquals(ownerChatId, result.getOwnerChatId());
        verify(mockEventDAO).findByGoogleIdAndOwnerChatId(googleId, ownerChatId);
        verify(mockEventDAO).updateEvent(any(Event.class));
        verify(mockEventDAO, never()).createEvent(any(Event.class));
        Event updatedEvent = eventCaptor.getValue();
        assertEquals("New Title", updatedEvent.getTitle());
        assertEquals(ownerChatId, updatedEvent.getOwnerChatId());
        assertTrue(updatedEvent.isRemindersEnabled());
    }

    @Test
    void updateEvent_validEvent_updatesSuccessfully() {
        when(mockEventDAO.findByIdAndOwnerChatId(sampleEvent1.getId(), ownerChatId)).thenReturn(Optional.of(sampleEvent1));
        when(mockEventDAO.updateEvent(sampleEvent1)).thenReturn(sampleEvent1);
        Event result = eventService.updateEvent(sampleEvent1, ownerChatId);
        assertNotNull(result);
        assertEquals(sampleEvent1.getId(), result.getId());
        verify(mockEventDAO).updateEvent(sampleEvent1);
    }

    @Test
    void updateEvent_invalidTime_throwsIllegalArgumentException() {
        Event invalidEvent = new Event("id", "title", "desc", LocalDateTime.now(), LocalDateTime.now().minusHours(1), null, null, null);
        assertThrows(IllegalArgumentException.class, () -> eventService.updateEvent(invalidEvent, ownerChatId));
        verify(mockEventDAO, never()).updateEvent(any(Event.class));
    }

    @Test
    void deleteEvent_callsDaoDelete() {
        String eventId = "event-to-delete";
        doNothing().when(mockEventDAO).deleteByIdAndOwnerChatId(eventId, ownerChatId);
        eventService.deleteEvent(eventId, ownerChatId);
        verify(mockEventDAO).deleteByIdAndOwnerChatId(eventId, ownerChatId);
    }

    @Test
    void getEventByIdAndOwner_callsDaoGetByIdAndOwner() {
        String eventId = "event-123";
        when(mockEventDAO.findByIdAndOwnerChatId(eventId, ownerChatId)).thenReturn(Optional.of(sampleEvent1));
        Optional<Event> result = eventService.getEventByIdAndOwner(eventId, ownerChatId);
        assertTrue(result.isPresent());
        assertEquals(sampleEvent1.getId(), result.get().getId());
        verify(mockEventDAO).findByIdAndOwnerChatId(eventId, ownerChatId);
    }

    @Test
    void getEventsForOwner_callsDaoFindAllByOwnerChatId() {
        List<Event> events = new ArrayList<>();
        events.add(sampleEvent1);
        when(mockEventDAO.findAllByOwnerChatId(ownerChatId)).thenReturn(events);
        List<Event> result = eventService.getEventsForOwner(ownerChatId);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(mockEventDAO).findAllByOwnerChatId(ownerChatId);
    }

    @Test
    void getEventsForPeriodForOwner_callsDaoFindEventsBetweenForOwner() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        List<Event> events = new ArrayList<>();
        events.add(sampleEvent1);
        when(mockEventDAO.findEventsBetweenForOwner(start, end, ownerChatId)).thenReturn(events);
        List<Event> result = eventService.getEventsForPeriodForOwner(start, end, ownerChatId);
        assertFalse(result.isEmpty());
        verify(mockEventDAO).findEventsBetweenForOwner(start, end, ownerChatId);
    }

    @Test
    void setEventReminderTime_eventExists_updatesAndReturnsEvent() {
        String eventId = sampleEvent1.getId();
        LocalDateTime newReminderTime = LocalDateTime.now().plusMinutes(15);
        when(mockEventDAO.findByIdAndOwnerChatId(eventId, ownerChatId)).thenReturn(Optional.of(sampleEvent1));
        when(mockEventDAO.updateEvent(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Event result = eventService.setEventReminderTime(eventId, newReminderTime, ownerChatId);
        assertNotNull(result);
        assertEquals(newReminderTime, result.getReminderTime());
        assertFalse(result.isReminderSent(), "ReminderSent flag should be reset");
        verify(mockEventDAO).findByIdAndOwnerChatId(eventId, ownerChatId);
        verify(mockEventDAO).updateEvent(sampleEvent1);
    }

    @Test
    void setEventReminderTime_eventNotFound_throwsIllegalArgumentException() {
        String eventId = "non-existent-id";
        LocalDateTime reminderTime = LocalDateTime.now();
        when(mockEventDAO.findByIdAndOwnerChatId(eventId, ownerChatId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> eventService.setEventReminderTime(eventId, reminderTime, ownerChatId));
        verify(mockEventDAO).findByIdAndOwnerChatId(eventId, ownerChatId);
        verify(mockEventDAO, never()).updateEvent(any(Event.class));
    }

    @Test
    void toggleEventReminders_eventExists_updatesAndReturnsEvent() {
        String eventId = sampleEvent1.getId();
        boolean enable = false;
        sampleEvent1.setRemindersEnabled(true);
        when(mockEventDAO.findByIdAndOwnerChatId(eventId, ownerChatId)).thenReturn(Optional.of(sampleEvent1));
        when(mockEventDAO.updateEvent(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Event result = eventService.toggleEventReminders(eventId, enable, ownerChatId);
        assertNotNull(result);
        assertEquals(enable, result.isRemindersEnabled());
        verify(mockEventDAO).findByIdAndOwnerChatId(eventId, ownerChatId);
        verify(mockEventDAO).updateEvent(sampleEvent1);
    }

    @Test
    void toggleEventReminders_eventNotFound_throwsIllegalArgumentException() {
        String eventId = "non-existent-id";
        boolean enable = true;
        when(mockEventDAO.findByIdAndOwnerChatId(eventId, ownerChatId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> eventService.toggleEventReminders(eventId, enable, ownerChatId));
        verify(mockEventDAO).findByIdAndOwnerChatId(eventId, ownerChatId);
        verify(mockEventDAO, never()).updateEvent(any(Event.class));
    }
} 