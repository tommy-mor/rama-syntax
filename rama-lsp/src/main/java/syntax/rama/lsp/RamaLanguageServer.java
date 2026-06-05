package syntax.rama.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public final class RamaLanguageServer implements LanguageServer {
  private final RamaSemanticHighlighter highlighter;
  private final RamaTextDocumentService textDocuments;
  private volatile boolean shutdown;

  public RamaLanguageServer(RamaSemanticHighlighter highlighter) {
    this.highlighter = highlighter;
    this.textDocuments = new RamaTextDocumentService(highlighter);
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    var legend = new SemanticTokensLegend(RamaSemanticHighlighter.TOKEN_TYPES, List.of());

    var semanticTokens = new SemanticTokensWithRegistrationOptions(legend);
    semanticTokens.setFull(true);
    semanticTokens.setRange(false);

    var sync = new org.eclipse.lsp4j.TextDocumentSyncOptions();
    sync.setOpenClose(true);
    sync.setChange(TextDocumentSyncKind.Full);

    var capabilities = new ServerCapabilities();
    capabilities.setTextDocumentSync(sync);
    capabilities.setSemanticTokensProvider(semanticTokens);

    return CompletableFuture.completedFuture(new InitializeResult(capabilities));
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    shutdown = true;
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void exit() {
    if (!shutdown) {
      System.exit(0);
    }
    System.exit(0);
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return textDocuments;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return new RamaWorkspaceService();
  }
}
