package org.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.network.NetworkClient;
import org.example.protocol.CommandType;
import org.example.protocol.Message;

public class LoginController {
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setActiveController(this);
    }

    @FXML
    private void handleSignIn() {
        String username = loginField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields!");
            return;
        }
        errorLabel.setText("");

        try {
            if (!NetworkClient.getInstance().isConnected()) {
                NetworkClient.getInstance().connect();
            }
            NetworkClient.getInstance().sendLoginRequest(username, password);
        } catch (Exception e) {
            errorLabel.setText("Connection error: Server is offline.");
            e.printStackTrace();
        }
    }

    public void handleAuthResponse(Message responseMessage) {
        Platform.runLater(() -> {
            if (responseMessage.getCommandType() == CommandType.STATUS_OK) {
                NetworkClient.getInstance().setMyUserId(responseMessage.getUserId());

                String responseText = responseMessage.getText();
                if (responseText != null && responseText.contains(";")) {
                    String[] parts = responseText.split(";");
                    if (parts.length > 1) {
                        NetworkClient.getInstance().setMyRole(parts[1]);
                        System.out.println("[UI] Login successful. Assigned User ID: " + responseMessage.getUserId() + ", Role: " + parts[1]);
                    }
                } else {
                    System.out.println("[UI] Login successful. Assigned User ID: " + responseMessage.getUserId());
                }
                SceneSwitcher.navigate(loginField, "ChatPanels.fxml");
            } else {
                String errorText = responseMessage.getText();
                errorLabel.setText(errorText.replace("ERROR:", "").replace("_", " "));
            }
        });
    }

    @FXML
    private void switchToRegister() {
        SceneSwitcher.navigate(loginField, "Register.fxml");
    }
}