// PortfolioRepository.java
package Repository;

import Database.DataAccessException;
import model.Moeda;
import model.Portfolio;
import model.Utilizador;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para Portfolio, implementa operações CRUD básicas
 * e mantém métodos de negócio (aumentar/diminuir quantidade, obter por utilizador).
 */
public class PortfolioRepository implements DAO<Portfolio, Integer> {

    public PortfolioRepository() {}

    // --- CRUD via DAO<Portfolio,Integer> ---

    @Override
    public Optional<Portfolio> get(Integer id) {
        String sql = """
            SELECT id_portfolio, id_utilizador, id_moeda,
                   nome, simbolo, quantidade, valor_atual, preco_medio_compra
              FROM v_PortfolioDetalhado
             WHERE id_portfolio = ?
        """;
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao buscar Portfolio ID " + id, e);
        }
    }

    @Override
    public List<Portfolio> getAll() {
        String sql = """
            SELECT id_portfolio, id_utilizador, id_moeda,
                   nome, simbolo, quantidade, valor_atual, preco_medio_compra
              FROM v_PortfolioDetalhado
        """;
        List<Portfolio> lista = new ArrayList<>();
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao listar todos os Portfolios", e);
        }
    }

    @Override
    public boolean save(Portfolio p) {
        String sql = "INSERT INTO Portfolio (id_utilizador, id_moeda, quantidade) VALUES (?, ?, ?)";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getUtilizador().getId());
            ps.setInt(2, p.getMoeda().getId());
            ps.setBigDecimal(3, p.getQuantidade());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        p.setId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao inserir Portfolio", e);
        }
    }

    @Override
    public boolean update(Portfolio p) {
        String sql = "UPDATE Portfolio SET quantidade = ? WHERE id_portfolio = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, p.getQuantidade());
            ps.setInt(2, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao atualizar Portfolio ID " + p.getId(), e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Portfolio WHERE id_portfolio = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao eliminar Portfolio ID " + id, e);
        }
    }

    // --- Métodos de negócio adicionais ---

    /**
     * Lista todos os itens do portfólio de um utilizador.
     */
    public List<Portfolio> listarPorUtilizador(int userId) {
        String sql = """
            SELECT id_portfolio, id_utilizador, id_moeda,
                   nome, simbolo, quantidade, valor_atual, preco_medio_compra
              FROM v_PortfolioDetalhado
             WHERE id_utilizador = ?
        """;
        List<Portfolio> lista = new ArrayList<>();
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapRow(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao listar Portfolio do utilizador ID " + userId, e);
        }
    }

    /**
     * Incrementa (ou insere) quantidade de uma moeda no portfólio.
     */
    public void aumentarQuantidade(int userId, int idMoeda, BigDecimal quantidade) {
        String sqlUp = """
            UPDATE Portfolio
               SET quantidade = quantidade + ?
             WHERE id_utilizador = ? AND id_moeda = ?
        """;
        String sqlIn = """
            INSERT INTO Portfolio (id_utilizador, id_moeda, quantidade)
            VALUES (?, ?, ?)
        """;
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUp)) {
            ps.setBigDecimal(1, quantidade);
            ps.setInt(2, userId);
            ps.setInt(3, idMoeda);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                try (PreparedStatement ps2 = conn.prepareStatement(sqlIn)) {
                    ps2.setInt(1, userId);
                    ps2.setInt(2, idMoeda);
                    ps2.setBigDecimal(3, quantidade);
                    ps2.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao aumentar quantidade no Portfolio", e);
        }
    }

    /**
     * Decrementa quantidade de uma moeda; retorna true se sucesso.
     */
    public boolean diminuirQuantidade(int userId, int idMoeda, BigDecimal quantidade) {
        String sqlCheck = """
            SELECT quantidade
              FROM Portfolio
             WHERE id_utilizador = ? AND id_moeda = ?
        """;
        String sqlUp = """
            UPDATE Portfolio
               SET quantidade = quantidade - ?
             WHERE id_utilizador = ? AND id_moeda = ?
        """;
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps1 = conn.prepareStatement(sqlCheck)) {
            ps1.setInt(1, userId);
            ps1.setInt(2, idMoeda);
            try (ResultSet rs = ps1.executeQuery()) {
                if (rs.next() && rs.getBigDecimal("quantidade").compareTo(quantidade) >= 0) {
                    try (PreparedStatement ps2 = conn.prepareStatement(sqlUp)) {
                        ps2.setBigDecimal(1, quantidade);
                        ps2.setInt(2, userId);
                        ps2.setInt(3, idMoeda);
                        return ps2.executeUpdate() > 0;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao diminuir quantidade no Portfolio", e);
        }
    }

    /**
     * Retorna a quantidade atual de uma moeda no portfólio.
     */
    public BigDecimal getQuantidade(int userId, int idMoeda) {
        String sql = """
            SELECT quantidade
              FROM Portfolio
             WHERE id_utilizador = ? AND id_moeda = ?
        """;
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, idMoeda);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("quantidade");
                }
            }
            return BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao obter quantidade do Portfolio", e);
        }
    }

    // --- Mapeamento de ResultSet para Portfolio ---

    private Portfolio mapRow(ResultSet rs) throws SQLException {
        Portfolio p = new Portfolio();
        p.setId(rs.getInt("id_portfolio"));

        Utilizador u = new Utilizador();
        u.setId(rs.getInt("id_utilizador"));
        p.setUtilizador(u);

        Moeda m = new Moeda();
        m.setId(rs.getInt("id_moeda"));
        m.setNome(rs.getString("nome"));
        m.setSimbolo(rs.getString("simbolo"));
        m.setValorAtual(rs.getBigDecimal("valor_atual"));
        p.setMoeda(m);

        p.setQuantidade(rs.getBigDecimal("quantidade"));
        p.setPrecoMedioCompra(rs.getBigDecimal("preco_medio_compra"));
        return p;
    }
}
