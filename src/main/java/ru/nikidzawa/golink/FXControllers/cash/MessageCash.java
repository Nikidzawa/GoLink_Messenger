package ru.nikidzawa.golink.FXControllers.cash;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.*;
import ru.nikidzawa.golink.store.entities.MessageEntity;

import java.time.format.DateTimeFormatter;

public class MessageCash {
    @Getter
    @Setter
    private MessageEntity message;
    @Getter
    private final HBox GUI;
    private Text messageText;
    private Text date;

    public MessageCash (HBox GUI, MessageEntity message) {
        this.GUI = GUI;
        this.message = message;
        switch (message.getMessageType()) {
            case TEXT -> {
                BorderPane borderPane = (BorderPane) GUI.getChildren().stream().filter(node -> node instanceof BorderPane).findFirst().orElseThrow();
                TextFlow messageFlow = (TextFlow) borderPane.getTop();
                TextFlow dateFlow = (TextFlow) borderPane.getBottom();

                date = (Text) dateFlow.getChildren().stream().filter(node -> node instanceof Text).findFirst().orElseThrow();
                messageText = (Text) messageFlow.getChildren().stream().filter(node -> node instanceof Text).findFirst().orElseThrow();
            }
        }
    }

    public void changeText (String newMessageText) {
        message.setMessage(newMessageText);
        date.setText("изменено " + message.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
        messageText.setText(newMessageText);
    }
}