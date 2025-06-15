package Repository;

import Database.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class UserRepository {

    /**
     * Obtém o id_perfil a partir do texto a partir da função fn_GetPerfilId.
     */
    public Optional<Integer> getPerfilId(String perfilNome) {
        String sql = "SELECT dbo.fn_GetPerfilId(?) AS id_perfil";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, perfilNome);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getInt("id_perfil"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Procura um utilizador por e-mail ou username usando a view v_UtilizadorPerfil.
     */
    public Optional<Map<String, String>> findUserByEmailOrUsername(String input) {
        String sql = "SELECT * FROM v_UtilizadorPerfil WHERE email = ? OR nome = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, input);
            stmt.setString(2, input);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Map<String, String> user = new HashMap<>();
                user.put("id_utilizador",  String.valueOf(rs.getInt("id_utilizador")));
                user.put("nome",            rs.getString("nome"));
                user.put("email",           rs.getString("email"));
                user.put("password",        rs.getString("password"));
                user.put("tipoPerfil",      rs.getString("tipoPerfil"));
                user.put("foto",            rs.getString("foto"));
                return Optional.of(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Verifica existência de email usando função fn_UserExistsByEmail.
     */
    public boolean existsByEmail(String email) {
        String sql = "SELECT dbo.fn_UserExistsByEmail(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getBoolean(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica existência de username usando função fn_UserExistsByUsername.
     */
    public boolean existsByUsername(String username) {
        String sql = "SELECT dbo.fn_UserExistsByUsername(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getBoolean(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica se a conta do utilizador (por ID) está validada em VerificacaoEmail.
     */
    public boolean isContaVerificada(int utilizadorId) {
        String sql = "SELECT verificado FROM VerificacaoEmail WHERE id_utilizador = ? AND tipo = 'REGISTO'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getBoolean("verificado");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Regista um novo utilizador via stored procedure sp_RegistarNovoUtilizador.
     */
    public Optional<Integer> registarNovoUtilizador(String nome, String email, String senhaHashed) {
        String call = "{ call sp_RegistarNovoUtilizador(?, ?, ?, ?) }";
        try (Connection conn = DBConnection.getConnection();
             CallableStatement cstmt = conn.prepareCall(call)) {
            cstmt.setString(1, nome);
            cstmt.setString(2, email);
            cstmt.setString(3, senhaHashed);
            cstmt.registerOutParameter(4, Types.INTEGER);
            cstmt.execute();
            int newId = cstmt.getInt(4);
            return newId > 0 ? Optional.of(newId) : Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Insere (ou atualiza) um código de verificação em VerificacaoEmail.
     * Usa MERGE para SQL Server.
     */
    public boolean inserirCodigoVerificacao(int utilizadorId,
                                            String codigo,
                                            LocalDateTime expira,
                                            String tipo) {
        String sql = """
            MERGE VerificacaoEmail AS alvo
            USING (SELECT ? AS id_utilizador, ? AS tipo) AS fonte
            ON alvo.id_utilizador = fonte.id_utilizador
               AND alvo.tipo = fonte.tipo
               AND alvo.verificado = 0
            WHEN MATCHED THEN
              UPDATE SET codigo        = ?,
                         criado_em     = GETDATE(),
                         expira_em     = ?,
                         verificado    = 0,
                         verificado_em = NULL
            WHEN NOT MATCHED THEN
              INSERT (id_utilizador, codigo, criado_em, expira_em, verificado, tipo)
              VALUES (?, ?, GETDATE(), ?, 0, ?);
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utilizadorId);
            stmt.setString(2, tipo);
            stmt.setString(3, codigo);
            stmt.setTimestamp(4, Timestamp.valueOf(expira));
            stmt.setInt(5, utilizadorId);
            stmt.setString(6, codigo);
            stmt.setTimestamp(7, Timestamp.valueOf(expira));
            stmt.setString(8, tipo);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Valida um código de verificação para um utilizador e tipo específico.
     * Marca como verificado se for válido e não expirado.
     */
    public boolean validarCodigo(int utilizadorId, String tipo, String codigo) {
        String sqlBusca = """
            SELECT codigo, expira_em
              FROM VerificacaoEmail
             WHERE id_utilizador = ?
               AND tipo = ?
               AND verificado = 0
            """;

        String sqlUpdate = """
            UPDATE VerificacaoEmail
               SET verificado     = 1,
                   verificado_em  = GETDATE()
             WHERE id_utilizador = ?
               AND tipo = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmtBusca = conn.prepareStatement(sqlBusca)) {

            stmtBusca.setInt(1, utilizadorId);
            stmtBusca.setString(2, tipo);
            ResultSet rs = stmtBusca.executeQuery();

            if (!rs.next()) {
                return false;
            }
            String codigoBD = rs.getString("codigo");
            Timestamp expira  = rs.getTimestamp("expira_em");

            if (expira != null && LocalDateTime.now().isAfter(expira.toLocalDateTime())) {
                return false;
            }
            if (!codigo.equals(codigoBD)) {
                return false;
            }

            try (PreparedStatement stmtUpd = conn.prepareStatement(sqlUpdate)) {
                stmtUpd.setInt(1, utilizadorId);
                stmtUpd.setString(2, tipo);
                stmtUpd.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Atualiza a password de um utilizador.
     */
    public boolean atualizarSenha(int utilizadorId, String novaSenhaHashed) {
        String sql = "UPDATE Utilizador SET password = ? WHERE id_utilizador = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novaSenhaHashed);
            stmt.setInt(2, utilizadorId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtém o ID do utilizador a partir do e-mail.
     */
    public Optional<Integer> getUserIdByEmail(String email) {
        String sql = "SELECT id_utilizador FROM Utilizador WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getInt("id_utilizador"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Verifica se existe um código de verificação não expirado para este utilizador e tipo.
     */
    public boolean existeCodigoValido(int utilizadorId, String tipo) {
        String sql = """
            SELECT expira_em
              FROM VerificacaoEmail
             WHERE id_utilizador = ?
               AND tipo = ?
               AND verificado = 0
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utilizadorId);
            stmt.setString(2, tipo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp expira = rs.getTimestamp("expira_em");
                return expira != null && LocalDateTime.now().isBefore(expira.toLocalDateTime());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Marca um código já existente como verificado.
     */
    public boolean marcarCodigoComoVerificado(int utilizadorId, String tipo) {
        String sql = """
            UPDATE VerificacaoEmail
               SET verificado     = 1,
                   verificado_em  = GETDATE()
             WHERE id_utilizador = ?
               AND tipo = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utilizadorId);
            stmt.setString(2, tipo);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Actualiza nome, email e perfil de um utilizador.
     */
    public static boolean atualizarUtilizador(int id, String nome, String email, int idPerfil) {
        String sql = "UPDATE Utilizador SET nome = ?, email = ?, id_perfil = ? WHERE id_utilizador = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setInt   (3, idPerfil);
            stmt.setInt   (4, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Atualiza apenas o caminho da foto de perfil.
     */
    public static boolean atualizarFoto(int id, String fotoPath) {
        String sql = "UPDATE Utilizador SET foto = ? WHERE id_utilizador = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fotoPath);
            stmt.setInt   (2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
