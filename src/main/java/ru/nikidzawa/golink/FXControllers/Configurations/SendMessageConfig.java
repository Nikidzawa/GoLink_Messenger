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
    private final ImageView microphoneButton;
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
        this.microphoneButton = microphoneButton;
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
                TCPConnection.POST(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), MessageType.AUDIO, null, metadata);
                selectedContact.addMessageOnCashAndPutLastMessage(messageCash);
                chatField.getChildren().add(messageCash.getMessageBackground());

                scrollConfig.scrolling();

                contactsField.getChildren().remove(selectedContact.getGUI());
                contactsField.getChildren().add(0, selectedContact.getGUI());

                messageCash.getMessageBackground().setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getButton() == MouseButton.SECONDARY) {setMessageFunctions(messageCash, mouseEvent);}
                });

            } catch (IOException | UnsupportedAudioFileException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessageConfiguration() {
        String text = input.getText();
        if (!text.isEmpty()) {
            text = text.trim();
        }
        if (isEditMessageStage) {
            MessageEntity messageEntity = messageCash.getMessage();
            boolean isEdited = false;
            if (selectedImage == null && text.isEmpty()) {
                deleteMessageFunction(messageEntity);
                disable();
                return;
            }
            if (messageEntity.getMetadata() != selectedImage) {
                isEdited = true;
                messageEntity.setMetadata(selectedImage);
            }
            if (!messageEntity.getText().equals(text)) {
                isEdited = true;
                messageEntity.setText(text);
            }
            if (isEdited) {
                selectedContact.editMessageAndFile(messageCash, messageEntity);
                messageRepository.save(messageEntity);
                TCPConnection.EDIT(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), MessageType.MESSAGE, messageEntity.getText(), selectedImage);
            }
            disable();
        } else {
            if (!input.getText().isEmpty() || selectedImage != null) {
                MessageEntity messageEntity = MessageEntity.builder()
                        .text(text)
                        .date(LocalDateTime.now())
                        .sender(userEntity)
                        .chat(selectedContact.getChat())
                        .metadata(selectedImage)
                        .messageType(MessageType.MESSAGE)
                        .build();
                messageRepository.save(messageEntity);
                TCPConnection.POST(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), MessageType.MESSAGE, text, selectedImage);
                MessageCash messageCash = GUIPatterns.makeMyMessageGUIAndGetCash(messageEntity);
                chatField.getChildren().add(addMessageToGlobalCash(messageCash));

                scrollConfig.scrolling();

                contactsField.getChildren().remove(selectedContact.getGUI());
                contactsField.getChildren().add(0, selectedContact.getGUI());
                disable();
            }
        }
        input.clear();
    }

    public void sendImageConfig () {
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Изображения", "*.jpg", "*.png", "*.jpeg")
            );
            File selectedFile = fileChooser.showOpenDialog(scene.getWindow());

            if (selectedFile != null) {
                try {
                    if (isEditMessageStage) {
                        imageView.setImage(new Image(selectedFile.toURI().toString()));
                        selectedImage = Files.readAllBytes(selectedFile.toPath());
                        sendImageButton.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/reloadImage.png"))));
                    } else {
                        Pair<BorderPane, ImageView> pair = GUIPatterns.getBackgroundSelectImageInterface();
                        imageView = pair.getValue();
                        imageView.setImage(new Image(selectedFile.toURI().toString()));
                        BorderPane borderPane = pair.getKey();
                        ImageView cancelButton = GUIPatterns.getCancelButton();
                        BorderPane.setMargin(cancelButton, new Insets(10, 10, 0 , 0));
                        cancelButton.setOnMouseClicked(mouseEvent -> disable());
                        borderPane.setRight(cancelButton);
                        sendZone.setTop(borderPane);

                        selectedImage = Files.readAllBytes(selectedFile.toPath());
                        imageView.setFitWidth(40);
                        imageView.setFitHeight(40);
                        microphoneButton.setVisible(false);
                        sendImageButton.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/reloadImage.png"))));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
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
            case MESSAGE -> {
                BorderPane editButton = GUIPatterns.editeMessageButton();
                editButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    editFunction(messageCash);
                    input.setText(messageCash.getMessage().getText());
                    messageStage.close();
                    GUIPatterns.setMessageStage(null);
                });

                BorderPane copyButton = GUIPatterns.copyMessageButton();
                copyButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    copyMessageFunction(messageCash.getMessage().getText());
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
        microphoneButton.setVisible(false);
        ImageView cancelButton = GUIPatterns.getCancelButton();
        Pair <BorderPane, ImageView> pair = GUIPatterns.getBackgroundEditInterface(messageCash);
        BorderPane backgroundEditeInterface = pair.getKey();
        imageView = pair.getValue();
        MessageEntity messageEntity = messageCash.getMessage();
        if (messageEntity.getMetadata() != null && messageEntity.getMetadata().length > 1) {
            sendImageButton.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/reloadImage.png"))));
        } else {
            sendImageButton.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/addImage.png"))));
        }
        selectedImage = messageEntity.getMetadata();


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
                messageCash.getMessageBackground().setStyle("-fx-background-color: rgba(24, 49, 77, 0.5);");
                Timer destructionTimer = new Timer();
                destructionTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> messageCash.getMessageBackground().setStyle("-fx-background-color: transparent"));
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
        microphoneButton.setVisible(true);
        sendImageButton.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/img.png"))));
        isEditMessageStage = false;
        selectedImage = null;
        imageView = null;
        sendZone.setTop(null);
        messageCash = null;
        input.clear();
    }
}
