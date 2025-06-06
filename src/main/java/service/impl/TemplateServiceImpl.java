package service.impl;

import dao.ResponseTemplateDAO;
import service.TemplateService;

import java.util.Map;

public class TemplateServiceImpl implements TemplateService {
    private final ResponseTemplateDAO templateDAO;

    public TemplateServiceImpl(ResponseTemplateDAO templateDAO) {
        this.templateDAO = templateDAO;
    }

    @Override
    public String getTemplate(String key, String defaultValue) {
        return templateDAO.findByKey(key).orElse(defaultValue);
    }

    @Override
    public void setTemplate(String key, String value) {
        templateDAO.saveOrUpdate(key, value);
    }

    @Override
    public void resetTemplate(String key) {
        templateDAO.delete(key);
    }

    @Override
    public Map<String, String> getAllCustomTemplates() {
        return templateDAO.findAll();
    }

    @Override
    public Map<String, String> getAllAvailableTemplates() {
        return templateDAO.findAll();
    }

    @Override
    public boolean isCustomTemplate(String key) {
        return templateDAO.findByKey(key).isPresent();
    }
} 