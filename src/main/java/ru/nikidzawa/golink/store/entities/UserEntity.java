package ru.nikidzawa.golink.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String nickname;

    @Column(unique = true)
    private Long phone;

    private String password;

    private boolean connected;

    @OneToOne
    private ImageEntity avatar;

    @ManyToMany(mappedBy = "participants", fetch = FetchType.EAGER)
    private List<ChatEntity> chats;
}