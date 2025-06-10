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

    public TransacaoRepository() { }

    /**
     * Insere nova transação (compra ou venda).
     */
    public void inserirTransacao(int idUtilizador,
                                 int idMoeda,
                                 BigDecimal quantidade,
                                 BigDecimal precoUnitarioEur,
                                 int idTipoOrdem) throws SQLException {

        String sql = """
        INSERT INTO Transacao
          (id_utilizador, id_moeda, quantidade, preco_unitario_eur, data_hora, id_tipo_ordem)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUtilizador);
            stmt.setInt(2, idMoeda);
            stmt.setBigDecimal(3, quantidade.setScale(8, RoundingMode.HALF_UP));
            stmt.setBigDecimal(4, precoUnitarioEur.setScale(8, RoundingMode.HALF_UP));
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(6, idTipoOrdem);

            stmt.executeUpdate();
        }
    }

    /**
     * Lista todas as transações de um utilizador, ordenadas por data decrescente.
     */
    public List<Transacao> listarPorUsuario(int idUtilizador) {
        List<Transacao> lista = new ArrayList<>();

        String sql = """
            SELECT t.id_transacao,
                   t.id_utilizador,
                   t.id_moeda,
                   t.quantidade,
                   t.preco_unitario_eur,
                   t.data_hora,
                   m.nome,
                   m.simbolo,
                   m.id_tipo       AS idTipoMoeda,
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

                    // monta o Utilizador se quiseres guardar:
                    // Utilizador u = new Utilizador();
                    // u.setIdUtilizador(rs.getInt("id_utilizador"));
                    // tx.setUtilizador(u);

                    // monta a Moeda
                    Moeda m = new Moeda();
                    m.setIdMoeda(rs.getInt("id_moeda"));
                    m.setNome(rs.getString("nome"));
                    m.setSimbolo(rs.getString("simbolo"));
                    m.setIdTipo(rs.getInt("idTipoMoeda"));
                    m.setValorAtual(rs.getBigDecimal("valor_atual"));
                    tx.setMoeda(m);

                    BigDecimal qty   = rs.getBigDecimal("quantidade");
                    BigDecimal price = rs.getBigDecimal("preco_unitario_eur");
                    tx.setQuantidade(qty);
                    tx.setPrecoUnitarioEur(price);
                    tx.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());

                    // calcula total em EUR
                    BigDecimal total = qty.multiply(price).setScale(8, RoundingMode.HALF_UP);
                    tx.setTotalEur(total);

                    lista.add(tx);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }
}
