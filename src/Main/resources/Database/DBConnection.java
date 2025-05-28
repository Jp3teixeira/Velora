package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Atualize estas constantes com suas novas credenciais
    private static final String URL = "jdbc:mysql://sql7.freesqldatabase.com:3306/sql7781596";
    private static final String USER = "sql7781596";
    private static final String PASSWORD = "D8GRzJkduq";

    public static Connection getConnection() throws SQLException {
        try {
            // Verifica se o driver JDBC está carregado
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Tenta estabelecer a conexão
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexão bem-sucedida!"); // Debug
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC não encontrado", e);
        }
    }
}