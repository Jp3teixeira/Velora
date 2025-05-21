package controller;

import Database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;

public class LoginController {
    @FXML
    private TextField emailField;  // Nome deve bater com o fx:id do FXML
    @FXML
    private PasswordField passwordField;  // Nome deve bater com o fx:id do FXML

    @FXML
    private void handleLogin() {  // Nome deve bater com onAction do FXML
        String email = this.emailField.getText();
        String password = this.passwordField.getText();

        if(emailField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showAlert("Por favor, preencha todos os campos.", AlertType.ERROR);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM utilizadores WHERE nome = ? AND senha = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                showAlert("Login bem-sucedido!", AlertType.INFORMATION);
                // Aqui você deve carregar a próxima tela
            } else {
                showAlert("Credenciais inválidas.", AlertType.ERROR);
            }
        } catch (SQLException e) {
            showAlert("Erro de conexão: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String mensagem, AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Mensagem");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}