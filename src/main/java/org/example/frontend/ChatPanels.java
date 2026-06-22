package org.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.example.network.NetworkClient;
import org.example.protocol.CommandType;
import org.example.protocol.Message;

import java.io.IOException;

public class ChatPanels {

    @FXML private ListView<String> ChatListView;
    @FXML private HBox chatHeaderBox;
    @FXML private VBox messagesVBox;
    @FXML private TextField searchField;
    @FXML private Label chatContactNameLabel;
    @FXML private TextField messageInputField;
    @FXML private Button fabButton;
    @FXML private Button attachmentButton;
    @FXML private Button sendButton;

    @FXML private Button searchHeaderButton;
    @FXML private Button menuHeaderButton;

    private int currentActiveChatId = -1;
    private String currentChatType = "PRIVATE";
    private String currentChatUserRole = "USER";

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setActiveController(this);
        NetworkClient.getInstance().sendGetChatsRequest();

        searchField.setOnAction(event -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                NetworkClient.getInstance().sendSearchRequest(query);
            } else {
                NetworkClient.getInstance().sendGetChatsRequest();
            }
        });

        attachmentButton.setOnAction(event -> handleAttachment());
        sendButton.setOnAction(event -> sendMessage());

        ChatListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                parseAndOpenChat(newValue);
            }
        });

        setupFabPopup();
        setupChatCells();
    }

    private void parseAndOpenChat(String selectedItem) {
        try {
            String cleanItem = selectedItem.trim();
            if (cleanItem.contains(":::")) {
                String[] metadata = cleanItem.split(":::");
                String chatName = metadata[0].trim();
                this.currentChatType = metadata[1].trim();
                String[] roleAndId = metadata[2].split(" \\(ID: ");
                this.currentChatUserRole = roleAndId[0].trim();
                this.currentActiveChatId = Integer.parseInt(roleAndId[1].replace(")", "").trim());
                chatContactNameLabel.setText(chatName);
            } else if (cleanItem.contains(" (ID: ")) {
                String chatName = cleanItem.split(" \\(ID:")[0].trim();
                String idString = cleanItem.substring(cleanItem.lastIndexOf(":") + 1, cleanItem.lastIndexOf(")")).trim();
                this.currentActiveChatId = Integer.parseInt(idString);

                if (chatName.equalsIgnoreCase("Girlss") || chatName.equalsIgnoreCase("TEST") || chatName.toLowerCase().contains("group")) {
                    this.currentChatType = "GROUP";
                    this.currentChatUserRole = "ADMIN";
                } else {
                    this.currentChatType = "PRIVATE";
                    this.currentChatUserRole = "USER";
                }
                chatContactNameLabel.setText(chatName);
            }

            if (searchHeaderButton != null) {
                searchHeaderButton.setVisible(false);
                searchHeaderButton.setManaged(false);
            }

            setupHeaderMenu();

        } catch (Exception e) {
            System.err.println("[UI ERROR] Failed to parse chat metadata: " + e.getMessage());
            this.currentActiveChatId = -1;
            chatContactNameLabel.setText("Select Chat");
        }

        if (messagesVBox != null) {
            messagesVBox.getChildren().clear();
        }

        if (currentActiveChatId != -1) {
            NetworkClient.getInstance().sendGetHistoryRequest(currentActiveChatId);
        }
    }

    private void setupHeaderMenu() {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-padding: 5;");
        if ("PRIVATE".equalsIgnoreCase(currentChatType)) {
            MenuItem deleteChatItem = new MenuItem("Delete Chat");
            deleteChatItem.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            deleteChatItem.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete Chat");
                alert.setContentText("This action will delete the chat for BOTH users. Proceed?");
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        NetworkClient.getInstance().sendDeleteChatRequest(currentActiveChatId);
                        messagesVBox.getChildren().clear();
                        chatContactNameLabel.setText("Select Chat");
                        currentActiveChatId = -1;
                    }
                });
            });
            contextMenu.getItems().add(deleteChatItem);
        } else {
            MenuItem groupInfoItem = new MenuItem("Group Members & Roles");
            groupInfoItem.setOnAction(e -> NetworkClient.getInstance().sendGetGroupMembersRequest(currentActiveChatId));
            contextMenu.getItems().add(groupInfoItem);

            if ("ADMIN".equalsIgnoreCase(currentChatUserRole)) {
                MenuItem renameGroupItem = new MenuItem("Rename Group");
                renameGroupItem.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog(chatContactNameLabel.getText());
                    dialog.setTitle("Rename Group");
                    dialog.setHeaderText("Enter new name:");
                    dialog.showAndWait().ifPresent(newName -> {
                        if (!newName.trim().isEmpty()) {
                            NetworkClient.getInstance().sendRenameChatRequest(currentActiveChatId, newName.trim());
                        }
                    });
                });

                MenuItem deleteGroupItem = new MenuItem("Delete Group");
                deleteGroupItem.setStyle("-fx-text-fill: #ef4444;");
                deleteGroupItem.setOnAction(e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Delete Group");
                    alert.setContentText("Permanently delete this group chat for all participants?");
                    alert.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) {
                            NetworkClient.getInstance().sendDeleteChatRequest(currentActiveChatId);
                        }
                    });
                });

                contextMenu.getItems().addAll(new SeparatorMenuItem(), renameGroupItem, deleteGroupItem);
            }
        }

        menuHeaderButton.setOnAction(event -> {
            Bounds bounds = menuHeaderButton.localToScreen(menuHeaderButton.getBoundsInLocal());
            contextMenu.show(menuHeaderButton, bounds.getMinX() - 120, bounds.getMaxY() + 5);
        });
    }
    public void showGroupMembersWindow(String rawMembersData) {
        Platform.runLater(() -> {
            VBox root = new VBox(10);
            root.setStyle("-fx-padding: 20; -fx-background-color: #ffffff;");
            root.setAlignment(Pos.TOP_CENTER);

            Label titleLabel = new Label("Group Participants & Roles");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1c1c1e;");
            root.getChildren().add(titleLabel);

            ListView<HBox> membersListView = new ListView<>();
            membersListView.setPrefHeight(320);

            if (rawMembersData != null && !rawMembersData.trim().isEmpty()) {
                String[] items = rawMembersData.split("\\|\\|\\|");
                for (String item : items) {
                    String[] parts = item.split(":::");
                    if (parts.length < 3) continue;
                    int uId = Integer.parseInt(parts[0]);
                    String uName = parts[1];
                    String uRole = parts[2];
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label(uName);
                    nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    Label roleLbl = new Label("[" + uRole + "]");
                    roleLbl.setStyle("-fx-text-fill: " + ("ADMIN".equalsIgnoreCase(uRole) ? "#ef4444" : "#6c757d") + "; -fx-font-size: 12px;");
                    row.getChildren().addAll(nameLbl, roleLbl);
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    row.getChildren().add(spacer);

                    if ("ADMIN".equalsIgnoreCase(currentChatUserRole) && !"ADMIN".equalsIgnoreCase(uRole)) {
                        Button promoteBtn = new Button("⚡ Promote");
                        promoteBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
                        promoteBtn.setOnAction(e -> {
                            NetworkClient.getInstance().sendPromoteToAdminRequest(currentActiveChatId, uId);
                            promoteBtn.setDisable(true);
                        });
                        row.getChildren().add(promoteBtn);
                    }
                    membersListView.getItems().add(row);
                }
            }
            root.getChildren().add(membersListView);
            Stage stage = new Stage();
            stage.setTitle("Members Control Panel");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(menuHeaderButton.getScene().getWindow());
            stage.setScene(new Scene(root, 390, 420));
            stage.show();
        });
    }

    public void handleSystemStatus(Message msg) {
        Platform.runLater(() -> {
            if (msg.getCommandType() == CommandType.STATUS_OK) {
                String text = msg.getText();

                if ("SILENT_OK".equals(text)) return;

                if ("SUCCESS:GROUP_CREATED".equals(text) || "SUCCESS:REFRESH_CHATS".equals(text)) {
                    NetworkClient.getInstance().sendGetChatsRequest();
                    return;
                }

                if (text.startsWith("SUCCESS:RENAME;")) {
                    String newName = text.split(";")[1];
                    chatContactNameLabel.setText(newName);
                    NetworkClient.getInstance().sendGetChatsRequest();
                    return;
                }

                ChatListView.getItems().clear();
                if (text != null && !text.trim().isEmpty()) {
                    String[] chats = text.split(";");
                    for (String chatRow : chats) {
                        if (chatRow != null && !chatRow.trim().isEmpty()) {
                            ChatListView.getItems().add(chatRow);
                        }
                    }
                }
            }
        });
    }

    public void handleHistoryResponse(Message responseMessage) {
        String rawMessages = responseMessage.getText();
        Platform.runLater(() -> {
            if (messagesVBox == null) return;
            messagesVBox.getChildren().clear();

            if (rawMessages != null && !rawMessages.trim().isEmpty() && !rawMessages.startsWith("ERROR")) {
                String[] lines = rawMessages.split("\n");
                for (String line : lines) {
                    if (line == null || !line.contains(":::")) continue;

                    String[] parts = line.split(":::", 2);
                    String senderName = parts[0].trim();
                    String textContent = parts[1].trim();
                    if (textContent.startsWith("FILE_ATTACHMENT:")) {
                        addMessageBubble(senderName, textContent.replace("FILE_ATTACHMENT:", ""), "FILE");
                    } else if (textContent.startsWith("📎 FILE:")) {
                        addMessageBubble(senderName, textContent.replace("📎 FILE:", ""), "FILE");
                    } else if (textContent.startsWith("FILE:")) {
                        addMessageBubble(senderName, textContent.replace("FILE:", ""), "FILE");
                    } else {
                        addMessageBubble(senderName, textContent, "TEXT");
                    }
                }
            }
            scrollMessagesToBottom();
        });
    }

    private void sendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty() || currentActiveChatId == -1) return;
        messageInputField.clear();
        NetworkClient.getInstance().sendTextMessage(currentActiveChatId, text);
    }

    public void handleIncomingMessage(Message msg) {
        Platform.runLater(() -> {
            String[] tokens = msg.getText().split(";", 3);
            if (tokens.length < 3) return;
            int msgChatId = Integer.parseInt(tokens[0]);
            if (msgChatId == currentActiveChatId) {
                addMessageBubble(tokens[1], tokens[2], "TEXT");
                scrollMessagesToBottom();
            }
        });
    }

    public void handleIncomingFile(Message msg) {
        Platform.runLater(() -> {
            String[] tokens = msg.getText().split(";", 4);
            if (tokens.length < 4) return;
            int msgChatId = Integer.parseInt(tokens[0]);
            String senderName = tokens[1].trim();
            String fileName = tokens[2];
            String uniqueName = tokens[3];
            if (msgChatId == currentActiveChatId) {
                addMessageBubble(senderName, fileName + "?" + uniqueName, "FILE");
                scrollMessagesToBottom();
            }
        });
    }

    public void handleIncomingFileResponse(Message msg) {
        Platform.runLater(() -> {
            try {
                String[] parts = msg.getText().split(";", 2);
                if (parts.length < 2) return;

                String originalName = parts[0];
                byte[] fileBytes = java.util.Base64.getDecoder().decode(parts[1]);
                String lowerName = originalName.toLowerCase();

                if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif")) {
                    showLargeImageWindow(originalName, fileBytes);
                } else {
                    saveDocumentViaChooser(originalName, fileBytes);
                }
            } catch (Exception e) {
                System.err.println("[UI ERROR] Failed to process file payload: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void showLargeImageWindow(String originalName, byte[] fileBytes) {
        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(fileBytes);
            Image image = new Image(bais);

            VBox root = new VBox(15);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #1e1e1e; -fx-padding: 20;");

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(650);
            imageView.setFitHeight(450);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            Button saveButton = new Button("Save Image");
            saveButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");

            saveButton.setOnAction(e -> {
                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                fileChooser.setTitle("Save Image As");
                fileChooser.setInitialFileName(originalName);
                java.io.File targetFile = fileChooser.showSaveDialog(saveButton.getScene().getWindow());
                if (targetFile != null) {
                    try {
                        java.nio.file.Files.write(targetFile.toPath(), fileBytes);
                    } catch (IOException ex) { ex.printStackTrace(); }
                }
            });

            Button closeButton = new Button("✕ Close");
            closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-cursor: hand;");
            HBox topBar = new HBox(15, saveButton, closeButton);
            topBar.setAlignment(Pos.CENTER_RIGHT);
            root.getChildren().addAll(topBar, imageView);
            Stage imageStage = new Stage();
            imageStage.setTitle(originalName);
            imageStage.initModality(Modality.APPLICATION_MODAL);
            imageStage.setScene(new Scene(root));
            closeButton.setOnAction(e -> imageStage.close());
            imageStage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveDocumentViaChooser(String originalName, byte[] fileBytes) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save Document");
        fileChooser.setInitialFileName(originalName);
        java.io.File targetFile = fileChooser.showSaveDialog(fabButton.getScene().getWindow());
        if (targetFile != null) {
            try {
                java.nio.file.Files.write(targetFile.toPath(), fileBytes);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
    private void addMessageBubble(String senderName, String text, String type) {
        HBox messageContainer = new HBox();
        VBox bubble = new VBox(5);
        String currentLoggedUser = NetworkClient.getInstance().getMyUsername();
        boolean isMine = senderName.equalsIgnoreCase(currentLoggedUser);
        messageContainer.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubble.setStyle("-fx-background-color: " + (isMine ? "#0078FF" : "#f2f2f7") +
                "; -fx-background-radius: " + (isMine ? "15 15 0 15" : "15 15 15 0") +
                "; -fx-padding: 10 14;");

        if (!isMine && !senderName.equalsIgnoreCase(chatContactNameLabel.getText())) {
            Label nameLabel = new Label(senderName);
            nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8e8e93;");
            bubble.getChildren().add(nameLabel);
        }

        if ("FILE".equals(type) && text.contains("?")) {
            String[] fileParts = text.split("\\?");
            String originalName = fileParts[0];
            String uniqueServerName = fileParts[1];
            String lowerName = originalName.toLowerCase();

            if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif")) {
                try {
                    java.io.File localFile = new java.io.File("uploads", uniqueServerName);
                    if (localFile.exists()) {
                        Image img = new Image(localFile.toURI().toString());
                        ImageView imageView = new ImageView(img);
                        imageView.setFitWidth(180);
                        imageView.setPreserveRatio(true);
                        imageView.setSmooth(true);
                        bubble.getChildren().add(imageView);
                    } else {
                        Label photoIcon = new Label("Photo");
                        photoIcon.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + (isMine ? "white" : "#1c1c1e") + ";");
                        bubble.getChildren().add(photoIcon);
                    }
                } catch (Exception ignored) {}
                Label infoLabel = new Label(originalName + "\nClick to open");
                infoLabel.setStyle("-fx-text-fill: " + (isMine ? "#e0e0e0" : "#555555") + "; -fx-font-size: 12px; -fx-underline: true;");
                bubble.getChildren().add(infoLabel);
            } else {
                Label docLabel = new Label(originalName + "\nClick to download");
                docLabel.setWrapText(true);
                docLabel.setStyle("-fx-text-fill: " + (isMine ? "white" : "#0078FF") + "; -fx-font-size: 14px; -fx-font-weight: bold;");
                bubble.getChildren().add(docLabel);
            }

            bubble.setOnMouseClicked(e -> downloadAndOpenFile(uniqueServerName));
            bubble.setStyle(bubble.getStyle() + " -fx-cursor: hand;");
        } else {
            Label textLabel = new Label(text);
            textLabel.setWrapText(true);
            textLabel.setMaxWidth(450);
            textLabel.setStyle("-fx-text-fill: " + (isMine ? "white" : "#1c1c1e") + "; -fx-font-size: 15px;");
            bubble.getChildren().add(textLabel);
        }

        messageContainer.getChildren().add(bubble);
        messagesVBox.getChildren().add(messageContainer);
    }

    private void downloadAndOpenFile(String uniqueServerName) {
        System.out.println("[UI] Requesting file download from server: " + uniqueServerName);
        NetworkClient.getInstance().sendPacket(
                new org.example.protocol.MessagePacket((byte) 1, System.currentTimeMillis(),
                        new Message(CommandType.DOWNLOAD_FILE, NetworkClient.getInstance().getMyUserId(), uniqueServerName))
        );
    }
    @FXML
    private void handleAttachment() {
        if (currentActiveChatId == -1) return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select File to Send");
        java.io.File file = fileChooser.showOpenDialog(attachmentButton.getScene().getWindow());

        if (file != null) {
            try {
                byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
                System.out.println("[UI] Sending binary file payload: " + file.getName());

                NetworkClient.getInstance().sendFileRequest(currentActiveChatId, file.getName(), fileBytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void scrollMessagesToBottom() {
        if (messagesVBox.getParent() instanceof ScrollPane) {
            ((ScrollPane) messagesVBox.getParent()).setVvalue(1.0);
        }
    }

    private void setupFabPopup() {
        Popup fabPopup = new Popup();
        fabPopup.setAutoHide(true);
        VBox popupMenu = new VBox(5);
        popupMenu.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-padding: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        Button btnNewGroup = new Button("New Group");
        btnNewGroup.setStyle("-fx-background-color: transparent; -fx-text-fill: #1c1c1e; -fx-font-size: 14px; -fx-cursor: hand;");
        btnNewGroup.setOnAction(e -> { fabPopup.hide(); openModalWindow("AddGroup.fxml", "New Group"); });

        Button btnAddContact = new Button("Add Contact");
        btnAddContact.setStyle("-fx-background-color: transparent; -fx-text-fill: #1c1c1e; -fx-font-size: 14px; -fx-cursor: hand;");
        btnAddContact.setOnAction(e -> { fabPopup.hide(); openModalWindow("AddContact.fxml", "New Contact"); });

        Button btnLogout = new Button("Log Out");
        btnLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
        btnLogout.setOnAction(e -> {
            fabPopup.hide();
            NetworkClient.getInstance().disconnect();
            SceneSwitcher.navigate(fabButton, "Login.fxml");
        });

        popupMenu.getChildren().addAll(btnNewGroup, btnAddContact, new Separator(), btnLogout);
        fabPopup.getContent().add(popupMenu);

        fabButton.setOnAction(event -> {
            Bounds bounds = fabButton.localToScreen(fabButton.getBoundsInLocal());
            if (bounds != null) {
                fabPopup.show(fabButton, bounds.getMinX() - 140, bounds.getMinY() - 130);
            }
        });
    }

    private void openModalWindow(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlFile));
            Parent root = loader.load();
            Stage modalStage = new Stage();
            modalStage.setTitle(title);
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.initOwner(fabButton.getScene().getWindow());
            modalStage.setScene(new Scene(root));
            modalStage.showAndWait();
            NetworkClient.getInstance().setActiveController(this);
            NetworkClient.getInstance().sendGetChatsRequest();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void setupChatCells() {
        ChatListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox root = new HBox(15);
                    root.setAlignment(Pos.CENTER_LEFT);
                    Circle avatar = new Circle(22, Color.web("#e2e8f0"));
                    String displayName = item;
                    if (item.contains(":::")) {
                        displayName = item.split(":::")[0];
                    }
                    if (displayName.contains(" (ID:")) {
                        displayName = displayName.split(" \\(ID:")[0];
                    }
                    Label nameLabel = new Label(displayName.trim());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1c1c1e; -fx-font-size: 14px;");
                    root.getChildren().addAll(avatar, nameLabel);
                    setGraphic(root);
                    setText(null);
                }
            }
        });
    }
}