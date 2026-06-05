# rama-jtreesitter

Loads the [`tree-sitter-rama`](../tree-sitter-rama) grammar through [official jtreesitter](https://github.com/tree-sitter/java-tree-sitter) (FFM, JDK 22+).

## Prerequisites

- JDK **22+** (jtreesitter **0.26** is built for JDK 23+; JDK 22 works with **0.25.0** if you change the POM)
- **libtree-sitter** (`brew install tree-sitter`)
- `tree-sitter` CLI (`npm install -g tree-sitter-cli`)
- Built grammar shared library (macOS example):

```bash
cd ../tree-sitter-rama
npm install
npx tree-sitter generate
cc -shared -fPIC -o tree-sitter-rama.dylib src/parser.c -std=c11
```

## Run

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 23 2>/dev/null || echo /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home)"

mvn -q test

# exec:java needs the same native path as tests; this works reliably:
mvn -q -DskipTests dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java --enable-native-access=ALL-UNNAMED -Djava.library.path=/opt/homebrew/lib \
  -cp "target/classes:$(cat target/cp.txt)" syntax.rama.ParseMain ../examples/first.rama
```

Override the dylib path:

```bash
mvn -q exec:java -Drama.grammar.dylib=/absolute/path/to/tree-sitter-rama.dylib
```

JVM needs native access for jtreesitter:

```bash
java --enable-native-access=ALL-UNNAMED -jar ...
```

(Maven Surefire and `exec:java` are configured with that flag.)
