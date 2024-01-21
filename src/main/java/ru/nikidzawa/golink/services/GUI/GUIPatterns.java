package ru.nikidzawa.golink.services.GUI;

import io.github.gleidson28.AvatarType;
import io.github.gleidson28.GNAvatarView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
import javafx.util.Pair;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import ru.nikidzawa.golink.FXControllers.GoLink;
import ru.nikidzawa.golink.FXControllers.cash.ContactCash;
import ru.nikidzawa.golink.FXControllers.cash.MessageCash;
import ru.nikidzawa.golink.FXControllers.cash.MessageStage;
import ru.nikidzawa.golink.services.GUI.TrayIcon.GoLinkTrayIcon;
import ru.nikidzawa.golink.services.sound.AudioPlayer;
import ru.nikidzawa.networkAPI.store.entities.MessageEntity;
import ru.nikidzawa.networkAPI.store.entities.PersonalChatEntity;
import ru.nikidzawa.networkAPI.store.entities.UserEntity;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Setter
@Component
public class GUIPatterns {
    private double xOffset = 0;
    private double yOffset = 0;

    public void setBaseWindowTitleCommands(Pane titleBar, Button minimizeButton, Button scaleButton, Button closeButton, ConfigurableApplicationContext context) {
        setWindowTitleButtons(titleBar, minimizeButton, scaleButton, closeButton);
        closeButton.setOnAction(actionEvent -> {
            if (context != null) {
                context.close();
            }
            Platform.exit();
        });
    }

    public void setGoLinkBaseTitleCommands(GoLink goLink) {
        setWindowTitleButtons(goLink.getTitleBar(), goLink.getMinimizeButton(), goLink.getScaleButton(), goLink.getCloseButton());
        goLink.getCloseButton().setOnAction(actionEvent -> {
            goLink.setGoLinkTrayIcon(new GoLinkTrayIcon(goLink.getScene(), goLink.getUserEntity(), goLink.getTCPConnection()));
            goLink.exitChat();
        });
    }

    private void setWindowTitleButtons(Pane titleBar, Button minimizeButton, Button scaleButton, Button closeButton) {
        minimizeButton.setOnMouseEntered(mouseEvent -> minimizeButton.setStyle("-fx-background-color: GRAY; -fx-text-fill: white"));
        minimizeButton.setOnMouseExited(mouseEvent -> minimizeButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0"));
        scaleButton.setOnMouseEntered(mouseEvent -> scaleButton.setStyle("-fx-background-color: GRAY; -fx-text-fill: white"));
        scaleButton.setOnMouseExited(mouseEvent -> scaleButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0"));
        closeButton.setOnMouseEntered(mouseEvent -> closeButton.setStyle("-fx-background-color: red; -fx-text-fill: white"));
        closeButton.setOnMouseExited(mouseEvent -> closeButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0"));
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

    public void setConfig(TextField phone) {
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

    public BorderPane newChatBuilder(UserEntity myAccount, ContactCash contactCash, PersonalChatEntity personalChatEntity) {
        BorderPane borderPane = new BorderPane();
        borderPane.setCursor(Cursor.HAND);
        StackPane stackImg = new StackPane();
        GNAvatarView avatar = new GNAvatarView();
        avatar.setImage(new Image(new ByteArrayInputStream(contactCash.getInterlocutor().getAvatar())));
        avatar.setType(AvatarType.CIRCLE);
        avatar.setStroke(Paint.valueOf("#001933"));
        stackImg.getChildren().add(avatar);
        stackImg.setPrefHeight(60);
        stackImg.setPrefWidth(60);
        if (false) {
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
        Text name = new Text(contactCash.getInterlocutor().getName());
        name.setTextAlignment(TextAlignment.LEFT);
        name.setFont(Font.font("System", 18));
        name.setFill(Paint.valueOf("white"));
        nameAndLastMessage.setTop(name);
        TextField lastMessageInfo = new TextField();
        lastMessageInfo.setStyle("-fx-background-color: #001933; -fx-text-fill: white");
        lastMessageInfo.setDisable(true);
        nameAndLastMessage.setLeft(lastMessageInfo);
        BorderPane.setMargin(lastMessageInfo, new Insets(0, 0, 0, -8));
        List<MessageEntity> messages = contactCash.getChat().getMessages();
        nameAndLastMessage.setPadding(new Insets(0, 0, 0, 10));
        borderPane.setCenter(nameAndLastMessage);
        VBox vBox = new VBox();
        vBox.setSpacing(5);
        Text date = new Text();
        if (messages != null && !messages.isEmpty()) {
            messages = messages.stream().sorted(Comparator.comparing(MessageEntity::getDate).reversed()).toList();

            MessageEntity lastMessage = messages.get(0);

            switch (lastMessage.getMessageType()) {
                case MESSAGE -> {
                    if (lastMessage.getText().isEmpty()) {
                        lastMessageInfo.setText(lastMessage.getSender().getId().equals(myAccount.getId()) ? "Вы: фотография" : "Фотография");
                    } else {
                        lastMessageInfo.setText((lastMessage.getSender().getId().equals(myAccount.getId()) ? "Вы: " : " ") + lastMessage.getText());
                    }
                }
                case AUDIO ->
                        lastMessageInfo.setText(lastMessage.getSender().getId().equals(myAccount.getId()) ? "Вы: голосовое сообщение" : "Голосовое сообщение");
                case DOCUMENT ->
                        lastMessageInfo.setText(lastMessage.getSender().getId().equals(myAccount.getId()) ? "Вы: документ" : "Документ");
            }
            date.setText(lastMessage.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
            contactCash.setLastMessage(lastMessage);

        } else {
            lastMessageInfo.setText("Чат пуст");
        }
        date.setFill(Paint.valueOf("white"));
        vBox.getChildren().add(date);
        Translate translate = new Translate();
        translate.setX(-7);
        vBox.getTransforms().add(translate);

        int newMessages = personalChatEntity.getNewMessagesCount();

        StackPane newMessagesVisualize = new StackPane();
        Circle circle = new Circle();
        circle.setRadius(12);
        circle.setFill(Paint.valueOf("#80c3ff"));
        circle.setStroke(Paint.valueOf("black"));
        Text newMessageCount = new Text(newMessages >= 100 ? "99+" : String.valueOf(newMessages));
        newMessagesVisualize.getChildren().add(circle);
        newMessagesVisualize.getChildren().add(newMessageCount);
        vBox.getChildren().add(newMessagesVisualize);
        if (newMessages == 0) {
            newMessagesVisualize.setVisible(false);
        }
        borderPane.setRight(vBox);

        borderPane.setOnMouseEntered(mouseEvent -> {
            lastMessageInfo.setStyle("-fx-background-color: #34577F; -fx-text-fill: white");
            borderPane.setStyle("-fx-background-color: #34577F;");
        });
        borderPane.setOnMouseExited(mouseEvent -> {
            lastMessageInfo.setStyle("-fx-background-color: #001933; -fx-text-fill: white");
            borderPane.setStyle("-fx-background-color: #001933;");
        });

        contactCash.setNameAndLastMessage(nameAndLastMessage);
        contactCash.setDate(date);
        contactCash.setLastMessageText(lastMessageInfo);
        contactCash.setNewMessagesBlock(newMessagesVisualize);
        contactCash.setNewMessagesCount(newMessageCount);

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
        avatar.setImage(new Image(new ByteArrayInputStream(user.getAvatar())));
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
        deleteImage.setFitWidth(20);
        deleteImage.setFitHeight(20);
        AnchorPane deletePane = new AnchorPane();
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
        imageView.setFitWidth(20);
        imageView.setFitHeight(20);
        AnchorPane copyPane = new AnchorPane();
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

        buttonEdit.setTextFill(Color.WHITE);
        edit.setOnMouseEntered(mouseEvent1 -> {
            edit.setStyle("-fx-background-color: silver");
            buttonEdit.setStyle("-fx-background-color: silver;");
        });
        edit.setOnMouseExited(mouseEvent1 -> {
            edit.setStyle("-fx-background-color: #18314D");
            buttonEdit.setStyle("-fx-background-color: #18314D; -fx-text-fill: white");
        });
        buttonEdit.setPrefWidth(122);
        buttonEdit.setPrefHeight(31);
        buttonEdit.setStyle("-fx-background-color:  #18314D; -fx-text-fill: white");
        ImageView editImage = new ImageView(new Image(Objects.requireNonNull(GoLink.class.getResourceAsStream("/img/editTXT.png"))));

        editImage.setFitWidth(20);
        editImage.setFitHeight(20);
        AnchorPane editPane = new AnchorPane();

        editPane.setPrefWidth(5);
        editPane.setPrefHeight(31);
        edit.setLeft(editPane);
        edit.setCenter(editImage);
        edit.setRight(buttonEdit);
        return edit;
    }

    @SneakyThrows
    public MessageCash printMyAudioGUIAndGetCash(MessageEntity message, File file) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.setPadding(new Insets(0, 15, 0, 0));
        BorderPane borderPane = new BorderPane();

        borderPane.setStyle("-fx-background-color: rgb(15, 125, 242); -fx-background-radius: 15 0 25 20;");
        String hasBeenChanged = message.isHasBeenChanged() ? "изменено " : "";
        Text date = new Text(hasBeenChanged + message.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
        date.setFill(Color.color(0.934, 0.925, 0.996));
        TextFlow dateFlow = new TextFlow(date);
        dateFlow.setPadding(new Insets(0, 12, 5, 10));
        dateFlow.setTextAlignment(TextAlignment.RIGHT);
        borderPane.setBottom(dateFlow);

        ImageView imageView = new ImageView();
        imageView.setCursor(Cursor.HAND);
        BorderPane.setMargin(imageView, new Insets(10, 0, 0, 10));

        borderPane.setLeft(imageView);
        Text timer = new Text();
        ProgressBar progressBar = new ProgressBar();
        new AudioPlayer(file, progressBar, duration -> {
            long totalSeconds = (long) duration.toSeconds();
            int minutes = (int) (totalSeconds / 60);
            int seconds = (int) (totalSeconds % 60);
            timer.setText(String.format("%d%d:%d%d", minutes / 10, minutes % 10, seconds / 10, seconds % 10));
        }, true, imageView);

        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        BorderPane.setMargin(progressBar, new Insets(10, 0, 0, 5));
        progressBar.setPrefWidth(180);
        borderPane.setCenter(progressBar);
        BorderPane.setMargin(timer, new Insets(20, 10, 0, 5));
        timer.setFill(Color.WHITE);
        borderPane.setPrefWidth(280);
        borderPane.setRight(timer);
        hBox.getChildren().add(borderPane);
        return new MessageCash(hBox, borderPane, message);
    }

    public MessageCash printForeignAudioGUIAndGetCash(MessageEntity message, File file) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);

        hBox.setPadding(new Insets(0, 0, 0, 10));
        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: rgb(233, 233, 235); -fx-background-radius:  0 15 20 25;");
        String hasBeenChanged = message.isHasBeenChanged() ? "изменено " : "";
        Text date = new Text(hasBeenChanged + message.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
        date.setFill(Color.color(0, 0, 0));
        TextFlow dateFlow = new TextFlow(date);
        dateFlow.setPadding(new Insets(0, 10, 5, 12));
        dateFlow.setTextAlignment(TextAlignment.LEFT);
        borderPane.setBottom(dateFlow);

        ImageView imageView = new ImageView();
        imageView.setCursor(Cursor.HAND);
        BorderPane.setMargin(imageView, new Insets(10, 0, 0, 10));

        borderPane.setLeft(imageView);
        Text timer = new Text();
        ProgressBar progressBar = new ProgressBar();
        new AudioPlayer(file, progressBar, duration -> {
            long totalSeconds = (long) duration.toSeconds();
            int minutes = (int) (totalSeconds / 60);
            int seconds = (int) (totalSeconds % 60);
            timer.setText(String.format("%d%d:%d%d", minutes / 10, minutes % 10, seconds / 10, seconds % 10));
        }, false, imageView);

        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        BorderPane.setMargin(progressBar, new Insets(10, 0, 0, 5));
        progressBar.setPrefWidth(180);
        borderPane.setCenter(progressBar);
        BorderPane.setMargin(timer, new Insets(20, 10, 0, 5));
        timer.setFill(Color.BLACK);
        borderPane.setPrefWidth(280);
        borderPane.setRight(timer);
        hBox.getChildren().add(borderPane);

        return new MessageCash(hBox, borderPane, message);
    }

    public MessageCash makeForeignMessageGUIAndGetCash(MessageEntity message) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new Insets(0, 0, 0, 10));

        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: rgb(233, 233, 235); -fx-background-radius:  0 15 20 25;");
        String hasBeenChanged = message.isHasBeenChanged() ? "изменено " : "";
        Text date = new Text(hasBeenChanged + message.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
        date.setFill(Color.BLACK);
        TextFlow dateFlow = new TextFlow(date);
        dateFlow.setPadding(new Insets(0, 10, 5, 12));
        dateFlow.setTextAlignment(TextAlignment.LEFT);
        borderPane.setBottom(dateFlow);

        ImageView imageView = new ImageView();
        if (message.getMetadata() != null) {
            setImageConfig(message.getMetadata(), imageView, borderPane);
        }

        Text messageText = new Text();
        if (!message.getText().isEmpty()) {
            messageText.setText(message.getText());

            TextFlow textFlow = new TextFlow(messageText);
            textFlow.setStyle("-fx-font-family: Arial; -fx-font-size: 14px;");
            textFlow.setPadding(new Insets(5, 10, 3, 10));

            borderPane.setCenter(textFlow);
        }

        hBox.getChildren().add(borderPane);

        return new MessageCash(hBox, borderPane, message, imageView, messageText, date);
    }

    public MessageCash makeMyMessageGUIAndGetCash(MessageEntity message) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.setPadding(new Insets(0, 15, 0, 0));

        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: rgb(15, 125, 242); -fx-background-radius: 15 0 25 20;");
        String hasBeenChanged = message.isHasBeenChanged() ? "изменено " : "";
        Text date = new Text(hasBeenChanged + message.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
        date.setFill(Color.WHITE);
        TextFlow dateFlow = new TextFlow(date);
        dateFlow.setPadding(new Insets(0, 12, 5, 10));
        dateFlow.setTextAlignment(TextAlignment.RIGHT);
        borderPane.setBottom(dateFlow);

        ImageView imageView = new ImageView();
        if (message.getMetadata() != null) {
            setImageConfig(message.getMetadata(), imageView, borderPane);
        }

        Text messageText = new Text();
        messageText.setFill(Color.WHITE);
        if (!message.getText().isEmpty()) {
            messageText.setText(message.getText());

            TextFlow textFlow = new TextFlow(messageText);
            textFlow.setStyle("-fx-font-family: Arial; -fx-font-size: 14px;");
            textFlow.setPadding(new Insets(5, 10, 3, 10));

            borderPane.setCenter(textFlow);
        }

        hBox.getChildren().add(borderPane);

        return new MessageCash(hBox, borderPane, message, imageView, messageText, date);
    }

    public static void setImageConfig(byte[] imageBytes, ImageView imageView, BorderPane borderPane) {
        if (imageBytes == null || imageBytes.length == 0) {
            Platform.runLater(() -> {
                borderPane.setTop(null);
                imageView.setImage(null);
            });
        } else {
            Platform.runLater(() -> {
                borderPane.setTop(imageView);
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                double width = image.getWidth();
                double height = image.getHeight();
                if (width > 400) {
                    imageView.setFitWidth(400);
                } else {
                    imageView.setFitWidth(width);
                }
                if (height > 400) {
                    imageView.setFitHeight(400);
                } else {
                    imageView.setFitHeight(height);
                }
                imageView.setImage(image);
                imageView.setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getButton() != MouseButton.SECONDARY) {
                        Stage imageScene = new Stage();
                        ImageView enlargedImageView = new ImageView(image);
                        enlargedImageView.setFitWidth(width);
                        enlargedImageView.setFitHeight(height);
                        enlargedImageView.setPreserveRatio(true);
                        Scene scene = new Scene(new Pane(enlargedImageView));
                        imageScene.setScene(scene);
                        imageScene.show();
                    }
                });
            });
        }
    }

    public Pair<BorderPane, ImageView> getBackgroundSelectImageInterface() {
        BorderPane GUI = new BorderPane();
        ImageView selectedImage = new ImageView();
        selectedImage.setFitWidth(40);
        selectedImage.setFitHeight(40);
        BorderPane editMessageProperty = new BorderPane();
        Text redact = new Text("Добавить подпись");
        redact.setFont(Font.font(17));
        redact.setFill(Paint.valueOf("WHITE"));
        redact.prefWidth(766);
        redact.prefHeight(22);
        editMessageProperty.setCenter(redact);
        GUI.setCenter(editMessageProperty);
        GUI.setLeft(selectedImage);
        BorderPane.setMargin(editMessageProperty, new Insets(0, 0, 0, 10));
        BorderPane.setMargin(selectedImage, new Insets(5, 0, 5, 5));
        return new Pair<>(GUI, selectedImage);
    }

    public Pair<BorderPane, ImageView> getBackgroundEditInterface(MessageCash messageCash) {
        BorderPane editInterfaceBackground = new BorderPane();
        ImageView editImage = new ImageView();
        editImage.setFitWidth(40);
        editImage.setFitHeight(40);
        BorderPane editMessageProperty = new BorderPane();
        Text redact = new Text("Редактирование");
        redact.setFont(Font.font(17));
        redact.setFill(Paint.valueOf("WHITE"));
        redact.prefWidth(766);
        redact.prefHeight(22);
        TextField editableText = new TextField();
        if (!messageCash.getMessage().getText().isEmpty()) {
            editableText.setText(messageCash.getMessage().getText());
        } else {
            editableText.setText("вы можете добавить подпись");
        }
        if (messageCash.getImage() == null) {
            editImage.setImage(new Image(Objects.requireNonNull(GoLink.class.getResourceAsStream("/img/editTXT.png"))));
        } else {
            editImage.setImage(messageCash.getImage());
        }
        editableText.setPrefWidth(806);
        editableText.setPrefHeight(29);
        editableText.setEditable(false);
        editableText.setStyle("-fx-background-color: #001933; -fx-text-fill: white");
        editMessageProperty.setCenter(editableText);
        editMessageProperty.setTop(redact);
        editInterfaceBackground.setCenter(editMessageProperty);
        editInterfaceBackground.setLeft(editImage);
        BorderPane.setMargin(editableText, new Insets(0, 0, 0, -5));
        BorderPane.setMargin(editMessageProperty, new Insets(0, 0, 0, 10));
        BorderPane.setMargin(editImage, new Insets(5, 0, 0, 5));
        return new Pair<>(editInterfaceBackground, editImage);
    }

    public ImageView getCancelButton() {
        ImageView cancel = new ImageView(new Image(Objects.requireNonNull(GoLink.class.getResourceAsStream("/img/cancel.png"))));
        cancel.setCursor(Cursor.HAND);
        cancel.setFitWidth(30);
        cancel.setFitHeight(30);
        return cancel;
    }

    public void setEmptyChatConfiguration(VBox chat) {
        chat.getChildren().clear();
        AnchorPane anchorPane = new AnchorPane();
        anchorPane.setPrefWidth(876);
        anchorPane.setPrefHeight(382);
        TextField textField = new TextField("Выберите, кому хотели бы написать");
        textField.setFont(Font.font(17));
        textField.setAlignment(Pos.CENTER);
        textField.setStyle("-fx-text-fill: white; -fx-background-color: #001933");
        chat.getChildren().add(anchorPane);
        chat.getChildren().add(textField);
    }

//    private boolean checkMessageWindowStatusBeforeOpen(Long messageId, Stage stage) {
//        if (messageStage == null) {
//            messageStage = new MessageStage();
//            messageStage.setMessageId(messageId);
//            messageStage.setMessagesStage(stage);
//        } else {
//            if (!Objects.equals(messageId, messageStage.getMessageId())) {
//                messageStage.getMessagesStage().close();
//                messageStage.setMessagesStage(stage);
//                messageStage.setMessageId(messageId);
//            } else {
//                return true;
//            }
//        }
//        return false;
//    }
}