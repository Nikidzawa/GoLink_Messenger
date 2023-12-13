package ru.nikidzawa.golink.GUIPatterns;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Set;

public class WindowTitle {
    private static double xOffset = 0;
    private static double yOffset = 0;
    public static void setBaseCommands(Pane titleBar, Button minimizeButton, Button scaleButton, Button closeButton) {
        minimizeButton.setOnMouseEntered(mouseEvent -> {
            minimizeButton.setStyle("-fx-background-color: GRAY; -fx-text-fill: white");
        });
        minimizeButton.setOnMouseExited(mouseEvent -> {
            minimizeButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0");
        });
        scaleButton.setOnMouseEntered(mouseEvent -> {
            scaleButton.setStyle("-fx-background-color: GRAY; -fx-text-fill: white");
        });
        scaleButton.setOnMouseExited(mouseEvent -> {
            scaleButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0");
        });
        closeButton.setOnMouseEntered(mouseEvent -> {
            closeButton.setStyle("-fx-background-color: red; -fx-text-fill: white");
        });
        closeButton.setOnMouseExited(mouseEvent -> {
            closeButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0");
        });

        closeButton.setOnAction(actionEvent -> {
                Window window = closeButton.getScene().getWindow();
                Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                for (Thread t : threadSet) {
                    if (!t.getName().equals("JavaFX Application Thread")) {
                        t.interrupt();
                    }
                }
                ((Stage) window).close();
                Platform.exit();
        });

        minimizeButton.setOnAction(actionEvent -> {
            Window window = minimizeButton.getScene().getWindow();
            ((Stage) window).setIconified(true);
        });

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            Window window = titleBar.getScene().getWindow();
            (window).setX(event.getScreenX() - xOffset);
            (window).setY(event.getScreenY() - yOffset);
        });
    }
}
