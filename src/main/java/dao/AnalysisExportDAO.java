package dao;

import model.ScheduleAnalysis;

public interface AnalysisExportDAO {
    void exportAnalysis(ScheduleAnalysis analysis, String filePath) throws Exception;
} 