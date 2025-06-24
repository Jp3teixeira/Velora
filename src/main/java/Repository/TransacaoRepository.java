// src/Repository/TransacaoRepository.java
package Repository;

import Database.DataAccessException;
import model.Moeda;
import model.Transacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransacaoRepository implements DAO<Transacao, Integer> {

    public TransacaoRepository() {}

    @Override
    public Optional<Transacao> get(Integer id) {
        String sql = """
            SELECT id_transacao, id_utilizador, id_moeda,
                   nome, simbolo, idTipoMoeda, tipo,
                   quantidade, preco_unitario_eur,
                   data_hora, valor_atual
              FROM v_TransacaoDetalhada
             WHERE id_transacao = ?
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
            throw new DataAccessException("Erro ao buscar Transacao ID " + id, e);
        }
    }

    @Override
    public List<Transacao> getAll() {
        String sql = """
            SELECT id_transacao, id_utilizador, id_moeda,
                   nome, simbolo, idTipoMoeda, tipo,
                   quantidade, preco_unitario_eur,
                   data_hora, valor_atual
              FROM v_TransacaoDetalhada
          ORDER BY data_hora DESC
        """;
        List<Transacao> lista = new ArrayList<>();
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao listar todas as Transacoes", e);
        }
    }

    @Override
    public boolean save(Transacao t) {
        try {
            Optional<Integer> novoId = inserirTransacao(
                    t.getUtilizador().getId(),
                    t.getMoeda().getId(),
                    t.getQuantidade(),
                    t.getPrecoUnitarioEur(),
                    getIdTipoOrdem(t.getTipo())
            );
            if (novoId.isPresent()) {
                t.setId(novoId.get());
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao inserir Transacao", e);
        }
    }

    @Override
    public boolean update(Transacao t) {
        throw new UnsupportedOperationException("Atualização de Transacao não suportada");
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Transacao WHERE id_transacao = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao eliminar Transacao ID " + id, e);
        }
    }

    /**
     * Lista todas as transações de um determinado utilizador.
     */
    public List<Transacao> listarPorUsuario(int idUtilizador) {
        String sql = """
            SELECT id_transacao, id_utilizador, id_moeda,
                   nome, simbolo, idTipoMoeda, tipo,
                   quantidade, preco_unitario_eur,
                   data_hora, valor_atual
              FROM v_TransacaoDetalhada
             WHERE id_utilizador = ?
          ORDER BY data_hora DESC
        """;
        List<Transacao> lista = new ArrayList<>();
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilizador);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapRow(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao listar Transacoes do utilizador ID " + idUtilizador, e);
        }
    }

    // --- Métodos auxiliares ---

    private Transacao mapRow(ResultSet rs) throws SQLException {
        Transacao tx = new Transacao();
        tx.setId(rs.getInt("id_transacao"));

        Moeda m = new Moeda();
        m.setId(rs.getInt("id_moeda"));
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

        tx.setTipo(rs.getString("tipo"));
        return tx;
    }

    private Optional<Integer> inserirTransacao(int idUtilizador,
                                               int idMoeda,
                                               BigDecimal quantidade,
                                               BigDecimal precoUnitarioEur,
                                               int idTipoOrdem) throws SQLException {
        String call = "{ CALL sp_InserirTransacao(?, ?, ?, ?, ?, ?) }";
        try (Connection conn = Database.DBConnection.getConnection();
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

    private int getIdTipoOrdem(String tipo) throws SQLException {
        String sql = "SELECT id_tipo_ordem FROM OrdemTipo WHERE tipo_ordem = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Tipo de ordem não encontrado: " + tipo);
    }
}
