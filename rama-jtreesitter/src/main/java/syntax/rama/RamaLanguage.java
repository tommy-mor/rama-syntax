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
    return Path.of(System.getProperty("user.dir"))
        .resolve("../tree-sitter-rama/tree-sitter-rama.dylib")
        .normalize()
        .toString();
  }
}
