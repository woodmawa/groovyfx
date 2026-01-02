# GroovyFX DSL Patterns

This document describes the **supported and recommended DSL patterns**
for GroovyFX 1.0.0 and later, based on validation against JavaFX 25.

These rules are enforced and documented via contract tests.

---

## General principles

- GroovyFX follows **JavaFX-native semantics**
- DSL convenience never overrides JavaFX correctness
- Attributes are preferred over nested helper nodes where possible
- Container behavior is explicit and predictable

---

## Item-based controls

The following controls manage items via JavaFX collections:

- ListView
- TableView
- TreeView
- ChoiceBox
- ComboBox

### Recommended usage

Use the `items:` attribute:

```groovy
listView(items: ['A', 'B', 'C'])
comboBox(items: ['X', 'Y'])
```

### Not supported by default

```groovy
listView {
    items('A', 'B')   // ❌ not a DSL node
}
```

Rationale:
- `items` is not a visual node
- Attribute-based assignment is clearer and consistent

---

## Single-content controls

Some JavaFX controls accept **exactly one content Node**.

Examples:
- ScrollPane
- TitledPane
- Tab

### Usage

```groovy
scrollPane {
    label('Scrollable content')
}

titledPane(text: 'Title') {
    content {
        label('Body')
    }
}
```

Multiple content nodes are rejected.

---

## Container controls with special semantics

Some JavaFX controls do not use `children` internally.
GroovyFX provides dedicated factories for these controls.

| Control     | JavaFX collection |
|-------------|-------------------|
| TabPane     | tabs              |
| SplitPane  | items             |
| Accordion  | panes             |
| ToolBar    | items             |
| ButtonBar  | buttons           |
| ScrollPane | content            |

These mappings are enforced and covered by tests.

---

## TreeView and TreeItem

Tree structures are built using nested `treeItem` nodes.

```groovy
treeView {
    treeItem('Root') {
        treeItem('Child 1')
        treeItem('Child 2')
    }
}
```

The first `treeItem` becomes the root.

---

## TableView and TableColumn

Columns are defined via nested `tableColumn` nodes.

```groovy
tableView(items: data) {
    tableColumn('Group') {
        tableColumn('A')
        tableColumn('B')
    }
}
```

Nested columns form column groups.

---

## Event handlers

Event handlers are typically provided via closures:

```groovy
button('Click', onAction: {
    println 'Clicked'
})
```

Cell-related events use `cellFactory` and `onSelect` where applicable.

---

## Summary

GroovyFX DSL design favors:

- JavaFX correctness over magic
- Attribute-driven configuration
- Explicit container semantics
- Predictable behavior across JavaFX versions
