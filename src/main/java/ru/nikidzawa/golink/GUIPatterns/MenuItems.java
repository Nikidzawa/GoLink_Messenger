package ru.nikidzawa.golink.GUIPatterns;

import javafx.scene.control.TextField;

public class MenuItems {
    public static void makeInput (TextField input) {
        input.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                input.setStyle("-fx-background-color: #001933; -fx-border-color: blue; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
            } else {
                input.setStyle("-fx-background-color: #001933; -fx-border-color: Gray; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
            }
        });
    }
    public static void makeSearch (TextField searchPanel) {
        searchPanel.setStyle("-fx-background-color: #001933; -fx-border-color: blue; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
        searchPanel.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                searchPanel.setStyle("-fx-background-color: #001933; -fx-border-color: blue; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
            } else {
                searchPanel.setStyle("-fx-background-color: #001933; -fx-border-color: Gray; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
            }
        });
    }
}
