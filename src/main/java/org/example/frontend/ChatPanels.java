package org.example.frontend;

import javafx.application.Platform;
import javafx.event.ActionEvent;
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
    @FXML private Button logoutHeaderButton;
    @FXML private Label selectChatPlaceholderLabel;
    @FXML private ScrollPane chatScrollPane;
    @FXML private Label chatStatusLabel;
    @FXML private Button menuLeftHeaderButton;
    @FXML private Button searchHeaderButton;
    @FXML private Button menuHeaderButton;

    private String currentChatPeerStatus = "";
    private int currentActiveChatId = -1;
    private String currentChatType = "PRIVATE";
    private String currentChatUserRole = "USER";
    private final java.util.List<String> allChats = new java.util.ArrayList<>();

    @FXML private Button adminPanelButton;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setActiveController(this);
        NetworkClient.getInstance().sendGetChatsRequest();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterChats(newValue.trim().toLowerCase());
        });

        attachmentButton.setOnAction(event -> handleAttachment());
        sendButton.setOnAction(event -> sendMessage());

        ChatListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                parseAndOpenChat(newValue);
            }
        });

        setupLeftMenuPopup();
        setupFabPopup();
        setupChatCells();
        if ("ADMIN".equalsIgnoreCase(NetworkClient.getInstance().getMyRole())) {
            adminPanelButton.setVisible(true);
            adminPanelButton.setManaged(true);
        }
    }
    @FXML
    private void openAdminPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminPage.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Admin Control Panel");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateRightPanelState(false);
    }

    private void filterChats(String query) {
        if (query.isEmpty()) {
            ChatListView.getItems().setAll(allChats);
            return;
        }

        java.util.List<String> filtered = new java.util.ArrayList<>();
        for (String chatRow : allChats) {
            String displayName = chatRow;
            if (chatRow.contains(":::")) {
                displayName = chatRow.split(":::")[0];
            }
            if (displayName.contains(" (ID:")) {
                displayName = displayName.split(" \\(ID:")[0];
            }
            if (displayName.toLowerCase().contains(query)) {
                filtered.add(chatRow);
            }
        }
        ChatListView.getItems().setAll(filtered);
    }
    private void parseAndOpenChat(String selectedItem) {
        try {
            String cleanItem = selectedItem.trim();
            String chatName = "";

            if (cleanItem.contains(":::")) {
                String[] metadata = cleanItem.split(":::");
                chatName = metadata[0].trim();
                this.currentChatType = metadata[1].trim();

                String[] roleAndId = metadata[2].split(" \\(ID: ");
                this.currentChatUserRole = roleAndId[0].trim();
                this.currentActiveChatId = Integer.parseInt(roleAndId[1].replace(")", "").trim());
            } else if (cleanItem.contains(" (ID: ")) {
                chatName = cleanItem.split(" \\(ID:")[0].trim();
                String idString = cleanItem.substring(cleanItem.lastIndexOf(":") + 1, cleanItem.lastIndexOf(")")).trim();
                this.currentActiveChatId = Integer.parseInt(idString);

                this.currentChatType = "PRIVATE";
                this.currentChatUserRole = "USER";
            }

            this.currentChatPeerStatus = "";
            if (chatName.contains(" | ")) {
                String[] parts = chatName.split(" \\| ");
                chatName = parts[0].trim();
                this.currentChatPeerStatus = parts[1].trim().toUpperCase();
            }

            if (chatContactNameLabel != null) {
                chatContactNameLabel.setText(chatName);
            }

            updateHeaderStatusDisplay();

            if (searchHeaderButton != null) {
                searchHeaderButton.setVisible(false);
                searchHeaderButton.setManaged(false);
            }

            setupHeaderMenu();
            updateRightPanelState(true);

        } catch (Exception e) {
            System.err.println("[UI ERROR] Failed to parse chat metadata: " + e.getMessage());
            this.currentActiveChatId = -1;
        }

        if (messagesVBox != null) {
            messagesVBox.getChildren().clear();
        }

        if (currentActiveChatId != -1) {
            NetworkClient.getInstance().sendGetHistoryRequest(currentActiveChatId);
        }
    }

    private void updateHeaderStatusDisplay() {
        if (chatStatusLabel == null) return;
        if ("PRIVATE".equalsIgnoreCase(currentChatType) && !currentChatPeerStatus.isEmpty()) {
            if ("ONLINE".equals(currentChatPeerStatus)) {
                chatStatusLabel.setText("Online");
                chatStatusLabel.setStyle("-fx-text-fill: #0078FF; -fx-font-size: 12px;");
            } else {
                chatStatusLabel.setText("Offline");
                chatStatusLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
            }
        } else {
            chatStatusLabel.setText("");
        }
    }

    private void updateRightPanelState(boolean isChatSelected) {
        if (selectChatPlaceholderLabel != null) {
            selectChatPlaceholderLabel.setVisible(!isChatSelected);
            selectChatPlaceholderLabel.setManaged(!isChatSelected);
        }

        if (chatHeaderBox != null) {
            chatHeaderBox.setVisible(isChatSelected);
            chatHeaderBox.setManaged(isChatSelected);
        }

        if (chatScrollPane != null) {
            chatScrollPane.setVisible(isChatSelected);
            chatScrollPane.setManaged(isChatSelected);
        }

        if (messageInputField != null && messageInputField.getParent() instanceof HBox inputBar) {
            inputBar.setVisible(isChatSelected);
            inputBar.setManaged(isChatSelected);
        }
        if (!isChatSelected && chatStatusLabel != null) {
            chatStatusLabel.setText("");
        }
    }
    @FXML
    private void handleHeaderLogout() {
        NetworkClient.getInstance().disconnect();
        SceneSwitcher.navigate(logoutHeaderButton, "Login.fxml");
    }

    private void setupHeaderMenu() {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-padding: 5;");

        if ("PRIVATE".equalsIgnoreCase(currentChatType)) {
            MenuItem deleteChatItem = new MenuItem("Delete Chat");
            deleteChatItem.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            deleteChatItem.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete Chat Completely?");
                alert.setHeaderText("Warning: Dangerous action!");
                alert.setContentText("This chat, along with all history, will be permanently deleted for BOTH users. Proceed?");

                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        NetworkClient.getInstance().sendDeleteChatRequest(currentActiveChatId);

                        messagesVBox.getChildren().clear();
                        currentActiveChatId = -1;
                        updateRightPanelState(false);
                    }
                });
            });
            contextMenu.getItems().add(deleteChatItem);
        } else {
            MenuItem groupInfoItem = new MenuItem("Group Members");
            groupInfoItem.setOnAction(e -> NetworkClient.getInstance().sendGetGroupMembersRequest(currentActiveChatId));
            contextMenu.getItems().add(groupInfoItem);

            if ("ADMIN".equalsIgnoreCase(currentChatUserRole)) {
                MenuItem renameGroupItem = new MenuItem("Rename Group");
                renameGroupItem.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog(chatContactNameLabel.getText());
                    dialog.setTitle("Rename Group");
                    dialog.setHeaderText("Enter new name for this chat:");
                    dialog.showAndWait().ifPresent(newName -> {
                        if (!newName.trim().isEmpty()) {
                            NetworkClient.getInstance().sendRenameChatRequest(currentActiveChatId, newName.trim());
                        }
                    });
                });

                MenuItem leaveGroupItem = new MenuItem("Leave Group");
                leaveGroupItem.setStyle("-fx-text-fill: #f59e0b;");
                leaveGroupItem.setOnAction(e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Leave Group");
                    alert.setHeaderText("Confirmation required");
                    alert.setContentText("Are you sure you want to leave this group? If you are the sole administrator, the system will prevent this action.");
                    alert.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) {
                            NetworkClient.getInstance().sendLeaveChatRequest(currentActiveChatId);
                        }
                    });
                });

                MenuItem deleteGroupItem = new MenuItem("Delete Group");
                deleteGroupItem.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                deleteGroupItem.setOnAction(e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Delete Group");
                    alert.setHeaderText("Delete Group Chat?");
                    alert.setContentText("Are you sure you want to permanently delete this group chat for ALL participants?");
                    alert.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) {
                            NetworkClient.getInstance().sendDeleteChatRequest(currentActiveChatId);
                        }
                    });
                });

                contextMenu.getItems().addAll(new SeparatorMenuItem(), renameGroupItem, leaveGroupItem, deleteGroupItem);

            } else {
                MenuItem leaveGroupItem = new MenuItem("Leave Group");
                leaveGroupItem.setStyle("-fx-text-fill: #f59e0b;");
                leaveGroupItem.setOnAction(e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Leave Group");
                    alert.setHeaderText("Confirmation required");
                    alert.setContentText("Are you sure you want to leave this group? If you are the sole administrator, the system will prevent this action.");
                    alert.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) {
                            NetworkClient.getInstance().sendLeaveChatRequest(currentActiveChatId);
                        }
                    });
                });
                contextMenu.getItems().addAll(new SeparatorMenuItem(), leaveGroupItem);
            }
        }
        menuHeaderButton.setOnAction(event -> {
            Bounds bounds = menuHeaderButton.localToScreen(menuHeaderButton.getBoundsInLocal());
            contextMenu.show(menuHeaderButton, bounds.getMinX() - 140, bounds.getMaxY() + 5);
        });
    }

    public void showGroupMembersWindow(String rawMembersData) {
        Platform.runLater(() -> {
            VBox root = new VBox(12);
            root.setStyle("-fx-padding: 20; -fx-background-color: #ffffff;");
            root.setAlignment(Pos.TOP_CENTER);

            int membersCount = 0;
            if (rawMembersData != null && !rawMembersData.trim().isEmpty()) {
                String[] items = rawMembersData.split("\\|\\|\\|");
                membersCount = items.length;
            }

            Label titleLabel = new Label("Group Members (" + membersCount + ")");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1c1c1e;");
            root.getChildren().add(titleLabel);

            java.util.Set<String> currentChatUsernames = new java.util.HashSet<>();
            if (rawMembersData != null && !rawMembersData.trim().isEmpty()) {
                String[] items = rawMembersData.split("\\|\\|\\|");
                for (String item : items) {
                    String[] parts = item.split(":::");
                    if (parts.length >= 2) {
                        currentChatUsernames.add(parts[1].trim().toLowerCase());
                    }
                }
            }

            if ("ADMIN".equalsIgnoreCase(currentChatUserRole)) {
                HBox adminActionsBox = new HBox(10);
                adminActionsBox.setAlignment(Pos.CENTER);

                Button addAdminBtn = new Button("Add Admin");
                addAdminBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                addAdminBtn.setPrefWidth(170);
                addAdminBtn.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Promote to Admin");
                    dialog.setHeaderText("Grant Admin Rights");
                    dialog.setContentText("Enter Username of the participant:");
                    dialog.showAndWait().ifPresent(username -> {
                        String cleanUsername = username.trim();
                        if (!cleanUsername.isEmpty()) {
                            if (!currentChatUsernames.contains(cleanUsername.toLowerCase())) {
                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                alert.setTitle("Validation Error");
                                alert.setHeaderText("User Not in Group");
                                alert.setContentText("You can only promote users who are already participants of this group chat.");
                                alert.showAndWait();
                                return;
                            }
                            NetworkClient.getInstance().sendPromoteToAdminRequest(currentActiveChatId, cleanUsername);
                            addAdminBtn.getScene().getWindow().hide();
                        }
                    });
                });

                Button addMemberBtn = new Button("Add Member");
                addMemberBtn.setStyle("-fx-background-color: #0078FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                addMemberBtn.setPrefWidth(170);
                addMemberBtn.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Add Member");
                    dialog.setHeaderText("Invite New Participant");
                    dialog.setContentText("Enter Username or phone number:");
                    dialog.showAndWait().ifPresent(identifier -> {
                        if (!identifier.trim().isEmpty()) {
                            Message msg = new Message(CommandType.ADD_GROUP_MEMBER, NetworkClient.getInstance().getMyUserId(), currentActiveChatId + ";" + identifier.trim());
                            NetworkClient.getInstance().sendPacket(new org.example.protocol.MessagePacket((byte) 1, System.currentTimeMillis(), msg));
                            addMemberBtn.getScene().getWindow().hide();
                        }
                    });
                });

                adminActionsBox.getChildren().addAll(addAdminBtn, addMemberBtn);
                root.getChildren().add(adminActionsBox);
            }

            ListView<HBox> membersListView = new ListView<>();
            membersListView.setPrefHeight(300);

            if (rawMembersData != null && !rawMembersData.trim().isEmpty()) {
                String[] items = rawMembersData.split("\\|\\|\\|");
                for (String item : items) {
                    String[] parts = item.split(":::");
                    if (parts.length < 3) continue;

                    String uName = parts[1].trim();
                    String uRole = parts[2].trim();

                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label nameLbl = new Label(uName);
                    nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1c1c1e; -fx-font-size: 18px;");
                    Label roleLbl = new Label("[" + uRole + "]");
                    roleLbl.setStyle("-fx-text-fill: " + ("ADMIN".equalsIgnoreCase(uRole) ? "#ef4444" : "#6c757d") + "; -fx-font-size: 12px;");

                    row.getChildren().addAll(nameLbl, roleLbl);

                    if ("ADMIN".equalsIgnoreCase(currentChatUserRole) && "USER".equalsIgnoreCase(uRole)) {
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button removeUserBtn = new Button("Remove");
                        removeUserBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
                        removeUserBtn.setOnAction(e -> {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Remove User");
                            alert.setHeaderText("Remove participant from group");
                            alert.setContentText("Are you sure you want to remove " + uName + " from this chat?");
                            alert.showAndWait().ifPresent(r -> {
                                if (r == ButtonType.OK) {
                                    Message msg = new Message(CommandType.REMOVE_GROUP_MEMBER, NetworkClient.getInstance().getMyUserId(), currentActiveChatId + ";" + uName);
                                    NetworkClient.getInstance().sendPacket(new org.example.protocol.MessagePacket((byte) 1, System.currentTimeMillis(), msg));
                                    removeUserBtn.getScene().getWindow().hide();
                                }
                            });
                        });
                        row.getChildren().addAll(spacer, removeUserBtn);
                    }

                    membersListView.getItems().add(row);
                }
            }

            root.getChildren().add(membersListView);

            Stage stage = new Stage();
            stage.setTitle("Members Panel");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(menuHeaderButton.getScene().getWindow());
            stage.setScene(new Scene(root, 390, 430));
            stage.show();
        });
    }
    public void handleSystemStatus(Message msg) {
        Platform.runLater(() -> {
            if (msg.getCommandType() == CommandType.STATUS_OK) {
                String text = msg.getText();

                if (text != null && text.startsWith("STATUS_UPDATE;")) {
                    String[] tokens = text.split(";");
                    int updatedUserId = Integer.parseInt(tokens[1]);
                    String newStatus = tokens[2];
                    if (currentActiveChatId == updatedUserId && "PRIVATE".equalsIgnoreCase(currentChatType)) {
                        currentChatPeerStatus = newStatus;
                        updateHeaderStatusDisplay();
                    }
                    NetworkClient.getInstance().sendGetChatsRequest();
                    return;
                }
                if ("SILENT_OK".equals(text)) return;

                if ("SUCCESS:USER_ADDED_TO_GROUP".equals(text)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("The user has been successfully added to this group chat.");
                    alert.showAndWait();

                    NetworkClient.getInstance().sendGetGroupMembersRequest(currentActiveChatId);
                    return;
                }

                if ("SUCCESS:GROUP_CREATED".equals(text) || "SUCCESS:REFRESH_CHATS".equals(text)) {
                    if ("SUCCESS:REFRESH_CHATS".equals(text) && currentActiveChatId != -1) {
                        messagesVBox.getChildren().clear();
                        currentActiveChatId = -1;
                        updateRightPanelState(false);
                    }
                    NetworkClient.getInstance().sendGetChatsRequest();
                    return;
                }

                if (text.startsWith("SUCCESS:RENAME;")) {
                    String newName = text.split(";")[1];
                    chatContactNameLabel.setText(newName);
                    NetworkClient.getInstance().sendGetChatsRequest();
                    return;
                }

                if ("SUCCESS:USER_REMOVED".equals(text)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("The participant has been successfully removed from the group.");
                    alert.showAndWait();

                    NetworkClient.getInstance().sendGetGroupMembersRequest(currentActiveChatId);
                    return;
                }

                if (text.contains(":::") && !text.contains(" (ID: ")) {
                    showGroupMembersWindow(text);
                    return;
                }

                if (text != null && !text.trim().isEmpty() && text.contains(" (ID: ")) {
                    allChats.clear();
                    ChatListView.getItems().clear();

                    String[] chats = text.split(";");
                    for (String chatRow : chats) {
                        if (chatRow != null && !chatRow.trim().isEmpty()) {
                            allChats.add(chatRow);
                        }
                    }
                    ChatListView.getItems().setAll(allChats);

                    if (searchField != null && !searchField.getText().trim().isEmpty()) {
                        filterChats(searchField.getText().trim().toLowerCase());
                    }
                }
            }
            else if (msg.getCommandType() == CommandType.STATUS_ERROR) {
                String errorText = msg.getText();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Operation Error");
                alert.setHeaderText("Failed to complete action");

                if ("ERROR:PRIVATE_CHAT_ALREADY_EXISTS".equals(errorText)) {
                    alert.setContentText("A private chat with this user already exists in your chat list.");
                } else if ("ERROR:USER_NOT_FOUND".equals(errorText)) {
                    alert.setContentText("The specified user (Username or Phone) was not found in the system.");
                }
                if ("ERROR:USER_NOT_FOUND".equals(errorText)) {
                    alert.setContentText("The specified user (Username or Phone) was not found in the system.");
                } else if ("ERROR:ALREADY_IN_CHAT".equals(errorText)) {
                    alert.setContentText("This user is already a member of this chat room.");
                } else if ("ERROR:NOT_AN_ADMIN".equals(errorText)) {
                    alert.setContentText("You do not have administrative privileges to perform this action.");
                } else if ("ERROR:SOLE_ADMIN".equals(errorText)) {
                    alert.setAlertType(Alert.AlertType.WARNING);
                    alert.setTitle("Action Denied");
                    alert.setHeaderText("Cannot Leave Group");
                    alert.setContentText("You are the only administrator in this group. You cannot leave until you appoint another administrator or delete the group completely.");
                } else {
                    alert.setContentText("Server returned an error: " + errorText);
                }
                alert.showAndWait();
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

            Button closeButton = new Button("Close");
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

    private void setupLeftMenuPopup() {
        Popup leftMenuPopup = new Popup();
        leftMenuPopup.setAutoHide(true);
        VBox popupMenu = new VBox(5);
        popupMenu.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-padding: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        Button btnInfo = new Button("Info");
        btnInfo.setStyle("-fx-background-color: transparent; -fx-text-fill: #1c1c1e; -fx-font-size: 14px; -fx-cursor: hand; -fx-pref-width: 110; -fx-alignment: CENTER_LEFT;");
        btnInfo.setOnAction(e -> {
            leftMenuPopup.hide();
            openModalWindow("UresInfo.fxml", "My Profile");
        });

        Button btnLogout = new Button("Log Out");
        btnLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-pref-width: 110; -fx-alignment: CENTER_LEFT;");
        btnLogout.setOnAction(e -> {
            leftMenuPopup.hide();
            NetworkClient.getInstance().disconnect();
            SceneSwitcher.navigate(menuLeftHeaderButton, "Login.fxml");
        });

        popupMenu.getChildren().addAll(btnInfo, btnLogout);
        leftMenuPopup.getContent().add(popupMenu);

        menuLeftHeaderButton.setOnAction(event -> {
            Bounds bounds = menuLeftHeaderButton.localToScreen(menuLeftHeaderButton.getBoundsInLocal());
            if (bounds != null) {
                leftMenuPopup.show(menuLeftHeaderButton, bounds.getMinX(), bounds.getMaxY() + 5);
            }
        });
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


        popupMenu.getChildren().addAll(btnNewGroup, btnAddContact);
        fabPopup.getContent().add(popupMenu);

        fabButton.setOnAction(event -> {
            Bounds bounds = fabButton.localToScreen(fabButton.getBoundsInLocal());
            if (bounds != null) {
                fabPopup.show(fabButton, bounds.getMinX() - 140, bounds.getMinY() - 90);
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
                    HBox root = new HBox(10);
                    root.setAlignment(Pos.CENTER_LEFT);
                    root.setStyle("-fx-padding: 6px 12px;");

                    String displayName = item;
                    if (item.contains(":::")) {
                        displayName = item.split(":::")[0];
                    }
                    if (displayName.contains(" (ID:")) {
                        displayName = displayName.split(" \\(ID:")[0];
                    }

                    displayName = displayName.trim();

                    Label nameLabel = new Label();
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1c1c1e; -fx-font-size: 16px;");

                    if (displayName.contains(" | ")) {
                        String[] parts = displayName.split(" \\| ");
                        String realName = parts[0].trim();
                        String statusText = parts[1].trim().toUpperCase();

                        nameLabel.setText(realName);

                        Label statusLabel = new Label(statusText.equalsIgnoreCase("ONLINE") ? "Online" : "Offline");

                        selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                            if (isSelected) {
                                statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0;");
                            } else {
                                statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                                        (statusText.equalsIgnoreCase("ONLINE") ? "#0078FF;" : "#6c757d;"));
                            }
                        });

                        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                                (statusText.equalsIgnoreCase("ONLINE") ? "#0078FF;" : "#6c757d;"));

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        root.getChildren().addAll(nameLabel, spacer, statusLabel);
                    } else {
                        nameLabel.setText(displayName);
                        root.getChildren().add(nameLabel);
                    }

                    setGraphic(root);
                    setText(null);
                }
            }
        });
    }
}