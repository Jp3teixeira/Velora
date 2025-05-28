package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import utils.NavigationHelper;

public class NavigationController {

    @FXML private Button homeButton;
    @FXML private Button portfolioButton;
    @FXML private Button marketButton;
    @FXML private Button researchButton;
    @FXML private Button transferButton;
    @FXML private Button reportsButton;
    @FXML private Button coinsButton;
    @FXML private Button accountButton;

    @FXML
    private void handleMenuNavigation(ActionEvent event) {
        Button source = (Button) event.getSource();
        String route = switch (source.getId()) {
            case "homeButton" -> "/view/homepage.fxml";
            case "portfolioButton" -> "/view/portfolio.fxml";
            case "marketButton" -> "/view/market.fxml";
            case "researchButton" -> "/view/research.fxml";
            case "transferButton" -> "/view/transfer.fxml";
            case "reportsButton" -> "/view/reports.fxml";
            case "coinsButton" -> "/view/moeda.fxml";
            case "accountButton" -> "/view/account.fxml";
            default -> null;
        };

        if (route != null) {
            NavigationHelper.goTo(route, true);
        }
    }
}
