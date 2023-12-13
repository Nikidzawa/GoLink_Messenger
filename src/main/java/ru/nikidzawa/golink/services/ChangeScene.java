package ru.nikidzawa.golink.services;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.SneakyThrows;
public class ChangeScene {
    @SneakyThrows
    public static void change (FXMLLoader loader) {
        loader.load();
        Parent root = loader.getRoot();
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root));
        stage.show();
    }
}
