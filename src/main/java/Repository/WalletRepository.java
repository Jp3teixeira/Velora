package Repository;

import Database.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositório para gerir o saldo em euros de cada utilizador e obter histórico via função fnSaldoHistorico.
 */
public class WalletRepository {

    // Singleton
    private static WalletRepository instance;

    private WalletRepository() {}

    public static WalletRepository getInstance() {
        if (instance == null) {
            instance = new WalletRepository();
        }
        return instance;
    }

    /**
     * Obtém conexão JDBC.
     */
    public Connection getConnection() {
        try {
            return DBConnection.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter conexão: " + e.getMessage(), e);
        }
    }

    /**
     * Cria carteira com saldo zero.
     */
    public boolean createWalletForUser(int userId) {
        String sql = "INSERT INTO Carteira (id_utilizador, saldo_eur) VALUES (?, 0)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retorna o saldo atual em euros.
     */
    public BigDecimal getSaldo(int userId) {
        String sql = "SELECT saldo_eur FROM Carteira WHERE id_utilizador = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("saldo_eur");
                }
            }
            throw new RuntimeException("Carteira não encontrada para o utilizador ID: " + userId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar saldo: " + e.getMessage(), e);
        }
    }

    /**
     * Acrescenta valor ao saldo.
     */
    public boolean deposit(int userId, BigDecimal amount) {
        String sql = "UPDATE Carteira SET saldo_eur = saldo_eur + ? WHERE id_utilizador = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Subtrai valor do saldo se houver saldo suficiente.
     */
    public boolean withdraw(int userId, BigDecimal amount) {
        String sql = "UPDATE Carteira SET saldo_eur = saldo_eur - ? " +
                "WHERE id_utilizador = ? AND saldo_eur >= ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, userId);
            ps.setBigDecimal(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtém histórico de saldo como lista de pares [dataHora, saldoAcumulado], sem model específico.
     */
    public List<Object[]> getSaldoHistorico(int userId) {
        List<Object[]> historico = new ArrayList<>();
        String sql = "SELECT data_hora, saldo_acumulado FROM dbo.fnSaldoHistorico(?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime dataHora = rs.getTimestamp("data_hora").toLocalDateTime();
                    BigDecimal saldo = rs.getBigDecimal("saldo_acumulado");
                    historico.add(new Object[]{dataHora, saldo});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historico;
    }
}
