package application;

import Database.DBConnection;
import Repository.OrdemRepository;
import javafx.scene.image.Image;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.MarketSimulator;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        MarketSimulator.startSimulador();

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/view/login.fxml"));
        Parent root = loader.load();


        Scene scene = new Scene(root, 800, 800);

        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/moedas.png")));
        } catch (Exception e) {
            System.out.println("Ícone não encontrado: " + e.getMessage());
        }

        primaryStage.setTitle("Velora - Gestão de Criptomoedas");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();



    }
}
