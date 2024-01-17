package ru.nikidzawa.golink.FXControllers.Configurations;

import javafx.animation.PauseTransition;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ru.nikidzawa.golink.FXControllers.GoLink;
import ru.nikidzawa.golink.FXControllers.cash.ContactCash;
import ru.nikidzawa.golink.store.entities.ChatEntity;
import ru.nikidzawa.golink.store.entities.PersonalChat;
import ru.nikidzawa.golink.store.entities.UserEntity;

import java.util.List;
import java.util.Objects;

public class SearchConfig {
    private final GoLink goLink;
    private final VBox contactsField;
    private final TextField searchPanel;

    public SearchConfig(GoLink goLink, VBox contactsField, TextField searchPanel) {
        this.searchPanel = searchPanel;
        this.contactsField = contactsField;
        this.goLink = goLink;
        searchPanel.textProperty().addListener((observable, oldValue, newValue) -> setSearchConfig(newValue, new PauseTransition(Duration.millis(1000))));
    }

    private void setSearchConfig(String newValue, PauseTransition pause) {
        pause.stop();
        pause.playFromStart();
        pause.setOnFinished(event -> {
            contactsField.getChildren().clear();
            goLink.userRepository.findFirstByNickname(newValue).ifPresent(interlocutor -> {
                BorderPane contact = goLink.GUIPatterns.newChatBuilder(interlocutor);
                contactsField.getChildren().add(contact);
                contact.setOnMouseClicked(mouseEvent -> {
                    searchPanel.clear();
                    goLink.contacts.values().stream()
                            .filter(contactCash1 -> Objects.equals(contactCash1.getInterlocutor().getId(), interlocutor.getId()))
                            .findFirst().ifPresentOrElse(existingContactCash -> {
                                goLink.openChat(existingContactCash.getChat(), existingContactCash.getPersonalChat(), existingContactCash.getInterlocutor(), existingContactCash);
                                loadContactsFromCash();
                            }, () -> createNewChatRoom(interlocutor));
                });
            });
            if (searchPanel.getText().isEmpty()) loadContactsFromCash();
        });
    }

    private void createNewChatRoom(UserEntity interlocutor) {
        ChatEntity newChat = ChatEntity.builder().build();
        goLink.chatRepository.saveAndFlush(newChat);
        PersonalChat myPersonalChat = PersonalChat.builder()
                .chat(newChat)
                .user(goLink.userEntity)
                .interlocutor(interlocutor)
                .build();

        PersonalChat participantPersonalChat = PersonalChat.builder()
                .chat(newChat)
                .user(interlocutor)
                .interlocutor(goLink.userEntity)
                .build();

        goLink.personalChatRepository.saveAndFlush(myPersonalChat);
        goLink.personalChatRepository.saveAndFlush(participantPersonalChat);

        newChat.setPersonalChats(List.of(myPersonalChat, participantPersonalChat));
        goLink.chatRepository.saveAndFlush(newChat);
        loadContactsFromCash();
        ContactCash newContactCash = goLink.createContact(interlocutor, newChat, myPersonalChat);
        goLink.openChat(newChat, myPersonalChat, interlocutor, newContactCash);
        goLink.TCPConnection.CREATE_NEW_CHAT_ROOM(interlocutor.getId(), participantPersonalChat.getId());
        goLink.userEntity.getUserChats().add(myPersonalChat);
    }

    private void loadContactsFromCash() {
        contactsField.getChildren().clear();
        goLink.contacts.values().forEach(contactCash -> contactsField.getChildren().add(contactCash.getGUI()));
    }
}
