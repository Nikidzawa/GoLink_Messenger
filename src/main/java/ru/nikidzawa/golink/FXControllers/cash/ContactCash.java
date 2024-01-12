package ru.nikidzawa.golink.FXControllers.cash;

import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import ru.nikidzawa.golink.store.entities.ChatEntity;
import ru.nikidzawa.golink.store.entities.MessageEntity;
import ru.nikidzawa.golink.store.entities.PersonalChat;
import ru.nikidzawa.golink.store.entities.UserEntity;
import ru.nikidzawa.golink.store.repositories.PersonalChatRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class ContactCash {
    //GUI
    public HashMap<Long, MessageCash> cashedMessageInformation = new HashMap<>();
    public List <MessageEntity> newMessages = new ArrayList<>();
    
    private BorderPane GUI;
    private Text date;
    private BorderPane nameAndLastMessage;
    private TextField lastMessageText;
    private StackPane newMessagesBlock;
    private Text newMessagesCount;

    //Data base
    private PersonalChatRepository personalChatRepository;
    private PersonalChat personalChat;
    private UserEntity interlocutor;
    private ChatEntity chat;
    private MessageEntity lastMessage;

    public ContactCash(UserEntity interlocutor, ChatEntity chat, PersonalChat personalChat, PersonalChatRepository personalChatRepository) {
        this.interlocutor = interlocutor;
        this.chat = chat;
        this.personalChat = personalChat;
        this.personalChatRepository = personalChatRepository;
    }
    private void setTextIfMessagesIsEmpty () {
        lastMessageText.setText("Чат пуст");
        date.setVisible(false);
    }

    public void resetNotificationCount() {
        newMessages.clear();
        personalChat.setNewMessagesCount(0);
        personalChatRepository.save(personalChat);
        newMessagesBlock.setVisible(false);
        newMessagesCount.setText("0");
    }

    public void addNotification (MessageEntity message) {
        newMessages.add(message);
        personalChat.setNewMessagesCount(personalChat.getNewMessagesCount() + 1);
        newMessagesBlock.setVisible(true);
        newMessagesCount.setText(String.valueOf(Integer.parseInt(newMessagesCount.getText()) + 1));
    }

    public void addMessageOnCashAndPutLastMessage (HBox hBox, MessageEntity message) {
        cashedMessageInformation.put(message.getId(), new MessageCash(hBox, message));
        chat.getMessages().add(message);
        putLastMessage(message);
    }

    public void addMessageOnCash (HBox hBox, MessageEntity message) {
        cashedMessageInformation.put(message.getId(), new MessageCash(hBox, message));
    }

    public void putLastMessage (MessageEntity message) {
        date.setVisible(true);
        switch (message.getMessageType()) {
            case TEXT, IMAGE_AND_TEXT -> lastMessageText.setText(Objects.equals(message.getSender().getId(), interlocutor.getId()) ? message.getMessage() : "Вы: " + message.getMessage());
            case IMAGE ->  lastMessageText.setText(Objects.equals(message.getSender().getId(), interlocutor.getId()) ? "Фотография" : "Вы: " + "фотография");
            case AUDIO -> lastMessageText.setText(Objects.equals(message.getSender().getId(), interlocutor.getId()) ? "Аудиофайл" : "Вы: " + "аудиофайл");
            case DOCUMENT -> lastMessageText.setText(Objects.equals(message.getSender().getId(), interlocutor.getId()) ? "Файл" : "Вы: " + "файл");
        }
        date.setText(message.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    public void editMessage (MessageEntity message, String messageText) {
        message.setHasBeenChanged(true);
        cashedMessageInformation.get(message.getId()).changeText(messageText);
        message.setMessage(messageText);
        isLastMessage(message);
    }

    public void editMessage (Long messageId, String messageText) {
        if (cashedMessageInformation.containsKey(messageId)) {
            MessageCash messageCash = cashedMessageInformation.get(messageId);
            messageCash.changeText(messageText);
            messageCash.getMessage().setHasBeenChanged(true);
            isLastMessage(messageCash.getMessage());
        } else {
            MessageEntity message = chat.getMessages().stream().filter(msg -> Objects.equals(msg.getId(), messageId)).findFirst().orElseThrow();
            message.setHasBeenChanged(true);
            message.setMessage(messageText);
            isLastMessage(message);
        }
    }

    public void deleteMessage (MessageEntity message) {
        int index = chat.getMessages().indexOf(message);
        if (index == chat.getMessages().size() - 1) {
            try {
                putLastMessage(chat.getMessages().get(index - 1));
            } catch (IndexOutOfBoundsException ex) {
                setTextIfMessagesIsEmpty();
            }
        }
        chat.getMessages().remove(index);
        cashedMessageInformation.remove(message.getId());
        isNewMessage(message);
    }

    public void deleteMessageDefault (Long messageId) {
        List<MessageEntity> messageEntities = chat.getMessages();
        MessageEntity message = messageEntities.stream().filter(message1 -> Objects.equals(message1.getId(), messageId)).findFirst().orElseThrow();
        int indexMessage = messageEntities.indexOf(message);
        if (indexMessage == messageEntities.size() - 1) {
            try {
                putLastMessage(messageEntities.get(indexMessage - 1));
            } catch (IndexOutOfBoundsException ex) {
                setTextIfMessagesIsEmpty();
            }
        }
        isNewMessage(message);
        cashedMessageInformation.remove(messageId);
        chat.getMessages().remove(indexMessage);
    }

    public HBox deleteMessage (Long messageId) {
        MessageCash messageCash = cashedMessageInformation.get(messageId);
        List<MessageEntity> messageEntities = chat.getMessages();
        int indexMessage = messageEntities.indexOf(messageCash.getMessage());
        if (indexMessage == messageEntities.size() - 1) {
            try {
                putLastMessage(messageEntities.get(indexMessage - 1));
            } catch (IndexOutOfBoundsException ex) {
                setTextIfMessagesIsEmpty();
            }
        }
        chat.getMessages().remove(indexMessage);
        cashedMessageInformation.remove(messageId);
        isNewMessage(messageCash.getMessage());
        return messageCash.getGUI();
    }

    private void isNewMessage(MessageEntity message) {
        if (newMessages.contains(message)) {
            newMessages.remove(message);
            personalChat.setNewMessagesCount(personalChat.getNewMessagesCount() - 1);
            newMessagesCount.setText(String.valueOf(personalChat.getNewMessagesCount()));
            if (personalChat.getNewMessagesCount() == 0) {
                newMessagesBlock.setVisible(false);
                newMessagesCount.setText("0");
            }
            personalChatRepository.save(personalChat);
        }
    }

    private void isLastMessage(MessageEntity messageEntity) {
        int indexMessage = chat.getMessages().indexOf(messageEntity);
        if (indexMessage == chat.getMessages().size() - 1) {
            try {
                putLastMessage(messageEntity);
            } catch (IndexOutOfBoundsException ex) {
                setTextIfMessagesIsEmpty();
            }
        }
    }
}