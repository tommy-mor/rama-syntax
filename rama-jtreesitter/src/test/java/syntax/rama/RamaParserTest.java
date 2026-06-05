package syntax.rama;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RamaParserTest {
  @Test
  void parsesRamaopHeader() {
    try (var rama = new RamaParser();
        var tree = rama.parse("ramaop send-emits>(*a, *b) { anchor <root>; }")) {
      var root = tree.getRootNode();
      assertFalse(root.isError());
      assertEquals("source_file", root.getType());
      assertTrue(root.toSexp().contains("ramaop_definition"));
      assertTrue(root.toSexp().contains("anchor_statement"));
    }
  }

  @Test
  void parsesFirstExample() throws Exception {
    var path = Path.of("../examples/first.rama").normalize().toAbsolutePath();
    try (var rama = new RamaParser(); var tree = rama.parseFile(path)) {
      var root = tree.getRootNode();
      assertEquals("source_file", root.getType());
      assertTrue(root.toSexp().contains("ramaop_definition"));
      assertFalse(root.hasError());
    }
  }
}
