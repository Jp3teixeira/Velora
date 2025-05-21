package controller;

import Database.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField emailField;  // Deve corresponder ao fx:id no FXML

    @FXML
    private PasswordField passwordField;  // Deve corresponder ao fx:id no FXML

    @FXML
    private void handleLogin() {
        String email = this.emailField.getText().trim();
        String password = this.passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Por favor, preencha todos os campos.", AlertType.ERROR);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM utilizadores WHERE nome = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("senha");

                if (BCrypt.checkpw(password, hashedPassword)) {
                    showAlert("Login bem-sucedido!", AlertType.INFORMATION);
                    // Podes carregar aqui a próxima cena (dashboard, por exemplo)
                } else {
                    showAlert("Credenciais inválidas.", AlertType.ERROR);
                }
            } else {
                showAlert("Utilizador não encontrado.", AlertType.ERROR);
            }

        } catch (SQLException e) {
            showAlert("Erro de conexão: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    public void goToRegister(javafx.event.ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/register.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
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
