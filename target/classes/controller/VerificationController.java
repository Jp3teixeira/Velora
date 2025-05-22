package controller;

import Database.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.SessaoAtual;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;

public class VerificationController {

    @FXML
    private TextField codigoField;

    @FXML
    private void handleVerify(ActionEvent event) {
        String codigoInserido = codigoField.getText().trim();

        if (codigoInserido.isEmpty()) {
            showAlert("Por favor, insira o código.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT codigo, expira_em FROM verificacoes_email WHERE id = ? AND verificado = FALSE";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, SessaoAtual.utilizadorId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String codigoBD = rs.getString("codigo");
                Timestamp expira = rs.getTimestamp("expira_em");

                if (LocalDateTime.now().isAfter(expira.toLocalDateTime())) {
                    showAlert("O código expirou. Solicite um novo.");
                    return;
                }

                if (codigoInserido.equals(codigoBD)) {
                    // Atualiza verificação
                    String update = "UPDATE verificacoes_email SET verificado = TRUE, verificado_em = CURRENT_TIMESTAMP WHERE id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(update);
                    updateStmt.setInt(1, SessaoAtual.utilizadorId);
                    updateStmt.executeUpdate();

                    // Redireciona para a homepage
                    Parent root = FXMLLoader.load(getClass().getResource("/view/homepage.fxml"));
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.setFullScreen(true);
                    stage.setTitle("Velora - Gestão de Criptomoedas");
                    stage.show();

                } else {
                    showAlert("Código incorreto.");
                }
            } else {
                showAlert("Nenhum código encontrado ou já foi verificado.");
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showAlert("Erro: " + e.getMessage());
        }
    }

    private void showAlert(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
