package Repository;

import Database.DBConnection;
import model.Ordem;
import model.Utilizador;
import model.Moeda;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

/**
 * Repositório para CRUD de ordens e métodos de consulta de ordens.
 * Aproveita v_OrdemDetalhada, sp_InserirOrdem e sp_ExpirarOrdens24h.
 */
public class OrdemRepository {

    private final Connection connection;

    public OrdemRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Insere uma nova ordem via stored procedure sp_InserirOrdem e devolve o ID.
     */
    public Optional<Integer> inserirOrdem(Ordem ordem) throws SQLException {
        String call = "{ CALL dbo.sp_InserirOrdem(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }";
        try (CallableStatement cstmt = connection.prepareCall(call)) {
            cstmt.setInt(1, ordem.getUtilizador().getIdUtilizador());
            cstmt.setInt(2, ordem.getMoeda().getIdMoeda());
            cstmt.setInt(3, ordem.getIdTipoOrdem());
            cstmt.setInt(4, ordem.getIdStatus());
            cstmt.setInt(5, ordem.getIdModo());
            cstmt.setBigDecimal(6, ordem.getQuantidade());
            cstmt.setBigDecimal(7, ordem.getPrecoUnitarioEur());
            cstmt.setTimestamp(8, Timestamp.valueOf(ordem.getDataCriacao()));
            cstmt.setTimestamp(9, Timestamp.valueOf(ordem.getDataExpiracao()));
            cstmt.registerOutParameter(10, Types.INTEGER);
            cstmt.execute();
            int newId = cstmt.getInt(10);
            return newId > 0 ? Optional.of(newId) : Optional.empty();
        }
    }

    /**
     * Obtém id em OrdemTipo.
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
     * Obtém id em OrdemModo.
     */
    public int obterIdModo(String modo) throws SQLException {
        String sql = "SELECT id_modo FROM OrdemModo WHERE modo = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, modo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Modo de ordem não encontrado: " + modo);
    }

    /**
     * Obtém id em OrdemStatus.
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
     * Busca ordens pendentes do tipo e modo especificados usando view v_OrdemDetalhada.
     */
    public List<Ordem> obterOrdensPendentes(int idMoeda,
                                            String tipoContrario,
                                            String modoOrigem,
                                            BigDecimal precoLimite) throws SQLException {
        List<Ordem> ordens = new ArrayList<>();

        StringBuilder sb = new StringBuilder(
                "SELECT * FROM v_OrdemDetalhada " +
                        "WHERE id_moeda = ? " +
                        "AND tipo_ordem = ? " +
                        "AND status = 'ativa' " +
                        "AND data_expiracao > CURRENT_TIMESTAMP"
        );

        // se for LIMIT, aplicamos só o filtro de preço — nunca o filtro de modo!
        if ("limit".equalsIgnoreCase(modoOrigem)) {
            sb.append(" AND preco_unitario_eur ")
                    .append(tipoContrario.equalsIgnoreCase("venda") ? "<= ?" : ">= ?");
        }

        // ordenação: sempre pela melhor cotação, depois antiguidade
        sb.append(" ORDER BY ");
        if (tipoContrario.equalsIgnoreCase("venda")) {
            // estamos a comprar: vendas mais baratas primeiro
            sb.append(" preco_unitario_eur ASC, data_criacao ASC");
        } else {
            // estamos a vender: compras mais caras primeiro
            sb.append(" preco_unitario_eur DESC, data_criacao ASC");
        }

        try (PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
            stmt.setInt(1, idMoeda);
            stmt.setString(2, tipoContrario);
            if ("limit".equalsIgnoreCase(modoOrigem)) {
                stmt.setBigDecimal(3, precoLimite);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ordens.add(mapearOrdem(rs));
                }
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
     * Obtém uma ordem por ID via view v_OrdemDetalhada.
     */
    public Optional<Ordem> obterOrdemPorId(int id) throws SQLException {
        String sql = "SELECT * FROM v_OrdemDetalhada WHERE id_ordem = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapearOrdem(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Lista todas as ordens ativas e não expiradas de um utilizador.
     */
    public List<Ordem> listarOrdensPendentesPorUsuario(int idUtilizador) throws SQLException {
        List<Ordem> ordens = new ArrayList<>();
        String sql = "SELECT * FROM v_OrdemDetalhada WHERE id_utilizador = ? " +
                "AND status = 'ativa' AND data_expiracao > CURRENT_TIMESTAMP ORDER BY data_criacao DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idUtilizador);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ordens.add(mapearOrdem(rs));
            }
        }
        return ordens;
    }

    /**
     * Expira ordens ativas com mais de 24h sem match, chamando sp_ExpirarOrdens24h.
     */
    public void expirarOrdens() throws SQLException {
        try (CallableStatement cstmt = connection.prepareCall("{ CALL dbo.sp_ExpirarOrdens24h }")) {
            cstmt.execute();
        }
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
        m.setNome(rs.getString("nome_moeda")); // <--- essencial
        ordem.setMoeda(m);
        ordem.setIdTipoOrdem(  rs.getInt("id_tipo_ordem"));
        ordem.setTipoOrdem(    rs.getString("tipo_ordem"));
        ordem.setIdStatus(     rs.getInt("id_status"));
        ordem.setStatus(       rs.getString("status"));
        ordem.setIdModo(       rs.getInt("id_modo"));
        ordem.setModo(         rs.getString("modo"));

        ordem.setQuantidade(        rs.getBigDecimal("quantidade"));
        ordem.setPrecoUnitarioEur(  rs.getBigDecimal("preco_unitario_eur"));
        ordem.setDataCriacao(       rs.getTimestamp("data_criacao").toLocalDateTime());
        ordem.setDataExpiracao(     rs.getTimestamp("data_expiracao").toLocalDateTime());
        return ordem;
    }
}