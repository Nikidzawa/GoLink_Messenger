package ru.nikidzawa.golink.FXControllers.helpers;

import io.github.gleidson28.AvatarType;
import io.github.gleidson28.GNAvatarView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.nikidzawa.golink.FXControllers.GoLink;
import ru.nikidzawa.golink.network.TCPConnection;
import ru.nikidzawa.golink.services.GoMessage.TCPBroker;
import ru.nikidzawa.golink.store.entities.ChatEntity;
import ru.nikidzawa.golink.store.entities.MessageEntity;
import ru.nikidzawa.golink.store.entities.UserEntity;
import ru.nikidzawa.golink.store.repositories.ChatRepository;
import ru.nikidzawa.golink.store.repositories.UserRepository;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Setter
@Component
public class GUIPatterns {
    private double xOffset = 0;
    private double yOffset = 0;
    private TCPConnection tcpConnection;
    private MessageStage messageStage;

    public void setBaseWindowTitleCommands(Pane titleBar, Button minimizeButton, Button scaleButton, Button closeButton ) {
        setWindowTitleButtons(titleBar, minimizeButton, scaleButton, closeButton);
        closeButton.setOnAction(actionEvent -> Platform.exit());
    }

    public void setBaseWindowTitleCommands(Pane titleBar, Button minimizeButton, Button scaleButton, Button closeButton, TCPBroker tcpBroker, UserEntity userEntity, UserRepository userRepository, ChatRepository chatRepository) {
        setWindowTitleButtons(titleBar, minimizeButton, scaleButton, closeButton);
        closeButton.setOnAction(actionEvent -> {
            Platform.runLater(() -> {
                    userEntity.setConnected(false);
                    userRepository.saveAndFlush(userEntity);
                    chatRepository.findByParticipantsContaining(userEntity).forEach(chat -> chat.getParticipants().stream()
                            .filter(user -> !Objects.equals(user.getId(), userEntity.getId()))
                            .forEach(user -> tcpBroker.sendMessage("UPDATE_CHAT_ROOMS:" + user.getId())));
                    if (tcpConnection != null) {
                        tcpConnection.disconnect();
                    }
                    tcpBroker.disconnect();
                    Platform.exit();
            });
        });
    }

    private void setWindowTitleButtons(Pane titleBar, Button minimizeButton, Button scaleButton, Button closeButton) {
        minimizeButton.setOnMouseEntered(mouseEvent -> {
            minimizeButton.setStyle("-fx-background-color: GRAY; -fx-text-fill: white");
        });
        minimizeButton.setOnMouseExited(mouseEvent -> {
            minimizeButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0");
        });
        scaleButton.setOnMouseEntered(mouseEvent -> {
            scaleButton.setStyle("-fx-background-color: GRAY; -fx-text-fill: white");
        });
        scaleButton.setOnMouseExited(mouseEvent -> {
            scaleButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0");
        });
        closeButton.setOnMouseEntered(mouseEvent -> {
            closeButton.setStyle("-fx-background-color: red; -fx-text-fill: white");
        });
        closeButton.setOnMouseExited(mouseEvent -> {
            closeButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0");
        });
        minimizeButton.setOnAction(actionEvent -> {
            Window window = minimizeButton.getScene().getWindow();
            ((Stage) window).setIconified(true);
        });

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            Window window = titleBar.getScene().getWindow();
            (window).setX(event.getScreenX() - xOffset);
            (window).setY(event.getScreenY() - yOffset);
        });
    }

    public void createExceptionMessage(Image image, String message, VBox menuItem) {
        if (menuItem.getChildren().stream().toList().size() < 4) {
            HBox hBox = new HBox();
            BorderPane borderPane = new BorderPane();
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(30);
            imageView.setFitWidth(30);
            borderPane.setLeft(imageView);
            borderPane.setStyle("-fx-background-color: white;" + "-fx-background-radius: 20px;");

            Text text = new Text(message);
            text.setFill(Paint.valueOf("black"));
            text.setFont(Font.font(18));

            TextFlow textFlow = new TextFlow(text);
            borderPane.setCenter(textFlow);
            hBox.getChildren().add(borderPane);
            menuItem.getChildren().add(hBox);
            Timer destructionTimer = new Timer();
            destructionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        menuItem.getChildren().remove(hBox);
                    });
                }
            }, 4000);
        }
    }

    public void makeInput (TextField input) {
        input.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                input.setStyle("-fx-background-color: #001933; -fx-border-color: blue; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
            } else {
                input.setStyle("-fx-background-color: #001933; -fx-border-color: Gray; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
            }
        });
    }

    public void makeSearch (TextField searchPanel) {
        searchPanel.setStyle("-fx-background-color: #001933; -fx-border-color: blue; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
        searchPanel.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                searchPanel.setStyle("-fx-background-color: #001933; -fx-border-color: blue; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
            } else {
                searchPanel.setStyle("-fx-background-color: #001933; -fx-border-color: Gray; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
            }
        });
    }

    public void setConfig (TextField phone) {
        final int maxLength = 12;

        phone.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > maxLength) {
                phone.setText(oldValue);
            }
        });
        phone.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (!event.getCharacter().matches("[0-9]")) {
                event.consume();
            }
        });
    }

    public BorderPane newChatBuilder(UserEntity myAccount, UserEntity interlocutor, ChatEntity chat) {
        BorderPane borderPane = new BorderPane();
        borderPane.setCursor(Cursor.HAND);
        StackPane stackImg = new StackPane();
        GNAvatarView avatar = new GNAvatarView();
        avatar.setImage(new Image(new ByteArrayInputStream(interlocutor.getAvatar().getMetadata())));
        avatar.setType(AvatarType.CIRCLE);
        avatar.setStroke(Paint.valueOf("#001933"));
        stackImg.getChildren().add(avatar);
        stackImg.setPrefHeight(60);
        stackImg.setPrefWidth(60);
        if (interlocutor.isConnected()) {
            Circle circle = new Circle();
            circle.setFill(Paint.valueOf("GREEN"));
            circle.setRadius(7);
            circle.setStroke(Paint.valueOf("black"));
            stackImg.getChildren().add(circle);
            StackPane.setAlignment(circle, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(circle, new Insets(0, 7, 0, 0));
        }
        borderPane.setLeft(stackImg);

        BorderPane nameAndLastMessage = new BorderPane();
        Text name = new Text(interlocutor.getName());
        name.setTextAlignment(TextAlignment.LEFT);
        name.setFont(Font.font("System", 18));
        name.setFill(Paint.valueOf("white"));
        nameAndLastMessage.setTop(name);
        TextField lastMessageInfo = new TextField();
        lastMessageInfo.setStyle("-fx-background-color: #001933; -fx-text-fill: white");
        lastMessageInfo.setDisable(true);
        nameAndLastMessage.setLeft(lastMessageInfo);
        List<MessageEntity> messages = chat.getMessages();
        nameAndLastMessage.setPadding(new Insets(0, 0, 0, 10));
        borderPane.setCenter(nameAndLastMessage);
        if (!messages.isEmpty()) {
            MessageEntity lastMessage = messages.get(messages.size() - 1);
            String sender = lastMessage.getSender().getId().equals(myAccount.getId()) ? "Вы: " : "";
            lastMessageInfo.setText(sender + lastMessage.getMessage());
            VBox vBox = new VBox();
            vBox.setSpacing(5);
            Text date = new Text();
            try {
                date.setText(lastMessage.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
            } catch (IndexOutOfBoundsException ex) {
                date.setText("");
            }

            date.setFill(Paint.valueOf("white"));
            if (0 > 0) {
                StackPane newMessagesVisualize = new StackPane();
                Circle circle = new Circle();
                circle.setRadius(12);
                circle.setFill(Paint.valueOf("#80c3ff"));
                circle.setStroke(Paint.valueOf("black"));
                Text newMessageCount = new Text((0 >= 100 ? "99+" : String.valueOf(0)));
                newMessagesVisualize.getChildren().add(circle);
                newMessagesVisualize.getChildren().add(newMessageCount);
                vBox.getChildren().add(newMessagesVisualize);
            }
            vBox.getChildren().add(date);
            Translate translate = new Translate();
            translate.setX(-7);
            vBox.getTransforms().add(translate);

            borderPane.setRight(vBox);
        } else { lastMessageInfo.setText("Чат пуст"); }

        borderPane.setOnMouseEntered(mouseEvent -> {
            lastMessageInfo.setStyle("-fx-background-color: #34577F; -fx-text-fill: white");
            borderPane.setStyle("-fx-background-color: #34577F;");
        });
        borderPane.setOnMouseExited(mouseEvent -> {
            lastMessageInfo.setStyle("-fx-background-color: #001933; -fx-text-fill: white");
            borderPane.setStyle("-fx-background-color: #001933;");
        });
        return borderPane;
    }

    public BorderPane newChatBuilder(UserEntity user) {
        BorderPane borderPane = new BorderPane();
        borderPane.setOnMouseEntered(mouseEvent -> {
            borderPane.setStyle("-fx-background-color: #34577F;");
        });
        borderPane.setOnMouseExited(mouseEvent -> {
            borderPane.setStyle("-fx-background-color: #001933;");
        });
        StackPane stackImg = new StackPane();
        GNAvatarView avatar = new GNAvatarView();
        avatar.setImage(new Image(new ByteArrayInputStream(user.getAvatar().getMetadata())));
        avatar.setType(AvatarType.CIRCLE);
        avatar.setStroke(Paint.valueOf("#001933"));
        stackImg.getChildren().add(avatar);
        stackImg.setPrefHeight(60);
        stackImg.setPrefWidth(60);
        borderPane.setLeft(stackImg);
        StackPane nameStack = new StackPane();
        Text name = new Text(user.getName());
        name.setWrappingWidth(170);
        name.setTextAlignment(TextAlignment.LEFT);
        name.setFont(Font.font("System", 18));
        name.setFill(Paint.valueOf("white"));
        nameStack.getChildren().add(name);
        borderPane.setCenter(nameStack);

        return borderPane;
    }

    public BorderPane deleteMessageButton() {
        BorderPane delete = new BorderPane();
        delete.setPrefWidth(143);
        delete.setPrefHeight(31);
        Button buttonDelete = new Button("Удалить");
        buttonDelete.setDisable(true);
        delete.setOnMouseEntered(mouseEvent1 -> {
            delete.setStyle("-fx-background-color: silver");
            buttonDelete.setStyle("-fx-background-color: silver; -fx-text-fill: white");
        });
        delete.setOnMouseExited(mouseEvent1 -> {
            delete.setStyle("-fx-background-color: #18314D");
            buttonDelete.setStyle("-fx-background-color: #18314D; -fx-text-fill: white");
        });
        buttonDelete.setPrefWidth(122);
        buttonDelete.setPrefHeight(31);
        buttonDelete.setStyle("-fx-background-color:  #18314D; -fx-text-fill: white");
        ImageView deleteImage = new ImageView(new Image(Objects.requireNonNull(GoLink.class.getResourceAsStream("/img/deleteTXT.png"))));
        deleteImage.setDisable(true);
        deleteImage.setFitWidth(20);
        deleteImage.setFitHeight(20);
        AnchorPane deletePane = new AnchorPane();
        deletePane.setDisable(true);
        deletePane.setPrefWidth(5);
        deletePane.setPrefHeight(31);
        delete.setLeft(deletePane);
        delete.setCenter(deleteImage);
        delete.setRight(buttonDelete);
        return delete;
    }

    public BorderPane copyMessageButton() {
        BorderPane copy = new BorderPane();
        copy.setPrefWidth(143);
        copy.setPrefHeight(31);
        Button buttonCopy = new Button("Копировать");
        buttonCopy.setDisable(true);
        copy.setOnMouseEntered(mouseEvent1 -> {
            copy.setStyle("-fx-background-color: silver");
            buttonCopy.setStyle("-fx-background-color: silver; -fx-text-fill: white");
        });
        copy.setOnMouseExited(mouseEvent1 -> {
            copy.setStyle("-fx-background-color: #18314D");
            buttonCopy.setStyle("-fx-background-color: #18314D; -fx-text-fill: white");
        });
        buttonCopy.setPrefWidth(122);
        buttonCopy.setPrefHeight(31);
        buttonCopy.setStyle("-fx-background-color:  #18314D; -fx-text-fill: white");
        ImageView imageView = new ImageView(new Image(Objects.requireNonNull(GoLink.class.getResourceAsStream("/img/copyTXT.png"))));
        imageView.setDisable(true);
        imageView.setFitWidth(20);
        imageView.setFitHeight(20);
        AnchorPane copyPane = new AnchorPane();
        copyPane.setDisable(true);
        copyPane.setPrefWidth(5);
        copyPane.setPrefHeight(31);
        copy.setLeft(copyPane);
        copy.setCenter(imageView);
        copy.setRight(buttonCopy);
        return copy;
    }

    public BorderPane editeMessageButton() {
        BorderPane edit = new BorderPane();
        edit.setPrefWidth(143);
        edit.setPrefHeight(31);
        Button buttonEdit = new Button("Изменить");
        buttonEdit.setDisable(true);
        edit.setOnMouseEntered(mouseEvent1 -> {
            edit.setStyle("-fx-background-color: silver");
            buttonEdit.setStyle("-fx-background-color: silver; -fx-text-fill: white");
        });
        edit.setOnMouseExited(mouseEvent1 -> {
            edit.setStyle("-fx-background-color: #18314D");
            buttonEdit.setStyle("-fx-background-color: #18314D; -fx-text-fill: white");
        });
        buttonEdit.setPrefWidth(122);
        buttonEdit.setPrefHeight(31);
        buttonEdit.setStyle("-fx-background-color:  #18314D; -fx-text-fill: white");
        ImageView editImage = new ImageView(new Image(Objects.requireNonNull(GoLink.class.getResourceAsStream("/img/editTXT.png"))));
        editImage.setDisable(true);
        editImage.setFitWidth(20);
        editImage.setFitHeight(20);
        AnchorPane editPane = new AnchorPane();
        editPane.setDisable(true);
        editPane.setPrefWidth(5);
        editPane.setPrefHeight(31);
        edit.setLeft(editPane);
        edit.setCenter(editImage);
        edit.setRight(buttonEdit);
        return edit;
    }

    public void openMessageWindow(MessageEntity message, MouseEvent mouseEvent, Stage messageStage, BorderPane copy, BorderPane edit, BorderPane delete) {
        double mouseX = mouseEvent.getScreenX();
        double mouseY = mouseEvent.getScreenY();

        if (checkMessageWindowStatusBeforeOpen(message.getId(), messageStage)) {
            return;
        }
        messageStage.initStyle(StageStyle.UNDECORATED);
        checkMessageWindowStatusBeforeOpen(message.getId(), messageStage);

        VBox vBox = new VBox();
        vBox.setStyle("-fx-background-color:  #18314D; -fx-border-color: black; -fx-spacing: 5");

        vBox.getChildren().add(copy);
        vBox.getChildren().add(edit);
        vBox.getChildren().add(delete);

        Scene scene = new Scene(vBox, 154, 105);
        messageStage.setScene(scene);

        messageStage.setX(mouseX);
        messageStage.setY(mouseY);
        messageStage.show();
    }

    public HBox printForeignMessage(String message, LocalDateTime localDateTime) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new Insets(5, 5, 5, 10));
        BorderPane borderPane = foreignBasicMessagePattern(localDateTime);
        Text text = new Text(message);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle(
                "-fx-font-family: Arial;" +
                        "-fx-font-size: 14px;"
        );
        textFlow.setPadding(new Insets(5, 10, 3, 10));

        borderPane.setTop(textFlow);
        hBox.getChildren().add(borderPane);
        return hBox;
    }

    private BorderPane foreignBasicMessagePattern(LocalDateTime localDateTime) {
        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: rgb(233, 233, 235); -fx-background-radius: 20px;");
        Text date = new Text(localDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        date.setFill(Color.color(0, 0, 0));
        TextFlow dateFlow = new TextFlow(date);
        dateFlow.setPadding(new Insets(0, 10, 5, 10));
        dateFlow.setTextAlignment(TextAlignment.LEFT);
        borderPane.setBottom(dateFlow);
        return borderPane;
    }

    private BorderPane myBasicMessagePattern(LocalDateTime localDateTime) {
        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: rgb(15, 125, 242); -fx-background-radius: 20px;");
        Text date = new Text(localDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        date.setFill(Color.color(0.934, 0.925, 0.996));
        TextFlow dateFlow = new TextFlow(date);
        dateFlow.setPadding(new Insets(0, 10, 5, 10));
        dateFlow.setTextAlignment(TextAlignment.RIGHT);
        borderPane.setBottom(dateFlow);
        return borderPane;
    }

    public HBox printForeignPhoto(Image image, LocalDateTime localDateTime) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new Insets(5, 5, 5, 10));
        BorderPane borderPane = foreignBasicMessagePattern(localDateTime);
        ImageView imageView = new ImageView();
        double width = image.getWidth();
        double height = image.getHeight();
        if (width > 750) {
            imageView.setFitWidth(750);
        } else {
            imageView.setFitWidth(width);
        }
        if (height > 500) {
            imageView.setFitHeight(500);
        }
        else {
            imageView.setFitHeight(height);
        }
        imageView.setImage(image);
        borderPane.setTop(imageView);
        hBox.getChildren().add(borderPane);
        imageView.setOnMouseClicked(mouseEvent -> {
            Stage imageScene = new Stage();
            ImageView enlargedImageView = new ImageView(image);
            enlargedImageView.setFitWidth(width);
            enlargedImageView.setFitHeight(height);
            enlargedImageView.setPreserveRatio(true);
            Scene scene = new Scene(new Pane(enlargedImageView));
            imageScene.setScene(scene);
            imageScene.show();
        });
        return hBox;
    }

    public HBox printMyPhoto(Image image, LocalDateTime localDateTime) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.setPadding(new Insets(5, 5, 5, 10));
        BorderPane borderPane = myBasicMessagePattern(localDateTime);
        ImageView imageView = new ImageView();
        double width = image.getWidth();
        double height = image.getHeight();
        if (width > 750) {
            imageView.setFitWidth(750);
        } else {
            imageView.setFitWidth(width);
        }
        if (height > 500) {
            imageView.setFitHeight(500);
        }
        else {
            imageView.setFitHeight(height);
        }
        imageView.setImage(image);
        borderPane.setTop(imageView);
        hBox.getChildren().add(borderPane);
        imageView.setOnMouseClicked(mouseEvent -> {
            Stage imageScene = new Stage();
            ImageView enlargedImageView = new ImageView(image);
            enlargedImageView.setFitWidth(width);
            enlargedImageView.setFitHeight(height);
            enlargedImageView.setPreserveRatio(true);
            Scene scene = new Scene(new Pane(enlargedImageView));
            imageScene.setScene(scene);
            imageScene.show();
        });
        return hBox;
    }

    public HBox printMyMessage(MessageEntity message, LocalDateTime localDateTime) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.setPadding(new Insets(5, 5, 3, 10));
        BorderPane borderPane = myBasicMessagePattern(localDateTime);
        Text text = new Text(message.getMessage());
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle(
                "-fx-color: rgb(239, 242, 255);" +
                        "-fx-font-family: Arial;" + "-fx-font-size: 14px;"
        );
        textFlow.setPadding(new Insets(5, 10, 5, 10));
        text.setFill(Color.color(0.934, 0.925, 0.996));

        borderPane.setTop(textFlow);
        hBox.getChildren().add(borderPane);
        return hBox;
    }

    public BorderPane getBackgroundEditInterface (MessageEntity message)  {
        BorderPane editInterfaceBackground = new BorderPane();
        ImageView editImage = new ImageView(new Image(Objects.requireNonNull(GoLink.class.getResourceAsStream("/img/editTXT.png"))));
        editImage.setFitWidth(30);
        editImage.setFitHeight(30);
        editImage.setStyle("-fx-padding: 0 0 0 10 0");
        BorderPane editMessageProperty = new BorderPane();
        Text redact = new Text("Редактирование");
        redact.setFont(Font.font(17));
        redact.setFill(Paint.valueOf("WHITE"));
        redact.prefWidth(766);
        redact.prefHeight(22);
        TextField editableText = new TextField(message.getMessage());
        editableText.setPrefWidth(806);
        editableText.setPrefHeight(29);
        editableText.setEditable(false);
        editableText.setStyle("-fx-background-color: #001933; -fx-text-fill: white");
        editMessageProperty.setCenter(editableText);
        editMessageProperty.setTop(redact);
        editInterfaceBackground.setCenter(editMessageProperty);
        editInterfaceBackground.setLeft(editImage);
        return editInterfaceBackground;
    }

    public ImageView getCancelButton () {
        ImageView cancel = new ImageView(new Image(Objects.requireNonNull(GoLink.class.getResourceAsStream("/img/cancel.png"))));
        cancel.setFitWidth(30);
        cancel.setFitHeight(30);
        return cancel;
    }

    private boolean checkMessageWindowStatusBeforeOpen(Long messageId, Stage stage) {
        if (messageStage == null) {
            messageStage = new MessageStage();
            messageStage.setMessageId(messageId);
            messageStage.setMessagesStage(stage);
        } else {
            if (!Objects.equals(messageId, messageStage.getMessageId())) {
                messageStage.getMessagesStage().close();
                messageStage.setMessagesStage(stage);
                messageStage.setMessageId(messageId);
            } else {
                return true;
            }
        }
        return false;
    }
}