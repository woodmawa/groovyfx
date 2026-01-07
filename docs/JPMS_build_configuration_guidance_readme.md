# JPMS Build Configuration Guidance (GroovyFX) 

This document explains **why** and **how** this project is fully JPMS-compliant, and
records a few **post-JPMS hardening fixes** discovered during CI stabilization.

It exists to prevent future maintainers from accidentally breaking a delicate
Groovy + JavaFX + AST-transform + JPMS setup.

---

## Goals of this migration

- Produce a true JPMS modular JAR
- Work on the module-path (not the classpath)
- Support Groovy + Java joint compilation
- Support Groovy AST transformations
- Support JavaFX modules
- Pass `jar --describe-module` cleanly
- Keep CI and tests reproducible across machines

---

## Repositories (now centralized)

Dependency repositories are now defined in **settings.gradle** (preferred),
and `build.gradle` should not declare `repositories { ... }` for dependency resolution.

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        mavenLocal()
    }
}
```

Why:
- avoids Gradle warnings about project repositories
- improves reproducibility
- keeps JPMS/module-path dependency graphs consistent

---

## Source layout (CRITICAL)

```
src/
├─ main/
│  ├─ java/
│  ├─ groovy/
│  └─ resources/
│
├─ module-info/
│  └─ java/
│     └─ module-info.java
```

`module-info.java` MUST NOT live under `src/main/java`.

---

## Compilation order

1. compileJava / compileGroovy (joint compilation)
2. compileModuleInfo
3. jar

This order is mandatory for JPMS correctness.

---

## compileModuleInfo task (required)

```groovy
tasks.register('compileModuleInfo', JavaCompile) {
    dependsOn tasks.named('classes')

    source = fileTree('src/module-info/java') {
        include 'module-info.java'
    }

    destinationDirectory =
        layout.buildDirectory.dir('classes/java/module-info')

    classpath = files()

    options.compilerArgs += [
        '--module-path', sourceSets.main.compileClasspath.asPath,
        '--patch-module',
        "org.groovyfx=${sourceSets.main.output.classesDirs.asPath}"
    ]
}

tasks.named('jar').configure {
    dependsOn tasks.named('compileModuleInfo')
    from(tasks.named('compileModuleInfo')) {
        include 'module-info.class'
    }
}
```

Responsibilities:
- compiles module descriptor last
- validates JPMS metadata against compiled output
- ensures `module-info.class` is included exactly once

---

## AST transforms under JPMS

AST annotations must be Java.

JPMS requires a `provides` directive:

```java
provides org.codehaus.groovy.transform.ASTTransformation
    with groovyx.javafx.beans.FXBindableASTTransformation;
```

---

## Final module-info.java

```java
module org.groovyfx {
    requires java.base;
    requires org.apache.groovy;

    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.media;
    requires transitive javafx.web;
    requires transitive javafx.swing;

    exports groovyx.javafx;
    exports groovyx.javafx.beans;

    opens groovyx.javafx
        to javafx.fxml, javafx.graphics, org.apache.groovy;

    opens groovyx.javafx.beans
        to org.apache.groovy;

    provides org.codehaus.groovy.transform.ASTTransformation
        with groovyx.javafx.beans.FXBindableASTTransformation;
}
```

---

## Verification

```
./gradlew clean jar
jar --describe-module --file build/libs/groovyfx-ng-*.jar
```

---

## Post-JPMS hardening notes (CI fixes)

These are not “JPMS rules”, but were uncovered during JPMS + CI cleanup and are
worth keeping documented because they caused large regressions when removed.

### 1) Factories must not call Object.* helpers

Do **not** call:
- `Object.newInstance(builder, name, value, attrs)`
- `Object.setChild(...)`
- `Object.onNodeCompleted(...)`

Under JPMS (and modern Groovy/JavaFX), this leads to spurious attempts to
construct `java.lang.Object(builder, ...)` and cascades into missing DSL nodes.

Use `super.*` or explicit instantiation as appropriate.

### 2) Graphic wrapper node semantics

The `graphic { ... }` DSL node is a **wrapper**, not a JavaFX type.
It must:
- accept a `(Class)` constructor for builder compatibility
- capture exactly one child `Node`
- assign `parent.graphic = capturedNode` on completion

### 3) Test resource fixtures

Some stylesheet tests expect classpath resources:
- `app.css`
- `theme.css`

These are **test fixtures** and should live in:

```
src/test/resources/app.css
src/test/resources/theme.css
```

Minimal valid contents are enough (e.g. `.root { }`).

### 4) Gradle 9+ test startup safety

Some configurations don’t create `build/classes/java/test` when there are no Java
test sources. A defensive mkdir prevents intermittent test worker failures:

```groovy
import org.gradle.api.tasks.compile.AbstractCompile

tasks.withType(AbstractCompile).configureEach {
    doFirst {
        destinationDirectory.getAsFile().get().mkdirs()
    }
}
```

---

## Do NOT regress

- Do not move `module-info.java`
- Do not remove `compileModuleInfo`
- Do not change the compile order
- Do not convert AST annotations to Groovy
- Do not reintroduce `Object.*` factory helper calls
- Do not re-add dependency repositories to `build.gradle` (keep them in settings)

