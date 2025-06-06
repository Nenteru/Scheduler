package dao.impl;

import dao.EventDAO;
import model.Event;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// Простая реализация в памяти
public class InMemoryEventDAO implements EventDAO {

    private final Map<String, Event> eventStore = new ConcurrentHashMap<>();

    @Override
    public Event createEvent(Event event) {
        if (event.getId() == null || event.getId().trim().isEmpty()) {
            String newUuid = UUID.randomUUID().toString();
            System.out.println("[DAO InMemory] Event ID was null or empty. Assigning new UUID: " + newUuid + " for event titled: " + event.getTitle());
            event.setId(newUuid);
        } 
        // Если ID предоставлен (например, сервисом после проверки GoogleId или для локального события с заранее известным ID),
        // просто используем его. Сервис отвечает за предотвращение коллизий или обновление вместо создания.
        
        if (eventStore.containsKey(event.getId())) {
            // Эта ситуация не должна возникать, если сервис правильно отрабатывает логику create/update.
            // Если событие с таким ID уже есть, это ошибка на уровне сервиса, который должен был вызвать updateEvent.
            System.err.println("[DAO InMemory] CRITICAL: Attempting to create an event with an existing ID: " + event.getId() + ". This might indicate an issue in EventService logic.");
            // Можно бросить исключение или вернуть существующее, но лучше, чтобы сервис этого не допускал.
            // throw new IllegalArgumentException("Event with ID " + event.getId() + " already exists. Use update.");
            return eventStore.get(event.getId()); // Возвращаем существующее, чтобы избежать падения, но это сигнал проблемы
        }

        eventStore.put(event.getId(), event);
        System.out.println("[DAO InMemory] Created Event: " + event);
        return event;
    }

    @Override
    public Event updateEvent(Event event) {
        if (event.getId() == null || event.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty for an update.");
        }
        // Сервис должен был проверить, что событие принадлежит пользователю.
        // DAO просто обновляет по ID.
        if (!eventStore.containsKey(event.getId())) {
            // Эта ситуация также нежелательна, если сервис сначала проверяет существование.
            System.err.println("[DAO InMemory] Attempting to update a non-existing event with ID: " + event.getId() + ". This might indicate an issue in EventService logic.");
            throw new IllegalArgumentException("Event with ID " + event.getId() + " not found for update.");
        }
        eventStore.put(event.getId(), event);
        System.out.println("[DAO InMemory] Updated Event: " + event);
        return event;
    }

    @Override
    public Optional<Event> findByIdAndOwnerChatId(String eventId, Long ownerChatId) {
        Event event = eventStore.get(eventId);
        if (event != null && Objects.equals(event.getOwnerChatId(), ownerChatId)) {
            return Optional.of(event);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Event> findByGoogleIdAndOwnerChatId(String googleId, Long ownerChatId) {
        if (googleId == null || googleId.trim().isEmpty()) {
            return Optional.empty();
        }
        return eventStore.values().stream()
                .filter(event -> googleId.equals(event.getGoogleId()) && Objects.equals(event.getOwnerChatId(), ownerChatId))
                .findFirst();
    }

    @Override
    public List<Event> findAllByOwnerChatId(Long ownerChatId) {
        return eventStore.values().stream()
                .filter(event -> Objects.equals(event.getOwnerChatId(), ownerChatId))
                .sorted(Comparator.comparing(Event::getStartTime)) // Сортировка по времени начала
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> findEventsBetweenForOwner(LocalDateTime start, LocalDateTime end, Long ownerChatId) {
        return eventStore.values().stream()
                .filter(event -> Objects.equals(event.getOwnerChatId(), ownerChatId) &&
                                 event.getEndTime().isAfter(start) && 
                                 event.getStartTime().isBefore(end))
                .sorted(Comparator.comparing(Event::getStartTime)) // Сортировка по времени начала
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByIdAndOwnerChatId(String eventId, Long ownerChatId) {
        Event event = eventStore.get(eventId);
        if (event != null && Objects.equals(event.getOwnerChatId(), ownerChatId)) {
            eventStore.remove(eventId);
            System.out.println("[DAO InMemory] Deleted Event ID: " + eventId + " for owner: " + ownerChatId);
        } else {
            System.out.println("[DAO InMemory] Event ID: " + eventId + " not found for owner: " + ownerChatId + " or does not belong to them. No deletion performed.");
            // Можно бросить исключение, если требуется более строгая обработка
            // throw new IllegalArgumentException("Event not found or not owned by user");
        }
    }

    @Override
    public List<Event> getAllEventsGlobally() {
        System.out.println("[DAO InMemory] Getting all events globally. Count: " + eventStore.size());
        return new ArrayList<>(eventStore.values());
    }
}
