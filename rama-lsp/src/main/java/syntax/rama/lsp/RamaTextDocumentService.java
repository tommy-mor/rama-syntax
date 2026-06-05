package syntax.rama.lsp;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.services.TextDocumentService;

public final class RamaTextDocumentService implements TextDocumentService {
  private final RamaSemanticHighlighter highlighter;
  private final Map<String, String> documents = new ConcurrentHashMap<>();

  public RamaTextDocumentService(RamaSemanticHighlighter highlighter) {
    this.highlighter = highlighter;
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    documents.put(uriKey(params.getTextDocument().getUri()), params.getTextDocument().getText());
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    var uri = uriKey(params.getTextDocument().getUri());
    // Full sync only for now.
    var change = params.getContentChanges().getLast();
    documents.put(uri, change.getText());
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    documents.remove(uriKey(params.getTextDocument().getUri()));
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {}

  @Override
  public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
    var source = documents.get(uriKey(params.getTextDocument().getUri()));
    if (source == null) {
      return CompletableFuture.completedFuture(new SemanticTokens(List.of()));
    }
    var spans = highlighter.tokensFor(source);
    return CompletableFuture.completedFuture(SemanticTokensEncoder.encode(spans));
  }

  private static String uriKey(String uri) {
    return URI.create(uri).normalize().toString();
  }
}
