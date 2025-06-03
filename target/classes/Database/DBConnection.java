package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:sqlserver://ctespbd.dei.isep.ipp.pt;databaseName=2025_LP2_G5_ERM;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "2025_LP2_G5_ERM";
    private static final String PASSWORD = "LP2Grupo5";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC do SQL Server não encontrado", e);
        }
    }
}
