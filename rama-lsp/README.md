# rama-lsp

Stdio LSP server for `.rama` files. Semantic highlighting is driven by the tree-sitter grammar in [`tree-sitter-rama`](../tree-sitter-rama) and the existing [`highlights.scm`](../tree-sitter-rama/queries/highlights.scm) query (same captures as the grammar’s highlight config).

## Prerequisites

Same as [`rama-jtreesitter`](../rama-jtreesitter/README.md): JDK 22+, `libtree-sitter`, and a built `tree-sitter-rama.dylib`.

```bash
cd ../tree-sitter-rama && ./BUILD.sh
```

## Build

```bash
cd ..
mvn -q -pl rama-lsp -am package
```

Fat JAR: `rama-lsp/target/rama-lsp-0.1.0-SNAPSHOT.jar`

## Run (stdio)

```bash
java --enable-native-access=ALL-UNNAMED \
  -Djava.library.path=/opt/homebrew/lib:/usr/local/lib \
  -Drama.highlights.scm="$(pwd)/../tree-sitter-rama/queries/highlights.scm" \
  -jar target/rama-lsp-0.1.0-SNAPSHOT.jar
```

Point your editor’s LSP client at that command with `languageId` / document selector `rama`.

## VS Code / Cursor

See [`vscode-rama`](../vscode-rama): launches this JAR and registers the `rama` language with semantic token scopes.

## Capabilities (current)

| LSP | Status |
|-----|--------|
| `textDocument/semanticTokens/full` | Yes |
| `textDocument/didOpen` / `didChange` (full sync) | Yes |
| completion, diagnostics, goto | Not yet |

Token legend: `function`, `variable`, `label`, `property`, `string`, `comment`, `type` — aligned with `highlights.scm` capture names.
