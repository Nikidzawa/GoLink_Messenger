package ru.nikidzawa.golink.GUIPatterns;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;


public class PhoneFieldConfig {

    final static int maxLength = 12;

    public static void setConfig (TextField phone) {
        phone.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > maxLength) {
                phone.setText(oldValue);
            }
        });
        phone.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (!event.getCharacter().matches("[0-9]")) {
                event.consume();
            }
        });
    }
}
