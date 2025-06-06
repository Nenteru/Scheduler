package dao;

import java.util.Map;
import java.util.Optional;

/**
 * DAO для управления шаблонами ответов бота.
 */
public interface ResponseTemplateDAO {

    /**
     * Находит шаблон по его ключу.
     *
     * @param key ключ шаблона.
     * @return Optional с текстом шаблона, или пустой Optional, если не найден.
     */
    Optional<String> findByKey(String key);

    /**
     * Сохраняет или обновляет шаблон в хранилище.
     *
     * @param key   уникальный ключ шаблона (e.g., "event_details").
     * @param value текст шаблона с плейсхолдерами (e.g., "Название: {title}").
     */
    void saveOrUpdate(String key, String value);

    /**
     * Удаляет шаблон по ключу (сброс к значению по умолчанию).
     *
     * @param key ключ шаблона для удаления.
     */
    void delete(String key);

    /**
     * Возвращает все сохраненные шаблоны.
     *
     * @return Карта [ключ -> текст шаблона].
     */
    Map<String, String> findAll();
} 