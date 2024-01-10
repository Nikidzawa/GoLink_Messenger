package ru.nikidzawa.golink.FXControllers.helpers;

import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import ru.nikidzawa.golink.store.entities.ChatEntity;
import ru.nikidzawa.golink.store.entities.MessageEntity;
import ru.nikidzawa.golink.store.entities.PersonalChat;
import ru.nikidzawa.golink.store.entities.UserEntity;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class Contact {
    //GUI
    private BorderPane GUIContact;
    private Text date;
    private BorderPane nameAndLastMessage;
    private TextField lastMessageText;
    private StackPane newMessagesBlock;
    private Text newMessagesCount;

    //CASH
    private PersonalChat personalChat;
    private UserEntity interlocutor;
    private ChatEntity chat;
    private int cashingNotificationCount;
    private int port;

    public Contact (UserEntity interlocutor, ChatEntity chat, PersonalChat personalChat) {
        this.interlocutor = interlocutor;
        this.chat = chat;
        this.personalChat = personalChat;
    }

    private void setLastMessage (MessageEntity message) {
        date.setVisible(true);
        lastMessageText.setText(Objects.equals(message.getSender().getId(), interlocutor.getId()) ? message.getMessage() : "Вы: " + message.getMessage());
        date.setText(message.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
    }
    private void setTextIfMessagesIsEmpty () {
        lastMessageText.setText("Чат пуст");
        date.setVisible(false);
    }

    public void resetNotificationCount() {
        cashingNotificationCount = 0;
        newMessagesBlock.setVisible(false);
        newMessagesCount.setText("0");
    }

    public void addNotification () {
        cashingNotificationCount ++;
        newMessagesBlock.setVisible(true);
        newMessagesCount.setText(String.valueOf(Integer.parseInt(newMessagesCount.getText()) + 1));
    }

    public void addMessageOnCashAndPutLastMessage (MessageEntity message) {
        chat.getMessages().add(message);
        setLastMessage(message);
    }

    public void editMessage (MessageEntity message, String messageText) {
        message.setMessage(messageText);
        int indexMessage = chat.getMessages().indexOf(message);
        if (indexMessage == chat.getMessages().size() - 1) {
            try {
                setLastMessage(message);
            } catch (IndexOutOfBoundsException ex) {
                setTextIfMessagesIsEmpty();
            }
        }
    }

    public MessageEntity editMessage (int messageId, String messageText) {
        MessageEntity messageEntity = chat.getMessages().stream().filter(message -> message.getId() == messageId).findFirst().get();
        messageEntity.setMessage(messageText);
        int indexMessage = chat.getMessages().indexOf(messageEntity);
        if (indexMessage == chat.getMessages().size() - 1) {
            try {
                setLastMessage(messageEntity);
            } catch (IndexOutOfBoundsException ex) {
                setTextIfMessagesIsEmpty();
            }
        }
        return messageEntity;
    }

    public void deleteMessage (MessageEntity message) {
        int index = chat.getMessages().indexOf(message);
        if (index == chat.getMessages().size() - 1) {
            try {
                setLastMessage(chat.getMessages().get(index - 1));
            } catch (IndexOutOfBoundsException ex) {
                setTextIfMessagesIsEmpty();
            }
        }
        chat.getMessages().remove(index);
    }
    public void deleteMessage (int messageId) {
        MessageEntity messageEntity = chat.getMessages().stream().filter(message -> message.getId() == messageId).findFirst().get();
        int indexMessage = chat.getMessages().indexOf(messageEntity);
        if (indexMessage == chat.getMessages().size() - 1) {
            try {
                setLastMessage(chat.getMessages().get((indexMessage - 1)));
            } catch (IndexOutOfBoundsException ex) {
                setTextIfMessagesIsEmpty();
            }
        }
        chat.getMessages().remove(indexMessage);
    }
}