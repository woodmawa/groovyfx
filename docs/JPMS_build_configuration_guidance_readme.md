# JPMS Build Configuration Guidance (GroovyFX)

This document explains **why** and **how** this project was made fully JPMS-compliant.
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

1. compileJava
2. compileGroovy
3. compileModuleInfo
4. jar

This order is mandatory for JPMS correctness.

---

## compileModuleInfo task

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

## Do NOT regress

- Do not move module-info.java
- Do not remove compileModuleInfo
- Do not change compile order
