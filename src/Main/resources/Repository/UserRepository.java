package repository;

import Database.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class UserRepository {

    public Optional<Map<String, String>> findUserByEmailOrUsername(String input) {
        String sql = "SELECT * FROM utilizadores WHERE email = ? OR nome = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, input);
            stmt.setString(2, input);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Map<String, String> user = new HashMap<>();
                user.put("id", String.valueOf(rs.getInt("id")));
                user.put("nome", rs.getString("nome"));
                user.put("email", rs.getString("email"));
                user.put("senha", rs.getString("senha"));
                user.put("tipo", rs.getString("tipo"));
                return Optional.of(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean isContaVerificada(int utilizadorId) {
        String sql = "SELECT verificado FROM verificacoes_email WHERE utilizador_id = ?";
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

    public Optional<Integer> registarNovoUtilizador(String nome, String email, String senhaHashed) {
        String insertUser = "INSERT INTO utilizadores (nome, email, senha, tipo) VALUES (?, ?, ?, 'Cliente')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setString(3, senhaHashed);
            int linhas = stmt.executeUpdate();
            if (linhas > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) return Optional.of(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean inserirCodigoVerificacao(int utilizadorId, String codigo, LocalDateTime expira, String tipo) {
        String sql = """
                INSERT INTO verificacoes_email 
                (utilizador_id, codigo, criado_em, expira_em, verificado, tipo) 
                VALUES (?, ?, CURRENT_TIMESTAMP, ?, FALSE, ?)
                ON DUPLICATE KEY UPDATE
                    codigo = VALUES(codigo),
                    criado_em = CURRENT_TIMESTAMP,
                    expira_em = VALUES(expira_em),
                    verificado = FALSE,
                    verificado_em = NULL,
                    tipo = VALUES(tipo)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            stmt.setString(2, codigo);
            stmt.setTimestamp(3, Timestamp.valueOf(expira));
            stmt.setString(4, tipo);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean validarCodigo(int utilizadorId, String tipo, String codigo) {
        String sql = """
                SELECT codigo, expira_em FROM verificacoes_email 
                WHERE utilizador_id = ? AND tipo = ? AND verificado = FALSE
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            stmt.setString(2, tipo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String codigoBD = rs.getString("codigo");
                Timestamp expira = rs.getTimestamp("expira_em");

                if (LocalDateTime.now().isAfter(expira.toLocalDateTime())) return false;
                if (!codigo.equals(codigoBD)) return false;

                // Atualizar verificado
                String update = """
                        UPDATE verificacoes_email 
                        SET verificado = TRUE, verificado_em = CURRENT_TIMESTAMP 
                        WHERE utilizador_id = ? AND tipo = ?
                        """;
                PreparedStatement updateStmt = conn.prepareStatement(update);
                updateStmt.setInt(1, utilizadorId);
                updateStmt.setString(2, tipo);
                updateStmt.executeUpdate();

                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean atualizarSenha(int utilizadorId, String novaSenhaHashed) {
        String sql = "UPDATE utilizadores SET senha = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novaSenhaHashed);
            stmt.setInt(2, utilizadorId);
            int linhas = stmt.executeUpdate();
            return linhas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Integer> getUserIdByEmail(String email) {
        String sql = "SELECT id FROM utilizadores WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(rs.getInt("id"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
    public boolean existeCodigoValido(int utilizadorId, String tipo) {
        String sql = """
        SELECT expira_em 
        FROM verificacoes_email 
        WHERE utilizador_id = ? AND tipo = ? AND verificado = FALSE
    """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilizadorId);
            stmt.setString(2, tipo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp expira = rs.getTimestamp("expira_em");
                return LocalDateTime.now().isBefore(expira.toLocalDateTime());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean marcarCodigoComoVerificado(int utilizadorId, String tipo) {
        String update = """
        UPDATE verificacoes_email 
        SET verificado = TRUE, verificado_em = CURRENT_TIMESTAMP 
        WHERE utilizador_id = ? AND tipo = ?
    """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(update)) {
            stmt.setInt(1, utilizadorId);
            stmt.setString(2, tipo);
            int linhas = stmt.executeUpdate();
            return linhas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


}


