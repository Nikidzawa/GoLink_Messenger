package ru.nikidzawa.golink.services.GUI.TrayIcon;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import ru.nikidzawa.golink.FXControllers.helpers.EmptyStage;
import ru.nikidzawa.networkAPI.network.TCPConnection;
import ru.nikidzawa.networkAPI.store.entities.UserEntity;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class GoLinkTrayIcon {
    private final TrayIcon trayIcon;
    private final SystemTray systemTray;
    private final Stage stage;
    private Stage trayIconStage;

    public GoLinkTrayIcon (Scene scene, UserEntity userEntity, TCPConnection tcpConnection) {
        systemTray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().createImage(Objects.requireNonNull(getClass().getResource("/img/logo.png")));
        trayIcon = new TrayIcon(image, "GoLink messenger");
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("GoLink messenger");
        stage = (Stage) scene.getWindow();
        Platform.setImplicitExit(false);
        stage.hide();
        show();
        setCommands(userEntity, tcpConnection);
    }

    private void setCommands(UserEntity userEntity, TCPConnection tcpConnection) {
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            @SneakyThrows
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON3) {
                    if (trayIconStage == null) {
                        Platform.runLater(() -> showTrayScene(userEntity, tcpConnection, event));
                    }
                } else if (event.getButton() == MouseEvent.BUTTON1) {
                    showStage();
                }
            }
        });
    }

    @SneakyThrows
    private void showTrayScene (UserEntity userEntity, TCPConnection tcpConnection, MouseEvent event) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/nikidzawa/goLink/goLinkTrayIcon.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);

        TrayIconController trayIconController = loader.getController();
        trayIconController.setGoLinkTrayIcon(this);
        trayIconController.setUserEntity(userEntity);
        trayIconController.setTcpConnection(tcpConnection);

        Stage stage = EmptyStage.getEmptyStage(scene);

        Point2D cursorLocation = new Point2D(event.getX(), event.getY());
        stage.setX(cursorLocation.getX() - 150);
        stage.setY(cursorLocation.getY() - 205);
        trayIconStage = stage;

        stage.show();
    }

    @SneakyThrows
    public void showStage() {
        systemTray.remove(trayIcon);
        if (trayIconStage != null) {
            Platform.runLater(trayIconStage::close);
        }
        Platform.runLater(stage::show);
    }

    @SneakyThrows
    public void show () {systemTray.add(trayIcon);}

    public void hide () {systemTray.remove(trayIcon);}
}