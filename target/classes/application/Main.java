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
        // ✅ Inicia o agendador de simulação
        MarketSimulator.iniciarAgendador();

        Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));

        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/moedas.png")));
        } catch (Exception e) {
            System.out.println("Ícone não encontrado: " + e.getMessage());
            e.printStackTrace();
        }

        primaryStage.setTitle("Velora - Gestão de Criptomoedas");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
