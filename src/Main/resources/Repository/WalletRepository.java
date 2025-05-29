// src/main/java/repository/WalletRepository.java
package Repository;

import Database.DBConnection;
import java.math.BigDecimal;
import java.sql.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;

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
        String updateSql = "UPDATE wallets SET saldo = saldo - ? WHERE id_user = ? AND saldo >= ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            updateStmt.setBigDecimal(1, amount);
            updateStmt.setInt(2, userId);
            updateStmt.setBigDecimal(3, amount); // Garante saldo suficiente

            int rows = updateStmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Buscar novo Saldo

    public static BigDecimal getSaldo(int userId) {
        String sql = "SELECT saldo FROM wallets WHERE id_user = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("saldo");
            } else {
                throw new RuntimeException("Carteira não encontrada para o usuário ID: " + userId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar saldo: " + e.getMessage(), e);
        }
    }


}