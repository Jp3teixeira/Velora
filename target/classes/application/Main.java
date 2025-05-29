package application;

import javafx.scene.image.Image;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.MarketSimulator;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        MarketSimulator.iniciarAgendador();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
        Parent root = loader.load();

        // Define um tamanho menor para a janela (ajuste conforme necessário)
        Scene scene = new Scene(root, 400, 500); // Largura 400, Altura 500

        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/moedas.png")));
        } catch (Exception e) {
            System.out.println("Ícone não encontrado: " + e.getMessage());
        }

        primaryStage.setTitle("Velora - Gestão de Criptomoedas");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // Impede redimensionamento
        primaryStage.centerOnScreen(); // Centraliza na tela
        primaryStage.show();
    }
}
