package ru.nikidzawa.golink.services.GUI.TrayIcon.notifications;

import io.github.gleidson28.AvatarType;
import io.github.gleidson28.GNAvatarView;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Objects;

public class Notification extends BorderPane {
    public static int maxSize = 80;
    
    public Notification (Builder builder) {
        this.setPrefHeight(maxSize);
        this.setMaxHeight(maxSize);
        this.setStyle("-fx-background-color: " + builder.backgroundHex);
        this.setLeft(setAvatarConfig(builder));
        this.setCenter(setInformationContent(builder));
    }

    public static class Builder {
        private String title = "";
        private String message = "";
        private Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/logo.png")));
        private String backgroundHex = "#001933";
        private Color textColor = Color.WHITE;

        public Builder setImage (Image image) {
            this.image = image;
            return this;
        }
        public Builder setTitle (String title) {
            this.title = title;
            return this;
        }
        public Builder setMessage (String message) {
            this.message = message;
            return this;
        }
        public Builder setBackgroundColor (String hex) {
            backgroundHex = hex;
            return this;
        }
        public Builder setTextColor (Color textColor) {
            this.textColor = textColor;
            return this;
        }

        public Notification build () {
            return new Notification(this);
        }
    }

    private static BorderPane setInformationContent (Builder builder) {
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(setTitleConfig(builder));
        borderPane.setCenter(setMessageConfig(builder));
        return borderPane;
    }

    private static GNAvatarView setAvatarConfig(Builder builder) {
        GNAvatarView gnAvatarView = new GNAvatarView();
        gnAvatarView.setPrefWidth(60);
        gnAvatarView.setPrefHeight(60);
        gnAvatarView.setImage(builder.image);
        gnAvatarView.setStroke(Paint.valueOf(builder.backgroundHex));
        gnAvatarView.setType(AvatarType.CIRCLE);
        BorderPane.setMargin(gnAvatarView, new Insets(0, 0, 0, 5));
        return gnAvatarView;
    }

    private static TextFlow setMessageConfig(Builder builder) {
        Text message = new Text();
        message.setFont(Font.font(15));
        message.setFill(builder.textColor);
        String finalString = builder.message;
        if (finalString.length() > 160) {
            finalString = finalString.substring(0, 160);
            finalString = STR."\{finalString}...";
        }
        message.setText(finalString);
        TextFlow textFlow = new TextFlow(message);
        BorderPane.setMargin(textFlow, new Insets(0, 10, 0, 12));
        return textFlow;
    }

    private static Text setTitleConfig(Builder builder) {
        Text title = new Text(builder.title);
        title.setFont(Font.font(20));
        title.setFill(builder.textColor);
        BorderPane.setMargin(title, new Insets(2, 0, 0, 10));
        return title;
    }
}
