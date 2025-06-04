package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utilitário para navegação entre cenas FXML.
 */
public class NavigationHelper {

    /**
     * Abre um FXML sem passagem de dados.
     *
     */
    public static void goTo(String fxmlPath, boolean fullscreen) {
        System.out.println("Path carregado: " + NavigationHelper.class.getResource(fxmlPath));
        try {
            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(fxmlPath));
            Parent root = loader.load();
            showScene(root, fullscreen);
        } catch (IOException e) {
            System.err.println("Erro ao navegar para: " + fxmlPath);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erro inesperado ao mudar de cena.");
            e.printStackTrace();
        }
    }
/**
 * Abre um FXML e permite configurar o controller antes de mostrar a cena.
 */
    public static <T> void goToWithController(String fxmlPath, ControllerConsumer<T> consumer, boolean fullscreen) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(fxmlPath));
            Parent root = loader.load();
            T controller = loader.getController();
            consumer.accept(controller);
            showScene(root, fullscreen);
        } catch (IOException e) {
            System.err.println("Erro ao navegar com controller para: " + fxmlPath);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erro inesperado ao carregar controlador de: " + fxmlPath);
            e.printStackTrace();
        }
    }


    private static void showScene(Parent root, boolean fullscreen) {
        try {
            Stage stage = (Stage) Stage.getWindows().stream()
                    .filter(window -> window.isShowing())
                    .findFirst()
                    .orElseThrow()
                    .getScene()
                    .getWindow();

            if (stage.getScene() == null) {

                Scene scene = new Scene(root);
                stage.setScene(scene);
            } else {

                stage.getScene().setRoot(root);
            }

            if (fullscreen) {

                stage.setFullScreenExitHint("");
                stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
                stage.setFullScreen(true);
            } else {
                stage.setFullScreen(false);
            }

            stage.show();
        } catch (Exception e) {
            System.err.println("Erro ao mostrar a nova cena.");
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    public interface ControllerConsumer<T> {
        void accept(T controller);
    }
}
