package controller;

import Database.DBConnection;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import utils.SessaoAtual;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class VerificationController {

    @FXML
    private TextField codigoField;

    @FXML
    private Label statusLabel;

    @FXML
    public void handleVerify() {
        String codigoInserido = codigoField.getText().trim();

        if (codigoInserido.isEmpty()) {
            statusLabel.setText("Insira o código de verificação.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM verificacoes_email WHERE id = ? AND codigo = ? AND verificado = FALSE AND expira_em > NOW()";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, SessaoAtual.utilizadorId);
            stmt.setString(2, codigoInserido);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Código válido: atualizar verificado = true e definir data de verificação
                String update = "UPDATE verificacoes_email SET verificado = TRUE, verificado_em = ? WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(update);
                updateStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setInt(2, SessaoAtual.utilizadorId);
                updateStmt.executeUpdate();

                statusLabel.setText("Email verificado com sucesso!");
            } else {
                statusLabel.setText("Código inválido ou expirado.");
            }

        } catch (Exception e) {
            statusLabel.setText("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
