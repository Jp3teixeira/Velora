package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationHelper {

    public static void goTo(String fxmlPath, boolean fullscreen) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(fxmlPath));
            Parent root = loader.load();
            showScene(root, fullscreen);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erro ao navegar para: " + fxmlPath);
        }
    }

    public static <T> void goToWithController(String fxmlPath, ControllerConsumer<T> consumer, boolean fullscreen) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(fxmlPath));
            Parent root = loader.load();
            T controller = loader.getController();
            consumer.accept(controller);
            showScene(root, fullscreen);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erro ao navegar com controller para: " + fxmlPath);
        }
    }

    private static void showScene(Parent root, boolean fullscreen) {
        Stage stage = (Stage) Stage.getWindows().stream()
                .filter(window -> window.isShowing())
                .findFirst()
                .orElseThrow()
                .getScene()
                .getWindow();

        stage.setScene(new Scene(root));
        if (fullscreen) stage.setFullScreen(true);
        stage.show();
    }

    @FunctionalInterface
    public interface ControllerConsumer<T> {
        void accept(T controller);
    }
}
