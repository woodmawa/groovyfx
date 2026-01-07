package groovyx.javafx.factory

import javafx.scene.control.Accordion
import javafx.scene.control.TitledPane

class AccordionFactory extends AbstractFXBeanFactory {

    AccordionFactory() {
        super(Accordion)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof TitledPane) {
            parent.panes.add((TitledPane) child)
            return
        }
        Object.setChild(builder, parent, child)
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        // Apply expandedPane/expandedIndex here if you support it
        Object.onNodeCompleted(builder, parent, node)
    }
}
