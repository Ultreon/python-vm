
#!/usr/bin/env bash
# ouro - wrapper to run the ouro.pyz shipped in ouro/wrapper/
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
PYZ="$HERE/ouro/wrapper/ouro.pyz"

if [ ! -f "$PYZ" ]; then
  echo "ouro.pyz not found at $PYZ"
  exit 1
fi

# Use same python that runs this script (if invoked via shebang), or fallback to 'python3'
PYTHON="${PYTHON:-$(command -v python3 || command -v python || true)}"
if [ -z "$PYTHON" ]; then
  echo "No python found in PATH"
  exit 2
fi

exec "$PYTHON" "$PYZ" "$@"
