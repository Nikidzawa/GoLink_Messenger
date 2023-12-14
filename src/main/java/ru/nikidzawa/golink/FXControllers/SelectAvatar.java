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
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.GUIPatterns.Message;
import ru.nikidzawa.golink.GUIPatterns.WindowTitle;
import ru.nikidzawa.golink.store.entities.UserEntity;
import ru.nikidzawa.golink.store.repositories.UserRepository;

import java.util.Objects;

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

    @Autowired
    UserRepository repository;
    @FXML
    void initialize() {
        Platform.runLater(() -> WindowTitle.setBaseCommands(titleBar, minimizeButton, scaleButton, closeButton));
        menuItem.setSpacing(15);

        enter.setOnAction(actionEvent -> {
            String userName = name.getText();
            String userNickname = nickname.getText();
            UserEntity user = repository.findByNickname(userNickname);
            if (userName.length() > 25) {
                exception("Ограничение на длину имени составляет 25 символов");
            }
            else if (user != null) {
                exception("Пользователь с таким никнеймом уже существует");
            }
            else {
                UserEntity userEntity = UserEntity.builder()
                        .name(userName)
                        .nickname(userNickname)
                        .phone(phone)
                        .password(password)
                        .build();
                repository.save(userEntity);
                load(userEntity);
            }
        });
    }
    @SneakyThrows
    private void load (UserEntity user) {
        enter.getScene().getWindow().hide();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
        loader.setControllerFactory(context::getBean);
        loader.load();

        GoLink goLink = loader.getController();
        goLink.setUserEntity(user);

        Parent root = loader.getRoot();
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root));
        stage.show();
    }
    private void exception (String message) {
        Message.create(new Image(Objects.requireNonNull(getClass().getResource("/exception.png")).toExternalForm()),
                message, menuItem);
    }
}
