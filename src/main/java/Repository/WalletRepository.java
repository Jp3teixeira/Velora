package Repository;

import Database.DBConnection;

import java.math.BigDecimal;
import java.sql.*;

/**
 * Repositório para gerir o saldo em euros de cada utilizador,
 *  tabela "Carteira" (colunas: id_carteira_euro, id_utilizador, saldo_eur).
 */
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

    // === CRIAR CARTEIRA (inserir linha da tabela Carteira com saldo_eur = 0) ===
    public boolean createWalletForUser(int userId) {
        String sql = "INSERT INTO Carteira (id_utilizador, saldo_eur) VALUES (?, 0)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // === OBTER SALDO (selecionar saldo_eur da tabela Carteira) ===
    public BigDecimal getSaldo(int userId) {
        String sql = "SELECT saldo_eur FROM Carteira WHERE id_utilizador = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("saldo_eur");
            } else {
                throw new RuntimeException("Carteira não encontrada para o utilizador ID: " + userId);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar saldo: " + e.getMessage(), e);
        }
    }

    // === DEPOSITAR (incrementar saldo_eur) ===
    public boolean deposit(int userId, BigDecimal amount) {
        String sql = "UPDATE Carteira SET saldo_eur = saldo_eur + ? WHERE id_utilizador = ?";

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

    // === LEVANTAR (levantar saldo_eur, só se houver saldo suficiente) ===
    public boolean withdraw(int userId, BigDecimal amount) {
        String sql = "UPDATE Carteira SET saldo_eur = saldo_eur - ? " +
                "WHERE id_utilizador = ? AND saldo_eur >= ?";

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
