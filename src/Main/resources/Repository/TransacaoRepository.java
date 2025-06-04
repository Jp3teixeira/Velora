package Repository;

import Database.DBConnection;
import model.Moeda;
import model.Transacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransacaoRepository {

    public TransacaoRepository() {

    }

    /**
     * Insere nova transação (compra/venda).
     */
    public void inserirTransacao(int idUtilizador,
                                 int idMoeda,
                                 String tipo,
                                 BigDecimal quantidade,
                                 BigDecimal precoUnitarioEur) throws SQLException {
        BigDecimal totalEur = quantidade.multiply(precoUnitarioEur).setScale(8, RoundingMode.HALF_UP);
        String sql = """
            INSERT INTO Transacao
              (id_utilizador, id_moeda, tipo, quantidade, preco_unitario_eur, total_eur, data_hora)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUtilizador);
            stmt.setInt(2, idMoeda);
            stmt.setString(3, tipo);
            stmt.setBigDecimal(4, quantidade.setScale(8, RoundingMode.HALF_UP));
            stmt.setBigDecimal(5, precoUnitarioEur.setScale(8, RoundingMode.HALF_UP));
            stmt.setBigDecimal(6, totalEur);
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        }
    }

    /**
     * Lista todas as transações de um usuário, ordenadas por data decrescente.
     */
    public List<Transacao> listarPorUsuario(int idUtilizador) {
        List<Transacao> lista = new ArrayList<>();
        String sql = """
            SELECT t.id_transacao,
                   t.id_utilizador,
                   t.id_moeda,
                   t.tipo,
                   t.quantidade,
                   t.preco_unitario_eur,
                   t.total_eur,
                   t.data_hora,
                   m.nome,
                   m.simbolo,
                   m.tipo AS tipo_moeda,
                   pm.preco_em_eur AS valor_atual
              FROM Transacao t
              JOIN Moeda m ON t.id_moeda = m.id_moeda
              LEFT JOIN (
                   SELECT p2.id_moeda, p2.preco_em_eur
                     FROM PrecoMoeda p2
                    WHERE p2.timestamp_hora = (
                        SELECT MAX(x.timestamp_hora)
                          FROM PrecoMoeda x
                         WHERE x.id_moeda = p2.id_moeda
                    )
              ) pm ON pm.id_moeda = m.id_moeda
             WHERE t.id_utilizador = ?
             ORDER BY t.data_hora DESC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUtilizador);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transacao tx = new Transacao();
                    tx.setIdTransacao(rs.getInt("id_transacao"));

                    // Preenche a Moeda associada à transação
                    Moeda m = new Moeda();
                    m.setIdMoeda(rs.getInt("id_moeda"));
                    m.setNome(rs.getString("nome"));
                    m.setSimbolo(rs.getString("simbolo"));
                    m.setTipo(rs.getString("tipo_moeda"));
                    m.setValorAtual(rs.getBigDecimal("valor_atual")); // valor atual só para referência
                    tx.setMoeda(m);

                    tx.setTipo(rs.getString("tipo"));
                    tx.setQuantidade(rs.getBigDecimal("quantidade"));
                    tx.setPrecoUnitarioEur(rs.getBigDecimal("preco_unitario_eur"));
                    tx.setTotalEur(rs.getBigDecimal("total_eur"));
                    tx.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());

                    lista.add(tx);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}
