package ru.nikidzawa.golink.FXControllers.cash;

import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

import ru.nikidzawa.networkAPI.store.MessageType;
import ru.nikidzawa.networkAPI.store.entities.ChatEntity;
import ru.nikidzawa.networkAPI.store.entities.MessageEntity;
import ru.nikidzawa.networkAPI.store.entities.PersonalChatEntity;
import ru.nikidzawa.networkAPI.store.entities.UserEntity;
import ru.nikidzawa.networkAPI.store.repositories.PersonalChatRepository;

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
    public List<MessageEntity> newMessages = new ArrayList<>();

    private BorderPane GUI;
    private Text date;
    private BorderPane nameAndLastMessage;
    private TextField lastMessageText;
    private StackPane newMessagesBlock;
    private Text newMessagesCount;

    //Data base
    private PersonalChatRepository personalChatRepository;
    private PersonalChatEntity personalChatEntity;
    private UserEntity interlocutor;
    private ChatEntity chat;
    private MessageEntity lastMessage;

    public ContactCash(UserEntity interlocutor, ChatEntity chat, PersonalChatEntity personalChatEntity, PersonalChatRepository personalChatRepository) {
        this.interlocutor = interlocutor;
        this.chat = chat;
        this.personalChatEntity = personalChatEntity;
        this.personalChatRepository = personalChatRepository;
    }

    private void setTextIfMessagesIsEmpty() {
        lastMessageText.setText("Чат пуст");
        date.setVisible(false);
    }

    public void resetNotificationCount() {
        newMessages.clear();
        personalChatEntity.setNewMessagesCount(0);
        personalChatRepository.save(personalChatEntity);
        newMessagesBlock.setVisible(false);
        newMessagesCount.setText("0");
    }

    public void addNotification(MessageEntity message) {
        newMessages.add(message);
        personalChatEntity.setNewMessagesCount(personalChatEntity.getNewMessagesCount() + 1);
        newMessagesBlock.setVisible(true);
        newMessagesCount.setText(String.valueOf(Integer.parseInt(newMessagesCount.getText()) + 1));
    }

    public void addMessageOnCashAndPutLastMessage(MessageCash messageCash) {
        cashedMessageInformation.put(messageCash.getMessage().getId(), messageCash);
        try {
            chat.getMessages().add(messageCash.getMessage());
        } catch (NullPointerException ex) {
            chat.setMessages(new ArrayList<>());
            chat.getMessages().add(messageCash.getMessage());
        }
        putLastMessage(messageCash.getMessage());
    }

    public void addMessageOnCash(MessageCash messageCash) {
        cashedMessageInformation.put(messageCash.getMessage().getId(), messageCash);
    }

    public void putLastMessage(MessageEntity message) {
        date.setVisible(true);
        switch (message.getMessageType()) {
            case MESSAGE -> {
                if (message.getText().isEmpty()) {
                    lastMessageText.setText(Objects.equals(message.getSender().getId(), interlocutor.getId()) ? "Фотография" : "Вы: " + "фотография");
                } else {
                    lastMessageText.setText(Objects.equals(message.getSender().getId(), interlocutor.getId()) ? message.getText() : "Вы: " + message.getText());
                }
            }
            case AUDIO ->
                    lastMessageText.setText(Objects.equals(message.getSender().getId(), interlocutor.getId()) ? "Голосовое сообщение" : "Вы: " + "голосовое сообщение");
            case DOCUMENT ->
                    lastMessageText.setText(Objects.equals(message.getSender().getId(), interlocutor.getId()) ? "Документ" : "Вы: " + "документ");
        }
        date.setText(message.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    public void editMessageAndFile(MessageCash messageCash, MessageEntity message) {
        messageCash.changeText(message.getText());
        messageCash.setImage(message.getMetadata());
        message.setHasBeenChanged(true);
        isLastMessage(message);
    }

    public void editMessage(Long messageId, String messageText, byte[] content, MessageType messageType) {
        if (cashedMessageInformation.containsKey(messageId)) {
            MessageCash messageCash = cashedMessageInformation.get(messageId);
            messageCash.setImage(content);
            messageCash.changeText(messageText);
            MessageEntity message = messageCash.getMessage();
            message.setHasBeenChanged(true);
            isLastMessage(message);
        } else {
            MessageEntity message = chat.getMessages().stream().filter(msg -> Objects.equals(msg.getId(), messageId)).findFirst().orElseThrow();
            message.setHasBeenChanged(true);
            message.setText(messageText);
            message.setMetadata(content);
            message.setMessageType(messageType);
            isLastMessage(message);
        }
    }

    public void deleteMessage(MessageEntity message) {
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

    public void deleteMessageDefault(Long messageId) {
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

    public HBox deleteMessage(Long messageId) {
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
        return messageCash.getMessageBackground();
    }

    private void isNewMessage(MessageEntity message) {
        if (newMessages.contains(message)) {
            newMessages.remove(message);
            personalChatEntity.setNewMessagesCount(personalChatEntity.getNewMessagesCount() - 1);
            newMessagesCount.setText(String.valueOf(personalChatEntity.getNewMessagesCount()));
            if (personalChatEntity.getNewMessagesCount() == 0) {
                newMessagesBlock.setVisible(false);
                newMessagesCount.setText("0");
            }
            personalChatRepository.save(personalChatEntity);
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