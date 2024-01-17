package ru.nikidzawa.golink.FXControllers.cash;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.Getter;
import ru.nikidzawa.golink.FXControllers.helpers.GUIPatterns;
import ru.nikidzawa.golink.store.entities.MessageEntity;

import java.time.format.DateTimeFormatter;

public class MessageCash {
    @Getter
    private MessageEntity message;
    @Getter
    private final HBox messageBackground;
    @Getter
    private final BorderPane GUI;
    private Text messageText;
    private ImageView imageView;
    private Text date;

    public MessageCash(HBox messageBackground, BorderPane GUI, MessageEntity message, ImageView imageView, Text messageText, Text date) {
        this.messageBackground = messageBackground;
        this.GUI = GUI;
        this.message = message;
        this.imageView = imageView;
        this.messageText = messageText;
        this.date = date;
    }

    public MessageCash(HBox messageBackground, BorderPane GUI, MessageEntity message) {
        this.messageBackground = messageBackground;
        this.GUI = GUI;
        this.message = message;
    }

    public Image getImage() {
        return imageView.getImage();
    }

    public void setImage(byte[] imageBytes) {
        message.setMetadata(imageBytes);
        GUIPatterns.setImageConfig(imageBytes, imageView, GUI);
    }

    public void changeText(String newMessageText) {
        Platform.runLater(() -> {
            message.setText(newMessageText);
            date.setText("изменено " + message.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
            if (newMessageText.isEmpty()) {
                GUI.setCenter(null);
            } else if (messageText.getText().isEmpty()) {
                TextFlow textFlow = new TextFlow(messageText);
                textFlow.setStyle("-fx-font-family: Arial; -fx-font-size: 14px;");
                textFlow.setPadding(new Insets(5, 10, 3, 10));
                GUI.setCenter(textFlow);
            }
            messageText.setText(newMessageText);
        });
    }
}