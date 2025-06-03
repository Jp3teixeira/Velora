package Repository;

import Database.DBConnection;
import model.Portfolio;
import model.Moeda;
import model.Utilizador;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositório para ler/escrever na tabela Portfolio.
 */
public class PortfolioRepository {

    /**
     * Retorna todos os itens do portfólio do utilizador (com Moeda embutida).
     */
    public List<Portfolio> listarPorUtilizador(int userId) {
        List<Portfolio> lista = new ArrayList<>();

        // Subconsulta que pega o PRECO MAIS RECENTE (timestamp_hora máximo) para cada moeda
        String sql = """
        SELECT
          p.id_portfolio,
          p.quantidade,
          m.id_moeda,
          m.nome,
          m.simbolo,
          pm.preco_em_eur AS valor_atual
        FROM Portfolio p
        JOIN Moeda m ON p.id_moeda = m.id_moeda
        LEFT JOIN (
          SELECT p2.id_moeda, p2.preco_em_eur
          FROM PrecoMoeda p2
          JOIN (
            SELECT id_moeda, MAX(timestamp_hora) AS max_hora
            FROM PrecoMoeda
            GROUP BY id_moeda
          ) ult ON p2.id_moeda = ult.id_moeda AND p2.timestamp_hora = ult.max_hora
        ) pm ON m.id_moeda = pm.id_moeda
        WHERE p.id_utilizador = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Portfolio p = new Portfolio();
                p.setIdPortfolio(rs.getInt("id_portfolio"));

                Utilizador u = new Utilizador();
                u.setIdUtilizador(userId);
                p.setUtilizador(u);

                Moeda m = new Moeda();
                m.setIdMoeda(rs.getInt("id_moeda"));
                m.setNome(rs.getString("nome"));
                m.setSimbolo(rs.getString("simbolo"));

                // Aqui atribuímos o valorAtual que veio na subconsulta:
                BigDecimal valorAtual = rs.getBigDecimal("valor_atual");
                if (valorAtual == null) {
                    // Caso não haja preço registrado ainda, coloca zero
                    m.setValorAtual(BigDecimal.ZERO);
                } else {
                    m.setValorAtual(valorAtual);
                }

                p.setMoeda(m);

                p.setQuantidade(rs.getBigDecimal("quantidade"));
                lista.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Incrementa (ou insere) quantidade de uma moeda para o utilizador.
     * Se não existir, insere nova linha; se existir, soma à quantidade atual.
     */
    public void incrementarQuantidade(int userId, int idMoeda, BigDecimal quantidade) {
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
     * Decrementa (bloqueia) quantidade de uma moeda no portfólio do utilizador.
     * Retorna true se conseguiu, false caso quantidade insuficiente.
     */
    public boolean decrementarQuantidade(int userId, int idMoeda, BigDecimal quantidade) {
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
            ResultSet rs = stCheck.executeQuery();
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * Retorna a quantidade atual de uma moeda específica no portfólio do utilizador.
     * Se não existir nenhuma linha, devolve BigDecimal.ZERO.
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
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("quantidade");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }
}
