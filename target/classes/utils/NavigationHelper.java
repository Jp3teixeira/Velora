package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utilitário para navegação entre cenas FXML.
 */
public class NavigationHelper {

    /**
     * Abre um FXML sem passagem de dados.
     *
     * @param fxmlPath   caminho relativo ao ficheiro FXML (ex: "/view/homepage.fxml")
     * @param fullscreen se a cena deve abrir em fullscreen
     */
    public static void goTo(String fxmlPath, boolean fullscreen) {
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

            stage.setScene(new Scene(root));
            if (fullscreen) stage.setFullScreen(true);
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
