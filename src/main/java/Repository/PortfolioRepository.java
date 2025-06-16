package Repository;

import Database.DBConnection;
import model.Portfolio;
import model.Moeda;
import model.Utilizador;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PortfolioRepository {

    /**
     * Lista todos os itens do portfólio de um utilizador usando view v_PortfolioDetalhado.
     */
    public List<Portfolio> listarPorUtilizador(int userId) {
        List<Portfolio> lista = new ArrayList<>();
        String sql = """
            SELECT id_portfolio,
                   id_utilizador,
                   id_moeda,
                   nome,
                   simbolo,
                   quantidade,
                   valor_atual,
                   preco_medio_compra
              FROM v_PortfolioDetalhado
             WHERE id_utilizador = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Portfolio p = new Portfolio();
                    p.setIdPortfolio(rs.getInt("id_portfolio"));

                    Utilizador u = new Utilizador();
                    u.setIdUtilizador(rs.getInt("id_utilizador"));
                    p.setUtilizador(u);

                    Moeda m = new Moeda();
                    m.setIdMoeda(rs.getInt("id_moeda"));
                    m.setNome(rs.getString("nome"));
                    m.setSimbolo(rs.getString("simbolo"));
                    m.setValorAtual(rs.getBigDecimal("valor_atual"));
                    p.setMoeda(m);

                    p.setQuantidade(rs.getBigDecimal("quantidade"));
                    p.setPrecoMedioCompra(rs.getBigDecimal("preco_medio_compra"));

                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Obtém o preço médio de compra de uma moeda para um utilizador
     * via função fn_CalcularPrecoMedioCompra.
     */
    public BigDecimal getPrecoMedioCompra(int userId, int idMoeda) {
        String sql = "SELECT dbo.fn_CalcularPrecoMedioCompra(?, ?) AS preco";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, idMoeda);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("preco").setScale(8, RoundingMode.HALF_UP);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Incrementa (ou insere) quantidade de uma moeda para o utilizador.
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
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stUp = conn.prepareStatement(sqlUp)) {

            stUp.setBigDecimal(1, quantidade);
            stUp.setInt(2, userId);
            stUp.setInt(3, idMoeda);
            int rows = stUp.executeUpdate();
            if (rows == 0) {
                try (PreparedStatement stIn = conn.prepareStatement(sqlIn)) {
                    stIn.setInt(1, userId);
                    stIn.setInt(2, idMoeda);
                    stIn.setBigDecimal(3, quantidade);
                    stIn.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Decrementa quantidade de uma moeda no portfólio do utilizador.
     * Retorna true se conseguiu, false caso quantidade insuficiente.
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

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stCheck = conn.prepareStatement(sqlCheck)) {

            stCheck.setInt(1, userId);
            stCheck.setInt(2, idMoeda);
            try (ResultSet rs = stCheck.executeQuery()) {
                if (rs.next()) {
                    BigDecimal atual = rs.getBigDecimal("quantidade");
                    if (atual.compareTo(quantidade) >= 0) {
                        try (PreparedStatement stUp = conn.prepareStatement(sqlUp)) {
                            stUp.setBigDecimal(1, quantidade);
                            stUp.setInt(2, userId);
                            stUp.setInt(3, idMoeda);
                            stUp.executeUpdate();
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retorna a quantidade atual de uma moeda específica no portfólio do utilizador.
     */
    public BigDecimal getQuantidade(int userId, int idMoeda) {
        String sql = """
            SELECT quantidade
              FROM Portfolio
             WHERE id_utilizador = ? AND id_moeda = ?
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, idMoeda);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("quantidade");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }
}
