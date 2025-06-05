package Repository;

import Database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class UtilizadorRepository {

    public static boolean atualizarUtilizador(int id, String nome, String email) {
        String sql = "UPDATE utilizador SET nome = ?, email = ? WHERE id_utilizador = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setInt(3, id);

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Erro ao atualizar utilizador: " + e.getMessage());
            return false;
        }
    }
}
