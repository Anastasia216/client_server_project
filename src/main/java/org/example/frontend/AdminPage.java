package org.example.frontend;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

public class AdminPage {

    @FXML
    private Label onlineCountLabel;

    @FXML
    private Label totalUsersLabel;

    @FXML
    private Label totalMessagesLabel;

    @FXML
    private TableView<?> usersTable;

    @FXML
    private Button changeRoleButton;

    @FXML
    private Button blockUserButton;

    @FXML
    private Button clearLogButton;

    @FXML
    private Button downloadLogButton;

    @FXML
    private TextArea serverLogsArea;

    @FXML
    public void initialize() {
        onlineCountLabel.setText("Користувачів онлайн: 42");
        totalUsersLabel.setText("Зареєстровано користувачів: 1050");
        totalMessagesLabel.setText("Загальна кількість повідомлень: 8400");
        serverLogsArea.setText("=== Логи сервера ===\n[15:30] Сервер запущено.\n[15:31] Підключено до бази даних.");
    }
}
