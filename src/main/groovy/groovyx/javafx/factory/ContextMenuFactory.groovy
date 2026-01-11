package groovyx.javafx.factory

import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem

class ContextMenuFactory extends AbstractFXBeanFactory {
    ContextMenuFactory() {
        super(ContextMenu)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof ContextMenu && child instanceof MenuItem) {
            ((ContextMenu) parent).items.add((MenuItem) child)
            return
        }
        super.setChild(builder, parent, child)
    }
}
