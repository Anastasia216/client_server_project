package org.example.frontend;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.network.NetworkClient;
import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;

import java.io.IOException;

public class AdminPage {

    @FXML private Label onlineCountLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalMessagesLabel;

    @FXML private TableView<UserTableRow> usersTable;
    @FXML private TableColumn<UserTableRow, String> colId;
    @FXML private TableColumn<UserTableRow, String> colStatus;
    @FXML private TableColumn<UserTableRow, String> colRole;
    @FXML private TableColumn<UserTableRow, String> colBlock;
    @FXML private TableColumn<UserTableRow, Void> colActions;

    @FXML private TextArea serverLogsArea;
    @FXML private Button clearLogButton;
    @FXML private Button exitButton;

    private ObservableList<UserTableRow> usersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setActiveController(this);

        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colRole.setCellValueFactory(cellData -> cellData.getValue().roleProperty());
        colBlock.setCellValueFactory(cellData -> cellData.getValue().blockProperty());

        setupActionsColumn();

        usersTable.setItems(usersList);

        requestStats();
        requestUsers();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button roleBtn = new Button("Change Role");
            private final Button blockBtn = new Button("Block");
            private final HBox pane = new HBox(10, roleBtn, blockBtn);

            {
                pane.setAlignment(Pos.CENTER);

                String btnStyle = "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-text-fill: #212529; -fx-cursor: hand; -fx-background-radius: 4; -fx-border-radius: 4; -fx-font-size: 12px;";
                roleBtn.setStyle(btnStyle);
                blockBtn.setStyle(btnStyle);

                roleBtn.setOnAction(event -> {
                    UserTableRow row = getTableView().getItems().get(getIndex());
                    if (row != null) {
                        String currentRole = row.getRole() != null ? row.getRole().trim() : "";
                        String newRole = currentRole.equalsIgnoreCase("ADMIN") ? "USER" : "ADMIN";

                        row.roleProperty().set(newRole);

                        Message msg = new Message(CommandType.ADMIN_ACTION_ROLE, NetworkClient.getInstance().getMyUserId(), row.getId() + ";" + newRole);
                        NetworkClient.getInstance().sendPacket(new MessagePacket((byte) 1, System.currentTimeMillis(), msg));
                    }
                });

                blockBtn.setOnAction(event -> {
                    UserTableRow row = getTableView().getItems().get(getIndex());
                    if (row != null) {
                        String currentBlock = row.getBlockStatus() != null ? row.getBlockStatus().trim() : "false";
                        boolean isBlocked = currentBlock.equalsIgnoreCase("true") || currentBlock.equals("1");
                        String newBlockStatus = isBlocked ? "false" : "true";
                        row.blockProperty().set(newBlockStatus);
                        Message msg = new Message(CommandType.ADMIN_ACTION_BLOCK, NetworkClient.getInstance().getMyUserId(), row.getId() + ";" + newBlockStatus);
                        NetworkClient.getInstance().sendPacket(new MessagePacket((byte) 1, System.currentTimeMillis(), msg));
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML
    private void handleExit() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ChatPanels.fxml"));
            Stage stage = (Stage) exitButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearLogs() {
        if(serverLogsArea != null) {
            serverLogsArea.clear();
        }
    }

    private void requestStats() {
        Message msg = new Message(CommandType.GET_ADMIN_STATS, NetworkClient.getInstance().getMyUserId(), "");
        NetworkClient.getInstance().sendPacket(new MessagePacket((byte) 1, System.currentTimeMillis(), msg));
    }

    private void requestUsers() {
        Message msg = new Message(CommandType.GET_ALL_USERS, NetworkClient.getInstance().getMyUserId(), "");
        NetworkClient.getInstance().sendPacket(new MessagePacket((byte) 1, System.currentTimeMillis(), msg));
    }

    public void handleAdminResponse(Message message) {
        Platform.runLater(() -> {
            switch (message.getCommandType()) {
                case GET_ADMIN_STATS -> {
                    String[] stats = message.getText().split(";");
                    if (stats.length == 3) {
                        onlineCountLabel.setText("Users Online: " + stats[0]);
                        totalUsersLabel.setText("Total Registered: " + stats[1]);
                        totalMessagesLabel.setText("Total Messages: " + stats[2]);
                    }
                }
                case GET_ALL_USERS -> {
                    usersList.clear();
                    String[] users = message.getText().split(";");
                    for (String u : users) {
                        if (u.isEmpty()) continue;
                        String[] data = u.split(",");
                        if (data.length == 4) {
                            usersList.add(new UserTableRow(data[0], data[1], data[2], data[3]));
                        }
                    }
                }
            }
        });
    }

    public static class UserTableRow {
        private final SimpleStringProperty id, status, role, blockStatus;
        public UserTableRow(String id, String status, String role, String blockStatus) {
            this.id = new SimpleStringProperty(id);
            this.status = new SimpleStringProperty(status);
            this.role = new SimpleStringProperty(role);
            this.blockStatus = new SimpleStringProperty(blockStatus);
        }
        public String getId() { return id.get(); }
        public String getRole() { return role.get(); }
        public String getBlockStatus() { return blockStatus.get(); }
        public SimpleStringProperty idProperty() { return id; }
        public SimpleStringProperty statusProperty() { return status; }
        public SimpleStringProperty roleProperty() { return role; }
        public SimpleStringProperty blockProperty() { return blockStatus; }
    }
}