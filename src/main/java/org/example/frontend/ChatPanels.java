package org.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.network.NetworkClient;
import org.example.protocol.CommandType;
import org.example.protocol.MessagePacket;

import java.io.IOException;

public class ChatPanels {

    @FXML private VBox ChatControlsBox;
    @FXML private ListView<String> ChatListView;
    @FXML private HBox chatHeaderBox;
    @FXML private VBox messagesVBox;

    @FXML private TextField searchField;
    @FXML private Label chatContactNameLabel;
    @FXML private TextField messageInputField;

    @FXML private Button fabButton;
    @FXML private Button attachmentButton;
    @FXML private Button sendButton;

    private int currentActiveChatId = -1;

    @FXML
    public void initialize() {
        new Thread(this::fetchUserChats).start();

        searchField.setOnAction(event -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                handleSearch(query);
            } else {
                new Thread(this::fetchUserChats).start();
            }
        });

        attachmentButton.setOnAction(event -> handleAttachment());
        sendButton.setOnAction(event -> sendMessage());

        ChatListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                openChat(newValue);
            }
        });

        Popup fabPopup = new Popup();
        fabPopup.setAutoHide(true);

        VBox popupMenu = new VBox();
        popupMenu.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 5); -fx-padding: 8;");

        Button btnNewGroup = createMenuButton("👥 New Group");
        btnNewGroup.setOnAction(e -> { fabPopup.hide(); handleCreateGroup(); });

        Button btnNewContact = createMenuButton("👤 New Contact");
        btnNewContact.setOnAction(e -> { fabPopup.hide(); handleAddContact(); });

        popupMenu.getChildren().addAll(btnNewGroup, btnNewContact);
        fabPopup.getContent().add(popupMenu);

        fabButton.setOnAction(event -> {
            Bounds bounds = fabButton.localToScreen(fabButton.getBoundsInLocal());
            if (bounds != null) {
                fabPopup.show(fabButton, bounds.getMinX() - 110, bounds.getMinY() - 100);
            }
        });

        ChatListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox root = new HBox(15);
                    root.setAlignment(Pos.CENTER_LEFT);

                    Circle avatar = new Circle(22, Color.web("#e2e8f0"));

                    Label nameLabel = new Label(item);
                    nameLabel.setStyle("-fx-text-fill: #1c1c1e; -fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-weight: bold; -fx-font-size: 16px; -fx-font-smoothing-type: lcd; -fx-padding: 2 0 0 0;");

                    root.getChildren().addAll(avatar, nameLabel);

                    setText(null);
                    setGraphic(root);

                    setStyle("-fx-background-color: transparent; -fx-padding: 8px 15px; -fx-background-radius: 10; -fx-min-height: 55px;");

                    root.setOnMouseEntered(e -> {
                        if (!isSelected()) setStyle("-fx-background-color: #f2f2f7; -fx-padding: 8px 15px; -fx-background-radius: 10; -fx-cursor: hand; -fx-min-height: 55px;");
                    });
                    root.setOnMouseExited(e -> {
                        if (!isSelected()) setStyle("-fx-background-color: transparent; -fx-padding: 8px 15px; -fx-background-radius: 10; -fx-cursor: hand; -fx-min-height: 55px;");
                    });
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (selected && !isEmpty()) {
                    setStyle("-fx-background-color: #e5e5ea; -fx-padding: 8px 15px; -fx-background-radius: 10; -fx-min-height: 55px;");
                } else if (!isEmpty()) {
                    setStyle("-fx-background-color: transparent; -fx-padding: 8px 15px; -fx-background-radius: 10; -fx-min-height: 55px;");
                }
            }
        });
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(160);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1c1c1e; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 10 15; -fx-cursor: hand; -fx-background-radius: 8;");

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #f2f2f7; -fx-text-fill: #1c1c1e; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 10 15; -fx-cursor: hand; -fx-background-radius: 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1c1c1e; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 10 15; -fx-cursor: hand; -fx-background-radius: 8;"));
        return btn;
    }

    private void openChat(String chatName) {
        chatContactNameLabel.setText(chatName);
        messagesVBox.getChildren().clear();
        currentActiveChatId = -1;

        new Thread(() -> {
            try {
                NetworkClient.getInstance().sendGetChatHistoryRequest(chatName);
                MessagePacket response = NetworkClient.getInstance().receivePacket();
                if (response.getMessage().getCommandType() == CommandType.STATUS_OK) {
                    currentActiveChatId = response.getMessage().getUserId();
                    String rawMessages = response.getMessage().getText();
                    Platform.runLater(() -> {
                        if (rawMessages != null && !rawMessages.trim().isEmpty()) {
                            String[] msgs = rawMessages.split("\\|\\|\\|");

                            for (String m : msgs) {
                                String[] parts = m.split(":::", 4);
                                if (parts.length == 4) {
                                    String sender = parts[0];
                                    String type = parts[1];
                                    long messageId = Long.parseLong(parts[2]);
                                    String text = parts[3];
                                    addMessageBubble(sender, text, type, messageId);
                                } else if (parts.length == 2) {
                                    addMessageBubble(parts[0], parts[1], "TEXT", -1);
                                }
                            }
                        }
                        if (messagesVBox.getParent() instanceof ScrollPane) {
                            ((ScrollPane) messagesVBox.getParent()).setVvalue(1.0);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addMessageBubble(String sender, String text, String type, long messageId) {
        boolean isMine = sender.equals("MINE");
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        VBox bubble = new VBox(3);
        bubble.setStyle("-fx-background-color: " + (isMine ? "#0078FF" : "#f2f2f7") +
                "; -fx-background-radius: " + (isMine ? "15 15 0 15" : "15 15 15 0") +
                "; -fx-padding: 10 14; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        if ("FILE".equals(type) && messageId != -1) {
            bubble.setStyle(bubble.getStyle() + " -fx-cursor: hand;");
            bubble.setOnMouseClicked(e -> downloadAndOpenFile(messageId));
            text = "📥 " + text;
        }
        if (!isMine) {
            Label nameLabel = new Label(sender);
            nameLabel.setStyle("-fx-text-fill: #0078FF; -fx-font-size: 12px; -fx-font-weight: bold;");
            bubble.getChildren().add(nameLabel);
        }
        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(450);
        textLabel.setStyle("-fx-text-fill: " + (isMine ? "white" : "#1c1c1e") + "; -fx-font-size: 15px;");

        bubble.getChildren().add(textLabel);
        messageContainer.getChildren().add(bubble);

        messagesVBox.getChildren().add(messageContainer);
    }
    private void downloadAndOpenFile(long messageId) {
        new Thread(() -> {
            try {
                NetworkClient.getInstance().sendDownloadFileRequest(messageId);
                MessagePacket response = NetworkClient.getInstance().receivePacket();
                if (response.getMessage().getCommandType() == CommandType.STATUS_OK) {
                    String[] data = response.getMessage().getText().split(";", 2);
                    if (data.length == 2) {
                        String fileName = data[0];
                        String base64 = data[1];
                        byte[] fileBytes = java.util.Base64.getDecoder().decode(base64);
                        java.io.File downloadDir = new java.io.File(System.getProperty("user.home"), "Downloads");
                        if (!downloadDir.exists()) downloadDir.mkdir();
                        java.io.File file = new java.io.File(downloadDir, "chat_" + fileName);
                        java.nio.file.Files.write(file.toPath(), fileBytes);
                        Platform.runLater(() -> {
                            try {
                                java.awt.Desktop.getDesktop().open(file);
                            } catch (Exception ex) {
                                System.err.println("Failed to open the file automatically.");
                            }
                        });
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void handleSearch(String query) {
        new Thread(() -> {
            try {
                NetworkClient.getInstance().sendSearchRequest(query);
                MessagePacket response = NetworkClient.getInstance().receivePacket();
                if (response.getMessage().getCommandType() == CommandType.STATUS_OK) {
                    String[] results = response.getMessage().getText().split(";");
                    Platform.runLater(() -> {
                        ChatListView.getItems().clear();
                        ChatListView.getItems().addAll(results);
                    });
                } else {
                    Platform.runLater(() -> ChatListView.getItems().clear());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty() || currentActiveChatId == -1) return;
        messageInputField.clear();
        addMessageBubble("MINE", text, "TEXT", -1);
        if (messagesVBox.getParent() instanceof ScrollPane) {
            ((ScrollPane) messagesVBox.getParent()).setVvalue(1.0);
        }
        new Thread(() -> {
            try {
                NetworkClient.getInstance().sendTextMessage(currentActiveChatId, 0, text, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchUserChats() {
        try {
            NetworkClient.getInstance().sendGetChatsRequest();
            MessagePacket response = NetworkClient.getInstance().receivePacket();

            if (response.getMessage().getCommandType() == CommandType.STATUS_OK) {
                String text = response.getMessage().getText();
                Platform.runLater(() -> {
                    ChatListView.getItems().clear();
                    if (text != null && !text.trim().isEmpty()) {
                        String[] chats = text.split(";");
                        ChatListView.getItems().addAll(chats);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateGroup() {
        openDialog("/AddGroup.fxml", "New Group");
    }

    @FXML
    private void handleAddContact() {
        openDialog("/AddContact.fxml", "New Contact");
    }

    private void openDialog(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            Stage mainStage = (Stage) fabButton.getScene().getWindow();
            if (mainStage != null) {
                dialogStage.initOwner(mainStage);
            }
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);
            if (mainStage != null) {
                dialogStage.setX(mainStage.getX());
                dialogStage.setY(mainStage.getY());
                dialogStage.setWidth(mainStage.getWidth());
                dialogStage.setHeight(mainStage.getHeight());
            }
            dialogStage.showAndWait();
            new Thread(this::fetchUserChats).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAttachment() {
        if (currentActiveChatId == -1) return;
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choose a file to send");
        java.io.File file = fileChooser.showOpenDialog(attachmentButton.getScene().getWindow());
        if (file != null) {
            if (file.length() > 10 * 1024 * 1024) {
                System.err.println("The file is too large! The limit is 10 MB.");
                return;
            }

            try {
                byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
                String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);

                addMessageBubble("MINE", "📎 File: " + file.getName(), "FILE", -1);
                if (messagesVBox.getParent() instanceof ScrollPane) {
                    ((ScrollPane) messagesVBox.getParent()).setVvalue(1.0);
                }

                new Thread(() -> {
                    NetworkClient.getInstance().sendFileMessage(currentActiveChatId, file.getName(), base64Data);
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}