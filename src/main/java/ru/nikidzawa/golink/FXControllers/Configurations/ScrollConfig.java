package ru.nikidzawa.golink.FXControllers.Configurations;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ru.nikidzawa.golink.FXControllers.GoLink;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class ScrollConfig {
    private static final long SCROLLBAR_HIDE_DELAY = 2500;
    private final ScrollPane scrollPane;
    private final VBox chatField;
    private Timer timer;
    private final ScrollBar vBar;
    private final Node thumb;
    private boolean wait;

    private final int spacing;

    public ScrollConfig(ScrollPane scrollPane, VBox chatField) {
        this.chatField = chatField;
        this.scrollPane = scrollPane;
        scrollPane.getStylesheets().add(Objects.requireNonNull(GoLink.class.getResource("/styles/scroll.css")).toExternalForm());
        vBar = (ScrollBar) scrollPane.lookup(".scroll-bar:vertical");
        thumb = scrollPane.lookup(".scroll-bar:vertical .thumb");
        spacing = (int) chatField.getSpacing();
        setScrollEventListener();
        initializeTimer();
        setConfig();
    }

    private void setConfig() {
        scrollPane.addEventFilter(ScrollEvent.SCROLL, _ -> {
            showVBar();
            startTimer();
        });
        vBar.setOnDragDetected(_ -> showVBar());
        vBar.setOnMouseEntered(_ -> {
            wait = true;
            showVBar();
        });
        vBar.setOnMouseExited(_ -> {
            wait = false;
            startTimer();
        });
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
            initializeTimer();
        }

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!wait) {
                    hideScrollPane();
                }
            }
        }, SCROLLBAR_HIDE_DELAY);
    }

    private void initializeTimer() {
        timer = new Timer(true);
    }

    public void startScrollEvent() {
        showVBar();
        startTimer();
    }

    public void showElement(int elementIndex) {
        double totalHeight = 0;

        for (int i = 0; i < elementIndex; i++) {
            Node child = chatField.getChildren().get(i);
            totalHeight += child.getBoundsInLocal().getHeight() + spacing;
        }

        double visibleHeight = scrollPane.getViewportBounds().getHeight();
        double vboxHeight = chatField.getBoundsInLocal().getHeight();

        double vValue = Math.min(1.0, totalHeight / (vboxHeight - visibleHeight));
        scrollPane.setVvalue(vValue);
    }

    private void showVBar() {
        thumb.setStyle("-fx-background-color: #002b54;");
    }

    private void hideScrollPane() {
        thumb.setStyle("-fx-background-color: transparent;");
    }

    private void setScrollEventListener() {
        chatField.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    if (scrollPane.getVvalue() >= 0.95) {
                        Platform.runLater(() -> {
                            PauseTransition pauseTransition = new PauseTransition(Duration.millis(20));
                            pauseTransition.setOnFinished(_ -> scrollPane.setVvalue(1));
                            pauseTransition.play();
                        });
                    }
                }
            }
        });
    }
}