package ru.nikidzawa.golink.services.sound;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.Objects;

public class SongPlayer {
    public static void notification() {
        Media media = new Media(Objects.requireNonNull(SongPlayer.class.getResource("/sounds/notification.wav")).toExternalForm());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.play();
    }
}
