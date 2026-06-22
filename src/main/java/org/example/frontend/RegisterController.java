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

public class RegisterController {
    @FXML private TextField regUsernameField;
    @FXML private TextField regPhoneField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML
    private void handleRegister() {
        String username = regUsernameField.getText();
        String phone = regPhoneField.getText();
        String pass = regPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || phone.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            errorLabel.setText("Please fill in all fields!");
            return;
        }
        if (!pass.equals(confirm)) {
            errorLabel.setText("Passwords do not match!");
            return;
        }
        errorLabel.setText("");
        new Thread(() -> {
            try {
                if (!NetworkClient.getInstance().isConnected()) {
                    NetworkClient.getInstance().connect();
                }
                NetworkClient.getInstance().sendRegisterRequest(username, phone, pass);
                MessagePacket responsePacket = NetworkClient.getInstance().receivePacket();
                CommandType status = responsePacket.getMessage().getCommandType();
                Platform.runLater(() -> {
                    if (status == CommandType.STATUS_OK) {
                        System.out.println("Registration successful, switching to ChatPanels...");
                        SceneSwitcher.navigate(regUsernameField, "ChatPanels.fxml");
                    } else {
                        String[] parts = responsePacket.getMessage().getText().split(":");
                        String errorMessage = parts.length > 1 ? parts[1] : "Unknown error";
                        errorLabel.setText("Registration failed: " + errorMessage);
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
    private void switchToLogin() {
        SceneSwitcher.navigate(regUsernameField, "Login.fxml");
    }
}