// src/main/java/repository/WalletRepository.java
package Repository;

import Database.DBConnection;
import java.math.BigDecimal;
import java.sql.*;

public class WalletRepository {

    public boolean createWalletForUser(int userId) {
        String sql = "INSERT INTO wallets (saldo, id_user) VALUES (0, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public BigDecimal getUserWalletBalance(int userId) {
        String sql = "SELECT saldo FROM wallets WHERE id_user = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("saldo");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }
}