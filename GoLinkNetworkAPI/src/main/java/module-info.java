module GoLinkNetworkAPI {
    requires spring.context;
    requires spring.data.jpa;
    requires spring.data.commons;
    requires jakarta.persistence;
    requires lombok;
    requires spring.boot.autoconfigure;
    requires spring.boot;
    requires spring.core;
    requires org.hibernate.orm.core;
    requires spring.beans;
    requires jakarta.annotation;

    opens ru.nikidzawa.networkAPI.network to spring.core, spring.beans, org.hibernate.orm.core, spring.context;
    opens ru.nikidzawa.networkAPI.store.entities to spring.core, spring.beans, org.hibernate.orm.core, spring.context;
    opens ru.nikidzawa.networkAPI to spring.core, spring.beans, org.hibernate.orm.core, spring.context;
    exports ru.nikidzawa.networkAPI to ru.nikidzawa.golink;
    exports ru.nikidzawa.networkAPI.store to ru.nikidzawa.golink, spring.core, spring.beans, org.hibernate.orm.core, spring.context;
    exports ru.nikidzawa.networkAPI.store.entities to ru.nikidzawa.golink, spring.core, spring.beans, org.hibernate.orm.core, spring.context;
    exports ru.nikidzawa.networkAPI.store.repositories to ru.nikidzawa.golink, spring.core, spring.beans, org.hibernate.orm.core, spring.context;
    exports ru.nikidzawa.networkAPI.network;
    exports ru.nikidzawa.networkAPI.network.helpers;
    opens ru.nikidzawa.networkAPI.network.helpers to org.hibernate.orm.core, spring.beans, spring.context, spring.core;
}