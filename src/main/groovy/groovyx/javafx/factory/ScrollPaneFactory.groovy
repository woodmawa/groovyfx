package groovyx.javafx.factory

import javafx.scene.Node
import javafx.scene.control.ScrollPane

/**
 * ScrollPane is a Control, but it holds a single Node in `content`.
 */
class ScrollPaneFactory extends AbstractFXBeanFactory {

    ScrollPaneFactory() {
        super(ScrollPane)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof Node) {
            ScrollPane sp = (ScrollPane) parent
            if (sp.content != null) {
                throw new IllegalStateException(
                        "ScrollPane content already set (${sp.content.class.name}); " +
                                "ScrollPane supports only one content Node."
                )
            }
            sp.content = (Node) child
            return
        }
        Object.setChild(builder, parent, child)
    }
}
