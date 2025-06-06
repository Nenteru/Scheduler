package service.impl;

import dao.EventDAO;
import dao.AnalysisExportDAO;
import dao.impl.JsonAnalysisExportDAO;
import dao.impl.SQLiteEventDAO;
import model.ScheduleAnalysis;
import service.ScheduleAnalysisService;

public class ScheduleAnalysisDemo {
    public static void main(String[] args) throws Exception {
        String dbPath = "data/events.db"; // путь к вашей базе
        Long ownerChatId = 1125972243L; // пример ID пользователя
        String outputPath = "analysis.json";

        EventDAO eventDAO = new SQLiteEventDAO(dbPath);
        ScheduleAnalysisService analysisService = new ScheduleAnalysisServiceImpl(eventDAO);
        ScheduleAnalysis analysis = analysisService.analyzeCurrentWeek(ownerChatId);

        AnalysisExportDAO exportDAO = new JsonAnalysisExportDAO();
        exportDAO.exportAnalysis(analysis, outputPath);

        System.out.println("Анализ недели экспортирован в " + outputPath);
    }
} 