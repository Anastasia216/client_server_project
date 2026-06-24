package org.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.network.NetworkClient;
import org.example.protocol.Message;

public class UserInfoController {
    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private Label errorLabel;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setActiveController(this);
        NetworkClient.getInstance().sendGetProfileRequest();

        cancelButton.setOnAction(e -> closeWindow());

        saveButton.setOnAction(e -> {
            if (!isEditMode) {
                setEditMode(true);
            } else {
                handleSave();
            }
        });
    }

    public void handleSystemStatus(Message msg) {
        Platform.runLater(() -> {
            String text = msg.getText();
            if (text == null) return;

            if (text.startsWith("PROFILE_INFO;")) {
                String[] tokens = text.split(";");
                usernameField.setText(tokens[1]);
                phoneField.setText(tokens[2]);
            }
            else if (text.startsWith("SUCCESS:PROFILE_UPDATED")) {
                String[] tokens = text.split(";");
                NetworkClient.getInstance().setMyUsername(tokens[1]);
                closeWindow();
            }
            else {
                String error = text.replace("ERROR:", "").replace("_", " ");
                errorLabel.setText(error);
                saveButton.setDisable(false);
            }
        });
    }

    private void setEditMode(boolean enable) {
        this.isEditMode = enable;

        usernameField.setEditable(enable);
        phoneField.setEditable(enable);

        if (enable) {
            usernameField.setStyle("-fx-background-radius: 8; -fx-border-color: #0078FF; -fx-background-color: #ffffff; -fx-text-fill: #212529;");
            phoneField.setStyle("-fx-background-radius: 8; -fx-border-color: #0078FF; -fx-background-color: #ffffff; -fx-text-fill: #212529;");

            saveButton.setText("Save Changes");
            saveButton.setStyle("-fx-background-color: #0078FF; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20 0 20;");
        } else {
            usernameField.setStyle("-fx-background-radius: 8; -fx-border-color: #ced4da; -fx-background-color: #e9ecef; -fx-text-fill: #495057;");
            phoneField.setStyle("-fx-background-radius: 8; -fx-border-color: #ced4da; -fx-background-color: #e9ecef; -fx-text-fill: #495057;");

            saveButton.setText("Edit Profile");
            saveButton.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20 0 20;");
        }
    }

    private void handleSave() {
        String username = usernameField.getText().trim();
        String phone = phoneField.getText().trim();

        if (username.isEmpty() || phone.isEmpty()) {
            errorLabel.setText("Fields cannot be empty!");
            return;
        }

        errorLabel.setText("");
        saveButton.setDisable(true);
        NetworkClient.getInstance().sendUpdateProfileRequest(username, phone);
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}