package groovyx.javafx.factory

import javafx.scene.Node
import javafx.scene.control.ButtonBar

/**
 * ButtonBar holds content in `buttons` (ObservableList<Node>), not in `children`.
 */
class ButtonBarFactory extends AbstractFXBeanFactory {

    ButtonBarFactory() {
        super(ButtonBar)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof Node) {
            ((ButtonBar) parent).buttons.add((Node) child)
            return
        }
        Object.setChild(builder, parent, child)
    }
}
