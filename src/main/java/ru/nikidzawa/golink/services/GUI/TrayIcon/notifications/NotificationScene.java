package ru.nikidzawa.golink.services.GUI.TrayIcon.notifications;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import ru.nikidzawa.golink.services.GUI.EmptyStage;


public class NotificationScene {
    private final VBox notificationField;
    private final Stage stage;
    int maxMessageCount = 3;
    int sceneWidth;
    int sceneHeight;
    int spacing = 10;
    Button button;

    public NotificationScene () {
        Rectangle2D rectangle2D = Screen.getPrimary().getBounds();
        sceneWidth = (int) rectangle2D.getWidth() / 8;
        sceneHeight = 40;
        int positionX = (int) (rectangle2D.getWidth() - sceneWidth);
        int positionY = (int) rectangle2D.getMaxY() - 85;

        button = new Button("Убрать уведомления");
        button.setOnMouseClicked(_ -> removeNotifications(positionX, positionY));
        button.setPrefWidth(sceneWidth);
        button.setPrefHeight(sceneHeight);
        button.setStyle("-fx-background-color: #001933; -fx-text-fill: white;");
        button.setCursor(Cursor.HAND);
        notificationField = new VBox();
        notificationField.setSpacing(spacing);
        BorderPane root = new BorderPane();
        root.setTop(button);
        root.setCenter(notificationField);
        root.setStyle("-fx-background-color: transparent;");
        root.setPrefHeight(sceneHeight);
        root.setMinWidth(sceneWidth);

        Scene scene;
        stage = EmptyStage.getEmptyStageAndSetScene(scene = new Scene(root));
        stage.initStyle(StageStyle.TRANSPARENT);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setX(positionX);
        stage.setY(positionY);

        BorderPane.setMargin(button, new Insets(0, 0, 10, 0));
        button.setVisible(false);
        stage.show();
    }

    private void removeNotifications(int posX, int posY) {
        button.setVisible(false);
        stage.setX(posX);
        stage.setY(posY);
        notificationField.getChildren().clear();
    }

    public void addNotification (Notification notification) {
        Platform.runLater(() -> {
            button.setVisible(true);
            int maxNotificationHeight = Notification.maxSize;
            TranslateTransition transitionIn = new TranslateTransition(Duration.seconds(0.20), notification);
            transitionIn.setFromX(sceneWidth);
            transitionIn.setToX(0);

            if (notificationField.getChildren().size() >= maxMessageCount) {
                notificationField.getChildren().remove(maxMessageCount - 1);
                notificationField.getChildren().addFirst(notification);
                transitionIn.play();
            } else {
                stage.setY(stage.getY() - maxNotificationHeight - spacing);
                stage.setHeight(stage.getY() + maxNotificationHeight + spacing);
                notificationField.getChildren().addFirst(notification);
                transitionIn.play();
            }
        });
    }



    public void close () {stage.close();}
}
