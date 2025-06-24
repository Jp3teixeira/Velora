package utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NotificationUtil {

    public static final String VERIFICATION = "verification";
    public static final String REGISTER     = "register";
    public static final String RECOVER      = "recover";


    public static void show(String message, Stage stage, boolean isSuccess) {
        Platform.runLater(() -> {
            Label label = new Label(message);
            label.setStyle("""
                    -fx-background-color: %s;
                    -fx-text-fill: white;
                    -fx-padding: 10px 20px;
                    -fx-background-radius: 8px;
                    -fx-font-size: 14px;
                    """.formatted(isSuccess ? "#2ecc71" : "#e74c3c"));

            StackPane root = new StackPane(label);
            root.setStyle("-fx-padding: 10px;");

            Popup popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            Scene scene = stage.getScene();
            if (scene != null) {
                double centerX = scene.getWindow().getX() + scene.getWidth() / 2 - 100;
                double bottomY = scene.getWindow().getY() + scene.getHeight() - 80;
                popup.show(stage, centerX, bottomY);
            }

            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> popup.hide()));
            timeline.play();
        });
    }
}
