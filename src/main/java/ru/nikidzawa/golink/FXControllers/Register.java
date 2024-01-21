package ru.nikidzawa.golink.FXControllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Setter;
import lombok.SneakyThrows;
import org.controlsfx.control.Notifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.services.GUI.GUIPatterns;
import ru.nikidzawa.golink.services.SMSAuthenticate.SMSAuthenticate;
import ru.nikidzawa.networkAPI.store.repositories.UserRepository;

import java.util.Objects;
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
    UserRepository userRepository;

    @Autowired
    GUIPatterns GUIPatterns;

    @Autowired
    SMSAuthenticate smsAuthenticate;

    @FXML
    void initialize() {
        Platform.runLater(() -> GUIPatterns.setBaseWindowTitleCommands(titleBar, minimizeButton, scaleButton, closeButton, context));

        GUIPatterns.setConfig(phone);
        login.setOnMouseClicked(e -> fxLogin());

        password.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) enterEvent();
        });
        phone.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER)
                if (password.getText().isEmpty()) {
                    password.requestFocus();
                } else enterEvent();
        });

        enter.setOnAction(e -> enterEvent());
    }


    private void enterEvent() {
        Long inputNumber;
        try {
            inputNumber = Long.parseLong(phone.getText());
        } catch (NumberFormatException ex) {
            exception("Неверный формат телефона");
            phone.clear();
            return;
        }

        if (password.getText().length() < 6) exception("Минимальный размер пароля должен составлять 6 символов");
        else if (password.getText().length() > 35) exception("Придумайте пароль покороче");
        else if (userRepository.findFirstByPhone(inputNumber).isPresent())
            exception("Номер телефона уже зарегистрирован");
        else loadAuth(inputNumber);
    }

    @SneakyThrows
    private void fxLogin() {
        login.getScene().getWindow().hide();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        Login loginController = loader.getController();
        loginController.setContext(context);

        Stage stage = new Stage();
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/logo.png"))));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("GoLink");
        stage.setScene(new Scene(root));
        stage.show();
    }

    @SneakyThrows
    private void loadAuth(Long inputNumber) {
        String code = generateCode();
        System.out.println(code);
//        smsAuthenticate.sendMessage(code, phone.getText());
        enter.getScene().getWindow().hide();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("verify.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        VerifyNumber verifyNumber = loader.getController();
        verifyNumber.setContext(context);
        verifyNumber.setPhone(inputNumber);
        verifyNumber.setPassword(password.getText());
        verifyNumber.setCode(code);

        Stage stage = new Stage();
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/logo.png"))));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("GoLink");
        stage.setScene(new Scene(root));
        stage.show();
    }

    private String generateCode() {
        Random random = new Random();
        int randomNumber = random.nextInt(999999) + 1;
        return String.format("%06d", randomNumber);
    }

    private void exception(String message) {
        Notifications.create()
                .owner(enter.getScene().getWindow())
                .position(Pos.TOP_RIGHT)
                .title("Ошибка")
                .text(message)
                .showError();
    }
}
