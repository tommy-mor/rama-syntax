#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if ! command -v cc >/dev/null 2>&1; then
  echo "error: C compiler (cc) not found" >&2
  exit 1
fi

if [[ ! -d node_modules/tree-sitter-cli ]]; then
  npm install
fi

npx tree-sitter generate

case "$(uname -s)" in
  Darwin)
    OUT="$ROOT/tree-sitter-rama.dylib"
    ;;
  Linux)
    OUT="$ROOT/tree-sitter-rama.so"
    ;;
  *)
    echo "error: unsupported OS (expected Darwin or Linux)" >&2
    exit 1
    ;;
esac

cc -shared -fPIC -o "$OUT" "$ROOT/src/parser.c" -std=c11 -I"$ROOT/src"

echo "built $OUT"
