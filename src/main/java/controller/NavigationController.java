package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import utils.NavigationHelper;
import utils.Routes;
import utils.SessaoAtual;

import java.io.InputStream;
import java.util.Map;

public class NavigationController {

    @FXML private Button homeButton;
    @FXML private Button portfolioButton;
    @FXML private Button marketButton;
    @FXML private Button researchButton;
    @FXML private Button transferButton;
    @FXML private Button reportsButton;
    @FXML private Button coinsButton;
    @FXML private Button accountButton;
    @FXML private Button adminButton;

    private static final Map<String, String> routeMap = Map.of(
            "homeButton",       Routes.HOMEPAGE,
            "portfolioButton",  Routes.PORTFOLIO,
            "marketButton",     Routes.MARKET,
            "researchButton",   Routes.RESEARCH,
            "transferButton",   Routes.TRANSFER,
            "reportsButton",    Routes.REPORTS,
            "coinsButton",      Routes.MOEDAS,
            "accountButton",    Routes.USER_PROFILE,
            "adminButton",      Routes.ADMIN_DASHBOARD
    );

    @FXML
    private void handleMenuNavigation(ActionEvent event) {
        Button source = (Button) event.getSource();
        String route = routeMap.get(source.getId());
        if (route != null) {
            NavigationHelper.goTo(route, true);
        } else {
            System.err.println("ID de botão desconhecido: " + source.getId());
        }
    }

    // ================= LOGOUT =================
    @FXML
    private void handleLogOut(ActionEvent event) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Tem certeza que deseja sair?",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        alert.setTitle("Confirmação de Logout");

        try (InputStream iconStream = getClass().getResourceAsStream("/icons/moedas.png")) {
            if (iconStream != null) {
                ((Stage) alert.getDialogPane().getScene().getWindow())
                        .getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {}

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                SessaoAtual.limparSessao();
                NavigationHelper.goTo(Routes.LOGIN, false);
            }
        });
    }

    @FXML
    public void initialize() {
        // Mostrar/esconder botão de admin conforme permissões em SessaoAtual
        boolean shouldShowAdminButton = SessaoAtual.tipo != null &&
                (SessaoAtual.tipo.equalsIgnoreCase("admin") || SessaoAtual.isSuperAdmin);

        if (adminButton != null) {
            adminButton.setVisible(shouldShowAdminButton);
            adminButton.setManaged(shouldShowAdminButton);
        }

        // Exibir primeiro nome no botão Account
        if (accountButton != null &&
                SessaoAtual.nome != null &&
                !SessaoAtual.nome.isEmpty()) {

            String firstName = SessaoAtual.nome.split(" ")[0];
            accountButton.setText(firstName);
        }

        // Debug (pode remover depois)
        System.out.println("Admin button visible: " + shouldShowAdminButton);
    }
}
