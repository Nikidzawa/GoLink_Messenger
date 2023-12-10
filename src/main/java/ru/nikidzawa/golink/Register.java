package ru.nikidzawa.golink;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
public class Register {
    @FXML
    private Button enter;

    @FXML
    private Text login;

    @FXML
    private TextField name;

    @FXML
    private TextField password;

    @FXML
    private Text password_error_message;

    @FXML
    private TextField phone;

    @FXML
    private Text phone_error_message;

    @Autowired
    UserRepository repository;
    @FXML
    void initialize() {
        phone.textProperty().addListener((observable, oldValue, newValue) -> {
            UserEntity entity = repository.findByPhone(Long.valueOf(phone.getText()));
            phone_error_message.setVisible(entity != null);
        });
        password.textProperty().addListener((observable, oldValue, newValue) -> {
            password_error_message.setVisible(newValue.length() < 6);
        });

        enter.setOnAction(actionEvent -> {
            if (!(password_error_message.isVisible() || phone_error_message.isVisible())) {
                UserEntity user = new UserEntity.UserEntityBuilder()
                        .name(name.getText()).password(password.getText()).phone(Long.valueOf(phone.getText())).build();
                repository.save(user);
                enter.getScene().getWindow().hide();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
                Parent root;
                try {
                    root = loader.load();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                GoLink goLink = loader.getController();
                goLink.setUserEntity(user);


                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("GoLink messenger");
                stage.show();
            }
        });
        login.setOnMouseClicked(e -> {
            login.getScene().getWindow().hide();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            try {
                loader.load();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            Parent root = loader.getRoot();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Авторизация");
            stage.show();
        });
    }
}
