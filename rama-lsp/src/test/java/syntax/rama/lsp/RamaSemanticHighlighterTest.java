package syntax.rama.lsp;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RamaSemanticHighlighterTest {
  @Test
  void highlightsFirstExample() throws Exception {
    var highlights =
        Path.of("../tree-sitter-rama/queries/highlights.scm").normalize().toAbsolutePath();
    var sourcePath = Path.of("../examples/first.rama").normalize().toAbsolutePath();
    try (var highlighter = new RamaSemanticHighlighter(highlights)) {
      var source = java.nio.file.Files.readString(sourcePath);
      var spans = highlighter.tokensFor(source);
      assertFalse(spans.isEmpty());
      assertTrue(spans.stream().anyMatch(s -> s.tokenType() == 2), () -> "expected * binding tokens");
    }
  }

  @Test
  void clipsMultiLineTokensToFirstLine() {
    var source = "abc\ndef";
    assertEquals(3, RamaSemanticHighlighter.tokenLength(source, 0, 0, 1, 0));
    assertEquals(2, RamaSemanticHighlighter.tokenLength(source, 0, 1, 0, 3));
  }
}
