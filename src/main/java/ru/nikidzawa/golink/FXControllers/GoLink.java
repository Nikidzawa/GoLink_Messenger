package ru.nikidzawa.golink.FXControllers;

import io.github.gleidson28.GNAvatarView;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.FXControllers.Configurations.ScrollConfig;
import ru.nikidzawa.golink.FXControllers.Configurations.SearchConfig;
import ru.nikidzawa.golink.FXControllers.Configurations.SendMessageConfig;
import ru.nikidzawa.golink.FXControllers.cash.ContactCash;
import ru.nikidzawa.golink.FXControllers.cash.MessageCash;
import ru.nikidzawa.golink.services.GUI.EmptyStage;
import ru.nikidzawa.golink.services.GUI.GUIPatterns;
import ru.nikidzawa.golink.services.GUI.TrayIcon.GoLinkTrayIcon;
import ru.nikidzawa.golink.services.GUI.TrayIcon.notifications.Notification;
import ru.nikidzawa.golink.services.GUI.TrayIcon.notifications.NotificationScene;
import ru.nikidzawa.golink.services.sound.AudioHelper;
import ru.nikidzawa.golink.services.sound.SongPlayer;
import ru.nikidzawa.networkAPI.network.ServerListener;
import ru.nikidzawa.networkAPI.network.TCPConnection;
import ru.nikidzawa.networkAPI.store.MessageType;
import ru.nikidzawa.networkAPI.store.entities.ChatEntity;
import ru.nikidzawa.networkAPI.store.entities.MessageEntity;
import ru.nikidzawa.networkAPI.store.entities.PersonalChatEntity;
import ru.nikidzawa.networkAPI.store.entities.UserEntity;
import ru.nikidzawa.networkAPI.store.repositories.ChatRepository;
import ru.nikidzawa.networkAPI.store.repositories.MessageRepository;
import ru.nikidzawa.networkAPI.store.repositories.PersonalChatRepository;
import ru.nikidzawa.networkAPI.store.repositories.UserRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Controller
public class GoLink implements ServerListener {
    @FXML
    @Getter
    private Pane titleBar;

    @FXML
    @Getter
    private Button closeButton;

    @FXML
    @Getter
    private Button scaleButton;

    @FXML
    @Getter
    private Button minimizeButton;

    @FXML
    @Getter
    private VBox chatField;

    @FXML
    @Getter
    private VBox contactsField;

    @FXML
    @Getter
    private Text chatRoomName;

    @FXML
    @Getter
    private TextField input;

    @FXML
    @Getter
    private Text profileName;

    @FXML
    @Getter
    private TextField searchPanel;

    @FXML
    @Getter
    private ImageView sendButton;

    @FXML
    @Getter
    private BorderPane sendZone;

    @FXML
    @Getter
    private ImageView settingsButton;

    @FXML
    @Getter
    private ScrollPane scrollPane;

    @FXML
    @Getter
    private Text status;

    @FXML
    @Getter
    private ImageView sendImageButton;

    @FXML
    @Getter
    private ImageView microphone;

    @FXML
    @Getter
    private GNAvatarView myAvatar;

    @Setter
    @Getter
    private ConfigurableApplicationContext context;

    @Getter
    @Setter
    private UserEntity userEntity;

    @Getter
    @Setter
    private Scene scene;

    @Getter
    private TCPConnection TCPConnection;

    @Autowired
    public UserRepository userRepository;
    @Autowired
    public ChatRepository chatRepository;
    @Autowired
    public MessageRepository messageRepository;
    @Autowired
    public PersonalChatRepository personalChatRepository;
    @Autowired
    public GUIPatterns GUIPatterns;

    public HashMap<Long, ContactCash> contacts = new HashMap<>();
    private ContactCash selectedContact;

    @Getter
    private ScrollConfig scrollConfig;

    private SendMessageConfig sendMessageConfig;

    private NotificationScene notificationScene;

    @Setter
    GoLinkTrayIcon goLinkTrayIcon;

    @FXML
    @SneakyThrows
    void initialize() {
        Platform.runLater(() -> {
            try {
                TCPConnection = new TCPConnection(new Socket("localhost", 8081), this, userEntity.getId().toString());
            } catch (IOException ex) {
                Platform.exit();
                throw new RuntimeException(ex);
            }
            Platform.setImplicitExit(false);
            scene.getWindow().setOnHidden(_ -> Platform.runLater(() -> notificationScene = new NotificationScene()));

            scene.getWindow().setOnShowing(_ -> {
                goLinkTrayIcon.hide();
                Platform.runLater(notificationScene::close);
                notificationScene = null;
            });

            GUIPatterns.setGoLinkBaseTitleCommands(this);
            scrollConfig = new ScrollConfig(scrollPane, chatField);
            sendMessageConfig = new SendMessageConfig(this);
            new SearchConfig(this);

            setUserConfig();

            scene.setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ENTER && selectedContact != null) {
                    sendMessageConfig.sendMessageConfiguration();
                }
                if (keyEvent.getCode() == KeyCode.ESCAPE) exitChat();
            });

            settingsButton.setOnMouseClicked(_ -> openSettings());

            input.textProperty().addListener((_, _, _) -> TCPConnection.WRITING_STATUS(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId()));

            loadChatRooms();
        });
    }

    public void exitChat() {
        scrollPane.setStyle("-fx-background: #001933; -fx-background-color: #001933; -fx-border-width: 1 0 1 1; -fx-border-color: black;");
        selectedContact = null;
        sendMessageConfig.disable();
        setVisibleChatContent(false);
        GUIPatterns.setEmptyChatConfiguration(chatField);
    }

    private void setVisibleChatContent(boolean visible) {
        status.setVisible(visible);
        sendImageButton.setVisible(visible);
        sendButton.setVisible(visible);
        input.setVisible(visible);
        microphone.setVisible(visible);
        chatRoomName.setVisible(visible);
    }

    public void setUserConfig() {
        myAvatar.setImage((new Image(new ByteArrayInputStream(userEntity.getAvatar()))));
        profileName.setText(userEntity.getName());
    }

    @SneakyThrows
    private void openSettings() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/nikidzawa/goLink/FXControllers/editProfile.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        EditProfile editProfile = loader.getController();
        editProfile.setUserEntity(userEntity);
        editProfile.setGoLink(this);
        editProfile.setScene(scene);

        EmptyStage.getEmptyStageAndSetScene(scene).show();
    }

    private void loadChatRooms() {
        contactsField.getChildren().clear();
        List<PersonalChatEntity> personalChatEntities = userEntity.getUserChats();
        if (personalChatEntities != null && !personalChatEntities.isEmpty()) {
            personalChatEntities.forEach(personalChat -> createContact(personalChat.getInterlocutor(), personalChat.getChat(), personalChat));
        }
        writeSortedContacts();
    }

    private void writeSortedContacts() {
        contactsField.getChildren().clear();
        List<ContactCash> GUI = contacts.values()
                .stream()
                .sorted(Comparator.comparing(contactCash ->
                        contactCash.getChat().getMessages() != null ?
                                contactCash.getChat().getMessages().getLast().getDate() :
                                null, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        GUI.forEach(contactCash -> contactsField.getChildren().add(contactCash.getGUI()));
    }

    public ContactCash createContact(UserEntity interlocutor, ChatEntity chat, PersonalChatEntity personalChatEntity) {
        ContactCash contactCash = new ContactCash(interlocutor, chat, personalChatEntity, personalChatRepository);
        BorderPane GUIContact = GUIPatterns.newChatBuilder(userEntity, contactCash, personalChatEntity);
        GUIContact.setOnMouseClicked(_ -> openChat(chat, personalChatEntity, interlocutor, contactCash));
        contactCash.setGUI(GUIContact);
        contacts.put(chat.getId(), contactCash);
        return contactCash;
    }

    @SneakyThrows
    public void openChat(ChatEntity chat, PersonalChatEntity personalChatEntity, UserEntity interlocutor, ContactCash contactCash) {
        if (selectedContact == null || !Objects.equals(selectedContact.getChat().getId(), chat.getId())) {
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-background-image: url('img/backgroundChatImage.jpg'); -fx-border-width: 1 0 1 1; -fx-border-color: black;");
            chatField.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-spacing: 10");
            selectedContact = contactCash;
            sendMessageConfig.setSelectedContact(selectedContact);
            scrollConfig.startScrollEvent();
            setVisibleChatContent(true);

            if (personalChatEntity.getNewMessagesCount() > 0) {
                contactCash.resetNotificationCount();
            }

            new Thread(() -> TCPConnection.CHECK_USER_STATUS(interlocutor.getId())).start();

            chatRoomName.setText(interlocutor.getName());
            printMessages();
        }
    }

    @SneakyThrows
    public void printMessages() {
        chatField.getChildren().clear();
        List<MessageEntity> messages = selectedContact.getChat().getMessages();
        if (messages != null && !messages.isEmpty()) {
            messages = messages.stream().sorted(Comparator.comparing(MessageEntity::getDate)).toList();
            messages.forEach(message -> {
                if (selectedContact.cashedMessageInformation.containsKey(message.getId())) {
                    chatField.getChildren().add(selectedContact.cashedMessageInformation.get(message.getId()).getMessageBackground());
                } else {
                    boolean isMyMessage = message.getSender().getId().equals(userEntity.getId());
                    MessageCash messageCash = null;
                    switch (message.getMessageType()) {
                        case MESSAGE ->
                                messageCash = isMyMessage ? GUIPatterns.makeMyMessageGUIAndGetCash(message) : GUIPatterns.makeForeignMessageGUIAndGetCash(message);
                        case AUDIO, DOCUMENT ->
                                messageCash = isMyMessage ? GUIPatterns.printMyAudioGUIAndGetCash(message, AudioHelper.convertBytesToAudio(message.getMetadata())) : GUIPatterns.printForeignAudioGUIAndGetCash(message, AudioHelper.convertBytesToAudio(message.getMetadata()));
                    }
                    MessageCash finalMessageCash = messageCash;

                    selectedContact.addMessageOnCash(finalMessageCash);
                    chatField.getChildren().add(finalMessageCash.getMessageBackground());
                    finalMessageCash.getGUI().setOnMouseClicked(clickOnMessage -> {
                        if (clickOnMessage.getButton() == MouseButton.SECONDARY)
                            sendMessageConfig.setMessageFunctions(finalMessageCash, clickOnMessage);
                    });
                }
            });
        }
    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {}

    @Override
    public void onReceiveMessage(TCPConnection tcpConnection, String string) {
        String[] strings = string.split(":");
        String command = strings[0];
        String value;
        String value2;
        try {
            value = strings[1];
        } catch (ArrayIndexOutOfBoundsException ex) {
            value = null;
        }
        try {
            value2 = strings[2];
        } catch (ArrayIndexOutOfBoundsException ex) {
            value2 = null;
        }
        switch (command) {
            case "DELETE" -> {
                long messageId = Long.parseLong(value2);
                long chatId = Long.parseLong(value);
                if (selectedContact != null && selectedContact.getChat().getId() == chatId) {
                    Platform.runLater(() -> chatField.getChildren().remove(selectedContact.deleteMessage(messageId)));
                } else contacts.get(chatId).deleteMessageDefault(messageId);
            }
            case "CREATE_NEW_CHAT_ROOM" -> {
                PersonalChatEntity personalChatEntity = personalChatRepository.findById(Long.parseLong(value)).orElseThrow();
                UserEntity interlocutor = personalChatEntity.getInterlocutor();
                ChatEntity chatEntity = personalChatEntity.getChat();
                Platform.runLater(() -> createContact(interlocutor, chatEntity, personalChatEntity));
            }
            case "CHANGE_USER_STATUS" -> status.setText(Boolean.parseBoolean(value) ? "В сети" : "Не в сети");
        }
    }

    @Override
    public void onReceiveFile(TCPConnection tcpConnection, String protocol, byte[] content) {
        String[] strings = protocol.split(":");
        String command = strings[0];
        String value = strings[1];
        String value2 = strings[2];
        String value3 = strings[3];
        String value4;
        try {
            value4 = strings[4];
        } catch (ArrayIndexOutOfBoundsException ex) {
            value4 = "";
        }
        switch (command) {
            case "POST" -> {
                long chatId = Long.parseLong(value);
                ContactCash contactCash = contacts.get(chatId);
                ChatEntity chatEntity = contactCash.getChat();
                UserEntity interlocutor = contactCash.getInterlocutor();
                MessageType messageType = MessageType.valueOf(value3);
                MessageEntity messageEntity = MessageEntity.builder()
                        .date(LocalDateTime.now())
                        .chat(chatEntity)
                        .id(Long.parseLong(value2))
                        .sender(interlocutor)
                        .messageType(messageType)
                        .text(value4)
                        .metadata(content)
                        .build();

                switch (messageType) {
                    case MESSAGE -> writeReceivedMessage(messageEntity, contactCash, chatId);
                    case AUDIO -> writeReceivedAudio(messageEntity, contactCash, chatId);
                }
                Platform.runLater(() -> {
                    contactsField.getChildren().remove(contactCash.getGUI());
                    contactsField.getChildren().addFirst(contactCash.getGUI());
                });
            }
            case "EDIT" -> {
                long chatId = Long.parseLong(value);
                long messageId = Long.parseLong(value2);
                ContactCash contactCash = contacts.get(chatId);
                contactCash.editMessage(messageId, value4, content, MessageType.valueOf(value3));
            }
        }
    }

    private void writeReceivedAudio(MessageEntity message, ContactCash contactCash, Long chatId) {
        MessageCash messageCash = GUIPatterns.printForeignAudioGUIAndGetCash(message, AudioHelper.convertBytesToAudio(message.getMetadata()));
        contactCash.addMessageOnCashAndPutLastMessage(messageCash);
        if (selectedContact != null && Objects.equals(selectedContact.getChat().getId(), chatId)) {
            Platform.runLater(() -> chatField.getChildren().add(messageCash.getMessageBackground()));
        } else {pushNotification(message, contactCash);}
    }

    private void writeReceivedMessage(MessageEntity message, ContactCash contactCash, Long chatId) {
        MessageCash messageCash = GUIPatterns.makeForeignMessageGUIAndGetCash(message);
        contactCash.addMessageOnCashAndPutLastMessage(messageCash);
        if (selectedContact != null && Objects.equals(selectedContact.getChat().getId(), chatId)) {
            Platform.runLater(() -> chatField.getChildren().add(messageCash.getMessageBackground()));
        } else {pushNotification(message, contactCash);}
    }

    private void pushNotification(MessageEntity message, ContactCash contactCash) {
        contactCash.addNotification(message);
        SongPlayer.notification();
        if (notificationScene != null) {
            String messageNotification = null;
            switch (message.getMessageType()) {
                case MESSAGE -> messageNotification = message.getText();
                case AUDIO -> messageNotification = "Голосовое сообщение";
                case DOCUMENT -> messageNotification = "Документ";
            }

            Notification notification = new Notification.Builder()
                    .setMessage(messageNotification)
                    .setTitle(message.getSender().getName())
                    .setImage(new Image(new ByteArrayInputStream(message.getSender().getAvatar())))
                    .build();
            notification.setOnMouseClicked(_ -> {
                Platform.runLater(() -> {
                    Stage stage = (Stage) scene.getWindow();
                    stage.show();
                    openChat(message.getChat(), contactCash.getPersonalChatEntity(), contactCash.getInterlocutor(), contactCash);
                });
            });
            notificationScene.addNotification(notification);
        }
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {Platform.exit();}
}