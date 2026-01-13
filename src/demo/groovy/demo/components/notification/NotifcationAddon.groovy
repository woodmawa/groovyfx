package demo.components.notification

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.spi.SceneGraphAddon

class NotificationAddon implements SceneGraphAddon {
    @Override
    void apply(SceneGraphBuilder builder) {
        // If your Notification is factory-backed, registerFactory here.
        // If it's simple JavaBean-ish, you can do:
        builder.registerComponentNode("notification", groovyx.javafx.components.Notification)
    }
}
