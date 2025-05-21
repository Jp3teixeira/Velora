//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://sql7.freesqldatabase.com:3306/sql7780078";
    private static final String USER = "sql7780078";
    private static final String PASSWORD = "bz1R5hCj1N";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://sql7.freesqldatabase.com:3306/sql7780078", "sql7780078", "bz1R5hCj1N");
    }
}