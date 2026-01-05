/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.javafx.factory

import groovyx.javafx.components.Card
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox

/**
 * Factory for the modern Card component.
 */
class CardSection {
    String name
    Closure content
}

class CardFactory extends AbstractNodeFactory {
    CardFactory() {
        super(Card)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof Card) {
            // Standard Node children are added to body by default
            if (child instanceof Node) {
                 if (child === parent.getHeaderContainer() || 
                     child === parent.getBodyContainer() || 
                     child === parent.getFooterContainer()) {
                     return
                 }
                 parent.getBodyContainer().getChildren().add((Node)child)
            }
        } else if (parent instanceof VBox) {
            // Check if it's one of our containers
            if (child instanceof Node) {
                parent.getChildren().add((Node)child)
            }
        } else {
             super.setChild(builder, parent, child)
        }
    }
}

class CardSectionFactory extends AbstractNodeFactory {
    String sectionName

    CardSectionFactory(String sectionName) {
        super(VBox)
        this.sectionName = sectionName
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        Card card = (Card) builder.context.get(FactoryBuilderSupport.CURRENT_NODE)
        if (card == null) {
            card = (Card) builder.getParentNode()
        }
        if (card == null) {
            Object parent = builder.getCurrent()
            if (parent instanceof Card) card = (Card) parent
        }
        if (card == null) {
             throw new RuntimeException("Card section '${name}' must be nested within a card.")
        }
        VBox container = sectionName == 'cardHeader' ? card.getHeaderContainer() :
                         sectionName == 'cardFooter' ? card.getFooterContainer() :
                         card.getBodyContainer()
        return container
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof VBox && child instanceof Node) {
            parent.getChildren().add((Node)child)
        } else {
            super.setChild(builder, parent, child)
        }
    }
}
