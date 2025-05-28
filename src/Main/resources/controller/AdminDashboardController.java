package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import utils.SessaoAtual;
import utils.NavigationHelper;

import java.io.IOException;

public class AdminDashboardController {

    @FXML
    private Button adminButton;

    @FXML
    public void initialize() {
        // Controle de visibilidade do botão admin
        if (adminButton != null) {
            boolean isAdmin = "Admin".equals(SessaoAtual.tipo);
            adminButton.setVisible(isAdmin);
            adminButton.setManaged(isAdmin);
        }
    }

    @FXML
    private void handleManageUsers(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/user_management.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            showAlert("Erro ao carregar o gerenciamento de usuários.", AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewStatistics(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/statistics.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            showAlert("Erro ao carregar as estatísticas.", AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogOut(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmação de Logout");
        alert.setHeaderText(null);
        alert.setContentText("Tem certeza que deseja sair?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    SessaoAtual.limparSessao();

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setFullScreen(false);
                    stage.show();
                } catch (IOException e) {
                    showAlert("Erro ao fazer logout.", AlertType.ERROR);
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void goToHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/homepage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            showAlert("Erro ao carregar a página inicial.", AlertType.ERROR);
            e.printStackTrace();
        }
    }


    @FXML
    private void goToMarket(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/market.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            showAlert("Erro ao carregar o mercado.", AlertType.ERROR);
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