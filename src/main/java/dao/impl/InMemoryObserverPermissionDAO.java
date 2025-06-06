package dao.impl;

import dao.ObserverPermissionDAO;
import java.util.Collections;
// import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryObserverPermissionDAO implements ObserverPermissionDAO {

    // Ключ: observerChatId, Значение: Set<targetOwnerChatId>
    private final Map<Long, Set<Long>> observerPermissions = new ConcurrentHashMap<>();

    @Override
    public void addPermission(Long observerChatId, Long targetOwnerChatId) {
        observerPermissions.computeIfAbsent(observerChatId, k -> ConcurrentHashMap.newKeySet()).add(targetOwnerChatId);
        System.out.println("[ObserverDAO] Permission granted for observer " + observerChatId + " to view events of " + targetOwnerChatId);
    }

    @Override
    public void removePermission(Long observerChatId, Long targetOwnerChatId) {
        Set<Long> targets = observerPermissions.get(observerChatId);
        if (targets != null) {
            if (targets.remove(targetOwnerChatId)) {
                System.out.println("[ObserverDAO] Permission revoked for observer " + observerChatId + " from viewing events of " + targetOwnerChatId);
            }
            if (targets.isEmpty()) {
                observerPermissions.remove(observerChatId);
            }
        }
    }

    @Override
    public boolean hasPermission(Long observerChatId, Long targetOwnerChatId) {
        return observerPermissions.getOrDefault(observerChatId, Collections.emptySet()).contains(targetOwnerChatId);
    }

    @Override
    public Set<Long> getObservedTargetChatIds(Long observerChatId) {
        return observerPermissions.getOrDefault(observerChatId, Collections.emptySet());
    }
} 