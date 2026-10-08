#!/usr/bin/env bash
# Diff the theme maker's banding against the eaglermotd plugin's.
#
# The page previews what other clients will see, but only the plugin decides it.
# If the two drift apart the preview becomes a lie, and that is invisible from
# inside a browser. Both read the same gradients and print "<steps> <codes>", so
# a plain diff is the whole check.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
plugin_src="${MOTD_PLUGIN_SRC:-/home/lucas/lcc-server/velocity/plugin-src}"

command -v node >/dev/null || { echo "node is required" >&2; exit 1; }
[ -d "$plugin_src" ] || {
  echo "cannot find plugin source at $plugin_src; set MOTD_PLUGIN_SRC" >&2; exit 1
}

java_bin="${JAVA25:-/usr/lib/jvm/java-25-openjdk-arm64}/bin/java"

echo "building the plugin (also runs its own tests)"
bash "$plugin_src/build.sh" >/dev/null

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

node "$here/test-page.js" --bands "$here" > "$tmp/page.txt"
"$java_bin" -cp "$plugin_src/build/classes:$(cat "$plugin_src/build/classpath")" \
  net.lcc.eaglermotd.BandsCheck > "$tmp/plugin.txt"

if diff -u "$tmp/plugin.txt" "$tmp/page.txt"; then
  echo "OK: the page and the plugin agree on every gradient"
else
  echo >&2
  echo "MISMATCH: the preview no longer matches what the server sends." >&2
  echo "Left is the plugin (source of truth), right is the page." >&2
  exit 1
fi