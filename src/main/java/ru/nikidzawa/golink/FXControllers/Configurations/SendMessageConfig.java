package ru.nikidzawa.golink.FXControllers.Configurations;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Setter;
import ru.nikidzawa.golink.FXControllers.cash.ContactCash;
import ru.nikidzawa.golink.FXControllers.cash.MessageCash;
import ru.nikidzawa.golink.FXControllers.helpers.GUIPatterns;
import ru.nikidzawa.golink.network.TCPConnection;
import ru.nikidzawa.golink.services.sound.AudioHelper;
import ru.nikidzawa.golink.store.MessageType;
import ru.nikidzawa.golink.store.entities.MessageEntity;
import ru.nikidzawa.golink.store.entities.UserEntity;
import ru.nikidzawa.golink.store.repositories.MessageRepository;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

public class SendMessageConfig {

    private final VBox contactsField;
    private final VBox chatField;

    private final ImageView sendButton;
    private final ImageView sendImageButton;
    private final Scene scene;
    private final TextField input;

    private final TCPConnection TCPConnection;
    private final ScrollConfig scrollConfig;
    private final MessageRepository messageRepository;
    private final BorderPane sendZone;
    private final UserEntity userEntity;

    GUIPatterns GUIPatterns = new GUIPatterns();

    @Setter
    private ContactCash selectedContact;

    private boolean isEditMessageStage;
    private MessageCash messageCash;

    private byte[] selectedImage;
    private ImageView imageView;

    public SendMessageConfig(UserEntity userEntity, Scene scene, ImageView sendImageButton, ImageView microphoneButton, ImageView sendButton, VBox contactsField, VBox chatField, TextField input, TCPConnection tcpConnection, ScrollConfig scrollConfig, MessageRepository messageRepository, BorderPane sendZone) {
        this.userEntity = userEntity;
        this.scene = scene;
        this.sendImageButton = sendImageButton;
        this.sendButton = sendButton;
        this.contactsField = contactsField;
        this.chatField = chatField;
        this.input = input;
        this.TCPConnection = tcpConnection;
        this.scrollConfig = scrollConfig;
        this.messageRepository = messageRepository;
        this.sendZone = sendZone;
        microphoneButton.setOnMousePressed(mouseEvent -> AudioHelper.startRecording());
        microphoneButton.setOnMouseReleased(mouseEvent -> sendAudi0Configuration());
        sendImageButton.setOnMouseClicked(mouseEvent -> sendMessageConfiguration());
        sendButton.setOnMouseClicked(mouseEvent -> sendMessageConfiguration());
    }

    public void sendAudi0Configuration () {
        if (selectedContact != null) {
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
                MessageCash messageCash = GUIPatterns.printMyAudioGUIAndGetCash(messageEntity, AudioHelper.getFile());
                messageRepository.save(messageEntity);
                TCPConnection.FILE_POST(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), MessageType.AUDIO, null, metadata);
                selectedContact.addMessageOnCashAndPutLastMessage(messageCash);
                chatField.getChildren().add(messageCash.getMessageBackground());
                scrollConfig.scrolling();

                contactsField.getChildren().remove(selectedContact.getGUI());
                contactsField.getChildren().add(0, selectedContact.getGUI());
            } catch (IOException | UnsupportedAudioFileException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessageConfiguration() {
        String text = input.getText().trim();
            if (isEditMessageStage) {
                MessageEntity messageEntity = messageCash.getMessage();
                switch (messageEntity.getMessageType()) {
                    case TEXT -> processEditTextMessage(messageEntity, text);
                    case IMAGE -> processEditImageMessage(messageEntity, text);
                    case IMAGE_AND_TEXT -> processEditImageAndTextMessage(messageEntity, text);
                }
                disable();
            } else {
                if (!input.getText().isEmpty()) {
                    MessageEntity messageEntity = MessageEntity.builder()
                            .message(text)
                            .date(LocalDateTime.now())
                            .sender(userEntity)
                            .chat(selectedContact.getChat())
                            .messageType(MessageType.TEXT)
                            .build();
                    messageRepository.save(messageEntity);

                    TCPConnection.POST(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), text);
                    chatField.getChildren().add(addMessageToGlobalCash(GUIPatterns.makeMyMessageGUIAndGetCash(messageEntity)));

                    scrollConfig.scrolling();

                    contactsField.getChildren().remove(selectedContact.getGUI());
                    contactsField.getChildren().add(0, selectedContact.getGUI());
                }
            }
            input.clear();

    }
    private void processEditTextMessage(MessageEntity messageEntity, String text) {
        if (selectedImage == null) {
            if (text.isEmpty()) {
                deleteMessageFunction(messageEntity);
            } else if (!messageEntity.getMessage().equals(text)) {
                saveMessageText(messageEntity, text);
            }
        } else {
            messageEntity.setMetadata(selectedImage);
            if (text.isEmpty()) {
                messageEntity.setMessageType(MessageType.IMAGE);
            } else {
                messageEntity.setMessageType(MessageType.IMAGE_AND_TEXT);
                if (!messageEntity.getMessage().equals(text)) {
                    messageEntity.setMessage(text);
                }
            }
            saveTextAndFile(messageEntity);
        }
    }

    private void saveMessageText (MessageEntity messageEntity, String text) {
        selectedContact.editMessage(messageCash, text);
        messageRepository.save(messageEntity);
        TCPConnection.EDIT(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), text);
    }

    private void saveTextAndFile (MessageEntity messageEntity) {
        selectedContact.editMessageAndFile(messageCash, messageEntity);
        messageRepository.save(messageEntity);
        TCPConnection.FILE_EDIT(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), messageEntity.getMessageType(), messageEntity.getMessage(), selectedImage);
    }

    private void processEditImageMessage(MessageEntity messageEntity, String text) {
        boolean isEdited = false;
        if (selectedImage == null) {
            if (!text.isEmpty()) {
                messageEntity.setMessage(text);
                messageEntity.setMessageType(MessageType.TEXT);
                isEdited = true;
            } else {
                deleteMessageFunction(messageEntity);
            }
        } else {
            if (selectedImage != messageEntity.getMetadata()) {
                messageEntity.setMetadata(selectedImage);
                isEdited = true;
            }
            if (!text.isEmpty()) {
                messageEntity.setMessage(text);
                messageEntity.setMessageType(MessageType.IMAGE_AND_TEXT);
                isEdited = true;
            }
        }
        if (isEdited) {saveTextAndFile(messageEntity);}
    }

    private void processEditImageAndTextMessage(MessageEntity messageEntity, String text) {
        boolean isEdited = false;
        if (selectedImage == null) {
            if (!text.isEmpty()) {
                messageEntity.setMessage(text);
                messageEntity.setMessageType(MessageType.TEXT);
                isEdited = true;
            } else {
                deleteMessageFunction(messageEntity);
            }
        } else {
            if (selectedImage != messageEntity.getMetadata()) {
                messageEntity.setMetadata(selectedImage);
                isEdited = true;
            }
            if (!text.isEmpty()) {
                if (!Objects.equals(messageEntity.getMessage(), text)) {
                    messageEntity.setMessage(text);
                    isEdited = true;
                }
            } else {
                messageEntity.setMessage(null);
                messageEntity.setMessageType(MessageType.IMAGE);
                isEdited = true;
            }
        }
        if (isEdited) {saveTextAndFile(messageEntity);}
    }

    public void sendImageConfig () {
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Изображения", "*.jpg", "*.png", "*.jpeg")
            );
            File selectedFile = fileChooser.showOpenDialog(scene.getWindow());

            if (selectedFile != null) {
                byte[] imageBytes;
                try {
                    imageBytes = Files.readAllBytes(selectedFile.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (isEditMessageStage) {
                    selectedImage = imageBytes;
                    imageView.setFitWidth(40);
                    imageView.setFitHeight(40);
                    imageView.setImage(new Image(selectedFile.getPath()));
                } else {
                    MessageEntity messageEntity = MessageEntity.builder()
                            .metadata(imageBytes)
                            .sender(userEntity)
                            .chat(selectedContact.getChat())
                            .messageType(MessageType.IMAGE)
                            .date(LocalDateTime.now())
                            .build();
                    messageRepository.saveAndFlush(messageEntity);

                    TCPConnection.FILE_POST(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), MessageType.IMAGE, null, imageBytes);
                    chatField.getChildren().add(addMessageToGlobalCash(GUIPatterns.makeMyMessageGUIAndGetCash(messageEntity)));
                    scrollConfig.scrolling();
                }
            }
        });
    }

    private HBox addMessageToGlobalCash (MessageCash messageCash) {
        selectedContact.addMessageOnCashAndPutLastMessage(messageCash);
        messageCash.getGUI().setOnMouseClicked(clickOnMessage -> {
            if (clickOnMessage.getButton() == MouseButton.SECONDARY) setMessageFunctions(messageCash, clickOnMessage);
        });
        return messageCash.getMessageBackground();
    }

    public void setMessageFunctions(MessageCash messageCash, MouseEvent clickOnMessage) {
        Stage messageStage = new Stage();
        this.messageCash = messageCash;

        scene.setOnMouseClicked(mouseEvent -> {
            if (messageStage.isShowing()) {
                messageStage.close();
                GUIPatterns.setMessageStage(null);
            }
        });

        BorderPane deleteButton = GUIPatterns.deleteMessageButton();
        deleteButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            deleteMessageFunction(messageCash.getMessage());
            messageStage.close();
            GUIPatterns.setMessageStage(null);
        });

        switch (messageCash.getMessage().getMessageType()) {
            case TEXT, IMAGE, IMAGE_AND_TEXT -> {
                BorderPane editButton = GUIPatterns.editeMessageButton();
                editButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    editFunction(messageCash);
                    input.setText(messageCash.getMessage().getMessage());
                    messageStage.close();
                    GUIPatterns.setMessageStage(null);
                });

                BorderPane copyButton = GUIPatterns.copyMessageButton();
                copyButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    copyMessageFunction(messageCash.getMessage().getMessage());
                    messageStage.close();
                    GUIPatterns.setMessageStage(null);
                });
                Platform.runLater( () -> GUIPatterns.openMessageWindow(messageCash.getMessage(), clickOnMessage, messageStage, copyButton, editButton, deleteButton));
            }
            case AUDIO, DOCUMENT -> Platform.runLater(() -> GUIPatterns.openMessageWindow(messageCash.getMessage(), clickOnMessage, messageStage, null, null, deleteButton));
        }
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
        chatField.getChildren().remove(messageCash.getMessageBackground());
    }

    private void editFunction (MessageCash messageCash) {
        isEditMessageStage = true;
        ImageView cancelButton = GUIPatterns.getCancelButton();
        Pair <BorderPane, ImageView> pair = GUIPatterns.getBackgroundEditInterface(messageCash);
        BorderPane backgroundEditeInterface = pair.getKey();
        imageView = pair.getValue();
        selectedImage = messageCash.getMessage().getMetadata();

        imageView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY && selectedImage != null) {
                selectedImage = null;
                imageView.setFitHeight(30);
                imageView.setFitWidth(30);
                imageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/editTXT.png"))));
            } else {
                int index = chatField.getChildren().indexOf(messageCash.getMessageBackground());
                scrollConfig.showElement(index);
                chatField.getChildren().get(index).requestFocus();
                messageCash.getMessageBackground().setStyle("-fx-background-color: #18314D");;
                Timer destructionTimer = new Timer();
                destructionTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> messageCash.getMessageBackground().setStyle("-fx-background-color: #001933"));
                    }
                }, 1500);
            }
        });
        backgroundEditeInterface.setRight(cancelButton);
        sendZone.setTop(backgroundEditeInterface);
        BorderPane.setMargin(cancelButton, new Insets(10, 10, 0 , 0));
        cancelButton.setOnMouseClicked(mouseEvent -> disable());
    }

    public void disable () {
        isEditMessageStage = false;
        selectedImage = null;
        imageView = null;
        sendZone.setTop(null);
        messageCash = null;
    }
}
