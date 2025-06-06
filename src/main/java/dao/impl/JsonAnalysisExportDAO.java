package dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dao.AnalysisExportDAO;
import model.ScheduleAnalysis;
import java.io.File;

public class JsonAnalysisExportDAO implements AnalysisExportDAO {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void exportAnalysis(ScheduleAnalysis analysis, String filePath) throws Exception {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), analysis);
    }
} 