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

/**
 * Repositório para ler/escrever na tabela Portfolio.
 */
public class PortfolioRepository {

    /**
     * Retorna todos os itens do portfólio do utilizador (com Moeda embutida e preço médio).
     */
    public List<Portfolio> listarPorUtilizador(int userId) {
        List<Portfolio> lista = new ArrayList<>();

        // Subconsulta que vai a procura do PRECO MAIS RECENTE (timestamp_hora máximo) para cada moeda
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

                // Valor atual (último preço registrado):
                BigDecimal valorAtual = rs.getBigDecimal("valor_atual");
                if (valorAtual == null) {
                    m.setValorAtual(BigDecimal.ZERO);
                } else {
                    m.setValorAtual(valorAtual);
                }
                p.setMoeda(m);

                // Quantidade que o usuário possui:
                BigDecimal quantidade = rs.getBigDecimal("quantidade");
                p.setQuantidade(quantidade);

                // Agora, calcula o preço médio de compra dessa moeda para este usuário:
                BigDecimal precoMedio = this.calcularPrecoMedioCompra(userId, m.getIdMoeda());
                p.setPrecoMedioCompra(precoMedio);

                lista.add(p);
            }
            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Incrementa (ou insere) quantidade de uma moeda para o utilizador.
     * Se não existir, insere nova linha; se existir, soma à quantidade atual.
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
     * Decrementa (bloqueia) quantidade de uma moeda no portfólio do utilizador.
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
            rs.close();
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
                BigDecimal q = rs.getBigDecimal("quantidade");
                rs.close();
                return q;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }
    /**
     * Retorna o preço médio de compra de uma certa moeda para um utilizador.
     */
    public BigDecimal calcularPrecoMedioCompra(int userId, int idMoeda) {
        String sql = """
        SELECT 
            SUM(t.quantidade * t.preco_unitario_eur) AS soma_total_eur,
            SUM(t.quantidade)                      AS soma_quantidade
        FROM Transacao t
        JOIN OrdemTipo ot
          ON t.id_tipo_ordem = ot.id_tipo_ordem
        WHERE t.id_utilizador = ?
          AND t.id_moeda      = ?
          AND ot.tipo_ordem   = 'compra'
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, idMoeda);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal somaTotal = rs.getBigDecimal("soma_total_eur");
                BigDecimal somaQtd   = rs.getBigDecimal("soma_quantidade");
                if (somaTotal != null && somaQtd != null && somaQtd.compareTo(BigDecimal.ZERO) > 0) {
                    return somaTotal.divide(somaQtd, 8, RoundingMode.HALF_UP);
                }
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }
}
