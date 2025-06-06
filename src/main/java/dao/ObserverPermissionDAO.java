package dao;

// import java.util.List;
import java.util.Set;

public interface ObserverPermissionDAO {
    /**
     * Добавляет разрешение для наблюдателя на просмотр событий целевого пользователя.
     * @param observerChatId ID чата наблюдателя.
     * @param targetOwnerChatId ID чата владельца событий.
     */
    void addPermission(Long observerChatId, Long targetOwnerChatId);

    /**
     * Удаляет разрешение для наблюдателя.
     * @param observerChatId ID чата наблюдателя.
     * @param targetOwnerChatId ID чата владельца событий.
     */
    void removePermission(Long observerChatId, Long targetOwnerChatId);

    /**
     * Проверяет, имеет ли наблюдатель разрешение на просмотр событий целевого пользователя.
     * @param observerChatId ID чата наблюдателя.
     * @param targetOwnerChatId ID чата владельца событий.
     * @return true, если разрешение есть, иначе false.
     */
    boolean hasPermission(Long observerChatId, Long targetOwnerChatId);

    /**
     * Получает список ID чатов пользователей, события которых может просматривать данный наблюдатель.
     * @param observerChatId ID чата наблюдателя.
     * @return Список ID чатов целевых пользователей.
     */
    Set<Long> getObservedTargetChatIds(Long observerChatId);
} 