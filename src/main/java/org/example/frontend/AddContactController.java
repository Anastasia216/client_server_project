package org.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.network.NetworkClient;
import org.example.protocol.CommandType;
import org.example.protocol.Message;

public class AddContactController {

    @FXML private TextField searchUserField;
    @FXML private Label statusLabel;
    @FXML private Button cancelButton;
    @FXML private Button addButton;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setActiveController(this);
        cancelButton.setOnAction(event -> closeWindow());
        addButton.setOnAction(event -> handleAddContact());
    }

    private void handleAddContact() {
        String identifier = searchUserField.getText().trim();
        if (identifier.isEmpty()) {
            statusLabel.setText("Please enter a username or phone.");
            return;
        }
        addButton.setDisable(true);
        statusLabel.setText("Searching...");
        NetworkClient.getInstance().sendSearchRequest(identifier);
    }

    public void handleSystemStatus(Message response) {
        Platform.runLater(() -> {
            addButton.setDisable(false);
            if (response.getCommandType() == CommandType.STATUS_OK) {
                System.out.println("[ADD_CONTACT] Chat created successfully. Synchronizing view.");
                closeWindow();
            } else {
                String errorText = response.getText();
                statusLabel.setText(errorText.replace("ERROR:", "").replace("_", " "));
            }
        });
    }

    @FXML
    private void closeWindow() {
        if (cancelButton != null && cancelButton.getScene() != null) {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }
}