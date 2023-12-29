package ru.nikidzawa.golink.FXControllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.FXControllers.helpers.GUIPatterns;
import ru.nikidzawa.golink.store.entities.UserEntity;
import ru.nikidzawa.golink.store.repositories.UserRepository;

import java.util.Objects;

@Controller
public class Login {
    @Setter
    private ConfigurableApplicationContext context;

    @FXML
    private Button closeButton;

    @FXML
    private Button enter;

    @FXML
    private VBox menuItem;

    @FXML
    private Button minimizeButton;

    @FXML
    private PasswordField password;

    @FXML
    private TextField phone;

    @FXML
    private Text register;

    @FXML
    private Button scaleButton;

    @FXML
    private Pane titleBar;

    @Autowired
    UserRepository userRepository;

    @Autowired
    GUIPatterns GUIPatterns;

    @FXML
    void initialize() {
        Platform.runLater(() -> GUIPatterns.setBaseWindowTitleCommands(titleBar, minimizeButton, scaleButton, closeButton, context));

        GUIPatterns.setConfig(phone);

        enter.setOnAction(e -> {
            UserEntity user = null;
            try {
                long phoneValue = Long.parseLong(phone.getText());
                user = userRepository.findByPhone(phoneValue);
            } catch (NumberFormatException ex) {
                exception();
                initialize();
            }

            if (user != null && user.getPassword().equals(password.getText())) fxMenu(user);
            else exception();
        });

        register.setOnMouseClicked(MouseEvent -> fxRegister());
    }

    @SneakyThrows
    private void fxMenu(UserEntity user) {
        enter.getScene().getWindow().hide();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        GoLink goLink = loader.getController();
        goLink.setUserEntity(user);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root));
        stage.show();
    }
    @SneakyThrows
    private void fxRegister () {
        register.getScene().getWindow().hide();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/nikidzawa/goLink/register.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        Register registerController = loader.getController();
        registerController.setContext(context);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root));
        stage.show();
    }
    private void exception () {
            GUIPatterns.createExceptionMessage(new Image(Objects.requireNonNull(getClass().getResource("/img/exception.png")).toExternalForm()),
                    " Неверный телефон или пароль. Попробуйте ещё раз   ", menuItem);
    }
}
