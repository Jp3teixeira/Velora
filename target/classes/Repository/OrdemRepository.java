package Repository;

import model.Ordem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrdemRepository {

    private final Connection connection;

    public OrdemRepository(Connection connection) {
        this.connection = connection;
    }

    // Inserir nova ordem
    public void inserirOrdem(Ordem ordem) throws SQLException {
        String sql = "INSERT INTO ordem (id_user, id_moeda, tipo, quantidade_total, quantidade_restante, preco, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, ordem.getId_carteira());
        stmt.setInt(2, ordem.getId_moeda());
        stmt.setString(3, ordem.getTipo());
        stmt.setDouble(4, ordem.getQuantidade_total());
        stmt.setDouble(5, ordem.getQuantidade_restante());
        stmt.setDouble(6, ordem.getPreco_no_momento());
        stmt.setString(7, ordem.getStatus());

        stmt.executeUpdate();

        // Obter o ID gerado e definir no objeto
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            ordem.setId_ordem(rs.getInt(1)); // garante que o id_ordem é válido para futuras transações
        }

        rs.close();
        stmt.close();
    }


    // Obter ordens pendentes da moeda contrária (máx. 24h de validade)
    public List<Ordem> obterOrdensPendentes(int idMoeda, String tipoContrario) throws SQLException {
        List<Ordem> ordens = new ArrayList<>();
        String sql = "SELECT * FROM ordem " +
                "WHERE id_moeda = ? " +
                "AND tipo = ? " +
                "AND status IN ('PENDENTE', 'PARCIAL') " +
                "AND data_ordem >= NOW() - INTERVAL 24 HOUR " +
                "ORDER BY data_ordem ASC";

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, idMoeda);
        stmt.setString(2, tipoContrario);

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            Ordem ordem = new Ordem();
            ordem.setId_ordem(rs.getInt("id"));
            ordem.setId_carteira(rs.getInt("id_user"));
            ordem.setId_moeda(rs.getInt("id_moeda"));
            ordem.setTipo(rs.getString("tipo"));
            ordem.setQuantidade_total(rs.getDouble("quantidade_total"));
            ordem.setQuantidade_restante(rs.getDouble("quantidade_restante"));
            ordem.setPreco_no_momento(rs.getDouble("preco"));
            ordem.setTimestamp_criacao(rs.getTimestamp("data_ordem").toLocalDateTime());
            ordem.setStatus(rs.getString("status"));
            ordens.add(ordem);
        }

        rs.close();
        stmt.close();

        return ordens;
    }

    // Atualizar ordem (quantidade_restante e status)
    public void atualizarOrdem(Ordem ordem) throws SQLException {
        String sql = "UPDATE ordem SET quantidade_restante = ?, status = ? WHERE id = ?";

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setDouble(1, ordem.getQuantidade_restante());
        stmt.setString(2, ordem.getStatus());
        stmt.setInt(3, ordem.getId_ordem());

        stmt.executeUpdate();
        stmt.close();
    }

    // Obter ordem por ID (opcional, útil para debug)
    public Ordem obterOrdemPorId(int id) throws SQLException {
        String sql = "SELECT * FROM ordem WHERE id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, id);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            Ordem ordem = new Ordem();
            ordem.setId_ordem(rs.getInt("id"));
            ordem.setId_carteira(rs.getInt("id_user"));
            ordem.setId_moeda(rs.getInt("id_moeda"));
            ordem.setTipo(rs.getString("tipo"));
            ordem.setQuantidade_total(rs.getDouble("quantidade_total"));
            ordem.setQuantidade_restante(rs.getDouble("quantidade_restante"));
            ordem.setPreco_no_momento(rs.getDouble("preco"));
            ordem.setTimestamp_criacao(rs.getTimestamp("data_ordem").toLocalDateTime());
            ordem.setStatus(rs.getString("status"));
            return ordem;
        }

        rs.close();
        stmt.close();
        return null;
    }
}
