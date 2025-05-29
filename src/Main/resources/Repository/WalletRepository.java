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

    public boolean deposit(int userId, BigDecimal amount) {
        String sql = "UPDATE wallets SET saldo = saldo + ? WHERE id_user = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, amount);
            stmt.setInt(2, userId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean withdraw(int userId, BigDecimal amount) {
        // Verifica saldo antes de retirar
        String checkSql = "SELECT saldo FROM wallets WHERE id_user = ?";
        String updateSql = "UPDATE wallets SET saldo = saldo - ? WHERE id_user = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            // Verifica saldo atual
            checkStmt.setInt(1, userId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                BigDecimal currentBalance = rs.getBigDecimal("saldo");
                if (currentBalance.compareTo(amount) < 0) {
                    return false; // Saldo insuficiente
                }
            } else {
                return false; // Carteira não encontrada
            }

            // Executa a retirada
            updateStmt.setBigDecimal(1, amount);
            updateStmt.setInt(2, userId);
            int rows = updateStmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}