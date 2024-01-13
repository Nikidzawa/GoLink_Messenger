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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import ru.nikidzawa.golink.FXControllers.cash.ContactCash;
import ru.nikidzawa.golink.FXControllers.helpers.GUIPatterns;
import ru.nikidzawa.golink.FXControllers.Configurations.ScrollConfig;
import ru.nikidzawa.golink.services.sound.AudioHelper;
import ru.nikidzawa.golink.network.ServerListener;
import ru.nikidzawa.golink.network.TCPConnection;
import ru.nikidzawa.golink.services.sound.SongPlayer;
import ru.nikidzawa.golink.store.MessageType;
import ru.nikidzawa.golink.store.entities.*;
import ru.nikidzawa.golink.store.repositories.*;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
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
    private ImageView send;

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

    @Setter
    private UserEntity userEntity;
    @Setter
    private Scene scene;
    MessageEntity message;
    HBox messageBackground;
    private boolean editable = false;
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

            scrollConfig = new ScrollConfig(scrollPane);
            setUserConfig();
            GUIPatterns.setBaseWindowTitleCommands(titleBar, minimizeButton, scaleButton, closeButton, TCPConnection);
            sendImageButton.setOnMouseClicked(mouseEvent -> selectImageConfig());
            send.setOnMouseClicked(actionEvent -> sendMessageConfiguration());
            scene.setOnKeyPressed(keyEvent -> {
                if (selectedContact != null && keyEvent.getCode() == KeyCode.ENTER) {
                    sendMessageConfiguration();
                }
                if (keyEvent.getCode() == KeyCode.ESCAPE) {
                    selectedContact = null;
                    GUIPatterns.setEmptyChatConfiguration(chatField);
                    status.setVisible(false);
                    sendImageButton.setVisible(false);
                    send.setVisible(false);
                    input.setVisible(false);
                    microphone.setVisible(false);
                    editable = false;
                    chatRoomName.setText("");
                }
            });
            microphone.setOnMousePressed(mouseEvent -> {
                AudioHelper.startRecording();
            });
            microphone.setOnMouseReleased(mouseEvent -> {
                AudioHelper.stopRecording();
                try {
                    byte[] metadata = AudioHelper.convertAudioToBytes(AudioHelper.getFile());
                    MessageEntity messageEntity = MessageEntity.builder()
                            .messageType(MessageType.AUDIO)
                            .metadata(metadata)
                            .sender(userEntity)
                            .date(LocalDateTime.now())
                            .chat(selectedContact.getChat())
                            .build();
                    HBox GUI = GUIPatterns.printMyAudio(messageEntity, AudioHelper.getFile());
                    messageRepository.save(messageEntity);
                    TCPConnection.FILE_POST(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), MessageType.AUDIO, null, metadata);
                    selectedContact.addMessageOnCashAndPutLastMessage(GUI, messageEntity);
                    chatField.getChildren().add(GUI);
                    scrollConfig.scrolling();

                    contactsField.getChildren().remove(selectedContact.getGUI());
                    contactsField.getChildren().add(0, selectedContact.getGUI());
                } catch (IOException | UnsupportedAudioFileException e) {
                    throw new RuntimeException(e);
                }
            });
            settingsButton.setOnMouseClicked(mouseEvent -> openSettings());

            PauseTransition pause = new PauseTransition(Duration.millis(1000));
            searchPanel.textProperty().addListener((observable, oldValue, newValue) -> setSearchConfig(newValue, pause));

            loadChatRooms();
        });
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
        stage.initStyle(StageStyle.UNDECORATED);
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
                selectedContact = contactCash;
                scrollConfig.startScrollEvent();
                if (personalChat.getNewMessagesCount() > 0) {contactCash.resetNotificationCount();}

                status.setVisible(true);
                sendImageButton.setVisible(true);
                input.setVisible(true);
                send.setVisible(true);
                microphone.setVisible(true);

                TCPConnection.CHECK_USER_STATUS(interlocutor.getId());
                chatRoomName.setText(interlocutor.getName());

                printMessages();
                scrollConfig.scrolling();
            }
    }

    private void sendMessageConfiguration() {
        String text = input.getText();
        if (!text.isEmpty()) {
            if (editable) {
                selectedContact.editMessage(message, text);
                messageRepository.save(message);
                TCPConnection.EDIT(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), message.getId(), text);
                editable = false;
                sendZone.setTop(null);
            } else {
                MessageEntity message = MessageEntity
                        .builder()
                        .message(text)
                        .date(LocalDateTime.now())
                        .sender(userEntity)
                        .chat(selectedContact.getChat())
                        .messageType(MessageType.TEXT)
                        .build();
                messageRepository.save(message);

                HBox GUI = GUIPatterns.printMyMessage(message);
                selectedContact.addMessageOnCashAndPutLastMessage(GUI, message);
                TCPConnection.POST(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), message.getId(), text);
                GUI.setOnMouseClicked(clickOnMessage -> {
                    if (clickOnMessage.getButton() == MouseButton.SECONDARY) {
                        setMessageFunctions(message, GUI, clickOnMessage);
                    }
                });
                chatField.getChildren().add(GUI);
                scrollConfig.scrolling();

                contactsField.getChildren().remove(selectedContact.getGUI());
                contactsField.getChildren().add(0, selectedContact.getGUI());
            }
        }
        input.clear();
    }

    @SneakyThrows
    public void printMessages() {
        chatField.getChildren().clear();
        List<MessageEntity> messages = selectedContact.getChat().getMessages().stream().sorted(Comparator.comparing(MessageEntity::getDate)).toList();
        if (!messages.isEmpty()) {
            messages.forEach(message -> {
                boolean isMyMessage = message.getSender().getId().equals(userEntity.getId());
                if (selectedContact.cashedMessageInformation.containsKey(message.getId())) {
                    chatField.getChildren().add(selectedContact.cashedMessageInformation.get(message.getId()).getGUI());
                } else {
                    switch (message.getMessageType()) {
                        case TEXT -> {
                            HBox hBox = isMyMessage ? writeMyMessage(message) : writeForeignMessage(message);
                            selectedContact.addMessageOnCash(hBox, message);
                            chatField.getChildren().add(hBox);
                        }
                        case IMAGE -> {
                            HBox hBox = isMyMessage ? GUIPatterns.printMyPhoto(new Image(new ByteArrayInputStream(message.getMetadata())), message) : GUIPatterns.printForeignPhoto(new Image(new ByteArrayInputStream(message.getMetadata())), message);
                            selectedContact.addMessageOnCash(hBox, message);
                            chatField.getChildren().add(hBox);
                        }
                        case AUDIO -> {
                            HBox hBox = isMyMessage ? GUIPatterns.printMyAudio(message, AudioHelper.convertBytesToAudio(message.getMetadata())) : GUIPatterns.printForeignAudio(message, AudioHelper.convertBytesToAudio(message.getMetadata()));
                            selectedContact.addMessageOnCash(hBox, message);
                            chatField.getChildren().add(hBox);
                        }

                        case DOCUMENT -> System.out.println("Получен документ");
                    }
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
        messageRepository.deleteAllInBatch(Collections.singletonList(message));
        selectedContact.deleteMessage(message);
        TCPConnection.DELETE(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), message.getId());
        chatField.getChildren().remove(messageBackground);
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
                    MessageEntity messageEntity = MessageEntity.builder()
                            .metadata(imageBytes)
                            .sender(userEntity)
                            .chat(selectedContact.getChat())
                            .messageType(MessageType.IMAGE)
                            .date(LocalDateTime.now())
                            .build();
                    messageRepository.saveAndFlush(messageEntity);
                    TCPConnection.FILE_POST(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), MessageType.IMAGE, null, imageBytes);
                    HBox hBox = GUIPatterns.printMyPhoto((new Image(new ByteArrayInputStream(imageBytes))), messageEntity);
                    selectedContact.addMessageOnCashAndPutLastMessage(hBox, messageEntity);
                    chatField.getChildren().add(hBox);
                    scrollConfig.scrolling();
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
        ChatEntity newChat = ChatEntity.builder().personalChats(new ArrayList<>()).messages(new ArrayList<>()).build();
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
        String value3;
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
        try {
            value3 = strings[3];
        } catch (ArrayIndexOutOfBoundsException ex) {
            value3 = null;
        }
        switch (command) {
            case "POST" -> {
                long chatId = Long.parseLong(value);
                ContactCash contactCash = contacts.get(chatId);
                ChatEntity chatEntity = contactCash.getChat();
                UserEntity interlocutor = contactCash.getInterlocutor();
                writeReceivedMessage(MessageEntity.builder()
                        .date(LocalDateTime.now())
                        .chat(chatEntity)
                        .messageType(MessageType.TEXT)
                        .id(Long.parseLong(value2))
                        .sender(interlocutor)
                        .message(value3)
                        .build(), contactCash, chatId);

                Platform.runLater(() -> {
                    contactsField.getChildren().remove(contactCash.getGUI());
                    contactsField.getChildren().add(0, contactCash.getGUI());
                });
            }
            case "EDIT" -> {
                long chatId = Long.parseLong(value);
                long messageId = Long.parseLong(value2);
                ContactCash contactCash = contacts.get(chatId);
                contactCash.editMessage(messageId, value3);
            }
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
        String value;
        String value2;
        String value3;
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
        try {
            value3 = strings[3];
        } catch (ArrayIndexOutOfBoundsException ex) {
            value3 = null;
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
                        .metadata(content)
                        .build();
                switch (messageType) {
                    case IMAGE -> writeReceivedImage(messageEntity, contactCash, chatId);
                    case IMAGE_AND_TEXT -> messageEntity.setMessage(strings[4]);
                    case AUDIO -> writeReceivedAudio(messageEntity, contactCash, chatId);
                }
                Platform.runLater(() -> {
                    contactsField.getChildren().remove(contactCash.getGUI());
                    contactsField.getChildren().add(0, contactCash.getGUI());
                });
            }
        }
    }
    private void writeReceivedAudio (MessageEntity message, ContactCash contactCash, Long chatId) {
        HBox hBox = GUIPatterns.printForeignAudio(message, AudioHelper.convertBytesToAudio(message.getMetadata()));
        contactCash.addMessageOnCashAndPutLastMessage(hBox, message);
        if (selectedContact != null && Objects.equals(selectedContact.getChat().getId(), chatId)) {
            Platform.runLater(() -> {
                if (scrollPane.getVvalue() >= 0.95) {
                    chatField.getChildren().add(hBox);
                    scrollConfig.scrolling();
                } else chatField.getChildren().add(hBox);
            });
        } else {
            contactCash.addNotification(message);
            SongPlayer.notification();
        }
    }

    private void writeReceivedImage (MessageEntity message, ContactCash contactCash, Long chatId) {
        HBox hBox = GUIPatterns.printForeignPhoto(new Image(new ByteArrayInputStream(message.getMetadata())), message);
        contactCash.addMessageOnCashAndPutLastMessage(hBox, message);
        if (selectedContact != null && Objects.equals(selectedContact.getChat().getId(), chatId)) {
            Platform.runLater(() -> {
                if (scrollPane.getVvalue() >= 0.95) {
                    chatField.getChildren().add(hBox);
                    scrollConfig.scrolling();
                } else chatField.getChildren().add(hBox);
            });
        } else {
            contactCash.addNotification(message);
            SongPlayer.notification();
        }
    }

    private void writeReceivedMessage (MessageEntity message, ContactCash contactCash, Long chatId) {
        HBox hBox = GUIPatterns.printForeignMessage(message);
        contactCash.addMessageOnCashAndPutLastMessage(hBox, message);
        if (selectedContact != null && Objects.equals(selectedContact.getChat().getId(), chatId)) {
            Platform.runLater(() -> {
                if (scrollPane.getVvalue() >= 0.95) {
                    chatField.getChildren().add(hBox);
                    scrollConfig.scrolling();
                } else chatField.getChildren().add(hBox);
            });
        } else {
            contactCash.addNotification(message);
            SongPlayer.notification();
        }
    }

    private HBox writeForeignMessage (MessageEntity message) {
        return GUIPatterns.printForeignMessage(message);
    }

    private HBox writeMyMessage (MessageEntity message) {
        HBox GUI = GUIPatterns.printMyMessage(message);
        GUI.setOnMouseClicked(clickOnMessage -> {
            if (clickOnMessage.getButton() == MouseButton.SECONDARY) setMessageFunctions(message, GUI, clickOnMessage);
        });
        return GUI;
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {Platform.exit();}
}