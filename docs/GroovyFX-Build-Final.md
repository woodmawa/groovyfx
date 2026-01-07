# GroovyFX Build (JPMS / CI) — Final Reference

This document is a **clean, copy‑paste safe reference** for the *current, stable*
GroovyFX build after JPMS + CI fixes.

It reflects the **final working state**:
- All tests pass (`clean test`)
- JPMS is correct (`module-info.class` included once)
- JavaFX works on the module‑path
- No repository warnings
- Only remaining Gradle warning is from Asciidoctor (plugin-side, safe)

---

## 1. Design goals

This build is intentionally explicit to support:

- Java Platform Module System (JPMS)
- Groovy + Java joint compilation
- JavaFX modules
- Groovy AST transformations
- Maven Central publishing

Do **not** simplify without understanding JPMS consequences.

---

## 2. settings.gradle (single source of repositories)

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
- Central is preferred for reproducibility
- Local is available for development
- Prevents Gradle repository warnings

No `repositories {}` block should exist in `build.gradle`.

---

## 3. build.gradle — plugins

```groovy
plugins {
    id 'groovy'
    id 'java-library'
    id 'idea'
    id 'jacoco'

    id 'org.openjfx.javafxplugin' version '0.1.0'

    id 'maven-publish'
    id 'signing'
    id 'io.github.gradle-nexus.publish-plugin' version '2.0.0'

    // Documentation (applied unconditionally; safe, but emits Gradle 10 warning)
    id 'org.asciidoctor.jvm.convert' version '4.0.5'
}
```

> Note: The only remaining Gradle 10 deprecation warning comes from Asciidoctor
> (via grolifant). This is plugin‑side and safe to ignore for now.

---

## 4. Java toolchain

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

Ensures:
- Consistent JPMS behaviour
- IDE and CI parity
- Correct module‑path resolution

---

## 5. JavaFX configuration

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

These **must** match `requires transitive javafx.*` in `module-info.java`.

---

## 6. Source layout (important)

```
src/main/java
src/main/groovy
src/main/resources

src/module-info/java/module-info.java
```

Why:
- Gradle cannot compile `module-info.java` correctly during joint compilation
- Module descriptor must be compiled *after* classes exist

---

## 7. JPMS: compileModuleInfo task

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

Responsibilities:
- Compiles `module-info.java` last
- Uses module‑path, not classpath
- Validates JPMS correctness

---

## 8. Jar integration

```groovy
tasks.named('jar').configure {
    dependsOn tasks.named('compileModuleInfo')
    from(tasks.named('compileModuleInfo')) {
        include 'module-info.class'
    }
}
```

Guarantees:
- Exactly one `module-info.class`
- Correct JPMS metadata in published JAR

---

## 9. GroovyFX factory rule (critical)

**Factories must never call `Object.newInstance(...)`.**

Correct pattern:
```groovy
return super.newInstance(builder, name, value, attributes)
```

All container and wrapper factories were fixed to:
- use `super.*`
- avoid `Object.*` meta hacks
- behave correctly under JPMS

Special case:
- `GraphicFactory` is a wrapper factory and must:
  - accept a `(Class)` constructor (builder compatibility)
  - capture a child `Node`
  - assign `parent.graphic` on completion

---

## 10. Test runtime safety (Gradle 9+)

Some Gradle versions do not create output dirs when no Java sources exist.
To prevent test startup failures:

```groovy
import org.gradle.api.tasks.compile.AbstractCompile

tasks.withType(AbstractCompile).configureEach {
    doFirst {
        destinationDirectory.getAsFile().get().mkdirs()
    }
}
```

This is defensive and harmless.

---

## 11. Known warnings (safe)

### Asciidoctor / Gradle 10 warning
- Origin: Asciidoctor → grolifant
- Status: plugin-side, unavoidable in 4.0.5
- Action: ignore until plugin updates

### JavaFX unnamed module warning (tests)
- Cosmetic unless strict module-path testing is required

---

## 12. Mental model for maintainers

Think in **three layers**:

1. Java + Groovy compilation
2. JPMS validation (`compileModuleInfo`)
3. Packaging & publishing

If something breaks, identify the layer first.

---

## 13. Final status

✔ JPMS clean  
✔ CI green  
✔ Tests passing  
✔ Publishing-safe  

This build is **intentionally explicit** — and correct because of it.
