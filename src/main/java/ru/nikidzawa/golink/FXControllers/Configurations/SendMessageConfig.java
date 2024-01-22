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
import ru.nikidzawa.golink.FXControllers.GoLink;
import ru.nikidzawa.golink.FXControllers.cash.ContactCash;
import ru.nikidzawa.golink.FXControllers.cash.MessageCash;
import ru.nikidzawa.golink.services.GUI.EmptyStage;
import ru.nikidzawa.golink.services.GUI.GUIPatterns;
import ru.nikidzawa.golink.services.sound.AudioHelper;
import ru.nikidzawa.networkAPI.network.TCPConnection;
import ru.nikidzawa.networkAPI.store.MessageType;
import ru.nikidzawa.networkAPI.store.entities.MessageEntity;
import ru.nikidzawa.networkAPI.store.entities.PersonalChatEntity;
import ru.nikidzawa.networkAPI.store.entities.UserEntity;
import ru.nikidzawa.networkAPI.store.repositories.MessageRepository;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class SendMessageConfig {

    private final VBox contactsField;
    private final VBox chatField;

    private final ImageView sendImageButton;
    private final ImageView microphoneButton;
    private final TextField input;
    private final ScrollConfig scrollConfig;
    private final BorderPane sendZone;
    private final GUIPatterns GUIPatterns;
    private ContactCash selectedContact;
    private final TCPConnection TCPConnection;
    private final UserEntity userEntity;
    private final MessageRepository messageRepository;
    private final Scene scene;
    private boolean isEditMessageStage;
    private MessageCash messageCash;
    private PersonalChatEntity interlocutorPersonalChatEntity;

    private byte[] selectedImage;
    private ImageView imageView;

    private Stage messageContextOption;

    public SendMessageConfig(GoLink goLink) {
        this.messageRepository = goLink.messageRepository;
        this.scene = goLink.getScene();
        this.userEntity = goLink.getUserEntity();
        this.TCPConnection = goLink.getTCPConnection();
        this.GUIPatterns = goLink.GUIPatterns;
        this.sendImageButton = goLink.getSendImageButton();
        this.microphoneButton = goLink.getMicrophone();
        this.contactsField = goLink.getContactsField();
        this.chatField = goLink.getChatField();
        this.input = goLink.getInput();
        this.scrollConfig = goLink.getScrollConfig();
        this.sendZone = goLink.getSendZone();
        microphoneButton.setOnMousePressed(_ -> AudioHelper.startRecording());
        microphoneButton.setOnMouseReleased(_ -> sendAudi0Configuration());
        sendImageButton.setOnMouseClicked(_ -> sendImageConfig());
        goLink.getSendButton().setOnMouseClicked(_ -> sendMessageConfiguration());
    }

    public void setSelectedContact(ContactCash contactCash) {
        selectedContact = contactCash;
        interlocutorPersonalChatEntity = contactCash.getInterlocutor().getUserChats().stream().filter(personalChat -> Objects.equals(personalChat.getInterlocutor().getId(), userEntity.getId())).findFirst().orElseThrow();
    }

    public void sendAudi0Configuration() {
        if (selectedContact != null) {AudioHelper.stopRecording();
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

            TCPConnection.POST(selectedContact.getInterlocutor().getId(), interlocutorPersonalChatEntity.getId(), selectedContact.getChat().getId(), messageEntity.getId(), MessageType.AUDIO, null, metadata);
            selectedContact.addMessageOnCashAndPutLastMessage(messageCash);
            chatField.getChildren().add(messageCash.getMessageBackground());

            contactsField.getChildren().remove(selectedContact.getGUI());
            contactsField.getChildren().addFirst(selectedContact.getGUI());

            messageCash.getMessageBackground().setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    setMessageFunctions(messageCash, mouseEvent);
                }
            });
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
                microphoneButton.setVisible(true);
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
                TCPConnection.EDIT(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), messageEntity.getId(), ru.nikidzawa.networkAPI.store.MessageType.MESSAGE, messageEntity.getText(), selectedImage);
            }
            disable();
            microphoneButton.setVisible(true);
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
                TCPConnection.POST(selectedContact.getInterlocutor().getId(), interlocutorPersonalChatEntity.getId(), selectedContact.getChat().getId(), messageEntity.getId(), ru.nikidzawa.networkAPI.store.MessageType.MESSAGE, text, selectedImage);
                MessageCash messageCash = GUIPatterns.makeMyMessageGUIAndGetCash(messageEntity);
                chatField.getChildren().add(addMessageToGlobalCash(messageCash));

                contactsField.getChildren().remove(selectedContact.getGUI());
                contactsField.getChildren().addFirst(selectedContact.getGUI());
                disable();
                microphoneButton.setVisible(true);
            }
        }
        input.clear();
    }

    public void sendImageConfig() {
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
                        BorderPane.setMargin(cancelButton, new Insets(10, 10, 0, 0));
                        cancelButton.setOnMouseClicked(_ -> {
                            disable();
                            microphoneButton.setVisible(true);
                        });
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

    private HBox addMessageToGlobalCash(MessageCash messageCash) {
        selectedContact.addMessageOnCashAndPutLastMessage(messageCash);
        messageCash.getGUI().setOnMouseClicked(clickOnMessage -> {
            if (clickOnMessage.getButton() == MouseButton.SECONDARY) setMessageFunctions(messageCash, clickOnMessage);
        });
        return messageCash.getMessageBackground();
    }

    public void setMessageFunctions(MessageCash messageCash, MouseEvent clickOnMessage) {
        this.messageCash = messageCash;

        BorderPane deleteButton = GUIPatterns.deleteMessageButton();
        deleteButton.addEventFilter(MouseEvent.MOUSE_CLICKED, _ -> {
            deleteMessageFunction(messageCash.getMessage());
            messageContextOption.close();
        });

        switch (messageCash.getMessage().getMessageType()) {
            case MESSAGE -> {
                BorderPane editButton = GUIPatterns.editeMessageButton();
                editButton.addEventFilter(MouseEvent.MOUSE_CLICKED, _ -> {
                    editFunction(messageCash);
                    messageContextOption.close();
                });

                BorderPane copyButton = GUIPatterns.copyMessageButton();
                copyButton.addEventFilter(MouseEvent.MOUSE_CLICKED, _ -> {
                    copyMessageFunction(messageCash.getMessage().getText());
                    messageContextOption.close();
                });

                openMessageWindow(clickOnMessage, copyButton, editButton, deleteButton);
            }
            case AUDIO, DOCUMENT -> openMessageWindow(clickOnMessage,null, null, deleteButton);
        }
    }

    public void openMessageWindow(MouseEvent mouseEvent, BorderPane copy, BorderPane edit, BorderPane delete) {
        VBox vBox = new VBox();
        vBox.setStyle("-fx-background-color:  #18314D; -fx-border-color: black; -fx-spacing: 5");

        double finalHeight = delete.getPrefHeight();
        if (edit != null && copy != null) {
            finalHeight += copy.getPrefHeight();
            finalHeight += edit.getPrefHeight();
            vBox.getChildren().add(copy);
            vBox.getChildren().add(edit);
        }
        vBox.getChildren().add(delete);

        Scene scene = new Scene(vBox, 150, finalHeight);

        if (messageContextOption == null) {
            messageContextOption = EmptyStage.getEmptyStageAndSetScene(scene);
        }
        messageContextOption.focusedProperty().addListener((_, _, isNowFocused) -> {
            if (!isNowFocused) {
                messageContextOption.close();
            }
        });

        messageContextOption.setX(mouseEvent.getScreenX());
        messageContextOption.setY(mouseEvent.getScreenY());
        messageContextOption.toFront();
        Platform.runLater(() -> messageContextOption.show());
    }

    private void copyMessageFunction(String messageText) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(messageText);
        clipboard.setContent(content);
    }

    private void deleteMessageFunction(MessageEntity message) {
        Platform.runLater(() -> {
            messageRepository.deleteAllInBatch(Collections.singletonList(message));
            selectedContact.deleteMessage(message);
            TCPConnection.DELETE(selectedContact.getInterlocutor().getId(), selectedContact.getChat().getId(), message.getId());
            chatField.getChildren().remove(messageCash.getMessageBackground());
        });
    }

    private void editFunction(MessageCash messageCash) {
        isEditMessageStage = true;
        microphoneButton.setVisible(false);
        input.setText(messageCash.getMessage().getText());
        ImageView cancelButton = GUIPatterns.getCancelButton();
        Pair<BorderPane, ImageView> pair = GUIPatterns.getBackgroundEditInterface(messageCash);
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
        BorderPane.setMargin(cancelButton, new Insets(10, 10, 0, 0));
        cancelButton.setOnMouseClicked(_ -> {
            disable();
            microphoneButton.setVisible(true);
        });
    }

    public void disable() {
        Platform.runLater(() -> {
            sendImageButton.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/img.png"))));
            isEditMessageStage = false;
            selectedImage = null;
            imageView = null;
            sendZone.setTop(null);
            messageCash = null;
            input.clear();
        });
    }
}
