package ru.nikidzawa.golink;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.nikidzawa.golink.FXControllers.Register;

@SpringBootApplication
@EntityScan(basePackages = "ru.nikidzawa.golink")
public class Starter extends Application {
    private Parent rootNode;
    private ConfigurableApplicationContext context;

    @Override
    public void init() throws Exception {
        context = SpringApplication.run(Starter.class);
        FXMLLoader loader = new FXMLLoader(Starter.class.getResource("register.fxml"));
        loader.setControllerFactory(context::getBean);
        rootNode = loader.load();
        Register register = loader.getController();
        register.setContext(context);
    }
    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(rootNode, 1200, 1200));
        stage.setTitle("Регистрация");
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void stop() {
        context.close();
    }
}