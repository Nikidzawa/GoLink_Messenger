package ru.nikidzawa.golink.FXControllers.cash;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import lombok.*;
import ru.nikidzawa.golink.FXControllers.helpers.GUIPatterns;
import ru.nikidzawa.golink.store.entities.MessageEntity;

import java.io.ByteArrayInputStream;
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

    public MessageCash (HBox messageBackground, BorderPane GUI, MessageEntity message, ImageView imageView, Text messageText, Text date) {
        this.messageBackground = messageBackground;
        this.GUI = GUI;
        this.message = message;
        this.imageView = imageView;
        this.messageText = messageText;
        this.date = date;
    }

    public MessageCash (HBox messageBackground, BorderPane GUI, MessageEntity message) {
        this.messageBackground = messageBackground;
        this.GUI = GUI;
        this.message = message;
    }

    public Image getImage () {return imageView.getImage();}

    public void setImage (byte[] imageBytes) {
        message.setMetadata(imageBytes);
        GUIPatterns.setImageConfig(imageBytes, imageView, GUI);
    }

    public void changeText (String newMessageText) {
        message.setText(newMessageText);
        date.setText("изменено " + message.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
        messageText.setText(newMessageText);
    }
}