# GroovyFX Quickstart

This quickstart shows how to create and run a **minimal GroovyFX application**
using modern JavaFX (OpenJFX 25), Groovy 5, and Java 21/25.

---

## Prerequisites

- **Java:** 21 or 25 (LTS recommended)
- **Groovy:** 5.x
- **JavaFX:** 25.x

You can use either Gradle or a standalone Groovy script.

---

## Option 1: Gradle project (recommended)

### 1. Create a new Gradle project

```bash
gradle init --type groovy-application
cd your-project
```

### 2. Add JavaFX and GroovyFX dependencies

Edit `build.gradle`:

```groovy
plugins {
    id 'groovy'
    id 'application'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation platform("org.apache.groovy:groovy-bom:5.0.3")
    implementation "org.apache.groovy:groovy"

    implementation "org.openjfx:javafx-controls:25"
    implementation "org.openjfx:javafx-graphics:25"

    implementation "org.groovyfx:groovyfx:1.0.0"
}

application {
    mainClass = 'demo.Main'
}
```

> Adjust JavaFX classifiers (`:win`, `:mac`, `:linux`) if required for your platform.

---

### 3. Create a GroovyFX application

`src/main/groovy/demo/Main.groovy`:

```groovy
package demo

import groovyx.javafx.SceneGraphBuilder

class Main {

    static void main(String[] args) {
        new SceneGraphBuilder().build {
            stage(
                title: 'GroovyFX Quickstart',
                width: 400,
                height: 300,
                visible: true
            ) {
                scene {
                    vbox(spacing: 10, padding: 20) {
                        label('Hello GroovyFX')
                        button('Click me', onAction: {
                            println 'Button clicked'
                        })
                    }
                }
            }
        }
    }
}
```

### 4. Run the application

```bash
./gradlew run
```

---

## Option 2: Standalone Groovy script

This is useful for quick experiments.

```groovy
@Grab('org.apache.groovy:groovy:5.0.3')
@Grab('org.openjfx:javafx-controls:25')
@Grab('org.groovyfx:groovyfx:1.0.0')

import groovyx.javafx.SceneGraphBuilder

new SceneGraphBuilder().build {
    stage(title: 'GroovyFX Script', visible: true) {
        scene {
            vbox(spacing: 10, padding: 20) {
                label('Hello from GroovyFX')
                button('Close', onAction: { System.exit(0) })
            }
        }
    }
}
```

Run with:

```bash
groovy demo.groovy
```

---

## Notes on the DSL

- Item-based controls use **attributes**, not nested nodes:

```groovy
listView(items: ['A', 'B', 'C'])
comboBox(items: ['X', 'Y'])
```

- Single-content controls accept exactly one Node:

```groovy
scrollPane {
    label('Scrollable content')
}
```

- Container semantics (tabs/items/panes) are handled automatically by GroovyFX.

---

## Next steps

- Read `docs/dsl-patterns.md` for supported DSL conventions
- Explore container layouts (BorderPane, GridPane, TabPane)
- Review examples in the test suite for real-world usage

---

Happy hacking with GroovyFX!
