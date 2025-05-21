package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));

        primaryStage.setTitle("Velora - Gestão de Criptomoedas");
        primaryStage.setScene(new Scene(root, 800, 600));  // Ajuste para o tamanho do FXML
        primaryStage.setResizable(false);  // Opcional: impede redimensionamento
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}