package ru.nikidzawa.golink.FXControllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.context.ConfigurableApplicationContext;
import ru.nikidzawa.golink.GUIPatterns.Message;
import ru.nikidzawa.golink.GUIPatterns.WindowTitle;

import java.util.Objects;

public class VerifyNumber {
    @Setter
    private ConfigurableApplicationContext context;

    @Setter
    private Long phone;

    @Setter
    private String password;

    @Setter
    private String code;

    @FXML
    private HBox area;

    @FXML
    private Button closeButton;

    @FXML
    private Text goBack;

    @FXML
    private VBox menuItem;

    @FXML
    private Button minimizeButton;

    @FXML
    private Button scaleButton;

    @FXML
    private Pane titleBar;

    @FXML
    void initialize() {
        Platform.runLater(() -> WindowTitle.setBaseCommands(titleBar, minimizeButton, scaleButton, closeButton));
        menuItem.setSpacing(15);
        area.setSpacing(25);

        goBack.setOnMouseClicked(mouseEvent -> goBack());
        final int maxLength = 1;

        for (int i = 0; i < 6; i++) {
            TextField textField = new TextField();
            textField.setPrefWidth(90);
            textField.setStyle("-fx-border-width:0 0 2 0; -fx-text-fill: white; -fx-background-color:  #001933; -fx-border-color:  #18314D;");
            textField.setPrefColumnCount(1);
            textField.setAlignment(Pos.CENTER);
            textField.setFont(Font.font(20));
            int finalI = i;
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue.length() > maxLength) {
                            textField.setText(newValue.substring(newValue.length() - 1));
                        }
                    });
            textField.addEventHandler(KeyEvent.KEY_RELEASED, event -> handleInput(textField, finalI, event));
            textField.addEventHandler(KeyEvent.KEY_PRESSED, event -> handleBackspace(textField, finalI, event));
            textField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
                if (!event.getCharacter().matches("[0-9]")) {
                    event.consume();
                }
            });
            area.getChildren().add(textField);
        }
        area.getChildren().get(0).requestFocus();
    }
    private void handleInput(TextField currentTextField, int index, KeyEvent event) {
        String input = event.getText();
        if (input.length() == 1) {
            int nextIndex = index + 1;
            if (nextIndex < ((HBox) currentTextField.getParent()).getChildren().size()) {
                TextField nextTextField = (TextField) ((HBox) currentTextField.getParent()).getChildren().get(nextIndex);
                nextTextField.requestFocus();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < 6; i++) {
                    TextField txt = (TextField) area.getChildren().get(i);
                    stringBuilder.append(txt.getText());
                }
                if (code.contentEquals(stringBuilder)) {
                    fxAvatar();
                } else {
                    Message.create(new Image(Objects.requireNonNull(getClass().getResource("/exception.png")).toExternalForm()),
                            "Ошибка, неверный код", menuItem);
                }
            }
        }
    }
    @SneakyThrows
    private void fxAvatar() {
        area.getScene().getWindow().hide();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("avatar.fxml"));
        loader.setControllerFactory(context::getBean);
        loader.load();
        SelectAvatar selectAvatar = loader.getController();
        selectAvatar.setPhone(phone);
        selectAvatar.setPassword(password);
        selectAvatar.setContext(context);
        Parent root = loader.getRoot();
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root));
        stage.show();
    }
    private void handleBackspace(TextField currentTextField, int index, KeyEvent event) {
        if (event.getCode() == KeyCode.BACK_SPACE && currentTextField.getCaretPosition() == 0) {
            int prevIndex = index - 1;
            if (prevIndex >= 0) {
                TextField prevTextField = (TextField) ((HBox) currentTextField.getParent()).getChildren().get(prevIndex);
                prevTextField.setText("");
                prevTextField.requestFocus();
            }
        }
    }
    @SneakyThrows
    private void goBack () {
        goBack.getScene().getWindow().hide();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/nikidzawa/goLink/register.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        Register register = loader.getController();
        register.setContext(context);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root));
        stage.show();
    }
}
