package ru.nikidzawa.golink.FXControllers;

import io.github.gleidson28.GNAvatarView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.FXControllers.helpers.GUIPatterns;
import ru.nikidzawa.golink.store.entities.ImageEntity;
import ru.nikidzawa.golink.store.entities.UserEntity;
import ru.nikidzawa.golink.store.repositories.ImageRepository;
import ru.nikidzawa.golink.store.repositories.UserRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

@Controller
public class SelectAvatar {
    @Setter
    private ConfigurableApplicationContext context;

    @Setter
    private Long phone;

    @Setter
    private String password;

    @FXML
    private Button closeButton;

    @FXML
    private Button enter;

    @FXML
    private GNAvatarView image;

    @FXML
    private VBox menuItem;

    @FXML
    private Button minimizeButton;

    @FXML
    private TextField name;

    @FXML
    private TextField nickname;

    @FXML
    private Button scaleButton;

    @FXML
    private Pane titleBar;

    private byte[] imageMetadata;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ImageRepository imageRepository;
    @Autowired
    GUIPatterns GUIPatterns;

    @FXML
    void initialize() {
        Platform.runLater(() -> GUIPatterns.setBaseWindowTitleCommands(titleBar, minimizeButton, scaleButton, closeButton, context));

        image.setOnMouseClicked(mouseEvent -> selectImage());

        name.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                if (nickname.getText().isEmpty()) {
                    nickname.requestFocus();
                } else registration();
            }
        });

        nickname.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                if (name.getText().isEmpty()) {
                    name.requestFocus();
                }
                else if (imageMetadata == null) selectImage();
                else registration();
            }
        });

        enter.setOnAction(actionEvent -> registration());
    }

    private void selectImage () {
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Изображения", "*.jpg", "*.png")
            );
            File selectedFile = fileChooser.showOpenDialog(image.getScene().getWindow());

            if (selectedFile != null) {
                image.setImage(new Image(selectedFile.getAbsolutePath()));
            try {
                imageMetadata = Files.readAllBytes(selectedFile.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void registration () {
        String userName = name.getText();
        String userNickname = nickname.getText();
        if (userName.length() > 25) exception("Ограничение на длину имени составляет 25 символов");
        else if (imageMetadata == null) exception("Выберите фото");
        else if (userRepository.findFirstByNickname(userNickname).isPresent()) exception("Пользователь с таким никнеймом уже существует");
        else {
            UserEntity userEntity = UserEntity.builder()
                    .name(userName)
                    .nickname(userNickname)
                    .phone(phone)
                    .password(password)
                    .build();
            userRepository.saveAndFlush(userEntity);
            ImageEntity imageEntity = ImageEntity.builder()
                    .metadata(imageMetadata)
                    .owner(userEntity)
                    .build();
            imageRepository.saveAndFlush(imageEntity);
            userEntity.setAvatar(imageEntity);
            userRepository.saveAndFlush(userEntity);
            load(userEntity);
        }
    }

    @SneakyThrows
    private void load (UserEntity user) {
        enter.getScene().getWindow().hide();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
        loader.setControllerFactory(context::getBean);
        loader.load();
        Parent root = loader.getRoot();
        Scene scene = new Scene(root);

        GoLink goLink = loader.getController();
        goLink.setUserEntity(user);
        goLink.setScene(scene);
        goLink.setContext(context);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.show();
    }
    private void exception (String message) {
        GUIPatterns.createExceptionMessage(new Image(Objects.requireNonNull(getClass().getResource("/img/exception.png")).toExternalForm()),
                message, menuItem);
    }
}
