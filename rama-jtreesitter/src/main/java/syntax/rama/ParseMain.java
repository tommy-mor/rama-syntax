package syntax.rama;

import java.nio.file.Path;

/** CLI: print S-expression for a .rama file. */
public final class ParseMain {
  public static void main(String[] args) throws Exception {
    var path = Path.of(args.length > 0 ? args[0] : "../examples/first.rama");
    try (var rama = new RamaParser(); var tree = rama.parseFile(path)) {
      var root = tree.getRootNode();
      System.out.println(path.toAbsolutePath());
      System.out.println("hasError=" + root.hasError());
      System.out.println(root.toSexp());
    }
  }
}
