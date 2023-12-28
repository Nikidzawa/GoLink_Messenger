package ru.nikidzawa.golink.FXControllers;

import io.github.gleidson28.GNAvatarView;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.FXControllers.helpers.GUIPatterns;
import ru.nikidzawa.golink.network.TCPConnection;
import ru.nikidzawa.golink.network.TCPConnectionListener;
import ru.nikidzawa.golink.services.GoMessage.GoMessageListener;
import ru.nikidzawa.golink.services.GoMessage.TCPBroker;
import ru.nikidzawa.golink.services.SystemOfControlServers.SOCSConnection;
import ru.nikidzawa.golink.store.entities.*;
import ru.nikidzawa.golink.store.repositories.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

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
    private ImageView settingsButton;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Text status;

    @FXML
    private Pane titleBar;

    @FXML
    private ImageView sendImageButton;

    @FXML
    private GNAvatarView myAvatar;

    private TCPConnection tcpConnection;
    private TCPBroker tcpBroker;

    @Autowired
    UserRepository userRepository;
    @Autowired
    ChatRepository chatRepository;
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    ImageRepository imageRepository;
    @Autowired
    GUIPatterns GUIPatterns;

    private UserEntity interlocutor;
    @Setter
    private UserEntity userEntity;
    private ChatEntity selectedChat;
    private boolean editable = false;
    @FXML
    @SneakyThrows
    void initialize() {
        Platform.runLater(() -> {
            try {
                tcpBroker = new TCPBroker(new Socket("localhost", 8081), this, userEntity.getId().toString());
            } catch (IOException e) {
                Platform.exit();
            }
            GUIPatterns.setBaseWindowTitleCommands(titleBar, minimizeButton, scaleButton, closeButton, tcpBroker, userEntity, userRepository, chatRepository);
            myAvatar.setImage((new Image(new ByteArrayInputStream(userEntity.getAvatar().getMetadata()))));

            sendImageButton.setOnMouseClicked(mouseEvent -> selectImageConfig());
            send.setOnMouseClicked(mouseEvent -> selectSendConfig());
            GUIPatterns.makeInput(input);
            GUIPatterns.makeSearch(searchPanel);
            setSearchConfig();
            profileName.setText(userEntity.getName());
            updateChats();
        });
    }
    private void selectSendConfig() {
    }

    private void updateChats() {
        chats.getChildren().clear();
        chatRepository.findByParticipantsContaining(userEntity).forEach(chatEnt -> {
            UserEntity interlocutor = chatEnt.getParticipants().stream()
                    .filter(userEntity1 -> !Objects.equals(userEntity1.getId(), userEntity.getId())).findFirst().get();

            BorderPane contact = GUIPatterns.newChatBuilder(userEntity, interlocutor, chatEnt);
            chats.getChildren().add(contact);
            contact.setOnMouseClicked(e -> openChat(chatEnt, interlocutor));
        });
    }

    private void openChat (ChatEntity chatEnt, UserEntity interlocutor) {
            if (selectedChat != chatEnt) {
                this.interlocutor = interlocutor;
                status.setVisible(true);
                tcpBroker.sendMessage("CHECK_USER_STATUS:" + interlocutor.getId());
                sendImageButton.setVisible(true);
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
                GUIPatterns.setTcpConnection(tcpConnection);
                chat.getChildren().clear();
                chatRoomName.setText(interlocutor.getName());
                printMessages(chatEnt);
                scrolling();
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
                        chat.getChildren().add(GUIPatterns.printMyMessage(message, LocalDateTime.now()));
                        input.clear();
                        scrolling();
                    }
                });
            }
    }

    public void printMessages(ChatEntity chatEntity) {
        ChatEntity selectedChat = chatRepository.findById(chatEntity.getId()).orElseThrow();

        List<Object> sortedMessages = Stream.concat(
                        selectedChat.getMessages().stream(),
                        selectedChat.getImages().stream()
                )
                .sorted(Comparator.comparing(obj -> {
                    if (obj instanceof MessageEntity) {
                        return ((MessageEntity) obj).getDate();
                    } else if (obj instanceof ImageEntity) {
                        return ((ImageEntity) obj).getDate();
                    }
                    return null;
                }))
                .toList();

        sortedMessages.forEach(message -> {
            boolean isMyMessage;

            if (message instanceof MessageEntity) {
                isMyMessage = ((MessageEntity) message).getSender().getId().equals(userEntity.getId());
            } else {
                isMyMessage = ((ImageEntity) message).getSender().getId().equals(userEntity.getId());
            }

            if (message instanceof MessageEntity messageEntity) {
                if (isMyMessage) {
                    HBox myMessage = GUIPatterns.printMyMessage(messageEntity, (messageEntity.getDate()));
                    myMessage.setOnMouseClicked(clickOnMessage -> setEditeConfiguration(messageEntity, myMessage, clickOnMessage));
                    chat.getChildren().add(myMessage);
                } else {
                    chat.getChildren().add(GUIPatterns.printForeignMessage(messageEntity.getMessage(), (messageEntity.getDate())));
                }
            }
            else {
                chat.getChildren().add(isMyMessage
                        ? GUIPatterns.printMyPhoto(new Image(new ByteArrayInputStream(((ImageEntity) message).getMetadata())), ((ImageEntity) message).getDate())
                        : GUIPatterns.printForeignPhoto(new Image(new ByteArrayInputStream(((ImageEntity) message).getMetadata())), ((ImageEntity) message).getDate()));
            }
        });
    }
    private void setEditeConfiguration(MessageEntity message, HBox myMessage, MouseEvent clickOnMessage) {
        Stage messageStage = new Stage();

        BorderPane copy = GUIPatterns.copyMessageButton();
        copy.setOnMouseClicked(mouseEvent -> {
            copyMessageFunction(message);
            messageStage.close();
        });

        BorderPane edit = GUIPatterns.editeMessageButton();
        edit.setOnMouseClicked(editEvent -> {
            editMessageFunction(message);
            messageStage.close();
        });

        BorderPane delete = GUIPatterns.deleteMessageButton();
        delete.setOnMouseClicked(deleteEvent -> {
            deleteMessageFunction(message, myMessage);
            messageStage.close();
        });

        GUIPatterns.openMessageWindow(message, clickOnMessage, messageStage, copy, edit, delete);
    }

    private void copyMessageFunction (MessageEntity message) {
        String text = message.getMessage();
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
    private void deleteMessageFunction (MessageEntity message, HBox myMessage) {
        CompletableFuture.runAsync(() -> {
            messageRepository.delete(message);
            selectedChat.getMessages().clear();
            messageRepository.flush();
            if (Boolean.parseBoolean(new SOCSConnection().CHECK_USER(selectedChat.getPort(), interlocutor.getId().toString()))) {
                tcpBroker.sendMessage("UPDATE_MESSAGES:" + interlocutor.getId());
            }
        }).thenRun(() -> {
            Platform.runLater(() -> {
                chat.getChildren().remove(myMessage);
            });
        });
    }
    private void editMessageFunction (MessageEntity message) {
        editable = true;
        ImageView cancelButton = GUIPatterns.getCancelButton();
        BorderPane backgroundEditeInterface = GUIPatterns.getBackgroundEditInterface(message);
        backgroundEditeInterface.setRight(cancelButton);
        sendZone.setTop(backgroundEditeInterface);

        send.setOnMouseClicked(mouseEvent -> {
            if (editable) {
                message.setMessage(input.getText());
                input.clear();
                messageRepository.saveAndFlush(message);
                try {
                    chatRepository.saveAndFlush(selectedChat);
                } catch (JpaObjectRetrievalFailureException exception) {

                }
                if (Boolean.parseBoolean(new SOCSConnection().CHECK_USER(selectedChat.getPort(), interlocutor.getId().toString()))) {
                    tcpBroker.sendMessage("UPDATE_MESSAGES:" + interlocutor.getId());
                }
                editable = false;
                sendZone.setTop(null);
                updateMessages(selectedChat);
            }
        });
        cancelButton.setOnMouseClicked(mouseEvent -> {
            editable = false;
            sendZone.setTop(null);
            updateMessages(selectedChat);
        });
    }

    private void selectImageConfig() {
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Изображения", "*.jpg", "*.png", "*.jpeg")
            );
            File selectedFile = fileChooser.showOpenDialog(sendImageButton.getScene().getWindow());

            if (selectedFile != null) {
                try {
                    byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());
                    tcpConnection.sendPhoto(imageBytes);
                    chat.getChildren().add(GUIPatterns.printMyPhoto((new Image(new ByteArrayInputStream(imageBytes))), LocalDateTime.now()));
                    scrolling();
                    ImageEntity imageEntity = ImageEntity.builder()
                            .metadata(imageBytes)
                            .date(LocalDateTime.now())
                            .sender(userEntity)
                            .chat(selectedChat)
                            .build();
                    imageRepository.saveAndFlush(imageEntity);
                } catch (IOException ex) {
                    System.out.println("ошибка при попытке прочитать и отправить файл " + ex);
                }
            }
        });
    }

    private void setSearchConfig() {
        searchPanel.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                chats.getChildren().clear();
                Optional<UserEntity> user = userRepository.findByNickname(newValue);
                if (user.isPresent()) {
                    UserEntity interlocutor = user.get();
                    BorderPane contact = GUIPatterns.newChatBuilder(interlocutor);
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
                        updateChats();
                    });
                }
                if (searchPanel.getText().isEmpty()) updateChats();
            }
        });
    }

    private void updateMessages (ChatEntity chat) {
        Platform.runLater(() -> {
            this.chat.getChildren().clear();
            printMessages(chat);
        });
    }

    private void scrolling () {
        PauseTransition pauseTransition = new PauseTransition(Duration.millis(20));
        pauseTransition.setOnFinished(event -> scrollPane.setVvalue(1));
        pauseTransition.play();
    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {}

    @Override
    public void onReceiveMessage(TCPConnection tcpConnection, String string) {
        if (!string.equals(userEntity.getId().toString())) {
            Platform.runLater(() -> {
                if (scrollPane.getVvalue() >= 0.95) {
                    chat.getChildren().add(GUIPatterns.printForeignMessage(string, LocalDateTime.now()));
                    scrolling();
                }
                else chat.getChildren().add(GUIPatterns.printForeignMessage(string, LocalDateTime.now()));
            });
        }
    }

    @Override
    public void onReceiveImage(TCPConnection tcpConnection, byte[] image) {
        Platform.runLater(() -> {
            if (scrollPane.getVvalue() >= 0.95) {
                chat.getChildren().add(GUIPatterns.printForeignPhoto(new Image(new ByteArrayInputStream(image)), LocalDateTime.now()));
                scrolling();
            }
            else chat.getChildren().add(GUIPatterns.printForeignPhoto(new Image(new ByteArrayInputStream(image)), LocalDateTime.now()));
        });
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        tcpConnection.disconnect();
    }

    @Override
    public void onConnectionReady(TCPBroker tcpBroker) {
        userEntity.setConnected(true);
        userRepository.saveAndFlush(userEntity);
        chatRepository.findByParticipantsContaining(userEntity).forEach(chat -> {
            chat.getParticipants().stream()
                    .filter(user -> !Objects.equals(user.getId(), userEntity.getId()))
                    .forEach(user -> tcpBroker.sendMessage("UPDATE_CHAT_ROOMS:" + user.getId()));
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
            case "UPDATE_CHAT_ROOMS" -> Platform.runLater(this::updateChats);
            case "UPDATE_MESSAGES" -> updateMessages(selectedChat);
            case "NOTIFICATION" -> System.out.println("получено новое сообщение в чате " + value);
            case "STATUS" -> status.setText(Boolean.parseBoolean(value) ? "В сети" : "Не в сети");
        }
    }

    @Override
    public void onDisconnect(TCPBroker tcpBroker) {tcpBroker.disconnect();}
}