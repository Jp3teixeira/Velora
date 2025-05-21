package controller;

import Database.DBConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.scene.Node;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;



public class LoginController {

    @FXML
    private void abrirTelaRecuperacao() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/forgot_password.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Recuperar Senha");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
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
                // Carregar a tela principal
                try {
                    // Obter o stage atual
                    Stage stage = (Stage) emailField.getScene().getWindow();

                    // Carregar a tela principal
                    // No LoginController:
                    Parent root = FXMLLoader.load(getClass().getResource("../view/homepage.fxml"));
                    Scene scene = new Scene(root, 800, 600);

                    // Configurar para fullscreen
                    stage.setScene(scene);
                    stage.setFullScreen(true);
                    stage.setTitle("Velora - Gestão de Criptomoedas");
                    stage.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
    @FXML
    private void abrirRecuperacaoSenha() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/forgot_password.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Recuperar Senha");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}