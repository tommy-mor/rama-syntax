package syntax.rama;

import java.nio.file.Files;
import java.nio.file.Path;

/** Must run before any {@link io.github.treesitter.jtreesitter.Parser} use. */
final class NativeLibraries {
  private static final Object LOCK = new Object();
  private static boolean loaded;

  private NativeLibraries() {}

  static void ensureTreeSitterCore() {
    if (loaded) {
      return;
    }
    synchronized (LOCK) {
      if (loaded) {
        return;
      }
      loadTreeSitterCore();
      loaded = true;
    }
  }

  private static void loadTreeSitterCore() {
    var override = System.getProperty("tree-sitter.library");
    if (override != null && !override.isBlank()) {
      System.load(Path.of(override).toAbsolutePath().toString());
      return;
    }
    try {
      System.loadLibrary("tree-sitter");
    } catch (UnsatisfiedLinkError ignored) {
      for (var candidate :
          new String[] {
            "/opt/homebrew/lib/libtree-sitter.dylib",
            "/usr/local/lib/libtree-sitter.dylib",
          }) {
        if (Files.isRegularFile(Path.of(candidate))) {
          System.load(candidate);
          return;
        }
      }
      throw new IllegalStateException(
          "libtree-sitter not found — install with: brew install tree-sitter, or set -Dtree-sitter.library=/path/to/libtree-sitter.dylib");
    }
  }
}
