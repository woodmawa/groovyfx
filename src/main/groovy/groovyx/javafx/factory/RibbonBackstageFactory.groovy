package groovyx.javafx.factory

import groovyx.javafx.components.RibbonBackstage
import javafx.scene.control.MenuItem

class RibbonBackstageFactory extends AbstractFXBeanFactory {
    RibbonBackstageFactory() { super(RibbonBackstage) }

    @Override
    void setChild(Object parent, Object child) {
        if (child instanceof MenuItem) {
            parent.items.add(child)
        }
    }
}
