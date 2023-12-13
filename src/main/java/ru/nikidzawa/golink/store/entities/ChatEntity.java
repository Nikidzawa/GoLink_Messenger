package ru.nikidzawa.golink.store.entities;

import jakarta.persistence.*;
import lombok.*;
import ru.nikidzawa.golink.store.enums.ChatType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private ChatType type;

    private int port;

    @ManyToMany (fetch = FetchType.EAGER)
    @JoinTable(
            name = "chat_participants",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<UserEntity> participants;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<MessageEntity> messages;

    public void setMessages (MessageEntity message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        message.setChat(this);
    }
}