package Repository;

import Database.DBConnection;
import java.math.BigDecimal;
import java.sql.*;

public class WalletRepository {

    // === SINGLETON ===
    private static WalletRepository instance;

    private WalletRepository() {}

    public static WalletRepository getInstance() {
        if (instance == null) {
            instance = new WalletRepository();
        }
        return instance;
    }

    // === CRIAR CARTEIRA ===
    public boolean createWalletForUser(int userId) {
        String sql = "INSERT INTO wallets (saldo, id_user) VALUES (0, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // === OBTER SALDO ===
    public BigDecimal getSaldo(int userId) {
        String sql = "SELECT saldo FROM wallets WHERE id_user = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("saldo");
            } else {
                throw new RuntimeException("Carteira não encontrada para o utilizador ID: " + userId);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar saldo: " + e.getMessage(), e);
        }
    }

    // === DEPOSITAR ===
    public boolean deposit(int userId, BigDecimal amount) {
        String sql = "UPDATE wallets SET saldo = saldo + ? WHERE id_user = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, amount);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // === LEVANTAR ===
    public boolean withdraw(int userId, BigDecimal amount) {
        String sql = "UPDATE wallets SET saldo = saldo - ? WHERE id_user = ? AND saldo >= ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, amount);
            stmt.setInt(2, userId);
            stmt.setBigDecimal(3, amount);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
