package dao.impl;

import dao.ResponseTemplateDAO;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryResponseTemplateDAO implements ResponseTemplateDAO {

    private final ConcurrentHashMap<String, String> templates = new ConcurrentHashMap<>();

    @Override
    public Optional<String> findByKey(String key) {
        return Optional.ofNullable(templates.get(key));
    }

    @Override
    public void saveOrUpdate(String key, String value) {
        templates.put(key, value);
    }

    @Override
    public void delete(String key) {
        templates.remove(key);
    }

    @Override
    public Map<String, String> findAll() {
        return new ConcurrentHashMap<>(templates);
    }
} 