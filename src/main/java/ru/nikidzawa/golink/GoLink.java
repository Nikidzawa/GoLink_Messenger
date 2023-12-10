package ru.nikidzawa.golink;

import io.github.gleidson28.AvatarType;
import io.github.gleidson28.GNAvatarView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.SystemOfControlServers.SOCSConnection;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class GoLink implements TCPConnectionLink {

    @FXML
    private VBox chats;

    @FXML
    private Button send;

    @FXML
    private VBox chat;

    @FXML
    private Text chatRoomName;

    @FXML
    private TextField input;

    @FXML
    private Text profileName;
    private TCPConnection tcpConnection;

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
    void initialize() {
        userEntity = userRepository.findById(2L).get();

        profileName.setText(userEntity.getName());
        chatRepository.findByParticipantsContaining(userEntity).forEach(chatEnt -> {
            String name = chatEnt.getType() == ChatType.DIALOG || chatEnt.getType() == ChatType.ANONYMOUS_DIALOG ?
                    chatEnt.getParticipants()
                            .stream()
                            .filter(part -> !Objects.equals(part.getId(), userEntity.getId()))
                            .findFirst()
                            .get()
                            .getName()
                    : chatEnt.getName();
            BorderPane contact = newChatBuilder(name);
            chats.getChildren().add(contact);
            contact.setOnMouseClicked(e -> {
                if (tcpConnection != null) {
                    tcpConnection.disconnect();
                }
                try {
                    tcpConnection = new TCPConnection(new Socket("localhost", chatEnt.getPort()), this);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                chat.getChildren().clear();
                chatRoomName.setText(name);
                printMessages(chatEnt);
                send.setOnAction(actionEvent -> {
                    String text = input.getText();
                    MessageEntity message = new MessageEntity.MessageEntityBuilder()
                            .message(text).date(LocalDateTime.now()).sender(userEntity).build();
                    chatEnt.setMessages(message);
                    messageRepository.saveAndFlush(message);
                    chatRepository.saveAndFlush(chatEnt);
                    tcpConnection.sendMessage(text);
                    chat.getChildren().add(printMyMessage(text));

                });
            });
        });
    }
    private void printMessages (ChatEntity chatEntity) {
        List<MessageEntity> sortedMessages = chatRepository.findById(chatEntity.getId())
                .get()
                .getMessages()
                .stream().sorted(Comparator.comparing(MessageEntity::getDate))
                .toList();
        sortedMessages.forEach(message -> {
            if (message.getSender().getId().equals(userEntity.getId())) {
                chat.getChildren().add(printMyMessage(message.getMessage()));
            }
            else {
                chat.getChildren().add(printForeignMessage(message.getMessage()));
            }
        });
    }
    private HBox printForeignMessage (String message) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new Insets(5, 5, 5, 10));

        Text text = new Text(message);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle (
                "-fx-background-color: rgb(233, 233, 235);" +
                        "-fx-background-radius: 20px;" +
                        "-fx-font-family: Arial;" +
                        "-fx-font-size: 14px;"
        );
        textFlow.setPadding(new Insets(5, 10, 5, 10));
        hBox.getChildren().add(textFlow);

        return hBox;
    }
    private HBox printMyMessage(String message) {
        input.clear();
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.setPadding(new Insets(5, 5, 5, 10));

        Text text = new Text(message);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle (
                "-fx-color: rgb(239, 242, 255);" +
                        "-fx-background-color: rgb(15, 125, 242);" +
                        "-fx-background-radius: 20px;" +
                        "-fx-font-family: Arial;" + "-fx-font-size: 14px;"
        );
        textFlow.setPadding(new Insets(5, 10, 5, 10));
        text.setFill(Color.color(0.934, 0.925, 0.996));

        hBox.getChildren().add(textFlow);
        return hBox;
    }
    private BorderPane newChatBuilder (String userName) {
        chats.setSpacing(10);
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
            avatar.setImage(new Image(String.valueOf(new URL(getClass().getResource("/ava.jpg").toExternalForm()))));
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

        VBox vBox = new VBox();
        vBox.setSpacing(5);
        Text date = new Text("01.12.2023");
        date.setFill(Paint.valueOf("white"));
        StackPane newMessagesVisualize = new StackPane();
        Circle circle = new Circle();
        circle.setRadius(12);
        circle.setFill(Paint.valueOf("#80c3ff"));
        circle.setStroke(Paint.valueOf("black"));

        Text newMessageCount = new Text((0 >= 100 ? "99+" : String.valueOf(0)));
        newMessagesVisualize.getChildren().add(circle);
        newMessagesVisualize.getChildren().add(newMessageCount);
        vBox.getChildren().add(date);
        vBox.getChildren().add(newMessagesVisualize);
        borderPane.setRight(vBox);


        return borderPane;
    }
    private void setTextStyle (Text text) {
        text.setFont(Font.font("System", 18));
        text.setFill(Paint.valueOf("white"));
    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {

    }

    @Override
    public void onReceiveMessage(TCPConnection tcpConnection, String string) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                chat.getChildren().add(printForeignMessage(string));
            }
        });
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {

    }

    @Override
    public void onException(TCPConnection tcpConnection, Exception ex) {

    }
}