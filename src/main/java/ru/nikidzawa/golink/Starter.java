package ru.nikidzawa.golink;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.nikidzawa.golink.FXControllers.Register;

import java.util.Objects;

@SpringBootApplication
@EntityScan(basePackages = {"ru.nikidzawa.networkAPI.store.entities", "ru.nikidzawa.golink"})
@EnableJpaRepositories(basePackages = "ru.nikidzawa.networkAPI.store.repositories")
public class Starter extends Application {
    private Parent rootNode;
    private static ConfigurableApplicationContext context;

    @Override
    public void init() throws Exception {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(Starter.class);
        builder.headless(false);
        context = builder.run();
        FXMLLoader loader = new FXMLLoader(Starter.class.getResource("register.fxml"));
        loader.setControllerFactory(context::getBean);
        rootNode = loader.load();
        Register register = loader.getController();
        register.setContext(context);
    }

    @Override
    public void start(Stage stage) {
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/logo.png"))));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(rootNode, 1200, 1200));
        stage.setTitle("GoLink");
        stage.show();
    }

    public static void main(String[] args) {launch(args);}

    @Override
    public void stop() {
        context.close();
    }
}