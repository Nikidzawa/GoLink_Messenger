package ru.nikidzawa.golink.FXControllers.helpers;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class EmptyStage {
    public static Stage getEmptyStage (Scene scene) {
        Stage helperStage = new Stage();
        Stage mainStage = new Stage();

        helperStage.initStyle(StageStyle.UTILITY);
        helperStage.setOpacity(0);
        helperStage.setHeight(0);
        helperStage.setWidth(0);
        helperStage.show();

        mainStage.initStyle(StageStyle.UNDECORATED);
        mainStage.initOwner(helperStage);
        mainStage.setScene(scene);
        return mainStage;
    }
}
