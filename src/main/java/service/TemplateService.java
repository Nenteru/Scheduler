package service;

import java.util.Map;

/**
 * Сервис для управления шаблонами ответов бота.
 */
public interface TemplateService {

    /**
     * Получает текст шаблона по ключу.
     * Если шаблон не найден, возвращает значение по умолчанию.
     *
     * @param key          ключ шаблона
     * @param defaultValue значение по умолчанию
     * @return текст шаблона или значение по умолчанию
     */
    String getTemplate(String key, String defaultValue);

    /**
     * Устанавливает новый текст для шаблона.
     *
     * @param key   ключ шаблона
     * @param value новый текст шаблона
     */
    void setTemplate(String key, String value);

    /**
     * Сбрасывает шаблон к значению по умолчанию.
     *
     * @param key ключ шаблона
     */
    void resetTemplate(String key);

    /**
     * Возвращает все кастомные шаблоны.
     *
     * @return карта [ключ -> текст шаблона]
     */
    Map<String, String> getAllCustomTemplates();

    /**
     * Возвращает все доступные шаблоны (кастомные и по умолчанию).
     *
     * @return карта [ключ -> текст шаблона]
     */
    Map<String, String> getAllAvailableTemplates();

    /**
     * Проверяет, является ли шаблон кастомным.
     *
     * @param key ключ шаблона для проверки
     * @return true, если шаблон кастомный, иначе false
     */
    boolean isCustomTemplate(String key);
} 