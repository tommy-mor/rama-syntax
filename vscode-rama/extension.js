const path = require("path");
const vscode = require("vscode");
const fs = require("fs");
const {
  LanguageClient,
  TransportKind,
  Trace,
} = require("vscode-languageclient/node");

/** @type {LanguageClient | undefined} */
let client;

/**
 * @param {vscode.ExtensionContext} context
 */
function activate(context) {
  const config = vscode.workspace.getConfiguration("rama");
  const extensionRoot = context.extensionPath;

  const jar =
    config.get("languageServer.jar") ||
    path.join(extensionRoot, "../rama-lsp/target/rama-lsp-0.1.0-SNAPSHOT.jar");

  const highlightsScm =
    config.get("languageServer.highlightsScm") ||
    path.join(extensionRoot, "../tree-sitter-rama/queries/highlights.scm");

  const grammarDylib =
    config.get("languageServer.grammarDylib") ||
    path.join(extensionRoot, "../tree-sitter-rama/tree-sitter-rama.dylib");

  const javaPath = config.get("languageServer.javaPath") || "java";
  const traceSetting = config.get("trace.server") || "verbose";

  const log = vscode.window.createOutputChannel("Rama");
  log.appendLine(`extensionPath: ${extensionRoot}`);
  log.appendLine(`jar: ${jar} (exists: ${fs.existsSync(jar)})`);
  log.appendLine(`highlights.scm: ${highlightsScm} (exists: ${fs.existsSync(highlightsScm)})`);
  log.appendLine(`grammar dylib: ${grammarDylib} (exists: ${fs.existsSync(grammarDylib)})`);
  log.appendLine(`java: ${javaPath}`);
  if (!fs.existsSync(grammarDylib)) {
    vscode.window.showWarningMessage(
      `Rama grammar not built at ${grammarDylib}. Run: cd tree-sitter-rama && ./BUILD.sh`,
    );
  }
  if (!fs.existsSync(jar)) {
    vscode.window.showWarningMessage(
      `Rama LSP JAR not found at ${jar}. Build with: mvn -pl rama-lsp -am package`,
    );
  }
  log.show(true);

  const serverOptions = {
    command: javaPath,
    args: [
      "--enable-native-access=ALL-UNNAMED",
      `-Drama.highlights.scm=${highlightsScm}`,
      `-Drama.grammar.dylib=${grammarDylib}`,
      "-Djava.library.path=/opt/homebrew/lib:/usr/local/lib",
      "-jar",
      jar,
    ],
    transport: TransportKind.stdio,
  };

  const clientOptions = {
    documentSelector: [{ language: "rama" }],
    synchronize: {
      fileEvents: vscode.workspace.createFileSystemWatcher("**/*.rama"),
    },
  };

  client = new LanguageClient("rama", "Rama Language Server", serverOptions, clientOptions);
  client.setTrace(traceSetting === "off" ? Trace.Off : traceSetting === "messages" ? Trace.Messages : Trace.Verbose);
  context.subscriptions.push(
    client.start().then(undefined, (err) => {
      log.appendLine(`Language client failed to start: ${err}`);
      vscode.window.showErrorMessage(`Rama LSP failed to start: ${err}`);
    }),
  );
}

function deactivate() {
  if (!client) {
    return undefined;
  }
  return client.stop();
}

module.exports = { activate, deactivate };
