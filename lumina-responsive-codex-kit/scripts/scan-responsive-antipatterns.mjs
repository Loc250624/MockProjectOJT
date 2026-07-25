#!/usr/bin/env node
/**
 * Read-only responsive anti-pattern scanner.
 * Usage: node scripts/scan-responsive-antipatterns.mjs [root]
 */
import fs from "node:fs";
import path from "node:path";

const root = path.resolve(process.argv[2] ?? ".");
const ignored = new Set(["node_modules", ".git", "dist", "build", ".next", "coverage", "target", "vendor"]);
const extensions = new Set([".css", ".scss", ".sass", ".less", ".tsx", ".ts", ".jsx", ".js", ".vue", ".html"]);
const rules = [
  ["large fixed width", /\b(?:width|min-width)\s*:\s*(?:[7-9]\d{2}|\d{4,})px\b/gi],
  ["global overflow-x hidden", /(?:html|body|:root|\*)[^{}]*\{[^{}]*overflow-x\s*:\s*hidden/gi],
  ["nowrap", /white-space\s*:\s*nowrap/gi],
  ["absolute positioning", /position\s*:\s*absolute/gi],
  ["viewport width", /width\s*:\s*100vw/gi],
  ["scale workaround", /transform\s*:\s*scale\s*\(/gi],
  ["tiny mobile font", /@media[\s\S]{0,500}?font-size\s*:\s*(?:[0-9]|1[01])px/gi],
  ["large min-width utility", /\bmin-w-\[(?:[7-9]\d{2}|\d{4,})px\]/gi],
  ["nowrap utility", /\bwhitespace-nowrap\b/gi],
  ["100vw utility", /\bw-screen\b/gi],
];

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (ignored.has(entry.name)) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else if (extensions.has(path.extname(entry.name))) out.push(full);
  }
  return out;
}

if (!fs.existsSync(root)) {
  console.error(`Root does not exist: ${root}`);
  process.exit(1);
}

const findings = [];
for (const file of walk(root)) {
  const text = fs.readFileSync(file, "utf8");
  const lines = text.split(/\r?\n/);
  for (const [name, regex] of rules) {
    regex.lastIndex = 0;
    let match;
    while ((match = regex.exec(text))) {
      const line = text.slice(0, match.index).split(/\r?\n/).length;
      findings.push({ name, file: path.relative(root, file), line, excerpt: lines[line - 1]?.trim().slice(0, 180) ?? "" });
      if (match.index === regex.lastIndex) regex.lastIndex++;
    }
  }
}

findings.sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line);
console.log(`# Responsive anti-pattern report\nRoot: ${root}\nFindings: ${findings.length}\n`);
for (const item of findings) {
  console.log(`- [${item.name}] ${item.file}:${item.line}\n  ${item.excerpt}`);
}

console.log("\nThese are review candidates, not automatic bugs. Trace usage before editing.");
