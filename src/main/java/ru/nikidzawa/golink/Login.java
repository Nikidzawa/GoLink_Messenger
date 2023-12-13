package ru.nikidzawa.golink;

import com.pavlobu.emojitextflow.Emoji;
import com.pavlobu.emojitextflow.EmojiParser;
import com.pavlobu.emojitextflow.EmojiTextFlow;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.GUIPatterns.Message;
import ru.nikidzawa.golink.GUIPatterns.WindowTitle;
import ru.nikidzawa.golink.services.ChangeScene;
import ru.nikidzawa.golink.store.entities.UserEntity;
import ru.nikidzawa.golink.store.repositories.UserRepository;

import java.io.IOException;
import java.util.List;

@Controller
public class Login {

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

    @FXML
    void initialize() {
        Platform.runLater(() -> WindowTitle.setBaseCommands(titleBar, minimizeButton, scaleButton, closeButton));
        menuItem.setSpacing(15);

        enter.setOnAction(e -> {
            try {
                long phoneValue = Long.parseLong(phone.getText());
                UserEntity user = userRepository.findByPhone(phoneValue);

                if (user != null && user.getPassword().equals(password.getText())) {
                    enter.getScene().getWindow().hide();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
                    Parent root = loader.load();

                    GoLink goLink = loader.getController();
                    goLink.setUserEntity(user);

                    Stage stage = new Stage();
                    stage.initStyle(StageStyle.UNDECORATED);
                    stage.setScene(new Scene(root));
                    stage.show();
                } else {
                    exception();
                }
            } catch (NumberFormatException | IOException | NullPointerException ex) {
                exception();
            }
        });

        register.setOnMouseClicked(MouseEvent -> {
            register.getScene().getWindow().hide();
            ChangeScene.change(new FXMLLoader(getClass().getResource("register.fxml")));
        });
    }
    private void exception () {
            Message.create(new Image(getClass().getResource("/exception.png").toExternalForm()),
                    " Неверный телефон или пароль. Попробуйте ещё раз   ", menuItem);
    }
}
