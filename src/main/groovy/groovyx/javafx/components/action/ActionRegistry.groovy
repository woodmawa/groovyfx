package groovyx.javafx.components.action


/**
 * Non-UI model class.
 * Not a JavaFX Node and does not require a SceneGraphBuilder factory.
 */

class ActionRegistry {
    private Map<String, Action> actions = [:]
    void register(Action a) { actions[a.id] = a }
    Action get(String id) { actions[id] }
    void fire(String id) { actions[id]?.fire() }
}