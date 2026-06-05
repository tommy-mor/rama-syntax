package syntax.rama.lsp;

import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Query;
import io.github.treesitter.jtreesitter.QueryCursor;
import io.github.treesitter.jtreesitter.Tree;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import syntax.rama.RamaLanguage;
import syntax.rama.RamaParser;

/** Runs tree-sitter highlight queries and maps captures to LSP semantic token spans. */
public final class RamaSemanticHighlighter implements AutoCloseable {
  public static final List<String> TOKEN_TYPES =
      List.of("function", "variable", "starVar", "label", "property", "string", "comment", "type");

  private static final Map<String, Integer> CAPTURE_TO_TYPE =
      Map.ofEntries(
          Map.entry("function", 0),
          Map.entry("variable", 1),
          Map.entry("starVar", 2),
          Map.entry("label", 3),
          Map.entry("property", 4),
          Map.entry("string", 5),
          Map.entry("comment", 6),
          Map.entry("type", 7));

  private final RamaParser parser;
  private final Query highlights;

  public RamaSemanticHighlighter(Path highlightsScm) throws IOException {
    parser = new RamaParser();
    var querySource = Files.readString(highlightsScm);
    highlights = new Query(RamaLanguage.get(), querySource);
  }

  public List<SemanticTokenSpan> tokensFor(String source) {
    try (Tree tree = parser.parse(source)) {
      var root = tree.getRootNode();
      var spans = new ArrayList<SemanticTokenSpan>();
      try (var cursor = new QueryCursor(highlights)) {
        cursor
            .findCaptures(root)
            .forEach(
                entry -> {
                  for (var capture : entry.getValue().captures()) {
                    var typeIndex = CAPTURE_TO_TYPE.get(capture.name());
                    if (typeIndex == null) {
                      continue;
                    }
                    var span = spanFor(source, capture.node(), typeIndex);
                    if (span != null) {
                      spans.add(span);
                    }
                  }
                });
      }
      spans.sort(
          Comparator.comparingInt(SemanticTokenSpan::line)
              .thenComparingInt(SemanticTokenSpan::startChar)
              .thenComparingInt(SemanticTokenSpan::length));
      return spans;
    }
  }

  private static SemanticTokenSpan spanFor(String source, Node node, int typeIndex) {
    var start = node.getStartPoint();
    var end = node.getEndPoint();
    int length = tokenLength(source, start.row(), start.column(), end.row(), end.column());
    if (length <= 0) {
      return null;
    }
    return new SemanticTokenSpan(start.row(), start.column(), length, typeIndex, 0);
  }

  /** LSP semantic tokens cannot span lines; clip to the start line. */
  static int tokenLength(String source, int startRow, int startCol, int endRow, int endCol) {
    if (startRow == endRow) {
      return Math.max(0, endCol - startCol);
    }
    int lineStart = lineStartOffset(source, startRow);
    int lineEnd = lineEndOffset(source, lineStart);
    return Math.max(0, lineEnd - lineStart - startCol);
  }

  private static int lineStartOffset(String source, int row) {
    int line = 0;
    for (int i = 0; i < source.length(); i++) {
      if (line == row) {
        return i;
      }
      if (source.charAt(i) == '\n') {
        line++;
      }
    }
    return source.length();
  }

  private static int lineEndOffset(String source, int lineStart) {
    for (int i = lineStart; i < source.length(); i++) {
      if (source.charAt(i) == '\n') {
        return i;
      }
    }
    return source.length();
  }

  @Override
  public void close() {
    highlights.close();
    parser.close();
  }

  public record SemanticTokenSpan(int line, int startChar, int length, int tokenType, int tokenModifiers) {}
}
