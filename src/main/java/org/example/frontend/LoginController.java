package org.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.network.NetworkClient;
import org.example.protocol.CommandType;
import org.example.protocol.MessagePacket;

import java.io.IOException;

public class LoginController {
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML
    private void handleSignIn() {
        String username = loginField.getText();
        String password = passwordField.getText();
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorLabel.setText("Please fill in all fields!");
            return;
        }
        errorLabel.setText("");
        new Thread(() -> {
            try {
                if (!NetworkClient.getInstance().isConnected()) {
                    NetworkClient.getInstance().connect();
                }
                NetworkClient.getInstance().sendLoginRequest(username, password);
                MessagePacket responsePacket = NetworkClient.getInstance().receivePacket();
                CommandType status = responsePacket.getMessage().getCommandType();
                Platform.runLater(() -> {
                    if (status == CommandType.STATUS_OK) {
                        System.out.println("Login successful, switching to ChatPanels...");
                        SceneSwitcher.navigate(loginField, "ChatPanels.fxml");
                    } else {
                        String[] parts = responsePacket.getMessage().getText().split(":");
                        String errorMessage = parts.length > 1 ? parts[1] : "Invalid credentials";
                        errorLabel.setText("Login failed: " + errorMessage);
                    }
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    errorLabel.setText("Connection error: Server is not responding.");
                });
                e.printStackTrace();
            }
        }).start();
    }
    @FXML
    private void switchToRegister() {
        SceneSwitcher.navigate(loginField, "Register.fxml");
    }
}