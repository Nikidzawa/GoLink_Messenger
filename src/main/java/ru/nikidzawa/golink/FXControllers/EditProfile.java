package ru.nikidzawa.golink.FXControllers;

import io.github.gleidson28.GNAvatarView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Setter;
import org.controlsfx.control.Notifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.nikidzawa.golink.services.GUI.GUIPatterns;
import ru.nikidzawa.networkAPI.store.entities.UserEntity;

import java.io.ByteArrayInputStream;

@Component
public class EditProfile {
    @FXML
    private GNAvatarView avatar;

    @FXML
    private Button closeButton;

    @FXML
    private Button enter;

    @FXML
    private Button minimizeButton;

    @FXML
    private TextField name;

    @FXML
    private TextField nickname;

    @FXML
    private TextField number;

    @FXML
    private Button scaleButton;

    @FXML
    private Pane titleBar;

    @Autowired
    GUIPatterns GUIPatterns;

    @Setter
    private GoLink goLink;
    @Setter
    private Scene scene;

    @Setter
    private UserEntity userEntity;

    @FXML
    void initialize() {
        Platform.runLater(() -> {
            enter.requestFocus();
            GUIPatterns.setBaseWindowTitleCommands(titleBar, minimizeButton, scaleButton, closeButton, null);

            name.setText(userEntity.getName());
            nickname.setText("@" + userEntity.getNickname());
            number.setText(userEntity.getPhone().toString());
            avatar.setImage(new Image(new ByteArrayInputStream(userEntity.getAvatar())));

            enter.setOnAction(_ -> {
                if (updateUserEntity()) {
                    goLink.userRepository.saveAndFlush(userEntity);
                    goLink.setUserConfig();
                }
                scene.getWindow().hide();
            });

            nickname.textProperty().addListener((_, _, newValue) -> {
                if (newValue.isEmpty()) {
                    nickname.setText("@");
                }
            });
        });
    }

    private boolean updateUserEntity() {
        boolean isEdited = false;

        if (!userEntity.getName().equals(name.getText())) {
            userEntity.setName(name.getText());
            isEdited = true;
        }

        String newNickname = nickname.getText().substring(1);
        if (!newNickname.equals(userEntity.getNickname())) {
            if (goLink.userRepository.findFirstByNickname(newNickname).isEmpty()) {
                userEntity.setNickname(newNickname);
                isEdited = true;
            } else {
                exception("Никнейм уже занят");
                return false;
            }
        }

        if (!number.getText().equals(userEntity.getPhone().toString())) {
            Long phone = Long.parseLong(number.getText());
            if (goLink.userRepository.findFirstByPhone(phone).isEmpty()) {
                userEntity.setPhone(phone);
                isEdited = true;
            } else {
                exception("Номер телефона уже зарегистрирован");
                return false;
            }
        }

        return isEdited;
    }

    private void exception (String message) {
        Notifications.create()
                .owner((Stage) scene.getWindow())
                .text(message)
                .position(Pos.TOP_RIGHT)
                .title("Ошибка")
                .showError();
    }
}
