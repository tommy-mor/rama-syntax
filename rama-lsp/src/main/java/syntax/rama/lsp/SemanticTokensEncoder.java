package syntax.rama.lsp;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.SemanticTokens;

/** Encodes semantic token spans into LSP delta-compressed integer arrays. */
public final class SemanticTokensEncoder {
  private SemanticTokensEncoder() {}

  public static SemanticTokens encode(List<RamaSemanticHighlighter.SemanticTokenSpan> spans) {
    var data = new ArrayList<Integer>();
    int prevLine = 0;
    int prevChar = 0;
    for (var span : spans) {
      int deltaLine = span.line() - prevLine;
      int deltaChar = deltaLine == 0 ? span.startChar() - prevChar : span.startChar();
      data.add(deltaLine);
      data.add(deltaChar);
      data.add(span.length());
      data.add(span.tokenType());
      data.add(span.tokenModifiers());
      prevLine = span.line();
      prevChar = span.startChar();
    }
    return new SemanticTokens(data);
  }
}
