package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import Repository.UtilizadorRepository;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import model.Utilizador;
import utils.NavigationHelper;
import utils.Routes;
import utils.SessaoAtual;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;

import static utils.SessaoAtual.utilizadorId;

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
    public void initialize(URL location, ResourceBundle resources) {
        Utilizador utilizador = SessaoAtual.getUtilizador();

        if (utilizador != null) {
            nomeTopoLabel.setText(utilizador.getNome());
            emailTopoLabel.setText(utilizador.getEmail());

            nomeField.setText(utilizador.getNome());
            emailField.setText(utilizador.getEmail());

            if (utilizador.getFoto() != null && !utilizador.getFoto().isEmpty()) {
                fotoPerfil.setImage(new Image(utilizador.getFoto()));
            } else {
                System.out.println("Foto de perfil não encontrada.");
            }
        } else {
            System.out.println("Utilizador não encontrado na sessão.");
        }
    }



    @FXML
    private void handleGuardar() {
        try {
            boolean sucesso = UtilizadorRepository.atualizarUtilizador(
                    utilizadorId,
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

    private String caminhoFotoSelecionada = null;

    @FXML
    private void handleAdicionarFoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar foto de perfil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File ficheiro = fileChooser.showOpenDialog(null);

        Circle clip = new Circle(50, 50, 50); // raio = metade da largura/altura
        fotoPerfil.setClip(clip);

        if (ficheiro != null) {
            caminhoFotoSelecionada = ficheiro.toURI().toString(); // Caminho no formato URI
            fotoPerfil.setImage(new Image(caminhoFotoSelecionada));
            atualizarFotoNaBaseDeDados(caminhoFotoSelecionada); // <-- aqui chamas o método
        } else {
            System.out.println("Foto de perfil não selecionada.");
        }
    }


    private void atualizarFotoNaBaseDeDados(String caminho) {
        int idUtilizador = utilizadorId; // ou como acedes ao ID

        try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://ctespbd.dei.isep.ipp.pt;databaseName=2025_LP2_G5_ERM;encrypt=true;trustServerCertificate=true;", "2025_LP2_G5_ERM", "LP2Grupo5")) {
            String sql = "UPDATE utilizador SET foto = ? WHERE id_utilizador = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, caminho);
            stmt.setInt(2, idUtilizador);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
