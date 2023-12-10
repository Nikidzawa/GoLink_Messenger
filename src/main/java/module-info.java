module ru.nikidzawa.golink {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.media;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.swing;
    requires com.gluonhq.charm.glisten;
    requires GNAvatarView;
    requires spring.beans;
    requires spring.core;
    requires spring.data.jpa;
    requires spring.aop;
    requires org.postgresql.jdbc;
    requires spring.data.commons;
    requires spring.tx;
    requires spring.expression;
    requires spring.context;
    requires java.sql;
    requires spring.boot.starter;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires jakarta.persistence;
    requires jakarta.annotation;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires org.hibernate.orm.core;
    requires lombok;

    opens ru.nikidzawa.golink to javafx.fxml, spring.core, org.hibernate.orm.core;
    exports ru.nikidzawa.golink;
    exports ru.nikidzawa.golink.SystemOfControlServers;
    opens ru.nikidzawa.golink.SystemOfControlServers to javafx.fxml, org.hibernate.orm.core, spring.core;
}