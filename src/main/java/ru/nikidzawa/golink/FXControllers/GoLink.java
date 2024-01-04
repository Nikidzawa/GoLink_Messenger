package ru.nikidzawa.golink.FXControllers;

import io.github.gleidson28.GNAvatarView;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.FXControllers.helpers.ExitListener;
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

@Controller
public class GoLink implements TCPConnectionListener, GoMessageListener, ExitListener {
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

    @Setter
    private ConfigurableApplicationContext context;
    private TCPConnection tcpConnection;
    private TCPBroker tcpBroker;

    @Autowired
    UserRepository userRepository;
    @Autowired
    ChatRepository chatRepository;
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    PersonalChatRepository personalChatRepository;
    @Autowired
    GUIPatterns GUIPatterns;

    private UserEntity interlocutor;
    @Setter
    private UserEntity userEntity;
    @Setter
    private Scene scene;
    private ChatEntity selectedChat;
    MessageEntity message;
    HBox messageBackground;
    private boolean editable = false;
    @FXML
    @SneakyThrows
    void initialize() {
        Platform.runLater(() -> {
            try {
                tcpBroker = new TCPBroker(new Socket("localhost", 8081), this, userEntity.getId().toString());
            } catch (IOException ex) {
                Platform.exit();
                throw new RuntimeException(ex);
            }
            setUserConfig();
            GUIPatterns.setBaseWindowTitleCommands(titleBar, minimizeButton, scaleButton, closeButton, this);
            sendImageButton.setOnMouseClicked(mouseEvent -> selectImageConfig());
            send.setOnAction(actionEvent -> sendMessageConfiguration());
            scene.setOnKeyPressed(keyEvent -> {
                if (selectedChat != null && keyEvent.getCode() == KeyCode.ENTER) {
                    sendMessageConfiguration();
                }
                if (keyEvent.getCode() == KeyCode.ESCAPE && tcpConnection != null) {
                    tcpConnection.disconnect();
                    tcpConnection = null;
                    selectedChat = null;
                    GUIPatterns.setEmptyChatConfiguration(chat);
                    status.setVisible(false);
                    sendImageButton.setVisible(false);
                    send.setVisible(false);
                    input.setVisible(false);
                    editable = false;
                    chatRoomName.setText("");
                }
            });

            GUIPatterns.makeInput(input);

            settingsButton.setOnMouseClicked(mouseEvent -> openSettings());

            GUIPatterns.makeSearch(searchPanel);
            PauseTransition pause = new PauseTransition(Duration.millis(500));
            searchPanel.textProperty().addListener((observable, oldValue, newValue) -> setSearchConfig(newValue, pause));

            loadChatRooms();
        });
    }

    public void setUserConfig () {
        myAvatar.setImage((new Image(new ByteArrayInputStream(userEntity.getAvatar().getMetadata()))));
        profileName.setText(userEntity.getName());
    }

    @SneakyThrows
    private void openSettings(){
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/nikidzawa/goLink/FXControllers/editProfile.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        EditProfile editProfile = loader.getController();
        editProfile.setUserEntity(userEntity);
        editProfile.setGoLink(this);
        editProfile.setScene(scene);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.show();
    }

    private void loadChatRooms () {
        chats.getChildren().clear();
        List<PersonalChat> personalChats = userEntity.getUserChats();
        if (personalChats != null && !personalChats.isEmpty()) {
            personalChats.forEach(personalChat -> {
                UserEntity interlocutor = personalChat.getInterlocutor();
                BorderPane contact = GUIPatterns.newChatBuilder(userEntity, interlocutor, personalChat.getChat(), personalChat);
                chats.getChildren().add(contact);
                contact.setOnMouseClicked(e -> openChat(personalChat.getChat(), personalChat, interlocutor));
            });
        }
    }

    private void updateChatRooms() {
        Platform.runLater(() -> {
            chats.getChildren().clear();
            userEntity = userRepository.findById(userEntity.getId()).orElseThrow();
            List<PersonalChat> personalChats = userEntity.getUserChats();
            if (personalChats != null && !personalChats.isEmpty()) {
                personalChats.forEach(personalChat -> {
                    UserEntity interlocutor = personalChat.getInterlocutor();
                    BorderPane contact = GUIPatterns.newChatBuilder(userEntity, interlocutor, personalChat.getChat() , personalChat);
                    chats.getChildren().add(contact);
                    contact.setOnMouseClicked(e -> openChat(personalChat.getChat(), personalChat, interlocutor));
                });
            }
        });
    }

    private void openChat (ChatEntity chat, PersonalChat personalChat, UserEntity interlocutor) {
            if (selectedChat == null || !Objects.equals(selectedChat.getId(), chat.getId())) {
                this.interlocutor = interlocutor;
                if (personalChat.getNewMessagesCount() > 0) {
                    personalChat.setNewMessagesCount((byte) 0);
                    personalChatRepository.saveAndFlush(personalChat);
                }
                status.setVisible(true);
                tcpBroker.sendMessage("CHECK_USER_STATUS:" + interlocutor.getId());
                sendImageButton.setVisible(true);
                input.setVisible(true);
                input.setStyle("-fx-background-color: #001933; -fx-border-color: blue; -fx-text-fill: white; -fx-border-width: 0 0 2 0");
                send.setVisible(true);
                chatRoomName.setText(interlocutor.getName());

                selectedChat = chat;
                if (tcpConnection != null) {
                    tcpConnection.disconnect();
                }
                String userId = userEntity.getId().toString();
                try {
                    tcpConnection = new TCPConnection(new Socket("localhost", chat.getPort()), this, userId);
                } catch (IOException ex) {
                    int newPort = Integer.parseInt(new SOCSConnection().CREATE_SERVER());
                    chat.setPort(newPort);
                    try {
                        chatRepository.saveAndFlush(chat);
                    } catch (JpaObjectRetrievalFailureException exception) {
                        throw new RuntimeException(exception);
                    }
                    try {
                        tcpConnection = new TCPConnection(new Socket("localhost", newPort), this, userId);
                        tcpBroker.sendMessage("UPDATE_CHAT_ROOMS:" + interlocutor.getId());
                    } catch (IOException exc) {
                        throw new RuntimeException(exc);
                    }
                }
                this.chat.getChildren().clear();
                printMessages(chat);
                scrolling();
            }
    }

    private void sendMessageConfiguration() {
        String text = input.getText();
        if (!text.isEmpty()) {
            if (editable) {
                message.setMessage(text);
                input.clear();

                int number = chat.getChildren().indexOf(messageBackground);
                chat.getChildren().remove(messageBackground);
                HBox myMessage = GUIPatterns.printMyMessage(message, (message.getDate()));
                myMessage.setOnMouseClicked(clickOnMessage -> {
                    if (clickOnMessage.getButton() == MouseButton.SECONDARY) {
                        setMessageFunctions(message, myMessage, clickOnMessage);
                    }
                });
                chat.getChildren().add(number, myMessage);
                messageRepository.saveAndFlush(message);

                if (Boolean.parseBoolean(new SOCSConnection().CHECK_USER(selectedChat.getPort(), interlocutor.getId().toString()))) {
                    tcpBroker.sendMessage("UPDATE_MESSAGES:" + interlocutor.getId());
                }
                editable = false;
                sendZone.setTop(null);
            } else {
                MessageEntity message = MessageEntity.builder()
                        .message(text).date(LocalDateTime.now()).sender(userEntity).chat(selectedChat).build();
                messageRepository.saveAndFlush(message);
                tcpConnection.sendMessage(text);
                HBox myMessage = GUIPatterns.printMyMessage(message, LocalDateTime.now());
                myMessage.setOnMouseClicked(clickOnMessage -> {
                    if (clickOnMessage.getButton() == MouseButton.SECONDARY) {
                        setMessageFunctions(message, myMessage, clickOnMessage);
                    }
                });
                chat.getChildren().add(myMessage);
                input.clear();
                sendNotification();
                scrolling();
            }
        }
    }

    public void printMessages(ChatEntity chatEntity) {
        List<MessageEntity> messages = chatRepository.findById(chatEntity.getId()).get().getMessages();
        if (!messages.isEmpty()) {
            messages.stream().sorted(Comparator.comparing(MessageEntity::getDate)).toList().forEach(message -> {
                if (message.getSender().getId().equals(userEntity.getId())) {
                    if (message.getMetadata() != null) {
                        chat.getChildren().add(GUIPatterns.printMyPhoto(new Image(new ByteArrayInputStream((message).getMetadata())), (message).getDate()));
                    } else {
                        HBox myMessage = GUIPatterns.printMyMessage(message, (message.getDate()));
                        myMessage.setOnMouseClicked(clickOnMessage -> {
                            if (clickOnMessage.getButton() == MouseButton.SECONDARY) {
                                setMessageFunctions(message, myMessage, clickOnMessage);
                            }
                        });
                        chat.getChildren().add(myMessage);
                    }
                } else {
                    if (message.getMetadata() != null) {
                        chat.getChildren().add(GUIPatterns.printForeignPhoto(new Image(new ByteArrayInputStream(message.getMetadata())), message.getDate()));
                    } else chat.getChildren().add(GUIPatterns.printForeignMessage(message.getMessage(), message.getDate()));
                }
            });
        }
    }

    private void setMessageFunctions(MessageEntity message, HBox myMessage, MouseEvent clickOnMessage) {
        Stage messageStage = new Stage();
        scene.setOnMouseClicked(mouseEvent -> {
            if (messageStage.isShowing()) messageStage.close();
        });
        this.message = message;
        this.messageBackground = myMessage;
        String messageText = message.getMessage();

        BorderPane copyButton = GUIPatterns.copyMessageButton();
        copyButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            copyMessageFunction(messageText);
            messageStage.close();
        });

        BorderPane editButton = GUIPatterns.editeMessageButton();
        editButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            editMessageFunction(messageText);
            messageStage.close();
        });

        BorderPane deleteButton = GUIPatterns.deleteMessageButton();
        deleteButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            deleteMessageFunction(message);
            messageStage.close();
        });

        Platform.runLater(() -> GUIPatterns.openMessageWindow(message, clickOnMessage, messageStage, copyButton, editButton, deleteButton));
    }

    private void copyMessageFunction (String messageText) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(messageText);
        clipboard.setContent(content);
    }
    private void deleteMessageFunction (MessageEntity message) {
        messageRepository.delete(message);
        if (Boolean.parseBoolean(new SOCSConnection().CHECK_USER(selectedChat.getPort(), interlocutor.getId().toString()))) {
            tcpBroker.sendMessage("UPDATE_MESSAGES:" + interlocutor.getId());
        }
        Platform.runLater(() -> chat.getChildren().remove(messageBackground));
    }
    private void editMessageFunction (String messageText) {
        editable = true;
        ImageView cancelButton = GUIPatterns.getCancelButton();
        BorderPane backgroundEditeInterface = GUIPatterns.getBackgroundEditMessageInterface(messageText);
        backgroundEditeInterface.setRight(cancelButton);
        sendZone.setTop(backgroundEditeInterface);
        cancelButton.setOnMouseClicked(mouseEvent -> {
            editable = false;
            sendZone.setTop(null);
            updateMessages(selectedChat);
        });
    }

    private void sendNotification () {
        if (!Boolean.parseBoolean(new SOCSConnection().CHECK_USER(selectedChat.getPort(),
                interlocutor.getId().toString()))) {
            tcpBroker.sendMessage("NOTIFICATION:" + interlocutor.getId() + ":" + selectedChat.getId());
        }
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
                    MessageEntity messageEntity = MessageEntity.builder().metadata(imageBytes).sender(userEntity).chat(selectedChat).date(LocalDateTime.now()).build();
                    messageRepository.saveAndFlush(messageEntity);
                } catch (IOException ex) {
                    System.out.println("ошибка при попытке прочитать и отправить файл " + ex);
                }
            }
        });
    }

    private void setSearchConfig(String newValue, PauseTransition pause) {
        pause.stop();
        pause.playFromStart();
        pause.setOnFinished(event -> {
            chats.getChildren().clear();
            Optional<UserEntity> user = userRepository.findFirstByNickname(newValue);
            if (user.isPresent()) {
                UserEntity interlocutor = user.get();
                BorderPane contact = GUIPatterns.newChatBuilder(interlocutor);
                chats.getChildren().add(contact);
                contact.setOnMouseClicked(mouseEvent -> {
                    searchPanel.clear();
                    PersonalChat personalChat = userEntity.getUserChats()
                            .stream()
                            .filter(userChat -> Objects.equals(userChat.getInterlocutor().getId(), interlocutor.getId()))
                            .findFirst()
                            .orElseGet(() -> {
                                ChatEntity newChat = ChatEntity.builder()
                                        .port(Integer.parseInt(new SOCSConnection().CREATE_SERVER()))
                                        .build();
                                chatRepository.saveAndFlush(newChat);
                                PersonalChat myPersonalChat = PersonalChat.builder()
                                        .chat(newChat)
                                        .user(userEntity)
                                        .interlocutor(interlocutor)
                                        .build();
                                PersonalChat participantPersonalChat = PersonalChat.builder()
                                        .chat(newChat)
                                        .user(interlocutor)
                                        .interlocutor(userEntity)
                                        .build();

                                personalChatRepository.saveAndFlush(myPersonalChat);
                                personalChatRepository.saveAndFlush(participantPersonalChat);

                                newChat.setPersonalChats(List.of(myPersonalChat, participantPersonalChat));
                                chatRepository.saveAndFlush(newChat);
                                openChat(newChat, myPersonalChat, interlocutor);
                                tcpBroker.sendMessage("UPDATE_CHAT_ROOMS:" + interlocutor.getId());
                                return myPersonalChat;
                            });
                    openChat(personalChat.getChat(), personalChat, interlocutor);
                    updateChatRooms();
                });
            }
            if (searchPanel.getText().isEmpty()) updateChatRooms();
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
    public void onDisconnect(TCPConnection tcpConnection) {}

    @Override
    public void onConnectionReady(TCPBroker tcpBroker) {
        userEntity.setConnected(true);
        userRepository.saveAndFlush(userEntity);
        List<PersonalChat> personalChats = userEntity.getUserChats();
        if (personalChats != null && !personalChats.isEmpty()) {
            personalChats.forEach(chat -> tcpBroker.sendMessage("UPDATE_CHAT_ROOMS:" + chat.getInterlocutor().getId()));
        }
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
            case "UPDATE_CHAT_ROOMS" -> Platform.runLater(this::updateChatRooms);
            case "UPDATE_MESSAGES" -> updateMessages(selectedChat);
            case "NOTIFICATION" -> {
                Long id = Long.parseLong(value);
                if (selectedChat != null && !Objects.equals(selectedChat.getId(), id)) {
                    PersonalChat personalChat = userEntity.getUserChats().stream().filter(pc -> Objects.equals(pc.getChat().getId(), id)).findFirst().get();
                    personalChat.setNewMessagesCount((byte) (personalChat.getNewMessagesCount() + 1));
                    personalChatRepository.saveAndFlush(personalChat);
                    updateChatRooms();
                }
                System.out.println("получено новое сообщение в чате " + value);
            }
            case "STATUS" -> status.setText(Boolean.parseBoolean(value) ? "В сети" : "Не в сети");
        }
    }

    @Override
    public void onDisconnect(TCPBroker tcpBroker) {
        if (tcpConnection != null) {
            tcpConnection.disconnect();
        }
        Platform.exit();
    }

    @Override
    public void onExit() {
        userEntity.setConnected(false);
        userRepository.saveAndFlush(userEntity);
        userEntity.getUserChats().forEach(chat -> tcpBroker.sendMessage("UPDATE_CHAT_ROOMS:" + chat.getInterlocutor().getId()));
        tcpBroker.disconnect();
    }
}