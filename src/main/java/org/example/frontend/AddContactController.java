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
import org.example.protocol.MessagePacket;

public class AddContactController {

    @FXML private TextField searchUserField;
    @FXML private Label statusLabel;
    @FXML private Button cancelButton;
    @FXML private Button addButton;

    @FXML
    public void initialize() {
        cancelButton.setOnAction(event -> closeWindow());
        addButton.setOnAction(event -> handleAddContact());
    }

    private void handleAddContact() {
        String identifier = searchUserField.getText().trim();
        if (identifier.isEmpty()) {
            statusLabel.setText("Please enter a username or phone.");
            return;
        }

        // Блокуємо кнопку, щоб уникнути подвійних кліків
        addButton.setDisable(true);
        statusLabel.setText("Searching...");

        new Thread(() -> {
            try {
                // Використовуємо новий зручний метод!
                NetworkClient.getInstance().sendSearchRequest(identifier);

                // Очікуємо відповідь
                MessagePacket response = NetworkClient.getInstance().receivePacket();

                Platform.runLater(() -> {
                    addButton.setDisable(false);
                    if (response.getMessage().getCommandType() == CommandType.STATUS_OK) {
                        String foundUsername = response.getMessage().getText();
                        System.out.println("Contact found: " + foundUsername);
                        // Якщо юзер реально знайдений, закриваємо вікно!
                        closeWindow();
                    } else {
                        // Якщо помилка - виводимо РЕАЛЬНИЙ текст від сервера (наприклад ERROR:USER_NOT_FOUND)
                        String errorText = response.getMessage().getText();
                        statusLabel.setText(errorText.replace("ERROR:", "").replace("_", " "));
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    addButton.setDisable(false);
                    statusLabel.setText("Network error. Server offline?");
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void closeWindow() {
        try {
            if (cancelButton != null && cancelButton.getScene() != null) {
                Stage stage = (Stage) cancelButton.getScene().getWindow();
                stage.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}