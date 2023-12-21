package ru.nikidzawa.golink.FXControllers;

import io.github.gleidson28.AvatarType;
import io.github.gleidson28.GNAvatarView;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.GUIPatterns.WindowTitle;
import ru.nikidzawa.golink.GUIPatterns.MenuItems;
import ru.nikidzawa.golink.network.TCPConnection;
import ru.nikidzawa.golink.network.TCPConnectionListener;
import ru.nikidzawa.golink.services.GoMessage.GoMessageListener;
import ru.nikidzawa.golink.services.GoMessage.TCPBroker;
import ru.nikidzawa.golink.services.SystemOfControlServers.SOCSConnection;
import ru.nikidzawa.golink.store.entities.ChatEntity;
import ru.nikidzawa.golink.store.entities.MessageEntity;
import ru.nikidzawa.golink.store.entities.UserEntity;
import ru.nikidzawa.golink.store.repositories.ChatRepository;
import ru.nikidzawa.golink.store.repositories.MessageRepository;
import ru.nikidzawa.golink.store.repositories.UserRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private Text status;

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
    private ImageView settings;

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
                    List<UserEntity> userEntities = userRepository.findByNickname(newValue);
                    userEntities.forEach(interlocutor -> {
                        BorderPane contact = newChatBuilder(interlocutor.getName() + interlocutor.getId());
                        chats.getChildren().add(contact);
                        contact.setOnMouseClicked(mouseEvent -> {
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
                                        return newChat;
                                    });
                            openChat(chat1, interlocutor);
                        });
                    });
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
                    chatRepository.saveAndFlush(chatEnt);
                    try {
                        tcpConnection = new TCPConnection(new Socket("localhost", newPort), this, userId);
                        tcpBroker.sendMessage("UPDATE_CHATS:" + interlocutor.getId());
                    } catch (IOException exc) {
                        throw new RuntimeException(exc);
                    }
                }
                chat.getChildren().clear();
                chatRoomName.setText(interlocutor.getName());
                printMessages(chatEnt);
                send.setOnAction(actionEvent -> {
                    String text = input.getText();
                    if (text != null) {
                        boolean bool = Boolean.parseBoolean(new SOCSConnection().CHECK_USER(chatEnt.getPort(),
                                interlocutor.getId().toString()));
                        if (!bool) {
                            tcpBroker.sendMessage("NOTIFICATION:" + interlocutor.getId() + ":" + chatEnt.getId());
                        } else {
                            System.out.println("пользователь в сети");
                        }
                        MessageEntity message = MessageEntity.builder()
                                .message(text).date(LocalDateTime.now()).sender(userEntity).build();
                        chatEnt.setMessages(message);
                        messageRepository.saveAndFlush(message);
                        chatRepository.saveAndFlush(chatEnt);
                        tcpConnection.sendMessage(text);
                        chat.getChildren().add(printMyMessage(text, LocalDateTime.now()));
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
                chat.getChildren().add(printMyMessage(message.getMessage(), message.getDate()));
            } else {
                chat.getChildren().add(printForeignMessage(message.getMessage(), message.getDate()));
            }
        });
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

    private HBox printMyMessage(String message, LocalDateTime localDateTime) {
        input.clear();
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.setPadding(new Insets(5, 5, 3, 10));
        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: rgb(15, 125, 242); -fx-background-radius: 20px;");
        Text date = new Text(localDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        TextFlow dateFlow = new TextFlow(date);
        Text text = new Text(message);
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
        borderPane.setRight(vBox);


        return borderPane;
    }

    private void setTextStyle(Text text) {
        text.setFont(Font.font("System", 18));
        text.setFill(Paint.valueOf("white"));
    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {

    }

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
            this.tcpBroker.sendMessage("UPDATE_CHATS:" + user.getId());
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
            case "update":
                Platform.runLater(this::updateChats);
                break;
            case "NOTIFICATION":
                System.out.println("получено новое сообщение в чате " + value );
                break;
        }
    }

    @Override
    public void onDisconnect(TCPBroker tcpBroker) {

    }


    @Override
    public void onException(TCPBroker tcpBroker, Exception ex) {
    }
}