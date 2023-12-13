package ru.nikidzawa.golink;

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
import ru.nikidzawa.golink.GUIPatterns.Message;
import ru.nikidzawa.golink.GUIPatterns.WindowTitle;
import ru.nikidzawa.golink.services.ChangeScene;
import ru.nikidzawa.golink.store.entities.UserEntity;
import ru.nikidzawa.golink.store.repositories.UserRepository;

import java.util.Random;

@Controller
public class Register {
    @Setter
    private ConfigurableApplicationContext context;

    @FXML
    private Button closeButton;

    @FXML
    private Button enter;

    @FXML
    private Text login;

    @FXML
    private VBox menuItem;

    @FXML
    private Button minimizeButton;

    @FXML
    private PasswordField password;

    @FXML
    private TextField phone;

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

        login.setOnMouseClicked(e -> {
            login.getScene().getWindow().hide();
            ChangeScene.change(new FXMLLoader(getClass().getResource("login.fxml")));
        });

        enter.setOnAction(e -> {
            UserEntity user = repository.findByPhone(Long.parseLong(phone.getText()));

            if (user != null) {
                exception("Номер телефона уже зарегистрирован");
            }
            else if (password.getText().length() < 6) {
                exception("Минимальный размер пароля должен составлять 6 символов");
            }
            else if (password.getText().length() > 35) {
                exception("Придумайте пароль покороче");
            }
            else loadAuth();

        });
    }

    @SneakyThrows
    private void loadAuth() {
        String code = generateCode();
        Long number = null;
        try {
            number = Long.parseLong(phone.getText());
        } catch (NumberFormatException ex) {
            phone.clear();
            initialize();
            exception("Неверный формат телефона");
        }
        System.out.println(code);
//        SMSAuthenticate.sendMessage(code, phone.getText());
        enter.getScene().getWindow().hide();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("verify.fxml"));
        Parent root = loader.load();
        loader.setControllerFactory(context::getBean);

        VerifyNumber verifyNumber = loader.getController();
        verifyNumber.setContext(context);
        verifyNumber.setPhone(number);
        verifyNumber.setPassword(password.getText());
        verifyNumber.setCode(code);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root));
        stage.show();
    }
    private String generateCode () {
        Random random = new Random();
        int randomNumber = random.nextInt(999999) + 1;
        return String.format("%06d", randomNumber);
    }

    private void exception (String message) {
        Message.create(new Image(getClass().getResource("/exception.png").toExternalForm()),
                message, menuItem);
    }
}