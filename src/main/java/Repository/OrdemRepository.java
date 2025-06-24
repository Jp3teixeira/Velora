// OrdemRepository.java
package Repository;

import Database.DataAccessException;
import model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para Ordem, implementa operações CRUD básicas
 * e mantém métodos de consulta/expires conforme o domínio.
 */
public class OrdemRepository implements DAO<Ordem, Integer> {

    private final Connection connection;

    public OrdemRepository(Connection connection) {
        this.connection = connection;
    }

    // --- CRUD via DAO<Ordem,Integer> ---

    @Override
    public Optional<Ordem> get(Integer id) {
        try {
            return obterOrdemPorId(id);
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao buscar Ordem por ID " + id, e);
        }
    }

    @Override
    public List<Ordem> getAll() {
        String sql = "SELECT * FROM v_OrdemDetalhada ORDER BY data_criacao DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Ordem> ordens = new ArrayList<>();
            while (rs.next()) {
                ordens.add(mapearOrdem(rs));
            }
            return ordens;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao listar todas as Ordens", e);
        }
    }

    @Override
    public boolean save(Ordem ordem) {
        try {
            Optional<Integer> newId = inserirOrdem(ordem);
            if (newId.isPresent()) {
                ordem.setId(newId.get());
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao inserir nova Ordem", e);
        }
    }

    @Override
    public boolean update(Ordem ordem) {
        try {
            atualizarOrdem(ordem);
            return true;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao atualizar Ordem ID " + ordem.getId(), e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Ordem WHERE id_ordem = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao eliminar Ordem ID " + id, e);
        }
    }

    // --- Métodos auxiliares (lançam SQLException) ---

    private Optional<Integer> inserirOrdem(Ordem ordem) throws SQLException {
        String call = "{ CALL dbo.sp_InserirOrdem(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }";
        try (CallableStatement cstmt = connection.prepareCall(call)) {
            cstmt.setInt(1, ordem.getUtilizador().getId());
            cstmt.setInt(2, ordem.getMoeda().getId());
            cstmt.setInt(3, ordem.getIdTipoOrdem());
            cstmt.setInt(4, ordem.getIdStatus());
            cstmt.setInt(5, ordem.getIdModo());
            cstmt.setBigDecimal(6, ordem.getQuantidade());
            cstmt.setBigDecimal(7, ordem.getPrecoUnitarioEur());
            cstmt.setTimestamp(8, Timestamp.valueOf(ordem.getDataCriacao()));
            cstmt.setTimestamp(9, Timestamp.valueOf(ordem.getDataExpiracao()));
            cstmt.registerOutParameter(10, Types.INTEGER);
            cstmt.execute();
            int generatedId = cstmt.getInt(10);
            return generatedId > 0 ? Optional.of(generatedId) : Optional.empty();
        }
    }

    private Optional<Ordem> obterOrdemPorId(int id) throws SQLException {
        String sql = "SELECT * FROM v_OrdemDetalhada WHERE id_ordem = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearOrdem(rs));
                }
            }
        }
        return Optional.empty();
    }

    private void atualizarOrdem(Ordem ordem) throws SQLException {
        String sql = "UPDATE Ordem SET quantidade = ?, id_status = ? WHERE id_ordem = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, ordem.getQuantidade());
            ps.setInt(2, ordem.getIdStatus());
            ps.setInt(3, ordem.getId());
            ps.executeUpdate();
        }
    }

    // --- Mapeamento ResultSet -> Ordem ---

    private Ordem mapearOrdem(ResultSet rs) throws SQLException {
        Ordem ordem = new Ordem();
        ordem.setId(rs.getInt("id_ordem"));

        Utilizador u = new Utilizador();
        u.setId(rs.getInt("id_utilizador"));
        ordem.setUtilizador(u);

        Moeda m = new Moeda();
        m.setId(rs.getInt("id_moeda"));
        m.setNome(rs.getString("nome_moeda"));
        ordem.setMoeda(m);

        ordem.setIdTipoOrdem(rs.getInt("id_tipo_ordem"));
        ordem.setTipoOrdem(
                OrdemTipo.valueOf(rs.getString("tipo_ordem").toUpperCase())
        );

        ordem.setIdStatus(rs.getInt("id_status"));
        ordem.setStatus(
                OrdemStatus.valueOf(rs.getString("status").toUpperCase())
        );

        ordem.setIdModo(rs.getInt("id_modo"));
        ordem.setModo(
                OrdemModo.valueOf(rs.getString("modo").toUpperCase())
        );

        ordem.setQuantidade(rs.getBigDecimal("quantidade"));
        ordem.setPrecoUnitarioEur(rs.getBigDecimal("preco_unitario_eur"));
        ordem.setDataCriacao(
                rs.getTimestamp("data_criacao").toLocalDateTime()
        );
        ordem.setDataExpiracao(
                rs.getTimestamp("data_expiracao").toLocalDateTime()
        );

        return ordem;
    }
    // em Repository/OrdemRepository.java, abaixo dos outros métodos:

    /**
     * Retorna o id_tipo_ordem correspondente ao valor em base de dados.
     */
    public int obterIdTipoOrdem(String tipoOrdem) throws SQLException {
        String sql = "SELECT id_tipo_ordem FROM OrdemTipo WHERE tipo_ordem = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tipoOrdem.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Tipo de ordem não encontrado: " + tipoOrdem);
    }

    /**
     * Retorna o id_modo correspondente ao valor em base de dados.
     */
    public int obterIdModo(String modo) throws SQLException {
        String sql = "SELECT id_modo FROM OrdemModo WHERE modo = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, modo.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Modo de ordem não encontrado: " + modo);
    }

    /**
     * Retorna o id_status correspondente ao valor em base de dados.
     */
    public int obterIdStatus(String status) throws SQLException {
        String sql = "SELECT id_status FROM OrdemStatus WHERE status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Status de ordem não encontrado: " + status);
    }

}