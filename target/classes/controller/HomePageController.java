package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.io.InputStream;

public class HomePageController {

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmação de Logout");
        alert.setHeaderText(null);
        alert.setContentText("Tem certeza que deseja sair?");

        // Carrega o ícone para o alerta
        try (InputStream iconStream = getClass().getResourceAsStream("/moedas.png")) {
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().add(new Image(iconStream));
        } catch (Exception e) {
            System.err.println("Erro ao carregar ícone para alerta: " + e.getMessage());
        }

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Node source = (Node) event.getSource();
                    Stage currentStage = (Stage) source.getScene().getWindow();

                    // Carrega a tela de login
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
                    Parent root = loader.load();

                    // Configura a nova cena
                    Stage loginStage = new Stage();
                    loginStage.setTitle("Velora - Gestão de Criptomoedas");
                    loginStage.setScene(new Scene(root, 800, 600));
                    loginStage.setResizable(false);

                    // Adiciona o ícone à nova janela
                    try (InputStream iconStream = getClass().getResourceAsStream("/moedas.png")) {
                        loginStage.getIcons().add(new Image(iconStream));
                    } catch (Exception e) {
                        System.err.println("Erro ao carregar ícone: " + e.getMessage());
                    }

                    // Fecha a janela atual e abre a nova
                    currentStage.close();
                    loginStage.show();

                } catch (Exception e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erro");
                    errorAlert.setHeaderText("Erro ao carregar tela de login");
                    errorAlert.setContentText(e.getMessage());
                    errorAlert.showAndWait();
                }
            }
        });
    }
}