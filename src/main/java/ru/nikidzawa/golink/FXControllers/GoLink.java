package ru.nikidzawa.golink.FXControllers;

import io.github.gleidson28.AvatarType;
import io.github.gleidson28.GNAvatarView;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.FXControllers.helpers.MessageStage;
import ru.nikidzawa.golink.GUIPatterns.WindowTitle;
import ru.nikidzawa.golink.GUIPatterns.MenuItems;
import ru.nikidzawa.golink.network.TCPConnection;
import ru.nikidzawa.golink.network.TCPConnectionListener;
import ru.nikidzawa.golink.services.GoMessage.GoMessageListener;
import ru.nikidzawa.golink.services.GoMessage.TCPBroker;
import ru.nikidzawa.golink.services.SystemOfControlServers.SOCSConnection;
import ru.nikidzawa.golink.store.entities.*;
import ru.nikidzawa.golink.store.repositories.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Controller
public class GoLink implements TCPConnectionListener, GoMessageListener {

    @FXML
    private VBox chat;

    @FXML
    private Text chatRoomName;

    @FXML
    private VBox chats;

    @FXML
    private Button closeButton;

    @FXML
    private TextField input;

    @FXML
    private Button minimizeButton;

    @FXML
    private Text profileName;

    @FXML
    private Button scaleButton;

    @FXML
    private TextField searchPanel;

    @FXML
    private Button send;

    @FXML
    private BorderPane sendZone;

    @FXML
    private ImageView settings;

    @FXML
    private Text status;

    @FXML
    private Pane titleBar;

    private TCPConnection tcpConnection;
    private TCPBroker tcpBroker;

    @Autowired
    UserRepository userRepository;
    @Autowired
    ChatRepository chatRepository;
    @Autowired
    MessageRepository messageRepository;

    @Setter
    private UserEntity userEntity;
    private ChatEntity selectedChat;

    private MessageStage messageStage;
    private boolean editable = false;
    private UserEntity interlocutor;
    @FXML
    @SneakyThrows
    void initialize() {
        Platform.runLater(() -> {
            try {
                tcpBroker = new TCPBroker(new Socket("localhost", 8081), this, userEntity.getId().toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            WindowTitle.setBaseCommands(titleBar, minimizeButton, scaleButton, closeButton, tcpBroker, userEntity, userRepository, chatRepository);

            MenuItems.makeInput(input);
            chats.setSpacing(10);
            searchPanel.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    chats.getChildren().clear();
                    Optional<UserEntity> user = userRepository.findByNickname(newValue);
                    if (user.isPresent()) {
                        UserEntity interlocutor = user.get();
                        BorderPane contact = newChatBuilder(interlocutor.getName() + interlocutor.getId());
                        chats.getChildren().add(contact);
                        contact.setOnMouseClicked(mouseEvent -> {
                            searchPanel.clear();
                            ChatEntity chat1 = chatRepository.findByParticipantsContaining(interlocutor)
                                    .stream()
                                    .filter(chatEntity -> chatEntity.getParticipants().stream()
                                            .anyMatch(userEntity1 -> Objects.equals(userEntity1.getId(), userEntity.getId())))
                                    .findFirst()
                                    .orElseGet(() -> {
                                        ChatEntity newChat = ChatEntity.builder()
                                                .port(Integer.parseInt(new SOCSConnection().CREATE_SERVER()))
                                                .participants(List.of(userEntity, interlocutor))
                                                .build();
                                        chatRepository.saveAndFlush(newChat);
                                        openChat(newChat, interlocutor);
                                        tcpBroker.sendMessage("UPDATE_CHAT_ROOMS:" + interlocutor.getId());
                                        return newChat;
                                    });
                            openChat(chat1, interlocutor);
                        });
                    }
                    if (searchPanel.getText().isEmpty()) {
                        updateChats();
                    }
                }
            });
            MenuItems.makeSearch(searchPanel);
            profileName.setText(userEntity.getName());
            updateChats();
        });

    }

    private void updateChats() {
        chats.getChildren().clear();
        chatRepository.findByParticipantsContaining(userEntity).forEach(chatEnt -> {
            UserEntity interlocutor = chatEnt.getParticipants().stream()
                    .filter(userEntity1 -> !Objects.equals(userEntity1.getId(), userEntity.getId())).findFirst().get();

            BorderPane contact = newChatBuilder(interlocutor, chatEnt);
            chats.getChildren().add(contact);
            contact.setOnMouseClicked(e -> openChat(chatEnt, interlocutor));
        });
    }

    private void openChat (ChatEntity chatEnt, UserEntity interlocutor) {
            status.setVisible(true);
            if (selectedChat != chatEnt) {
                this.interlocutor = interlocutor;
                input.setVisible(true);
                input.setStyle("-fx-background-color: #001933; -fx-border-color: blue; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
                send.setVisible(true);

                selectedChat = chatEnt;
                if (tcpConnection != null) {
                    tcpConnection.disconnect();
                }
                String userId = userEntity.getId().toString();
                try {
                    tcpConnection = new TCPConnection(new Socket("localhost", chatEnt.getPort()), this, userId);
                } catch (IOException ex) {
                    int newPort = Integer.parseInt(new SOCSConnection().CREATE_SERVER());
                    chatEnt.setPort(newPort);
                    try {
                        chatRepository.saveAndFlush(chatEnt);
                    } catch (JpaObjectRetrievalFailureException exc) {

                    }
                    try {
                        tcpConnection = new TCPConnection(new Socket("localhost", newPort), this, userId);
                        tcpBroker.sendMessage("UPDATE_CHAT_ROOMS:" + interlocutor.getId());
                    } catch (IOException exc) {
                        throw new RuntimeException(exc);
                    }
                }
                chat.getChildren().clear();
                chatRoomName.setText(interlocutor.getName());
                printMessages(chatEnt);
                send.setOnAction(actionEvent -> {
                    if (editable) {return;}
                    String text = input.getText();
                    if (text != null) {
                        if (Boolean.parseBoolean(new SOCSConnection().CHECK_USER(chatEnt.getPort(),
                                interlocutor.getId().toString()))) {
                            System.out.println("пользователь в сети");
                        } else {
                            tcpBroker.sendMessage("NOTIFICATION:" + interlocutor.getId() + ":" + chatEnt.getId());
                        }
                        MessageEntity message = MessageEntity.builder()
                                .message(text).date(LocalDateTime.now()).sender(userEntity).build();

                        try {
                            try {
                                chatEnt.getMessages().clear();
                            } catch (NullPointerException ex) {}
                            chatEnt.setMessages(message);
                            messageRepository.saveAndFlush(message);
                            chatRepository.saveAndFlush(chatEnt);
                        } catch (JpaObjectRetrievalFailureException ex) {}
                        tcpConnection.sendMessage(text);
                        chat.getChildren().add(printMyMessage(message, chatEnt, LocalDateTime.now()));
                    }
                });
            }
    }

    private void printMessages(ChatEntity chatEntity) {
        List<MessageEntity> sortedMessages = chatRepository.findById(chatEntity.getId())
                .get()
                .getMessages()
                .stream().sorted(Comparator.comparing(MessageEntity::getDate))
                .toList();
        sortedMessages.forEach(message -> {
            if (message.getSender().getId().equals(userEntity.getId())) {
                chat.getChildren().add(printMyMessage(message, chatEntity, message.getDate()));
            } else {
                chat.getChildren().add(printForeignMessage(message.getMessage(), message.getDate()));
            }
        });
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

    private void openMessageWindow(MessageEntity message, MouseEvent mouseEvent, ChatEntity chatEntity, HBox hBox) {
        double mouseX = mouseEvent.getScreenX();
        double mouseY = mouseEvent.getScreenY();

        Stage messageStage = new Stage();
        if (checkMessageWindowStatusBeforeOpen(message.getId(), messageStage)) {
            return;
        }
        messageStage.initStyle(StageStyle.UNDECORATED);
        checkMessageWindowStatusBeforeOpen(message.getId(), messageStage);



        VBox vBox = new VBox();
        vBox.setStyle("-fx-background-color:  #18314D; -fx-border-color: black; -fx-spacing: 5");

        BorderPane copy = new BorderPane();
        copy.setPrefWidth(143);
        copy.setPrefHeight(31);
        Button buttonCopy = new Button("Копировать");
        buttonCopy.setOnMouseClicked(mouseEvent1 -> {
            String text = message.getMessage();
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
            messageStage.close();
        });
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

        BorderPane edit = new BorderPane();
        edit.setPrefWidth(143);
        edit.setPrefHeight(31);
        Button buttonEdit = new Button("Изменить");
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
        buttonEdit.setOnMouseClicked(mouseEvent1 -> {
            editMessage(message, editImage, chatEntity);
            messageStage.close();
        });
        editImage.setFitWidth(20);
        editImage.setFitHeight(20);
        AnchorPane editPane = new AnchorPane();
        editPane.setPrefWidth(5);
        editPane.setPrefHeight(31);
        edit.setLeft(editPane);
        edit.setCenter(editImage);
        edit.setRight(buttonEdit);

        BorderPane delete = new BorderPane();
        delete.setPrefWidth(143);
        delete.setPrefHeight(31);
        Button buttonDelete = new Button("Удалить");
        buttonDelete.setOnMouseClicked(mouseEvent1 -> {
            CompletableFuture.runAsync(() -> {
                messageRepository.delete(message);
                chatEntity.getMessages().clear();
                messageRepository.flush();
                if (Boolean.parseBoolean(new SOCSConnection().CHECK_USER(chatEntity.getPort(), interlocutor.getId().toString()))) {
                    tcpBroker.sendMessage("UPDATE_MESSAGES:" + interlocutor.getId());
                }
            }).thenRun(() -> {
                Platform.runLater(() -> {
                    chat.getChildren().remove(hBox);
                });
            });
            messageStage.close();
        });
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

        vBox.getChildren().add(copy);
        vBox.getChildren().add(edit);
        vBox.getChildren().add(delete);

        Scene scene = new Scene(vBox, 154, 105);
        messageStage.setScene(scene);

        messageStage.setX(mouseX);
        messageStage.setY(mouseY);
        messageStage.show();
    }

    private HBox printForeignMessage(String message, LocalDateTime localDateTime) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new Insets(5, 5, 5, 10));
        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: rgb(233, 233, 235); -fx-background-radius: 20px;");
        Text date = new Text(localDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        TextFlow dateFlow = new TextFlow(date);
        Text text = new Text(message);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle(
                "-fx-font-family: Arial;" +
                        "-fx-font-size: 14px;"
        );
        textFlow.setPadding(new Insets(5, 10, 3, 10));
        dateFlow.setPadding(new Insets(0, 10, 5, 10));

        borderPane.setTop(textFlow);
        dateFlow.setTextAlignment(TextAlignment.LEFT);
        borderPane.setBottom(dateFlow);

        hBox.getChildren().add(borderPane);
        return hBox;
    }

    private HBox printMyMessage(MessageEntity message, ChatEntity chat, LocalDateTime localDateTime) {
        input.clear();
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.setPadding(new Insets(5, 5, 3, 10));
        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: rgb(15, 125, 242); -fx-background-radius: 20px;");
        Text date = new Text(localDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        TextFlow dateFlow = new TextFlow(date);
        Text text = new Text(message.getMessage());
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle(
                "-fx-color: rgb(239, 242, 255);" +
                        "-fx-font-family: Arial;" + "-fx-font-size: 14px;"
        );
        textFlow.setPadding(new Insets(5, 10, 5, 10));
        text.setFill(Color.color(0.934, 0.925, 0.996));
        date.setFill(Color.color(0.934, 0.925, 0.996));

        borderPane.setTop(textFlow);
        dateFlow.setTextAlignment(TextAlignment.RIGHT);
        dateFlow.setPadding(new Insets(0, 10, 5, 10));
        borderPane.setBottom(dateFlow);
        hBox.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                openMessageWindow(message, mouseEvent, chat, hBox);
            }
        });
        hBox.getChildren().add(borderPane);
        return hBox;
    }

    private BorderPane newChatBuilder(String userName) {
        BorderPane borderPane = new BorderPane();
        borderPane.setOnMouseEntered(mouseEvent -> {
            borderPane.setStyle("-fx-background-color: #34577F;");
        });
        borderPane.setOnMouseExited(mouseEvent -> {
            borderPane.setStyle("-fx-background-color: #001933;");
        });
        StackPane stackImg = new StackPane();
        GNAvatarView avatar = new GNAvatarView();
        try {
            avatar.setImage(new Image(String.valueOf(new URL(getClass().getResource("/img/ava.jpg").toExternalForm()))));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        avatar.setType(AvatarType.CIRCLE);
        avatar.setStroke(Paint.valueOf("#001933"));
        stackImg.getChildren().add(avatar);
        stackImg.setPrefHeight(60);
        stackImg.setPrefWidth(60);
        borderPane.setLeft(stackImg);
        StackPane nameStack = new StackPane();
        Text name = new Text(userName);
        name.setWrappingWidth(170);
        name.setTextAlignment(TextAlignment.LEFT);
        setTextStyle(name);
        nameStack.getChildren().add(name);
        borderPane.setCenter(nameStack);

        return borderPane;
    }

    private BorderPane newChatBuilder(UserEntity user, ChatEntity chat) {
        BorderPane borderPane = new BorderPane();
        borderPane.setOnMouseEntered(mouseEvent -> {
            borderPane.setStyle("-fx-background-color: #34577F;");
        });
        borderPane.setOnMouseExited(mouseEvent -> {
            borderPane.setStyle("-fx-background-color: #001933;");
        });
        StackPane stackImg = new StackPane();
        GNAvatarView avatar = new GNAvatarView();
        try {
            avatar.setImage(new Image(String.valueOf(new URL(getClass().getResource("/img/ava.jpg").toExternalForm()))));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        avatar.setType(AvatarType.CIRCLE);
        avatar.setStroke(Paint.valueOf("#001933"));
        stackImg.getChildren().add(avatar);
        stackImg.setPrefHeight(60);
        stackImg.setPrefWidth(60);
        if (user.isConnected()) {
            Circle circle = new Circle();
            circle.setFill(Paint.valueOf("GREEN"));
            circle.setRadius(7);
            circle.setStroke(Paint.valueOf("black"));
            stackImg.getChildren().add(circle);
            StackPane.setAlignment(circle, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(circle, new Insets(0, 7, 0, 0));
            status.setText("В сети");
        }
        else status.setText("Не в сети");
        borderPane.setLeft(stackImg);
        StackPane nameStack = new StackPane();
        Text name = new Text(user.getName());
        name.setWrappingWidth(170);
        name.setTextAlignment(TextAlignment.LEFT);
        name.setFont(Font.font("System", 18));
        name.setFill(Paint.valueOf("white"));
        nameStack.getChildren().add(name);
        borderPane.setCenter(nameStack);

        VBox vBox = new VBox();
        vBox.setSpacing(5);
        Text date = new Text();
        try {
            date.setText(chat.getMessages().get(chat.getMessages().size() - 1).getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
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

        return borderPane;
    }

    private void editMessage (MessageEntity message, ImageView editImg, ChatEntity chat)  {
        editable = true;
        BorderPane editInterfaceBackground = new BorderPane();
        editImg.setFitWidth(30);
        editImg.setFitHeight(30);
        editImg.setStyle("-fx-padding: 0 0 0 10 0");
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
        ImageView cancel = new ImageView(new Image(Objects.requireNonNull(GoLink.class.getResourceAsStream("/img/cancel.png"))));
        cancel.setFitWidth(30);
        cancel.setFitHeight(30);
        editMessageProperty.setCenter(editableText);
        editMessageProperty.setRight(cancel);
        editMessageProperty.setTop(redact);
        editInterfaceBackground.setCenter(editMessageProperty);
        editInterfaceBackground.setLeft(editImg);

        send.setOnMouseClicked(mouseEvent -> {
            if (editable) {
                message.setMessage(input.getText());
                input.clear();
                messageRepository.saveAndFlush(message);
                try {
                    chatRepository.saveAndFlush(chat);
                } catch (JpaObjectRetrievalFailureException exception) {
                }
                if (Boolean.parseBoolean(new SOCSConnection().CHECK_USER(chat.getPort(), interlocutor.getId().toString()))) {
                    tcpBroker.sendMessage("UPDATE_MESSAGES:" + interlocutor.getId());
                }
                editable = false;
                sendZone.setTop(null);
                updateMessages(chat);
            }
        });
        sendZone.setTop(editInterfaceBackground);
    }

    private void updateMessages (ChatEntity chat) {
        Platform.runLater(() -> {
            this.chat.getChildren().clear();
            printMessages(chat);
        });
    }

    private void setTextStyle(Text text) {
        text.setFont(Font.font("System", 18));
        text.setFill(Paint.valueOf("white"));
    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {}

    @Override
    public void onReceiveMessage(TCPConnection tcpConnection, String string) {
        if (!string.equals(userEntity.getId().toString())) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    chat.getChildren().add(printForeignMessage(string, LocalDateTime.now()));
                }
            });
        }
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        tcpConnection.disconnect();
    }

    @Override
    public void onException(TCPConnection tcpConnection, Exception ex) {
        tcpConnection.disconnect();
    }

    @Override
    public void onConnectionReady(TCPBroker tcpBroker) {
        userEntity.setConnected(true);
        userRepository.saveAndFlush(userEntity);
        chatRepository.findByParticipantsContaining(userEntity).forEach(chat1 -> {
            UserEntity user = chat1.getParticipants().stream().filter(user1 -> !Objects.equals(user1.getId(), userEntity.getId())).findFirst().get();
            this.tcpBroker.sendMessage("UPDATE_CHAT_ROOMS:" + user.getId());
        });
    }

    @Override
    public void onReceiveMessage(TCPBroker tcpBroker, String string) {
        String[] strings = string.split(":");
        String command = strings[0];
        String value;
        try {
            value = strings[1];
        } catch (ArrayIndexOutOfBoundsException ex) {
            value = null;
        }
        switch (command) {
            case "UPDATE_CHAT_ROOMS":
                Platform.runLater(this::updateChats);
                break;
            case "UPDATE_MESSAGES":
                updateMessages(selectedChat);
                break;
            case "NOTIFICATION":
                System.out.println("получено новое сообщение в чате " + value);
                break;
        }
    }

    @Override
    public void onDisconnect(TCPBroker tcpBroker) {}


    @Override
    public void onException(TCPBroker tcpBroker, Exception ex) {}
}