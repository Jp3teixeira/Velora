package Repository;

import model.Ordem;
import model.Utilizador;
import model.Moeda;

import java.sql.*;
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
     * Insere uma nova ordem na tabela 'Ordem' e preenche o ID gerado em ordem.setIdOrdem(...)
     */
    public void inserirOrdem(Ordem ordem) throws SQLException {
        String sql = """
            INSERT INTO Ordem 
              (id_utilizador, id_moeda, tipo, modo, quantidade, preco_unitario_eur, data_criacao, data_expiracao, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, ordem.getUtilizador().getIdUtilizador());
        stmt.setInt(2, ordem.getMoeda().getIdMoeda());
        stmt.setString(3, ordem.getTipo());                              // "compra" ou "venda"
        stmt.setString(4, ordem.getModo());                               // "market" ou "limit"
        stmt.setBigDecimal(5, ordem.getQuantidade());                      // DECIMAL(32,16)
        stmt.setBigDecimal(6, ordem.getPrecoUnitarioEur());                // DECIMAL(18,8)
        stmt.setTimestamp(7, Timestamp.valueOf(ordem.getDataCriacao()));   // DATETIME
        stmt.setTimestamp(8, Timestamp.valueOf(ordem.getDataExpiracao())); // DATETIME
        stmt.setString(9, ordem.getStatus());                              // "ativa", "executada" ou "expirada"

        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            ordem.setIdOrdem(rs.getInt(1));
        }
        rs.close();
        stmt.close();
    }

    /**
     * Obter ordens pendentes (status = 'ativa', dentro do critério de preço para limit orders).
     *
     * @param idMoeda       ID da moeda em questão
     * @param tipoContrario "venda" se estou buscando ordens de venda (para um buy), ou "compra" se buscando ordens de compra (para um sell)
     * @param modoOrigem    modo da ordem que está pedindo este book ("market" ou "limit")
     * @param precoLimite   preço limite declarado pela ordem (usado apenas se modoOrigem="limit"); se modoOrigem="market", pode ser null
     * @return lista de Ordens ativas que satisfazem modo+preço, ordenadas por prioridade (preço e FIFO)
     */
    public List<Ordem> obterOrdensPendentes(int idMoeda,
                                            String tipoContrario,
                                            String modoOrigem,
                                            BigDecimal precoLimite) throws SQLException {
        List<Ordem> ordens = new ArrayList<>();

        String sql;
        if ("venda".equalsIgnoreCase(tipoContrario)) {
            // Buscando ordens de venda ativas
            if ("limit".equalsIgnoreCase(modoOrigem)) {
                // Compra limit: só ordens de venda cujo preço <= precoLimite
                sql = """
                    SELECT *
                      FROM Ordem
                     WHERE id_moeda = ?
                       AND tipo = 'venda'
                       AND modo = 'limit'
                       AND status = 'ativa'
                       AND preco_unitario_eur <= ?
                     ORDER BY preco_unitario_eur ASC, data_criacao ASC
                    """;
            } else {
                // Compra market: aceita qualquer venda ativa (market ou limit), FIFO por data
                sql = """
                    SELECT *
                      FROM Ordem
                     WHERE id_moeda = ?
                       AND tipo = 'venda'
                       AND status = 'ativa'
                     ORDER BY data_criacao ASC
                    """;
            }
        } else {
            // Buscando ordens de compra ativas
            if ("limit".equalsIgnoreCase(modoOrigem)) {
                // Venda limit: só ordens de compra cujo preço >= precoLimite
                sql = """
                    SELECT *
                      FROM Ordem
                     WHERE id_moeda = ?
                       AND tipo = 'compra'
                       AND modo = 'limit'
                       AND status = 'ativa'
                       AND preco_unitario_eur >= ?
                     ORDER BY preco_unitario_eur DESC, data_criacao ASC
                    """;
            } else {
                // Venda market: aceita qualquer compra ativa, FIFO por data
                sql = """
                    SELECT *
                      FROM Ordem
                     WHERE id_moeda = ?
                       AND tipo = 'compra'
                       AND status = 'ativa'
                     ORDER BY data_criacao ASC
                    """;
            }
        }

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, idMoeda);
        if ("limit".equalsIgnoreCase(modoOrigem)) {
            stmt.setBigDecimal(2, precoLimite);
        }

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            Ordem ordem = new Ordem();
            ordem.setIdOrdem(rs.getInt("id_ordem"));

            Utilizador u = new Utilizador();
            u.setIdUtilizador(rs.getInt("id_utilizador"));
            ordem.setUtilizador(u);

            Moeda m = new Moeda();
            m.setIdMoeda(rs.getInt("id_moeda"));
            ordem.setMoeda(m);

            ordem.setTipo(rs.getString("tipo"));
            ordem.setModo(rs.getString("modo"));
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
            ordem.setModo(rs.getString("modo"));
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

    /**
     * Lista todas as ordens com status 'ativa' e modo 'limit' de um usuário,
     * ordenadas por data de criação decrescente.
     */
    public List<Ordem> listarOrdensAbertasPorUsuario(int idUtilizador) throws SQLException {
        List<Ordem> ordens = new ArrayList<>();
        String sql = """
            SELECT *
              FROM Ordem
             WHERE id_utilizador = ?
               AND status = 'ativa'
               AND modo = 'limit'
             ORDER BY data_criacao DESC
            """;

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, idUtilizador);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            Ordem o = new Ordem();
            o.setIdOrdem(rs.getInt("id_ordem"));

            Utilizador u = new Utilizador();
            u.setIdUtilizador(rs.getInt("id_utilizador"));
            o.setUtilizador(u);

            Moeda m = new Moeda();
            m.setIdMoeda(rs.getInt("id_moeda"));
            o.setMoeda(m);

            o.setTipo(rs.getString("tipo"));
            o.setModo(rs.getString("modo"));
            o.setQuantidade(rs.getBigDecimal("quantidade"));
            o.setPrecoUnitarioEur(rs.getBigDecimal("preco_unitario_eur"));
            o.setDataCriacao(rs.getTimestamp("data_criacao").toLocalDateTime());
            o.setDataExpiracao(rs.getTimestamp("data_expiracao").toLocalDateTime());
            o.setStatus(rs.getString("status"));

            ordens.add(o);
        }
        rs.close();
        stmt.close();
        return ordens;
    }
}
