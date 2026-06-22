package org.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.network.NetworkClient;
import org.example.protocol.Message;

import java.util.ArrayList;
import java.util.List;

public class AddGroupController {

    @FXML private TextField groupNameField;
    @FXML private ListView<ContactPreview> contactListView;
    @FXML private Button cancelButton;
    @FXML private Button createButton;

    public static class ContactPreview {
        int id;
        String username;
        boolean isOnline;
        boolean isSelected;

        public ContactPreview(int id, String username, boolean isOnline) {
            this.id = id;
            this.username = username;
            this.isOnline = isOnline;
            this.isSelected = false;
        }
    }

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setActiveController(this);
        contactListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ContactPreview contact, boolean empty) {
                if (empty || contact == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(contact.isSelected);
                    checkBox.setOnAction(e -> contact.isSelected = checkBox.isSelected());

                    Circle avatar = new Circle(18, Color.web("#e2e8f0"));

                    Label nameLabel = new Label(contact.username);
                    nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                    nameLabel.setTextFill(Color.web("#212529"));

                    Label statusLabel = new Label(contact.isOnline ? "Online" : "Offline");
                    statusLabel.setFont(Font.font("System", 12));
                    statusLabel.setTextFill(contact.isOnline ? Color.web("#0078FF") : Color.web("#6c757d"));

                    VBox textContainer = new VBox(2, nameLabel, statusLabel);
                    textContainer.setAlignment(Pos.CENTER_LEFT);

                    HBox root = new HBox(12, checkBox, avatar, textContainer);
                    root.setAlignment(Pos.CENTER_LEFT);
                    root.setStyle("-fx-padding: 8px 5px;");

                    root.setOnMouseClicked(e -> {
                        checkBox.setSelected(!checkBox.isSelected());
                        contact.isSelected = checkBox.isSelected();
                    });

                    setGraphic(root);
                    setText(null);
                }
            }
        });

        NetworkClient.getInstance().sendGetContactsRequest();

        cancelButton.setOnAction(event -> closeWindow());
        createButton.setOnAction(event -> handleCreateGroup());
    }

    public void handleSystemStatus(Message response) {
        Platform.runLater(() -> {
            String rawData = response.getText();
            if (rawData != null && rawData.startsWith("SUCCESS:GROUP_CREATED")) {
                System.out.println("[ADD_GROUP] Group successfully deployed on server.");
                closeWindow();
                return;
            }
            contactListView.getItems().clear();
            if (rawData != null && !rawData.isEmpty()) {
                String[] contacts = rawData.split("\\|\\|\\|");
                for (String c : contacts) {
                    String[] parts = c.split(":::", 3);
                    if (parts.length == 3) {
                        int id = Integer.parseInt(parts[0]);
                        String name = parts[1];
                        boolean isOnline = "ONLINE".equalsIgnoreCase(parts[2]);
                        contactListView.getItems().add(new ContactPreview(id, name, isOnline));
                    }
                }
            }
        });
    }

    @FXML
    private void handleCreateGroup() {
        String groupName = groupNameField.getText().trim();
        if (groupName.isEmpty()) return;

        List<Integer> selectedUserIds = new ArrayList<>();
        for (ContactPreview contact : contactListView.getItems()) {
            if (contact.isSelected) {
                selectedUserIds.add(contact.id);
            }
        }

        if (selectedUserIds.isEmpty()) {
            System.err.println("Choose at least one participant!");
            return;
        }
        createButton.setDisable(true);
        NetworkClient.getInstance().sendCreateGroupRequest(groupName, selectedUserIds);
    }

    @FXML
    private void closeWindow() {
        if (cancelButton != null && cancelButton.getScene() != null) {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }
}