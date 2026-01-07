# Gradle Build Project Format & Design (GroovyFX)

This document explains the **structure, intent, and responsibilities** of the
current `build.gradle` configuration. It is written for maintainers who need to
understand *why the build looks the way it does*, not just what it does.

This build is **intentionally non-trivial** to support:
- JPMS
- Groovy + Java joint compilation
- JavaFX modules
- Groovy AST transformations

---

## 1. Plugins section

```groovy
plugins {
    id 'groovy'
    id 'java-library'
    id 'idea'
    id 'jacoco'
    id 'org.openjfx.javafxplugin'
    id 'maven-publish'
    id 'signing'
    id 'io.github.gradle-nexus.publish-plugin'
    id 'org.asciidoctor.jvm.convert'
}
```

### Purpose

- `groovy` – enables Groovy + joint compilation
- `java-library` – enables API vs implementation separation
- `org.openjfx.javafxplugin` – manages JavaFX modules per platform
- publishing/signing plugins – required for Maven Central
- asciidoctor – documentation generation

Removing any of these should be done with care.

---

## 2. Java toolchain & language level

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

### Why this exists

- Ensures consistent JPMS behavior across machines
- Prevents IDE vs CLI mismatches
- Required for module-path correctness

---

## 3. Repositories

```groovy
repositories {
    mavenCentral()
}
```

Simple by design. JPMS resolution relies on predictable artifacts.

---

## 4. Dependencies

### Groovy

```groovy
api "org.apache.groovy:groovy:${groovyVersion}"
```

- Declared as `api` so consumers see Groovy types
- Reflected in `module-info.java` as `requires org.apache.groovy`

### JavaFX

Managed via the JavaFX Gradle plugin:

```groovy
javafx {
    version = javafxVersion
    modules = [
        'javafx.base',
        'javafx.graphics',
        'javafx.controls',
        'javafx.fxml',
        'javafx.media',
        'javafx.web',
        'javafx.swing'
    ]
}
```

These must match `requires transitive javafx.*` in `module-info.java`.

---

## 5. Source sets

This project relies on **Gradle defaults** for Java and Groovy:

```groovy
src/main/java
src/main/groovy
src/main/resources
```

### Important

No custom source sets are used for JPMS.
Instead, `module-info.java` is isolated under:

```
src/module-info/java
```

This avoids premature JPMS compilation.

---

## 6. Groovy compilation

Groovy compilation is joint with Java and relies on Gradle defaults.

Key rule:
- Java code must not depend on Groovy types at compile time

AST annotations are Java for this reason.

---

## 7. JPMS: compileModuleInfo task

This task exists **because Gradle has no native JPMS lifecycle**.

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
```

### Responsibilities

- Compiles module-info last
- Uses module-path instead of classpath
- Validates JPMS correctness against compiled classes

---

## 8. Jar task integration

```groovy
tasks.named('jar').configure {
    dependsOn tasks.named('compileModuleInfo')
    from(tasks.named('compileModuleInfo')) {
        include 'module-info.class'
    }
}
```

Ensures:
- `module-info.class` is included exactly once
- No duplicate or early module-info compilation

---

## 9. Publishing & signing

Publishing configuration assumes:
- Modular JAR
- JPMS metadata correctness
- Reproducible builds

JPMS errors here usually mean `compileModuleInfo` is broken.

---

## 10. What NOT to change lightly

- Do not inline module-info.java into main sources
- Do not remove compileModuleInfo
- Do not switch back to classpath-based builds
- Do not convert AST annotations to Groovy

---

## 11. Mental model for maintainers

Think of the build as **three layers**:

1. Java + Groovy compilation
2. JPMS validation
3. Packaging & publishing

They are intentionally separated.

If something breaks, identify *which layer* failed first.

---

## 12. Final note

This build is correct **because it is explicit**.

Simplifying it without understanding JPMS will break it.
