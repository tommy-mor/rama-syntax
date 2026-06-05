package syntax.rama;

import io.github.treesitter.jtreesitter.Language;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads the tree-sitter-rama grammar from a native shared library. */
public final class RamaLanguage {
  private static final Language INSTANCE = load();

  private RamaLanguage() {}

  public static Language get() {
    return INSTANCE;
  }

  private static Language load() {
    NativeLibraries.ensureTreeSitterCore();
    var dylib = Path.of(System.getProperty("rama.grammar.dylib", defaultDylibPath()));
    if (!Files.isRegularFile(dylib)) {
      throw new IllegalStateException(
          "Missing grammar library %s — run: cd tree-sitter-rama && npx tree-sitter generate && cc -shared -fPIC -o tree-sitter-rama.dylib src/parser.c -std=c11"
              .formatted(dylib.toAbsolutePath()));
    }
    SymbolLookup symbols = SymbolLookup.libraryLookup(dylib.toAbsolutePath().toString(), Arena.global());
    return Language.load(symbols, "tree_sitter_rama");
  }

  private static String defaultDylibPath() {
    for (var start : candidateBases()) {
      var dir = start;
      for (int depth = 0; depth < 8 && dir != null; depth++) {
        var dylib = dir.resolve("tree-sitter-rama/tree-sitter-rama.dylib").normalize();
        if (Files.isRegularFile(dylib)) {
          return dylib.toString();
        }
        dir = dir.getParent();
      }
    }
    return Path.of(System.getProperty("user.dir"))
        .resolve("tree-sitter-rama/tree-sitter-rama.dylib")
        .normalize()
        .toString();
  }

  private static Iterable<Path> candidateBases() {
    var bases = new java.util.ArrayList<Path>();
    var fromJar = jarParentDirectory();
    if (fromJar != null) {
      bases.add(fromJar);
      bases.add(fromJar.getParent());
    }
    bases.add(Path.of(System.getProperty("user.dir")));
    return bases;
  }

  private static Path jarParentDirectory() {
    var location = RamaLanguage.class.getProtectionDomain().getCodeSource().getLocation();
    if (location == null) {
      return null;
    }
    try {
      var uri = location.toURI();
      var path = Path.of(uri);
      if (Files.isRegularFile(path)) {
        return path.getParent();
      }
      if (Files.isDirectory(path)) {
        return path;
      }
    } catch (Exception ignored) {
    }
    return null;
  }
}
