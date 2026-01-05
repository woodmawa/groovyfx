# GroovyFX

GroovyFX is a Groovy-based DSL for building JavaFX user interfaces.

This version of GroovyFX has been fully modernized for JavaFX 25, Groovy 5, and
modern Java runtimes (Java 21 / 25). It focuses on correct JavaFX semantics,
a clean DSL, and stable test execution.

---

## Documentation
Comprehensive documentation is available in the `src/docs/asciidoc` directory and can be generated as HTML/PDF using the `./gradlew asciidoctor` task.

### Key Guides:
- [Getting Started](src/docs/asciidoc/getting_started.adoc)
- [Modern Components](src/docs/asciidoc/modern_components.adoc)
- [Advanced Features](src/docs/asciidoc/advanced_features.adoc)
- [Component Authoring (SPI)](src/docs/asciidoc/component_authoring_guide.adoc)
- [Overall Architecture](src/docs/asciidoc/overall_architecture_approach.adoc)

## Status

**Current status:** Stable Baseline (v2.0.0)  
**Latest tested stack:**
- Java 23 and 25 (LTS)
- Groovy 5.0.3
- OpenJFX 25.0.1
- Gradle 9.1

GroovyFX has passed a full semantic and DSL audit against modern JavaFX and is
now at a stable 2.0.0 release.

---

## Goals

GroovyFX aims to:

- Provide a concise, expressive DSL for JavaFX UI construction
- Preserve correct JavaFX semantics (no illegal access, no deprecated internals)
- Work cleanly on modern JDKs (module-safe, no Swing/JFXPanel hacks)
- Be testable and CI-friendly (stable JavaFX lifecycle handling)

---

## Quick example

```groovy
import groovyx.javafx.SceneGraphBuilder

new SceneGraphBuilder().build {
    stage(title: 'GroovyFX', width: 400, height: 300, visible: true) {
        scene {
            vbox(spacing: 10, padding: 20) {
                label('Hello GroovyFX')
                button('Click me', onAction: { println 'Clicked!' })
            }
        }
    }
}
```

---

## Supported DSL patterns

### Item-based controls use attributes

Controls such as ListView, TableView, TreeView, ChoiceBox, and ComboBox
expect items via attributes, not nested nodes.

```groovy
listView(items: ['A', 'B', 'C'])
comboBox(items: ['X', 'Y'])
```

There is no `items {}` DSL node by default.

---

### Single-content controls accept one Node

Controls such as ScrollPane and TitledPane accept exactly one content node.

```groovy
scrollPane {
    label('Scrollable content')
}
```

Multiple children are rejected.

---

### Container semantics are explicit

Some JavaFX controls do not use `children` internally.
GroovyFX provides dedicated factories for these:

- TabPane → tabs
- SplitPane → items
- Accordion → panes
- ToolBar → items
- ButtonBar → buttons
- ScrollPane → content

These semantics are enforced and covered by contract tests.

---

## JavaFX lifecycle & testing

GroovyFX manages JavaFX startup explicitly and safely:

- JavaFX is initialized via Platform.startup
- Startup is idempotent
- Tests do not call Platform.exit
- No Swing or JFXPanel is used

This makes GroovyFX tests reliable in CI and on modern JDKs.

---

## Requirements

- Java 23 or 25 (JDK 23+ required for JavaFX 25)
- Groovy 5.0.3+
- JavaFX 25.x

JavaFX must be available either via build dependencies
or a locally installed OpenJFX distribution.

---

## Build & test

```bash
./gradlew clean test
```

---

## Roadmap

### Completed
- JavaFX 25 migration
- Removal of legacy internals and illegal access
- Container semantic fixes
- Modern Component Pack (Card, Badge, ToggleSwitch, Icon, etc.)
- Reactive State Management (Store API)
- JavaFX 25 Subscription API integration
- Responsive Layout support (ResponsivePane, FormLayout)
- Enhanced SPI for external component authoring
- Full Documentation refresh (Asciidoc)

### Planned
- Continuous CI matrix expansion
- Community component library integrations
- Performance benchmarking vs. web frameworks

---

## License

GroovyFX is licensed under the Apache License, Version 2.0.
