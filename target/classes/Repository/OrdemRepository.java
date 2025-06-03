package Repository;

import model.Ordem;
import model.Utilizador;
import model.Moeda;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrdemRepository {

    private final Connection connection;

    public OrdemRepository(Connection connection) {
        this.connection = connection;
    }


    public void inserirOrdem(Ordem ordem) throws SQLException {
        String sql = """
            INSERT INTO Ordem 
                (id_utilizador, id_moeda, tipo, quantidade, preco_unitario_eur, data_criacao, data_expiracao, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, ordem.getUtilizador().getIdUtilizador());
        stmt.setInt(2, ordem.getMoeda().getIdMoeda());
        stmt.setString(3, ordem.getTipo());                            // "compra" ou "venda"
        stmt.setBigDecimal(4, ordem.getQuantidade());                    // DECIMAL(32,16)
        stmt.setBigDecimal(5, ordem.getPrecoUnitarioEur());              // DECIMAL(18,8)
        stmt.setTimestamp(6, Timestamp.valueOf(ordem.getDataCriacao())); // DATETIME
        stmt.setTimestamp(7, Timestamp.valueOf(ordem.getDataExpiracao()));
        stmt.setString(8, ordem.getStatus());                            // "ativa", "executada" ou "expirada"

        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            ordem.setIdOrdem(rs.getInt(1));
        }
        rs.close();
        stmt.close();
    }

    // Obter ordens pendentes (status = 'ativa') da moeda oposta, criadas nas últimas 24 horas
    public List<Ordem> obterOrdensPendentes(int idMoeda, String tipoContrario) throws SQLException {
        List<Ordem> ordens = new ArrayList<>();
        String sql = """
            SELECT * 
              FROM Ordem
             WHERE id_moeda = ?
               AND tipo = ?
               AND status = 'ativa'
               AND data_criacao >= DATEADD(hour, -24, GETDATE())
             ORDER BY data_criacao ASC
            """;

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, idMoeda);
        stmt.setString(2, tipoContrario);

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

    // Atualizar ordem (quantidade e/ou status)
    public void atualizarOrdem(Ordem ordem) throws SQLException {
        String sql = "UPDATE Ordem SET quantidade = ?, status = ? WHERE id_ordem = ?";

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setBigDecimal(1, ordem.getQuantidade());
        stmt.setString(2, ordem.getStatus());
        stmt.setInt(3, ordem.getIdOrdem());

        stmt.executeUpdate();
        stmt.close();
    }

    // Obter ordem por ID
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
