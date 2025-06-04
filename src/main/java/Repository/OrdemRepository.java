package Repository;

import model.Ordem;
import model.Utilizador;
import model.Moeda;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Repositório para CRUD de ordens e método de obtenção de ordens pendentes.
 */
public class OrdemRepository {

    private final Connection connection;

    public OrdemRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Insere uma nova ordem na tabela 'Ordem' e devolve o ID gerado em ordem.setIdOrdem(...)
     */
    public void inserirOrdem(Ordem ordem) throws SQLException {
        String sql = """
            INSERT INTO Ordem 
              (id_utilizador, id_moeda, tipo, quantidade, preco_unitario_eur, data_criacao, data_expiracao, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, ordem.getUtilizador().getIdUtilizador());
        stmt.setInt(2, ordem.getMoeda().getIdMoeda());
        stmt.setString(3, ordem.getTipo());                              // "compra" ou "venda"
        stmt.setBigDecimal(4, ordem.getQuantidade());                      // DECIMAL(32,16)
        stmt.setBigDecimal(5, ordem.getPrecoUnitarioEur());                // DECIMAL(18,8)
        stmt.setTimestamp(6, Timestamp.valueOf(ordem.getDataCriacao()));   // DATETIME
        stmt.setTimestamp(7, Timestamp.valueOf(ordem.getDataExpiracao())); // DATETIME
        stmt.setString(8, ordem.getStatus());                              // "ativa", "executada" ou "expirada"

        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            ordem.setIdOrdem(rs.getInt(1));
        }
        rs.close();
        stmt.close();
    }

    /**
     * Obter ordens pendentes (status = 'ativa', criadas nas últimas 24h) da moeda oposta
     * que atendam ao critério de preço (≤ precoAlvo para vendas; ≥ precoAlvo para compras),
     * ordenadas por prioridade de preço e, em caso de empate, por data de criação.
     */
    public List<Ordem> obterOrdensPendentes(int idMoeda, String tipoContrario) throws SQLException {
        List<Ordem> ordens = new ArrayList<>();

        String sql;
        if ("venda".equalsIgnoreCase(tipoContrario)) {
            // Queremos todas as ordens de VENDA ativas (não só as ≤ preço), ordenadas por data de criação
            sql = """
            SELECT *
              FROM Ordem
             WHERE id_moeda = ?
               AND tipo = 'venda'
               AND status = 'ativa'
               -- removei o filtro de preco_unitario_eur
             ORDER BY data_criacao ASC
            """;
        } else {
            // Todas as ordens de COMPRA ativas, FIFO
            sql = """
            SELECT *
              FROM Ordem
             WHERE id_moeda = ?
               AND tipo = 'compra'
               AND status = 'ativa'
             ORDER BY data_criacao ASC
            """;
        }

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, idMoeda);

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            Ordem ordem = new Ordem();
            // preencher campos de ordem (id_ordem, id_utilizador, id_moeda, tipo, quantidade, preco_unitario_eur, data_criacao, data_expiracao, status)
            ordem.setIdOrdem(rs.getInt("id_ordem"));
            Utilizador u = new Utilizador();
            u.setIdUtilizador(rs.getInt("id_utilizador"));
            ordem.setUtilizador(u);
            Moeda m = new Moeda();
            m.setIdMoeda(rs.getInt("id_moeda"));
            ordem.setMoeda(m);
            ordem.setTipo(rs.getString("tipo"));
            ordem.setQuantidade(rs.getBigDecimal("quantidade"));
            ordem.setPrecoUnitarioEur(rs.getBigDecimal("preco_unitario_eur"));
            ordem.setDataCriacao(rs.getTimestamp("data_criacao").toLocalDateTime());
            ordem.setDataExpiracao(rs.getTimestamp("data_expiracao").toLocalDateTime());
            ordem.setStatus(rs.getString("status"));

            ordens.add(ordem);
        }
        rs.close();
        stmt.close();
        return ordens;
    }


    /**
     * Atualiza apenas a quantidade remanescente e o status de uma ordem já existente.
     */
    public void atualizarOrdem(Ordem ordem) throws SQLException {
        String sql = "UPDATE Ordem SET quantidade = ?, status = ? WHERE id_ordem = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setBigDecimal(1, ordem.getQuantidade());
        stmt.setString(2, ordem.getStatus());
        stmt.setInt(3, ordem.getIdOrdem());
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Retorna uma ordem pelo seu ID (pode ser útil em outros fluxos).
     */
    public Ordem obterOrdemPorId(int id) throws SQLException {
        String sql = "SELECT * FROM Ordem WHERE id_ordem = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, id);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            Ordem ordem = new Ordem();
            ordem.setIdOrdem(rs.getInt("id_ordem"));

            Utilizador u = new Utilizador();
            u.setIdUtilizador(rs.getInt("id_utilizador"));
            ordem.setUtilizador(u);

            Moeda m = new Moeda();
            m.setIdMoeda(rs.getInt("id_moeda"));
            ordem.setMoeda(m);

            ordem.setTipo(rs.getString("tipo"));
            ordem.setQuantidade(rs.getBigDecimal("quantidade"));
            ordem.setPrecoUnitarioEur(rs.getBigDecimal("preco_unitario_eur"));
            ordem.setDataCriacao(rs.getTimestamp("data_criacao").toLocalDateTime());
            ordem.setDataExpiracao(rs.getTimestamp("data_expiracao").toLocalDateTime());
            ordem.setStatus(rs.getString("status"));
            rs.close();
            stmt.close();
            return ordem;
        }
        rs.close();
        stmt.close();
        return null;
    }
}
