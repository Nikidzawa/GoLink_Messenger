package ru.nikidzawa.golink.FXControllers;

import io.github.gleidson28.GNAvatarView;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.FXControllers.Configurations.SendMessageConfig;
import ru.nikidzawa.golink.FXControllers.cash.ContactCash;
import ru.nikidzawa.golink.FXControllers.cash.MessageCash;
import ru.nikidzawa.golink.FXControllers.helpers.GUIPatterns;
import ru.nikidzawa.golink.FXControllers.Configurations.ScrollConfig;
import ru.nikidzawa.golink.services.sound.AudioHelper;
import ru.nikidzawa.golink.network.ServerListener;
import ru.nikidzawa.golink.network.TCPConnection;
import ru.nikidzawa.golink.services.sound.SongPlayer;
import ru.nikidzawa.golink.store.MessageType;
import ru.nikidzawa.golink.store.entities.*;
import ru.nikidzawa.golink.store.repositories.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class GoLink implements ServerListener {
    @FXML
    private Button closeButton;

    @FXML
    private Button scaleButton;

    @FXML
    private Button minimizeButton;

    @FXML
    private VBox chatField;

    @FXML
    private VBox contactsField;

    @FXML
    private Text chatRoomName;

    @FXML
    private TextField input;

    @FXML
    private Text profileName;

    @FXML
    private TextField searchPanel;

    @FXML
    private ImageView sendButton;

    @FXML
    private BorderPane sendZone;

    @FXML
    private ImageView settingsButton;

    @FXML
    private javafx.scene.control.ScrollPane scrollPane;

    @FXML
    private Text status;

    @FXML
    private Pane titleBar;

    @FXML
    private ImageView sendImageButton;

    @FXML
    private ImageView microphone;

    @FXML
    private GNAvatarView myAvatar;

    @Setter
    private ConfigurableApplicationContext context;
    private TCPConnection TCPConnection;

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

    HashMap<Long, ContactCash> contacts = new HashMap<>();
    ContactCash selectedContact;
    ScrollConfig scrollConfig;
    SendMessageConfig sendMessageConfig;

    @Setter
    private UserEntity userEntity;
    @Setter
    private Scene scene;

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
            GUIPatterns.setBaseWindowTitleCommands(titleBar, minimizeButton, scaleButton, closeButton, TCPConnection);
            scrollConfig = new ScrollConfig(scrollPane, chatField);
            sendMessageConfig = new SendMessageConfig(userEntity, scene, sendImageButton, microphone, sendButton, contactsField, chatField, input, TCPConnection, scrollConfig, messageRepository, sendZone);

            setUserConfig();
            sendImageButton.setOnMouseClicked(mouseEvent -> sendMessageConfig.sendImageConfig());
            scene.setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ENTER && selectedContact != null) {
                    sendMessageConfig.sendMessageConfiguration();
                }
                if (keyEvent.getCode() == KeyCode.ESCAPE) {
                    scrollPane.setStyle("-fx-background: #001933; -fx-background-color: #001933; -fx-border-width: 1 0 1 1; -fx-border-color: black;");
                    selectedContact = null;
                    setVisibleChatContent(false);
                    sendMessageConfig.disable();
                    GUIPatterns.setEmptyChatConfiguration(chatField);
                }
            });

            settingsButton.setOnMouseClicked(mouseEvent -> openSettings());

            PauseTransition pause = new PauseTransition(Duration.millis(1000));
            searchPanel.textProperty().addListener((observable, oldValue, newValue) -> setSearchConfig(newValue, pause));

            loadChatRooms();
        });
    }

    private void setVisibleChatContent (boolean visible) {
        status.setVisible(visible);
        sendImageButton.setVisible(visible);
        sendButton.setVisible(visible);
        input.setVisible(visible);
        microphone.setVisible(visible);
        chatRoomName.setVisible(visible);
    }

    public void setUserConfig () {
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

        Stage stage = new Stage();
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/logo.png"))));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("GoLink");
        stage.setScene(scene);
        stage.show();
    }

    private void loadChatRooms () {
        contactsField.getChildren().clear();
        List<PersonalChat> personalChats = userEntity.getUserChats();
        if (personalChats != null && !personalChats.isEmpty()) {
            personalChats.forEach(personalChat -> createContact(personalChat.getInterlocutor(), personalChat.getChat(), personalChat));
        }
        writeSortedContacts();
    }

    private void writeSortedContacts () {
        contactsField.getChildren().clear();
        List<ContactCash> GUI = contacts.values()
                .stream()
                .sorted(Comparator.comparing(contactCash ->
                        contactCash.getLastMessage() != null ?
                                contactCash.getLastMessage().getDate() :
                                null, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        GUI.forEach(contactCash -> contactsField.getChildren().add(contactCash.getGUI()));
    }

    private void loadContactsFromCash () {
        contactsField.getChildren().clear();
        contacts.values().forEach(contactCash -> contactsField.getChildren().add(contactCash.getGUI()));
    }

    private ContactCash createContact (UserEntity interlocutor, ChatEntity chat, PersonalChat personalChat) {
        ContactCash contactCash = new ContactCash(interlocutor, chat, personalChat, personalChatRepository);
        BorderPane GUIContact = GUIPatterns.newChatBuilder(userEntity, contactCash, personalChat);
        GUIContact.setOnMouseClicked(e -> openChat(chat, personalChat, interlocutor, contactCash));
        contactCash.setGUI(GUIContact);
        contacts.put(chat.getId(), contactCash);
        return contactCash;
    }

    @SneakyThrows
    private void openChat (ChatEntity chat, PersonalChat personalChat, UserEntity interlocutor, ContactCash contactCash) {
            if (selectedContact == null || !Objects.equals(selectedContact.getChat().getId(), chat.getId())) {
                scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-background-image: url('img/backgroundChatImage.jpg'); -fx-border-width: 1 0 1 1; -fx-border-color: black;");
                chatField.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-spacing: 10");
                selectedContact = contactCash;
                sendMessageConfig.setSelectedContact(selectedContact);
                scrollConfig.startScrollEvent();
                setVisibleChatContent(true);

                if (personalChat.getNewMessagesCount() > 0) {contactCash.resetNotificationCount();}

                new Thread(() -> TCPConnection.CHECK_USER_STATUS(interlocutor.getId())).start();

                chatRoomName.setText(interlocutor.getName());
                printMessages();
                scrollConfig.scrolling();
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
                        case MESSAGE -> messageCash = isMyMessage ? GUIPatterns.makeMyMessageGUIAndGetCash(message) : GUIPatterns.makeForeignMessageGUIAndGetCash(message);
                        case AUDIO, DOCUMENT -> messageCash = isMyMessage ? GUIPatterns.printMyAudioGUIAndGetCash(message, AudioHelper.convertBytesToAudio(message.getMetadata())) : GUIPatterns.printForeignAudioGUIAndGetCash(message, AudioHelper.convertBytesToAudio(message.getMetadata()));
                    }
                    MessageCash finalMessageCash = messageCash;

                    selectedContact.addMessageOnCash(finalMessageCash);
                    chatField.getChildren().add(finalMessageCash.getMessageBackground());
                    finalMessageCash.getGUI().setOnMouseClicked(clickOnMessage -> {
                        if (clickOnMessage.getButton() == MouseButton.SECONDARY) sendMessageConfig.setMessageFunctions(finalMessageCash, clickOnMessage);
                    });
                }
            });
        }
    }
    
    private void setSearchConfig(String newValue, PauseTransition pause) {
        pause.stop();
        pause.playFromStart();
        pause.setOnFinished(event -> {
            contactsField.getChildren().clear();
            userRepository.findFirstByNickname(newValue).ifPresent(interlocutor -> {
                BorderPane contact = GUIPatterns.newChatBuilder(interlocutor);
                contactsField.getChildren().add(contact);
                contact.setOnMouseClicked(mouseEvent -> {
                    searchPanel.clear();
                    contacts.values().stream()
                            .filter(contactCash1 -> Objects.equals(contactCash1.getInterlocutor().getId(), interlocutor.getId()))
                            .findFirst().ifPresentOrElse(existingContactCash -> {
                                openChat(existingContactCash.getChat(), existingContactCash.getPersonalChat(), existingContactCash.getInterlocutor(), existingContactCash);
                                loadContactsFromCash();}, () -> createNewChatRoom(interlocutor));
                });
            });
            if (searchPanel.getText().isEmpty()) loadContactsFromCash();
        });
    }

    private void createNewChatRoom(UserEntity interlocutor) {
        ChatEntity newChat = ChatEntity.builder().build();
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
        loadContactsFromCash();
        ContactCash newContactCash = createContact (interlocutor, newChat, myPersonalChat);
        openChat(newChat, myPersonalChat, interlocutor, newContactCash);
        TCPConnection.CREATE_NEW_CHAT_ROOM(interlocutor.getId(), participantPersonalChat.getId());
        userEntity.getUserChats().add(myPersonalChat);
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
                }
                else contacts.get(chatId).deleteMessageDefault(messageId);
            }
            case "CREATE_NEW_CHAT_ROOM" -> {
                PersonalChat personalChat = personalChatRepository.findById(Long.parseLong(value)).orElseThrow();
                UserEntity interlocutor = personalChat.getInterlocutor();
                ChatEntity chatEntity = personalChat.getChat();
                Platform.runLater(() -> createContact(interlocutor, chatEntity, personalChat));
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
                    contactsField.getChildren().add(0, contactCash.getGUI());
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
    private void writeReceivedAudio (MessageEntity message, ContactCash contactCash, Long chatId) {
        MessageCash messageCash = GUIPatterns.printForeignAudioGUIAndGetCash(message, AudioHelper.convertBytesToAudio(message.getMetadata()));
        contactCash.addMessageOnCashAndPutLastMessage(messageCash);
        if (selectedContact != null && Objects.equals(selectedContact.getChat().getId(), chatId)) {
            Platform.runLater(() -> {
                if (scrollPane.getVvalue() >= 0.95) {
                    chatField.getChildren().add(messageCash.getMessageBackground());
                    scrollConfig.scrolling();
                } else chatField.getChildren().add(messageCash.getMessageBackground());
            });
        } else {
            contactCash.addNotification(message);
            SongPlayer.notification();
        }
    }

    private void writeReceivedMessage (MessageEntity message, ContactCash contactCash, Long chatId) {
        MessageCash messageCash = GUIPatterns.makeForeignMessageGUIAndGetCash(message);
        contactCash.addMessageOnCashAndPutLastMessage(messageCash);
        if (selectedContact != null && Objects.equals(selectedContact.getChat().getId(), chatId)) {
            Platform.runLater(() -> {
                if (scrollPane.getVvalue() >= 0.95) {
                    chatField.getChildren().add(messageCash.getMessageBackground());
                    scrollConfig.scrolling();
                } else chatField.getChildren().add(messageCash.getMessageBackground());
            });
        } else {
            contactCash.addNotification(message);
            SongPlayer.notification();
        }
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {Platform.exit();}
}