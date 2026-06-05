package syntax.rama;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RamaParser implements AutoCloseable {
  private final Parser parser;

  public RamaParser() {
    NativeLibraries.ensureTreeSitterCore();
    parser = new Parser();
    parser.setLanguage(RamaLanguage.get());
  }

  public Tree parse(String source) {
    return parser.parse(source).orElseThrow(() -> new IllegalStateException("parse returned empty"));
  }

  public Tree parseFile(Path path) throws IOException {
    return parse(Files.readString(path));
  }

  @Override
  public void close() {
    parser.close();
  }
}
