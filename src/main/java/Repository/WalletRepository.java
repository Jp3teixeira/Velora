
package Repository;

import Database.DataAccessException;
import model.Carteira;
import model.Utilizador;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para Carteira, implementa operações CRUD básicas
 * e mantém métodos de negócio (deposit, withdraw, histórico).
 */
public class WalletRepository implements DAO<Carteira, Integer> {

    // Singleton
    private static WalletRepository instance;

    private WalletRepository() { }

    public static WalletRepository getInstance() {
        if (instance == null) {
            instance = new WalletRepository();
        }
        return instance;
    }

    // --- CRUD via DAO<Carteira,Integer> ---

    @Override
    public Optional<Carteira> get(Integer id) {
        String sql = "SELECT id_carteira, id_utilizador, saldo_eur FROM Carteira WHERE id_carteira = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Carteira c = new Carteira();
                    c.setId(rs.getInt("id_carteira"));
                    Utilizador u = new Utilizador();
                    u.setId(rs.getInt("id_utilizador"));
                    c.setUtilizador(u);
                    c.setSaldoEur(rs.getBigDecimal("saldo_eur"));
                    return Optional.of(c);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public List<Carteira> getAll() {
        List<Carteira> lista = new ArrayList<>();
        String sql = "SELECT id_carteira, id_utilizador, saldo_eur FROM Carteira";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Carteira c = new Carteira();
                c.setId(rs.getInt("id_carteira"));
                Utilizador u = new Utilizador();
                u.setId(rs.getInt("id_utilizador"));
                c.setUtilizador(u);
                c.setSaldoEur(rs.getBigDecimal("saldo_eur"));
                lista.add(c);
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public boolean save(Carteira c) {
        String sql = "INSERT INTO Carteira (id_utilizador, saldo_eur) VALUES (?, ?)";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getUtilizador().getId());
            ps.setBigDecimal(2, c.getSaldoEur());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        c.setId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public boolean update(Carteira c) {
        String sql = "UPDATE Carteira SET saldo_eur = ? WHERE id_carteira = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, c.getSaldoEur());
            ps.setInt(2, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Carteira WHERE id_carteira = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    // --- Métodos de negócio adicionais ---

    /**
     * Retorna o saldo atual em euros de um utilizador.
     */
    public BigDecimal getSaldoPorUtilizador(int userId) {
        String sql = "SELECT saldo_eur FROM Carteira WHERE id_utilizador = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("saldo_eur");
                }
                throw new DataAccessException(new SQLException("Carteira não encontrada para o utilizador ID: " + userId));
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /**
     * Deposit: acrescenta valor ao saldo de um utilizador.
     */
    public boolean deposit(int userId, BigDecimal amount) {
        Optional<Carteira> opt = getByUser(userId);
        if (opt.isPresent()) {
            Carteira c = opt.get();
            c.setSaldoEur(c.getSaldoEur().add(amount));
            return update(c);
        }
        return false;
    }

    /**
     * Withdraw: subtrai valor do saldo se houver saldo suficiente.
     */
    public boolean withdraw(int userId, BigDecimal amount) {
        Optional<Carteira> opt = getByUser(userId);
        if (opt.isPresent()) {
            Carteira c = opt.get();
            if (c.getSaldoEur().compareTo(amount) >= 0) {
                c.setSaldoEur(c.getSaldoEur().subtract(amount));
                return update(c);
            }
        }
        return false;
    }

    /**
     * Utilitário: obtém Carteira por userId.
     */
    private Optional<Carteira> getByUser(int userId) {
        String sql = "SELECT id_carteira, id_utilizador, saldo_eur FROM Carteira WHERE id_utilizador = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Carteira c = new Carteira();
                    c.setId(rs.getInt("id_carteira"));
                    Utilizador u = new Utilizador();
                    u.setId(userId);
                    c.setUtilizador(u);
                    c.setSaldoEur(rs.getBigDecimal("saldo_eur"));
                    return Optional.of(c);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /**
     * Obtém histórico de saldo.
     */
    public List<Object[]> getSaldoHistorico(int userId) {
        List<Object[]> historico = new ArrayList<>();
        String sql = """
            SELECT GETDATE() AS data_hora, saldo_eur AS saldo_acumulado
              FROM dbo.Carteira
             WHERE id_utilizador = ?
        """;
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    historico.add(new Object[]{
                            rs.getTimestamp("data_hora").toLocalDateTime(),
                            rs.getBigDecimal("saldo_acumulado")
                    });
                }
            }
            return historico;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }
}

