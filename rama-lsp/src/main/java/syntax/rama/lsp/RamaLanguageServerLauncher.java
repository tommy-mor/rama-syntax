package syntax.rama.lsp;

import java.nio.file.Path;
import org.eclipse.lsp4j.launch.LSPLauncher;
/** Stdio LSP entrypoint for editors (VS Code, Cursor, etc.). */
public final class RamaLanguageServerLauncher {
  public static void main(String[] args) throws Exception {
    var highlights =
        Path.of(
            System.getProperty(
                "rama.highlights.scm",
                Path.of(System.getProperty("user.dir"))
                    .resolve("../tree-sitter-rama/queries/highlights.scm")
                    .normalize()
                    .toString()));

    try (var highlighter = new RamaSemanticHighlighter(highlights)) {
      var server = new RamaLanguageServer(highlighter);
      var launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);
      launcher.startListening().get();
    }
  }
}
