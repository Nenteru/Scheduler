// package telegram;
// package dao.impl;

// import dao.ResponseTemplateDAO;

// import java.sql.*;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.Optional;

// public class SQLiteResponseTemplateDAO implements ResponseTemplateDAO {

//     private final String dbPath;

//     public SQLiteResponseTemplateDAO(String dbPath) {
//         this.dbPath = dbPath;
//         initializeDatabase();
//     }

//     private void initializeDatabase() {
//         String createTableSQL = """
//             CREATE TABLE IF NOT EXISTS response_templates (
//                 key TEXT PRIMARY KEY,
//                 value TEXT NOT NULL
//             )
//         """;

//         try (Connection conn = getConnection();
//              Statement stmt = conn.createStatement()) {
//             stmt.execute(createTableSQL);
//         } catch (SQLException e) {
//             throw new RuntimeException("Failed to initialize database", e);
//         }
//     }

//     private Connection getConnection() throws SQLException {
//         return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
//     }

//     @Override
//     public Optional<String> findByKey(String key) {
//         String sql = "SELECT value FROM response_templates WHERE key = ?";
//         try (Connection conn = getConnection();
//              PreparedStatement stmt = conn.prepareStatement(sql)) {
//             stmt.setString(1, key);
//             ResultSet rs = stmt.executeQuery();
//             if (rs.next()) {
//                 return Optional.of(rs.getString("value"));
//             }
//         } catch (SQLException e) {
//             throw new RuntimeException("Failed to find template by key: " + key, e);
//         }
//         return Optional.empty();
//     }

//     @Override
//     public void saveOrUpdate(String key, String value) {
//         String sql = "INSERT OR REPLACE INTO response_templates (key, value) VALUES (?, ?)";
//         try (Connection conn = getConnection();
//              PreparedStatement stmt = conn.prepareStatement(sql)) {
//             stmt.setString(1, key);
//             stmt.setString(2, value);
//             stmt.executeUpdate();
//         } catch (SQLException e) {
//             throw new RuntimeException("Failed to save/update template: " + key, e);
//         }
//     }

//     @Override
//     public void delete(String key) {
//         String sql = "DELETE FROM response_templates WHERE key = ?";
//         try (Connection conn = getConnection();
//              PreparedStatement stmt = conn.prepareStatement(sql)) {
//             stmt.setString(1, key);
//             stmt.executeUpdate();
//         } catch (SQLException e) {
//             throw new RuntimeException("Failed to delete template: " + key, e);
//         }
//     }

//     @Override
//     public Map<String, String> findAll() {
//         String sql = "SELECT key, value FROM response_templates";
//         Map<String, String> templates = new HashMap<>();
//         try (Connection conn = getConnection();
//              Statement stmt = conn.createStatement();
//              ResultSet rs = stmt.executeQuery(sql)) {
//             while (rs.next()) {
//                 templates.put(rs.getString("key"), rs.getString("value"));
//             }
//         } catch (SQLException e) {
//             throw new RuntimeException("Failed to get all templates", e);
//         }
//         return templates;
//     }

//     @Override
//     public Optional<String> getTemplate(String key) {
//         return findByKey(key);
//     }

//     @Override
//     public void setTemplate(String key, String value) {
//         saveOrUpdate(key, value);
//     }

//     @Override
//     public void deleteTemplate(String key) {
//         delete(key);
//     }

//     @Override
//     public Map<String, String> getAllTemplates() {
//         return findAll();
//     }
// } 