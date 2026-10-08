// Cross-check for the theme maker: print the colour sequence the page would
// send for each step count, in the format BandsCheck.java prints, so
// check-output.sh can diff the two with `diff`.
//
// The gradients must stay in step with GRADIENTS in the plugin's test/BandsCheck
//.java - they are the same MOTD expressed twice, deliberately.
"use strict";

const fs = require("fs");
const path = require("path");

const GRADIENTS = [
  // The lcc-server / lol its a server gradient from listeners.toml.
  ["l", "#4d70ff"], ["c", "#4d77f4"], ["c", "#4d7ee9"], ["-", "#4d85de"],
  ["s", "#4d8cd3"], ["e", "#4c92c9"], ["r", "#4c99be"], ["v", "#4ca0b3"],
  ["e", "#4ca7a8"], ["r", "#4cae9d"], ["\n", null], ["l", "#4dff91"],
  ["o", "#4df093"], ["l", "#4de196"], [" ", null], ["i", "#4dd298"],
  ["t", "#4dc39b"], ["s", "#4db49d"], [" ", null], ["a", "#4ea6a0"],
  [" ", null], ["s", "#4e97a2"], ["e", "#4e88a4"], ["r", "#4e79a7"],
  ["v", "#4e6aa9"], ["e", "#4e5bac"], ["r", "#4e4cae"],
  // A wide-spectrum gradient on its own line, which stresses the band
  // boundaries far more than the narrow brand colours above do.
  ["\n", null],
  ["a", "#ff0000"], ["b", "#ff3c00"], ["c", "#ff7800"], ["d", "#ffb400"],
  ["e", "#fff000"], ["f", "#e0ff00"], ["g", "#aaff00"], ["h", "#74ff00"],
  ["i", "#3cff00"], ["j", "#00ff00"], ["k", "#00ff3c"], ["l", "#00ff78"],
  ["m", "#00ffb4"], ["n", "#00ffe0"], ["o", "#00ffff"], ["p", "#00e0ff"]
];

// ---------------------------------------------------------------- page harness
function mkEl(id) {
  const e = {
    id, value: "", checked: false, textContent: "", innerHTML: "", hidden: false,
    style: new Proxy({}, { set: (t, k, v) => ((t[k] = v), true) }),
    files: null, appendChild() {}, addEventListener() {}, removeEventListener() {},
    focus() {}, setAttribute() {},
    classList: { add() {}, remove() {}, toggle() {}, contains: () => false },
    getContext: () => null
  };
  return e;
}

function loadPage(htmlPath) {
  const src = fs.readFileSync(htmlPath, "utf8").match(/<script>([\s\S]*?)<\/script>/)[1];
  const els = {};
  return new Function("document", "window", "FileReader", "navigator", src + `
    ; return {setSegs, refresh, $, buildJson, buildLegacy, buildMini,
              buildListenerToml, buildPluginSnippet, fallbackText, fallbackSegs,
              nearest, splitLines, parseLegacy, segs: () => segs, plain};`)(
    { getElementById: (id) => (els[id] = els[id] || mkEl(id)),
      createElement: () => mkEl("tmp"), querySelectorAll: () => [],
      addEventListener: () => {}, execCommand: () => false },
    { getSelection: () => ({ rangeCount: 0 }), addEventListener: () => {} },
    function () {}, { clipboard: null });
}

// --------------------------------------------------------------------- checks
const bandsMode = process.argv.includes("--bands");
const pagePath = process.argv[2] && process.argv[2].endsWith(".html")
  ? process.argv[2]
  : path.join(process.argv[3] || ".", "MOTD theme maker.html");

const api = loadPage(pagePath);
const $ = api.$;

["accent", "accentDark", "bg"].forEach((id) => ($(id).value = "#000000"));
$("accent").value = "#4caf50";
$("accentDark").value = "#2e7d32";
$("bg").value = "#101014";
$("angle").value = "265";

api.setSegs(GRADIENTS.map(([t, c]) => ({ t, c: c || null, b: false, i: false, u: false, s: false })));

if (bandsMode) {
  // One line per step count, matching BandsCheck.java exactly. Step 0 stands
  // for PER_CHARACTER, which has no step count of its own.
  const codes = (s) => (s.match(/§([0-9a-f])/g) || []).map((m) => m[1]).join("");
  $("fbMode").value = "stepped";
  for (let steps = 1; steps <= 16; steps++) {
    $("steps").value = String(steps);
    api.refresh();
    process.stdout.write(`${String(steps).padStart(2)} ${codes(api.buildLegacy())}\n`);
  }
  $("fbMode").value = "per-character";
  api.refresh();
  process.stdout.write(`${String(0).padStart(2)} ${codes(api.buildLegacy())}\n`);
  process.exit(0);
}

let failures = 0;
function eq(what, a, b) {
  const ok = JSON.stringify(a) === JSON.stringify(b);
  console.log(`${ok ? "PASS" : "FAIL"} ${what}`);
  if (!ok) {
    console.log(`      expected: ${JSON.stringify(b)}`);
    console.log(`      actual:   ${JSON.stringify(a)}`);
    failures++;
  }
}
function ok(what, cond, extra) {
  console.log(`${cond ? "PASS" : "FAIL"} ${what}`);
  if (!cond && extra) console.log(`      ${extra}`);
  if (!cond) failures++;
}

const EXPECTED_HEX = GRADIENTS.filter(([, c]) => c).length;
// Derived from the fixture rather than typed out, so adding a gradient cannot
// leave a stale expectation behind.
const EXPECTED_TEXT = GRADIENTS.map(([t]) => t).join("");

console.log("=== the gradient reaches the themed client ===");
const themed = api.buildJson();
const hexes = themed.match(/"color": "#[0-9a-f]{6}"/g) || [];
ok("themed JSON keeps hex colours", /"color": "#4d70ff"/.test(themed), themed.slice(0, 300));
ok("themed JSON has per-character hex", hexes.length === EXPECTED_HEX, `got ${hexes.length}, expected ${EXPECTED_HEX}`);
ok("themed JSON is not one flat colour", new Set(hexes).size > 10, `${new Set(hexes).size} distinct`);

console.log("\n=== other clients, per character ===");
$("fbMode").value = "per-character";
api.refresh();
const perChar = api.buildLegacy();
console.log("  " + perChar.replace(/§/g, "<s>").replace(/\n/g, " / "));
ok("per-character keeps the text", EXPECTED_TEXT, JSON.stringify(perChar.replace(/§[0-9a-fk-or]/g, "")));

console.log("\n=== other clients, stepped ===");
for (const steps of [1, 3, 5, 8, 16]) {
  $("fbMode").value = "stepped";
  $("steps").value = String(steps);
  api.refresh();
  const out = api.buildLegacy();
  const distinct = new Set(out.match(/§[0-9a-f]/g) || []).size;
  console.log(`  ${String(steps).padStart(2)} band(s), ${String(distinct).padStart(2)} colours: ${out.replace(/§/g, "<s>").replace(/\n/g, " / ")}`);
  ok(`  ${steps} bands keeps the text`, out.replace(/§[0-9a-fk-or]/g, "") === EXPECTED_TEXT);
}

const byStep = {};
$("fbMode").value = "stepped";
for (const s of [1, 2, 4, 8, 16]) { $("steps").value = String(s); api.refresh(); byStep[s] = api.buildLegacy(); }
// One band per gradient means one distinct colour per line. Uncoloured spaces
// inside a line still emit resets, so count distinct codes rather than codes.
const distinctIn = (s) => new Set(s.match(/§[0-9a-f]/g) || []).size;
ok("1 band is a single colour per line",
   byStep[1].split("\n").every((l) => distinctIn(l) <= 1),
   byStep[1]);
ok("16 bands differ from 4", byStep[16] !== byStep[4]);
ok("banding gets more colours as steps rise",
   (byStep[8].match(/§[0-9a-f]/g) || []).length > (byStep[2].match(/§[0-9a-f]/g) || []).length);

console.log("\n=== other clients, custom text ===");
$("fbMode").value = "custom";
$("fbText").value = "&bLCC Network\n&7lol its a server";
api.refresh();
eq("custom text is used verbatim", api.buildLegacy(), "§bLCC Network\n§7lol its a server");
const snippet = api.buildPluginSnippet();
ok("snippet selects CUSTOM", /FallbackMode\.CUSTOM/.test(snippet), snippet);
ok("snippet carries both lines", snippet.includes("LCC Network") && snippet.includes("lol its a server"), snippet);

console.log("\n=== generated plugin settings ===");
for (const [mode, steps, want] of [
  ["per-character", 5, /FallbackMode\.PER_CHARACTER/],
  ["stepped", 6, /FallbackMode\.STEPPED/],
  ["raw", 5, /FallbackMode\.RAW/]
]) {
  $("fbMode").value = mode;
  $("steps").value = String(steps);
  api.refresh();
  const s = api.buildPluginSnippet();
  ok(`${mode} snippet`, want.test(s), s);
  if (mode === "stepped") ok("stepped snippet sets the step count", /FALLBACK_STEPS = 6/.test(s), s);
}

console.log("\n=== legacy output stays 1.12-safe ===");
for (const mode of ["per-character", "stepped"]) {
  $("fbMode").value = mode;
  api.refresh();
  ok(`${mode} uses classic codes only`, !/§x/.test(api.buildLegacy()));
  ok(`${mode} has no hex leftovers`, !/#[0-9a-f]{6}/.test(api.buildLegacy()));
}

console.log("\n=== listeners.toml output ===");
$("fbMode").value = "stepped";
api.refresh();
const toml = api.buildListenerToml();
ok("uses the TOML literal-string wrapper", toml.startsWith("server_motd = ['''{") && toml.endsWith("''']"), toml.slice(0, 60));
const inner = toml.slice("server_motd = ['''".length, -"''']".length);
let parsed = null;
try { parsed = JSON.parse(inner); } catch (e) { console.log("      parse error: " + e.message); }
ok("payload is valid JSON", parsed !== null);
ok("keeps the theme marker", /eagler_theme/.test(toml));
ok("keeps per-character hex", (toml.match(/"color": "#[0-9a-f]{6}"/g) || []).length === EXPECTED_HEX,
   `got ${(toml.match(/"color": "#[0-9a-f]{6}"/g) || []).length}, expected ${EXPECTED_HEX}`);
ok("payload has no stray '''", !inner.includes("'''"));

console.log(failures === 0 ? "\nALL PASS" : `\n${failures} FAILED`);
process.exit(failures ? 1 : 0);