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
import java.util.Optional;

public class TransacaoRepository {

    public TransacaoRepository() { }

    /**
     * Insere nova transação via stored procedure sp_InserirTransacao.
     * Retorna Optional com novo id_transacao ou Optional.empty() se falhar.
     */
    public Optional<Integer> inserirTransacao(int idUtilizador,
                                              int idMoeda,
                                              BigDecimal quantidade,
                                              BigDecimal precoUnitarioEur,
                                              int idTipoOrdem) throws SQLException {

        String call = "{ CALL sp_InserirTransacao(?, ?, ?, ?, ?, ?) }";
        try (Connection conn = DBConnection.getConnection();
             CallableStatement cstmt = conn.prepareCall(call)) {

            cstmt.setInt(1, idUtilizador);
            cstmt.setInt(2, idMoeda);
            cstmt.setBigDecimal(3, quantidade.setScale(8, RoundingMode.HALF_UP));
            cstmt.setBigDecimal(4, precoUnitarioEur.setScale(8, RoundingMode.HALF_UP));
            cstmt.setInt(5, idTipoOrdem);
            cstmt.registerOutParameter(6, Types.INTEGER);
            cstmt.execute();

            int newId = cstmt.getInt(6);
            return newId > 0 ? Optional.of(newId) : Optional.empty();
        }
    }

    /**
     * Lista todas as transações de um utilizador usando view v_TransacaoDetalhada.
     */
    public List<Transacao> listarPorUsuario(int idUtilizador) {
        List<Transacao> lista = new ArrayList<>();

        String sql = "SELECT id_transacao, id_utilizador, id_moeda, tipo, nome, simbolo, idTipoMoeda, " +
                "quantidade, preco_unitario_eur, data_hora, valor_atual " +
                "FROM v_TransacaoDetalhada " +
                "WHERE id_utilizador = ? " +
                "ORDER BY data_hora DESC";


        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUtilizador);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transacao tx = new Transacao();
                    tx.setIdTransacao(rs.getInt("id_transacao"));

                    Moeda m = new Moeda();
                    m.setIdMoeda(rs.getInt("id_moeda"));
                    m.setNome(rs.getString("nome"));
                    m.setSimbolo(rs.getString("simbolo"));
                    m.setIdTipo(rs.getInt("idTipoMoeda"));
                    m.setValorAtual(rs.getBigDecimal("valor_atual"));
                    tx.setMoeda(m);

                    tx.setQuantidade(rs.getBigDecimal("quantidade"));
                    tx.setPrecoUnitarioEur(rs.getBigDecimal("preco_unitario_eur"));
                    tx.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());

                    BigDecimal total = tx.getQuantidade()
                            .multiply(tx.getPrecoUnitarioEur())
                            .setScale(8, RoundingMode.HALF_UP);
                    tx.setTotalEur(total);

                    lista.add(tx);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Obtém o preço atual de uma moeda via função fn_GetLatestPrecoMoeda.
     */
    public BigDecimal getLatestPrecoMoeda(int idMoeda) {
        String sql = "SELECT dbo.fn_GetLatestPrecoMoeda(?) AS preco";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMoeda);
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
}
