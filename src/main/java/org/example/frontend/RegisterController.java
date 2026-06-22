package org.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.network.NetworkClient;
import org.example.protocol.CommandType;
import org.example.protocol.Message;

public class RegisterController {
    @FXML private TextField regUsernameField;
    @FXML private TextField regPhoneField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setActiveController(this);
    }

    @FXML
    private void handleRegister() {
        String username = regUsernameField.getText().trim();
        String phone = regPhoneField.getText().trim();
        String pass = regPasswordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();

        if (username.isEmpty() || phone.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            errorLabel.setText("Please fill in all fields!");
            return;
        }
        if (!pass.equals(confirm)) {
            errorLabel.setText("Passwords do not match!");
            return;
        }
        errorLabel.setText("");

        try {
            if (!NetworkClient.getInstance().isConnected()) {
                NetworkClient.getInstance().connect();
            }
            NetworkClient.getInstance().sendRegisterRequest(username, phone, pass);
        } catch (Exception e) {
            errorLabel.setText("Connection error: Server offline.");
            e.printStackTrace();
        }
    }

    public void handleAuthResponse(Message responseMessage) {
        Platform.runLater(() -> {
            if (responseMessage.getCommandType() == CommandType.STATUS_OK) {
                String text = responseMessage.getText();

                if (text.startsWith("SUCCESS;")) {
                    NetworkClient.getInstance().setMyUserId(responseMessage.getUserId());
                    NetworkClient.getInstance().setMyUsername(regUsernameField.getText().trim());

                    System.out.println("[UI] Auto-login bypass successful. Moving straight to ChatPanels!");
                    SceneSwitcher.navigate(regUsernameField, "ChatPanels.fxml");
                } else {
                    System.out.println("[UI] Registration successful! Triggering silent background login...");
                    NetworkClient.getInstance().sendLoginRequest(
                            regUsernameField.getText().trim(),
                            regPasswordField.getText().trim()
                    );
                }
            } else {
                String errorText = responseMessage.getText();
                errorLabel.setText(errorText.replace("ERROR:", "").replace("_", " "));
            }
        });
    }

    @FXML
    private void switchToLogin() {
        SceneSwitcher.navigate(regUsernameField, "Login.fxml");
    }
}