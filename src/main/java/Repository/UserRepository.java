// src/Repository/UserRepository.java
package Repository;

import Database.DataAccessException;
import model.Perfil;
import model.Utilizador;
import model.Carteira;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para Utilizador, implementa operações CRUD e métodos de negócio.
 */
public class UserRepository implements DAO<Utilizador, Integer> {

    public UserRepository() {}

    @Override
    public Optional<Utilizador> get(Integer id) {
        String sql = """
            SELECT id_utilizador, nome, email, tipoPerfil, ativo, foto, password
              FROM v_UtilizadorPerfil
             WHERE id_utilizador = ?
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
            throw new DataAccessException("Erro ao buscar Utilizador ID " + id, e);
        }
    }

    @Override
    public List<Utilizador> getAll() {
        String sql = """
            SELECT id_utilizador, nome, email, tipoPerfil, ativo, foto, password
              FROM v_UtilizadorPerfil
        """;
        List<Utilizador> lista = new ArrayList<>();
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao listar todos os Utilizadores", e);
        }
    }

    @Override
    public boolean save(Utilizador u) {
        String call = "{ call sp_RegistarNovoUtilizador(?, ?, ?, ?) }";
        try (Connection conn = Database.DBConnection.getConnection();
             CallableStatement cstmt = conn.prepareCall(call)) {
            cstmt.setString(1, u.getNome());
            cstmt.setString(2, u.getEmail());
            cstmt.setString(3, u.getHashPwd());
            cstmt.registerOutParameter(4, Types.INTEGER);
            cstmt.execute();
            int newId = cstmt.getInt(4);
            if (newId > 0) {
                u.setId(newId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao registar novo Utilizador", e);
        }
    }

    @Override
    public boolean update(Utilizador u) {
        String sql = """
            UPDATE Utilizador
               SET nome = ?, email = ?, id_perfil = ?, foto = ?
             WHERE id_utilizador = ?
        """;
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNome());
            ps.setString(2, u.getEmail());
            ps.setInt(3, u.getIdPerfil());
            ps.setString(4, u.getFoto());
            ps.setInt(5, u.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao atualizar Utilizador ID " + u.getId(), e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "UPDATE Utilizador SET ativo = 0 WHERE id_utilizador = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao desativar Utilizador ID " + id, e);
        }
    }

    // --- Métodos de negócio adicionais ---

    public Optional<Integer> getPerfilId(String perfilNome) {
        String sql = "SELECT dbo.fn_GetPerfilId(?) AS id_perfil";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, perfilNome);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("id_perfil"));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao obter perfil ID para '" + perfilNome + "'", e);
        }
    }

    public Optional<Utilizador> findByEmailOrUsername(String input) {
        String sql = "SELECT * FROM v_UtilizadorPerfil WHERE email = ? OR nome = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, input);
            ps.setString(2, input);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao procurar Utilizador por '" + input + "'", e);
        }
    }

    /**
     * Verifica existência de email diretamente na tabela Utilizador.
     */
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM Utilizador WHERE email = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao verificar existência de email '" + email + "'", e);
        }
    }

    /**
     * Verifica existência de username (nome) diretamente na tabela Utilizador.
     */
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM Utilizador WHERE nome = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao verificar existência de username '" + username + "'", e);
        }
    }

    private Utilizador mapRow(ResultSet rs) throws SQLException {
        Utilizador user = new Utilizador();
        user.setId(rs.getInt("id_utilizador"));
        user.setNome(rs.getString("nome"));
        user.setEmail(rs.getString("email"));
        user.setPerfil(Perfil.valueOf(rs.getString("tipoPerfil").toUpperCase()));
        user.setAtivo(rs.getBoolean("ativo"));
        user.setFoto(rs.getString("foto"));
        user.setHashPwd(rs.getString("password"));
        return user;
    }

    // --- Verificação por código ---


    /**
     * Insere código de verificação para registro ou recuperação.
     */
    public boolean inserirCodigoVerificacao(int userId, String code, LocalDateTime expiry, String tipo) {
        // note: we write INTO VerificacaoEmail (not CodigoVerificacao)
        String sql = """
        INSERT INTO VerificacaoEmail
            (id_utilizador, codigo, expira_em, tipo)
        VALUES (?, ?, ?, ?)
        """;
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.setTimestamp(3, Timestamp.valueOf(expiry));
            ps.setString(4, tipo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao inserir código de verificação", e);
        }
    }

    /**
     * Valida um código de verificação: correta, não expirado e não utilizado.
     */
    public boolean validarCodigo(int userId, String code) {
        String select = """
        SELECT COUNT(*) 
          FROM dbo.VerificacaoEmail
         WHERE id_utilizador = ?
           AND codigo        = ?
           AND expira_em     >= GETDATE()
           AND verificado    = 0
        """;
        String update = """
        UPDATE dbo.VerificacaoEmail
           SET verificado = 1, verificado_em = GETDATE()
         WHERE id_utilizador = ? AND codigo = ?
        """;
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                if (count > 0) {
                    try (PreparedStatement pu = conn.prepareStatement(update)) {
                        pu.setInt(1, userId);
                        pu.setString(2, code);
                        pu.executeUpdate();
                    }
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao validar código de verificação", e);
        }
    }

    /**
     * Atualiza a password do utilizador.
     */
    public boolean atualizarSenha(int userId, String hashedPassword) {
        String sql = "UPDATE Utilizador SET password = ? WHERE id_utilizador = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao atualizar senha do utilizador", e);
        }
    }

    public boolean isContaVerificada(int userId) {
        return get(userId).map(Utilizador::isAtivo).orElse(false);
    }

    public boolean ativarUtilizador(int userId) {
        String sql = "UPDATE Utilizador SET ativo = 1 WHERE id_utilizador = ?";
        try (Connection conn = Database.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao ativar utilizador ID " + userId, e);
        }
    }

    public boolean podeSerAdmin(int userId) {
        BigDecimal saldo = WalletRepository.getInstance().getSaldoPorUtilizador(userId);
        if (saldo.compareTo(BigDecimal.ZERO) > 0) return false;
        boolean temPosicoes = new PortfolioRepository()
                .listarPorUtilizador(userId).stream()
                .anyMatch(p -> p.getQuantidade().compareTo(BigDecimal.ZERO) > 0);
        return !temPosicoes;
    }
}
