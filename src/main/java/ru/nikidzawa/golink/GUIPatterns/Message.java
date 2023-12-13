package ru.nikidzawa.golink.GUIPatterns;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Timer;
import java.util.TimerTask;

public class Message {
    public static void create (Image image, String message, VBox menuItem) {
        if (menuItem.getChildren().stream().toList().size() < 4) {
            HBox hBox = new HBox();
            BorderPane borderPane = new BorderPane();
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(30);
            imageView.setFitWidth(30);
            borderPane.setLeft(imageView);
            borderPane.setStyle("-fx-background-color: white;" + "-fx-background-radius: 20px;");

            Text text = new Text(message);
            text.setFill(Paint.valueOf("black"));
            text.setFont(Font.font(18));

            TextFlow textFlow = new TextFlow(text);
            borderPane.setCenter(textFlow);
            hBox.getChildren().add(borderPane);
            menuItem.getChildren().add(hBox);
            Timer destructionTimer = new Timer();
            destructionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        menuItem.getChildren().remove(hBox);
                    });
                }
            }, 4000);
        }
    }
}
