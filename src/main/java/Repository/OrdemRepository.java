package Repository;

import Database.DBConnection;
import model.Ordem;
import model.Utilizador;
import model.Moeda;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Repositório para CRUD de ordens e métodos de consulta de ordens pendentes e ordens abertas por usuário.
 */
public class OrdemRepository {

    private final Connection connection;

    public OrdemRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Insere uma nova ordem e retorna o ID gerado.
     */
    public void inserirOrdem(Ordem ordem) throws SQLException {
        String sql = """
            INSERT INTO Ordem
              (id_utilizador, id_moeda, id_tipo_ordem, id_status, id_modo,
               quantidade, preco_unitario_eur, data_criacao, data_expiracao)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, ordem.getUtilizador().getIdUtilizador());
            stmt.setInt(2, ordem.getMoeda().getIdMoeda());
            stmt.setInt(3, ordem.getIdTipoOrdem());
            stmt.setInt(4, ordem.getIdStatus());
            stmt.setInt(5, ordem.getIdModo());
            stmt.setBigDecimal(6, ordem.getQuantidade());
            stmt.setBigDecimal(7, ordem.getPrecoUnitarioEur());
            stmt.setTimestamp(8, Timestamp.valueOf(ordem.getDataCriacao()));
            stmt.setTimestamp(9, Timestamp.valueOf(ordem.getDataExpiracao()));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) ordem.setIdOrdem(rs.getInt(1));
            }
        }
    }

    /**
     * Busca ordens pendentes (status = 'ativa') do tipo contrario e modo especificado.
     */
    public List<Ordem> obterOrdensPendentes(int idMoeda,
                                            String tipoContrario,
                                            String modoOrigem,
                                            BigDecimal precoLimite) throws SQLException {
        List<Ordem> ordens = new ArrayList<>();
        String sql;
        boolean isVenda = "venda".equalsIgnoreCase(tipoContrario);

        if (isVenda) {
            if ("limit".equalsIgnoreCase(modoOrigem)) {
                sql = """
                    SELECT t.*, ot.tipo_ordem, os.status, om.modo
                      FROM Ordem t
                      JOIN OrdemTipo ot   ON t.id_tipo_ordem = ot.id_tipo_ordem
                      JOIN OrdemStatus os ON t.id_status     = os.id_status
                      JOIN OrdemModo om   ON t.id_modo       = om.id_modo
                     WHERE t.id_moeda = ?
                       AND ot.tipo_ordem = 'venda'
                       AND om.modo = 'limit'
                       AND os.status = 'ativa'
                       AND t.preco_unitario_eur <= ?
                     ORDER BY t.preco_unitario_eur ASC, t.data_criacao ASC
                    """;
            } else {
                sql = """
                    SELECT t.*, ot.tipo_ordem, os.status, om.modo
                      FROM Ordem t
                      JOIN OrdemTipo ot   ON t.id_tipo_ordem = ot.id_tipo_ordem
                      JOIN OrdemStatus os ON t.id_status     = os.id_status
                      JOIN OrdemModo om   ON t.id_modo       = om.id_modo
                     WHERE t.id_moeda = ?
                       AND ot.tipo_ordem = 'venda'
                       AND os.status = 'ativa'
                     ORDER BY t.data_criacao ASC
                    """;
            }
        } else {
            if ("limit".equalsIgnoreCase(modoOrigem)) {
                sql = """
                    SELECT t.*, ot.tipo_ordem, os.status, om.modo
                      FROM Ordem t
                      JOIN OrdemTipo ot   ON t.id_tipo_ordem = ot.id_tipo_ordem
                      JOIN OrdemStatus os ON t.id_status     = os.id_status
                      JOIN OrdemModo om   ON t.id_modo       = om.id_modo
                     WHERE t.id_moeda = ?
                       AND ot.tipo_ordem = 'compra'
                       AND om.modo = 'limit'
                       AND os.status = 'ativa'
                       AND t.preco_unitario_eur >= ?
                     ORDER BY t.preco_unitario_eur DESC, t.data_criacao ASC
                    """;
            } else {
                sql = """
                    SELECT t.*, ot.tipo_ordem, os.status, om.modo
                      FROM Ordem t
                      JOIN OrdemTipo ot   ON t.id_tipo_ordem = ot.id_tipo_ordem
                      JOIN OrdemStatus os ON t.id_status     = os.id_status
                      JOIN OrdemModo om   ON t.id_modo       = om.id_modo
                     WHERE t.id_moeda = ?
                       AND ot.tipo_ordem = 'compra'
                       AND os.status = 'ativa'
                     ORDER BY t.data_criacao ASC
                    """;
            }
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idMoeda);
            if ("limit".equalsIgnoreCase(modoOrigem)) stmt.setBigDecimal(2, precoLimite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ordens.add(mapearOrdem(rs));
            }
        }
        return ordens;
    }

    /**
     * Atualiza quantidade e status de uma ordem existente.
     */
    public void atualizarOrdem(Ordem ordem) throws SQLException {
        String sql = "UPDATE Ordem SET quantidade = ?, id_status = ? WHERE id_ordem = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, ordem.getQuantidade());
            stmt.setInt(2, ordem.getIdStatus());
            stmt.setInt(3, ordem.getIdOrdem());
            stmt.executeUpdate();
        }
    }

    /**
     * Obtém uma ordem por ID.
     */
    public Ordem obterOrdemPorId(int id) throws SQLException {
        String sql = """
            SELECT t.*, ot.tipo_ordem, os.status, om.modo
              FROM Ordem t
              JOIN OrdemTipo ot   ON t.id_tipo_ordem = ot.id_tipo_ordem
              JOIN OrdemStatus os ON t.id_status     = os.id_status
              JOIN OrdemModo om   ON t.id_modo       = om.id_modo
             WHERE t.id_ordem = ?
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapearOrdem(rs);
            }
        }
        return null;
    }

    /**
     * Lista ordens abertas (ativa & limit) de um usuário.
     */
    public List<Ordem> listarOrdensAbertasPorUsuario(int idUtilizador) throws SQLException {
        List<Ordem> ordens = new ArrayList<>();
        String sql = """
            SELECT t.*, ot.tipo_ordem, os.status, om.modo
              FROM Ordem t
              JOIN OrdemTipo ot   ON t.id_tipo_ordem = ot.id_tipo_ordem
              JOIN OrdemStatus os ON t.id_status     = os.id_status
              JOIN OrdemModo om   ON t.id_modo       = om.id_modo
             WHERE t.id_utilizador = ?
               AND os.status = 'ativa'
               AND om.modo   = 'limit'
             ORDER BY t.data_criacao DESC
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idUtilizador);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ordens.add(mapearOrdem(rs));
            }
        }
        return ordens;
    }

    /**
     * Busca id em OrdemTipo.
     */
    public int obterIdTipoOrdem(String tipo) throws SQLException {
        String sql = "SELECT id_tipo_ordem FROM OrdemTipo WHERE tipo_ordem = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Tipo de ordem não encontrado: " + tipo);
    }

    /**
     * Busca id em OrdemModo.
     */
    public int obterIdModo(String modo) throws SQLException {
        String sql = "SELECT id_modo FROM OrdemModo WHERE modo = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, modo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Modo (limit/market) de ordem não encontrado: " + modo);
    }

    /**
     * Busca id em OrdemStatus.
     */
    public int obterIdStatus(String status) throws SQLException {
        String sql = "SELECT id_status FROM OrdemStatus WHERE status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Status de ordem não encontrado: " + status);
    }

    /**
     * Constrói objeto Ordem a partir do ResultSet.
     */
    private Ordem mapearOrdem(ResultSet rs) throws SQLException {
        Ordem ordem = new Ordem();
        ordem.setIdOrdem(rs.getInt("id_ordem"));

        Utilizador u = new Utilizador();
        u.setIdUtilizador(rs.getInt("id_utilizador"));
        ordem.setUtilizador(u);

        Moeda m = new Moeda();
        m.setIdMoeda(rs.getInt("id_moeda"));
        ordem.setMoeda(m);

        ordem.setIdTipoOrdem( rs.getInt("id_tipo_ordem"));
        ordem.setTipoOrdem(   rs.getString("tipo_ordem"));

        ordem.setIdStatus(    rs.getInt("id_status"));
        ordem.setStatus(      rs.getString("status"));

        ordem.setIdModo(      rs.getInt("id_modo"));
        ordem.setModo(        rs.getString("modo"));

        ordem.setQuantidade(       rs.getBigDecimal("quantidade"));
        ordem.setPrecoUnitarioEur( rs.getBigDecimal("preco_unitario_eur"));
        ordem.setDataCriacao(      rs.getTimestamp("data_criacao").toLocalDateTime());
        ordem.setDataExpiracao(    rs.getTimestamp("data_expiracao").toLocalDateTime());

        return ordem;
    }
}
