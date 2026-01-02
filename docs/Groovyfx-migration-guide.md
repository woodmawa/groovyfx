# GroovyFX Migration Guide

This document outlines **important changes and migration notes**
for users upgrading to the modern GroovyFX (JavaFX 25 / Groovy 5).

---

## Supported runtimes

### Before
- Java 8–11
- Groovy 2.x–3.x
- Older JavaFX versions

### Now
- Java **21 or 25**
- Groovy **5.x**
- OpenJFX **25.x**

---

## JavaFX initialization changes

### Previous behavior
- JavaFX initialized implicitly
- Use of Swing / JFXPanel in some cases
- Platform.exit() sometimes invoked during tests

### Current behavior
- JavaFX initialized explicitly via Platform.startup
- Startup is idempotent
- No Swing dependencies
- Platform.exit() is never called automatically

Impact:
- Tests are reliable and CI-friendly
- JavaFX lifecycle is predictable

---

## Container semantics fixes

Several controls previously accepted incorrect child structures.
These have been corrected to match JavaFX semantics.

| Control     | Correct behavior |
|-------------|------------------|
| TabPane     | children → tabs  |
| SplitPane  | children → items |
| Accordion  | children → panes |
| ToolBar    | children → items |
| ButtonBar  | children → buttons |
| ScrollPane | children → content |

Code relying on incorrect child routing must be updated.

---

## Items DSL changes

### Legacy (unsupported)
```groovy
listView {
    items('A', 'B')
}
```

### Current (supported)
```groovy
listView(items: ['A', 'B'])
```

Rationale:
- `items` is not a visual node
- Attribute-based assignment is consistent across controls

---

## Deprecated or removed behavior

- No reliance on internal JavaFX APIs
- No implicit wrapping of multiple content nodes
- No legacy Bintray / JCenter dependencies

---

## What did NOT change

- Core GroovyFX DSL style
- SceneGraphBuilder entry point
- Closure-based event handlers

---

## Migration strategy

1. Update Java, Groovy, and JavaFX versions
2. Update DSL usage to use attribute-based items
3. Review container usage for corrected semantics
4. Run test suite to validate behavior

---

## Getting help

If you encounter issues:
- Review contract tests for supported behavior
- Check DSL patterns documentation
- File an issue with a minimal reproducible example
