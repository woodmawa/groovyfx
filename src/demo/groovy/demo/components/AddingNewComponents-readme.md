# GroovyFX Demo Components – Extending via SPI

This document explains how to add **new UI components/widgets** to GroovyFX
*without modifying core code*, using the `SceneGraphAddon` SPI.

The `SimpleCarousel` demo component is the reference implementation.

---

## Overview

To add a new component you need four things:

1. **The component class** (JavaFX control or node)
2. **A factory** to integrate with the GroovyFX builder
3. **A `SceneGraphAddon`** to register the DSL node
4. **A ServiceLoader entry** so GroovyFX can discover it

All of these live in the **demo sourceset**.

---

## 1. Create the Component

Create your JavaFX component (usually extending `Control` or `Pane`).

Example location:

```
src/demo/groovy/demo/components/carousel/SimpleCarousel.groovy
```

Guidelines:
- Expose JavaFX properties (`IntegerProperty`, `BooleanProperty`, etc.)
- Provide standard getters/setters
- Keep layout/rendering logic self-contained

---

## 2. Create a Factory

Factories integrate your component into the GroovyFX DSL.

**Always extend `AbstractFXBeanFactory`.**
Do not implement `Factory` directly.

Example:

```
src/demo/groovy/demo/components/carousel/SimpleCarouselFactory.groovy
```

Minimal factory:

```groovy
class SimpleCarouselFactory extends AbstractFXBeanFactory {

    SimpleCarouselFactory() {
        super(SimpleCarousel, false) // not a leaf
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof SimpleCarousel && child instanceof Node) {
            parent.items.add(child)
        }
    }
}
```

Notes:
- `false` means the node accepts children
- Override `setChild` to collect child nodes
- Override `onNodeCompleted` if final setup is needed

---

## 3. Create a SceneGraphAddon

The addon registers the DSL node name.

Example:

```
src/demo/groovy/demo/components/carousel/SimpleCarouselAddon.groovy
```

```groovy
class SimpleCarouselAddon implements SceneGraphAddon {

    @Override
    void apply(SceneGraphBuilder builder) {
        builder.registerFactory(
            "simpleCarousel",
            new SimpleCarouselFactory()
        )
    }
}
```

This is where the **DSL name** is defined.

---

## 4. Register via ServiceLoader

Create the following file:

```
src/demo/resources/META-INF/services/groovyx.javafx.spi.SceneGraphAddon
```

Contents:

```
demo.components.carousel.SimpleCarouselAddon
```

Rules:
- No file extension
- One fully-qualified class name per line
- File name must exactly match the SPI interface

---

## 5. Use the Component

Once registered, the node is available like any built-in GroovyFX node.

Example:

```groovy
def c = simpleCarousel(wrap: true) {
    label("Slide 1")
    label("Slide 2")
}

label(bindText: c.indexProperty.add(1).asString("Slide %d of 2"))
```

---

## Optional: Attribute Binding (`bindText`)

This demo adds an explicit binding attribute:

```
label(bindText: observableValue)
```

Implemented in `LabeledFactory`, this avoids calling
`textProperty()` inside builder closures and keeps the DSL declarative.

---

## Design Principles

- Prefer explicit factories over meta-programming
- Keep demo extensions isolated from core
- Make bindings explicit in the DSL
- Optimize for IDE completion and readability

---

## Promoting a Demo Component to Core

If a demo component proves useful:

1. Move it to `src/main/.../components`
2. Add a factory under `groovyx.javafx.factory`
3. Register it in `SceneGraphBuilder.registerComponentWidgets()`
4. Add GDSL support

---

## Summary

This SPI-based model enables:

- Third-party extensions
- Clean separation of demos and core
- Stable, evolvable DSL design

Use `SimpleCarousel` as the canonical template.
