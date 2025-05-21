package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    @FXML
    private Label statusLabel;

    @FXML
    private void handleEnviarLink() {
        String email = emailField.getText();

        if (email == null || email.isEmpty()) {
            showAlert("Por favor, insira o seu e-mail.", Alert.AlertType.WARNING);
            return;
        }

        // Simulando envio
        showAlert("Um link de recuperação seria enviado para: " + email, Alert.AlertType.INFORMATION);
        statusLabel.setText("Link enviado para o email: " + email);
    }

    private void showAlert(String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Recuperação de Senha");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
