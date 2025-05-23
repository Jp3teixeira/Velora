package controller;

import Database.DBConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import org.mindrot.jbcrypt.BCrypt;
import utils.SessaoAtual;

import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String input = emailField.getText().trim(); // pode ser nome OU email
        String password = passwordField.getText();

        if (input.isEmpty() || password.isEmpty()) {
            showAlert("Por favor, preencha todos os campos.", AlertType.ERROR);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM utilizadores WHERE email = ? OR nome = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, input);
            stmt.setString(2, input);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashed = rs.getString("senha");

                if (BCrypt.checkpw(password, hashed)) {
                    // Guarda dados do utilizador autenticado
                    SessaoAtual.utilizadorId = rs.getInt("id");
                    SessaoAtual.nome = rs.getString("nome");
                    SessaoAtual.email = rs.getString("email");
                    SessaoAtual.tipo = rs.getString("tipo");

                    // Login com sucesso → ir para homepage
                    Stage stage = (Stage) emailField.getScene().getWindow();
                    Parent root = FXMLLoader.load(getClass().getResource("/view/homepage.fxml"));
                    Scene scene = new Scene(root, 800, 600);
                    stage.setScene(scene);
                    stage.setFullScreen(true);
                    stage.setTitle("Velora - Gestão de Criptomoedas");
                    stage.show();
                }
                else {
                    showAlert("Password incorreta.", AlertType.ERROR);
                }
            } else {
                showAlert("Utilizador não encontrado.", AlertType.ERROR);
            }
        } catch (SQLException | IOException e) {
            showAlert("Erro: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }


    @FXML
    private void abrirTelaRecuperacao() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/forgot_password.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Recuperar Senha");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            showAlert("Erro ao abrir a tela de recuperação de senha.", AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Erro ao abrir a tela de registo.", AlertType.ERROR);
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
