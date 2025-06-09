package controller;

import Repository.UserRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import model.Utilizador;
import utils.NavigationHelper;
import utils.Routes;
import utils.SessaoAtual;

import java.io.File;

public class UserProfileController {

    @FXML private ImageView fotoPerfil;
    @FXML private Label nomeTopoLabel;
    @FXML private Label emailTopoLabel;
    @FXML private TextField nomeField;
    @FXML private TextField emailField;
    @FXML private TextField perfilField; // se deixares o user escolher admin/user
    @FXML private Label mensagemLabel;

    private Utilizador u = SessaoAtual.getUtilizador();

    @FXML
    public void initialize() {
        if (u != null) {
            nomeTopoLabel.setText(u.getNome());
            emailTopoLabel.setText(u.getEmail());
            nomeField.setText(u.getNome());
            emailField.setText(u.getEmail());
            perfilField.setText(u.getIdPerfil().toString());
            if (u.getFoto() != null) {
                fotoPerfil.setImage(new Image(u.getFoto()));
            }
        }
    }

    @FXML
    private void handleGuardar() {
        int novoPerfil = Integer.parseInt(perfilField.getText().trim());
        boolean ok = UserRepository.atualizarUtilizador(
                SessaoAtual.utilizadorId,
                nomeField.getText(),
                emailField.getText(),
                novoPerfil
        );
        if (ok) {
            u.setNome(nomeField.getText());
            u.setEmail(emailField.getText());
            u.setIdPerfil(novoPerfil);
            SessaoAtual.setUtilizador(u);
            nomeTopoLabel.setText(u.getNome());
            emailTopoLabel.setText(u.getEmail());
            mensagemLabel.setText("Dados actualizados com sucesso.");
        } else {
            mensagemLabel.setText("Erro ao guardar alterações.");
        }
    }

    @FXML
    private void handleAdicionarFoto() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png","*.jpg","*.jpeg")
        );
        File f = fc.showOpenDialog(null);
        if (f == null) return;

        String uri = f.toURI().toString();
        Circle clip = new Circle(50,50,50);
        fotoPerfil.setClip(clip);
        fotoPerfil.setImage(new Image(uri));

        if (UserRepository.atualizarFoto(SessaoAtual.utilizadorId, uri)) {
            u.setFoto(uri);
            SessaoAtual.setUtilizador(u);
            mensagemLabel.setText("Foto actualizada!");
        } else {
            mensagemLabel.setText("Falha ao guardar a foto.");
        }
    }

    @FXML
    private void handleLogout() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja terminar a sessão?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                SessaoAtual.limparSessao();
                NavigationHelper.goTo(Routes.LOGIN, false);
            }
        });
    }
}
