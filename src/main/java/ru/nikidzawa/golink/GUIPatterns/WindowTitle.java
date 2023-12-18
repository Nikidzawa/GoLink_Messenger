package ru.nikidzawa.golink.GUIPatterns;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.nikidzawa.golink.network.TCPConnection;
import ru.nikidzawa.golink.services.GoMessage.TCPBroker;
import ru.nikidzawa.golink.store.entities.UserEntity;
import ru.nikidzawa.golink.store.repositories.ChatRepository;
import ru.nikidzawa.golink.store.repositories.UserRepository;

import java.util.Objects;
import java.util.Set;

public class WindowTitle {
    private static double xOffset = 0;
    private static double yOffset = 0;
    public static void setBaseCommands(Pane titleBar, Button minimizeButton, Button scaleButton, Button closeButton ) {
        setButtons(titleBar, minimizeButton, scaleButton, closeButton);

        closeButton.setOnAction(actionEvent -> {
                Window window = closeButton.getScene().getWindow();
                Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

                Platform.runLater(() -> {
                    for (Thread t : threadSet) {
                        if (!t.getName().equals("JavaFX Application Thread")) {
                            t.interrupt();
                        }
                    }
                    try {
                        ((Stage) window).close();
                        Platform.exit();
                    } catch (Exception ex) {
                        System.out.println("Клиент закрыт");
                    }
                });
        });
    }

    public static void setBaseCommands(Pane titleBar, Button minimizeButton, Button scaleButton, Button closeButton, TCPBroker tcpBroker, UserEntity userEntity, UserRepository userRepository, ChatRepository chatRepository) {
        setButtons(titleBar, minimizeButton, scaleButton, closeButton);

        closeButton.setOnAction(actionEvent -> {
            Window window = closeButton.getScene().getWindow();
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            Platform.runLater(() -> {
                    userEntity.setConnected(false);
                    userRepository.saveAndFlush(userEntity);
                    chatRepository.findByParticipantsContaining(userEntity).forEach(chat1 -> {
                        UserEntity user = chat1.getParticipants().stream().filter(user1 -> !Objects.equals(user1.getId(), userEntity.getId())).findFirst().get();
                        tcpBroker.sendMessage("UPDATE_CHATS:" + user.getId());
                        tcpBroker.disconnect();
                    });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for (Thread t : threadSet) {
                    if (!t.getName().equals("JavaFX Application Thread")) {
                        t.interrupt();
                    }
                }
                ((Stage) window).close();
            });
        });
    }
    private static void setButtons(Pane titleBar, Button minimizeButton, Button scaleButton, Button closeButton) {
        minimizeButton.setOnMouseEntered(mouseEvent -> {
            minimizeButton.setStyle("-fx-background-color: GRAY; -fx-text-fill: white");
        });
        minimizeButton.setOnMouseExited(mouseEvent -> {
            minimizeButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0");
        });
        scaleButton.setOnMouseEntered(mouseEvent -> {
            scaleButton.setStyle("-fx-background-color: GRAY; -fx-text-fill: white");
        });
        scaleButton.setOnMouseExited(mouseEvent -> {
            scaleButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0");
        });
        closeButton.setOnMouseEntered(mouseEvent -> {
            closeButton.setStyle("-fx-background-color: red; -fx-text-fill: white");
        });
        closeButton.setOnMouseExited(mouseEvent -> {
            closeButton.setStyle("-fx-background-color: #18314D; -fx-text-fill: #C0C0C0");
        });
        minimizeButton.setOnAction(actionEvent -> {
            Window window = minimizeButton.getScene().getWindow();
            ((Stage) window).setIconified(true);
        });

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            Window window = titleBar.getScene().getWindow();
            (window).setX(event.getScreenX() - xOffset);
            (window).setY(event.getScreenY() - yOffset);
        });
    }
}
