package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import Repository.UtilizadorRepository;
import utils.NavigationHelper;
import utils.Routes;
import utils.SessaoAtual;

public class UserProfileController {

    @FXML private ImageView fotoPerfil;
    @FXML private Label nomeTopoLabel;
    @FXML private Label emailTopoLabel;

    @FXML private TextField nomeField;
    @FXML private TextField emailField;
    @FXML private TextField telemovelField;
    @FXML private TextField localizacaoField;

    @FXML private Label mensagemLabel;

    @FXML
    public void initialize() {
        nomeTopoLabel.setText(SessaoAtual.nome);
        emailTopoLabel.setText(SessaoAtual.email);
        nomeField.setText(SessaoAtual.nome);
        emailField.setText(SessaoAtual.email);


        try {
            fotoPerfil.setImage(new Image("/icons/perfil.png"));
        } catch (Exception e) {
            System.out.println("Foto de perfil não encontrada.");
        }
    }

    @FXML
    private void handleGuardar() {
        try {
            boolean sucesso = UtilizadorRepository.atualizarUtilizador(
                    SessaoAtual.utilizadorId,
                    nomeField.getText(),
                    emailField.getText()
            );

            if (sucesso) {
                SessaoAtual.nome = nomeField.getText();
                SessaoAtual.email = emailField.getText();


                nomeTopoLabel.setText(SessaoAtual.nome);
                emailTopoLabel.setText(SessaoAtual.email);

                mensagemLabel.setText("Alterações guardadas com sucesso.");
            } else {
                mensagemLabel.setText("Erro ao guardar alterações.");
            }
        } catch (Exception e) {
            mensagemLabel.setText("Erro nos dados.");
        }
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Deseja terminar a sessão?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Terminar Sessão");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                SessaoAtual.limparSessao();
                NavigationHelper.goTo(Routes.LOGIN, false);
            }
        });
    }
}
