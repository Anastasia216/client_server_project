package org.example.frontend;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import java.io.IOException;

public class SceneSwitcher {
    public static void navigate(Control control, String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource("/" + fxmlFileName));
            Parent newRoot = loader.load();

            Scene currentScene = control.getScene();
            currentScene.setRoot(newRoot);

        } catch (IOException e) {
            System.err.println("Could not load the FXML file: " + fxmlFileName);
            e.printStackTrace();
        }
    }
}