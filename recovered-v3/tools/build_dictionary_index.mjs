import fs from "node:fs";
import path from "node:path";

const [sourceDirectory, outputFile] = process.argv.slice(2);
if (!sourceDirectory || !outputFile) {
  throw new Error("Uso: node tools/build_dictionary_index.mjs <OEBPS/Text> <salida.tsv>");
}

const decodeEntities = (value) => value
  .replace(/&#x([0-9a-f]+);/gi, (_, code) => String.fromCodePoint(parseInt(code, 16)))
  .replace(/&#([0-9]+);/g, (_, code) => String.fromCodePoint(parseInt(code, 10)))
  .replace(/&nbsp;/gi, " ")
  .replace(/&amp;/gi, "&")
  .replace(/&lt;/gi, "<")
  .replace(/&gt;/gi, ">")
  .replace(/&quot;/gi, "\"")
  .replace(/&apos;/gi, "'");

const files = fs.readdirSync(sourceDirectory)
  .filter((name) => /^RAEv15_.*\.xhtml$/i.test(name))
  .sort((left, right) => left.localeCompare(right, "es", { numeric: true }));

const rows = ["# Ministerium dictionary index v1"];
const matcher = /<span\s+class=["']masnegrita["'][^>]*>([\s\S]*?)<\/span>/gi;
for (const name of files) {
  const content = fs.readFileSync(path.join(sourceDirectory, name), "utf8");
  let match;
  while ((match = matcher.exec(content)) !== null) {
    const term = decodeEntities(match[1].replace(/<[^>]+>/g, " "))
      .replace(/[\t\r\n]+/g, " ")
      .replace(/\s+/g, " ")
      .trim()
      .replace(/\.$/, "")
      .trim();
    if (term) rows.push(`${term}\tOEBPS/Text/${name}`);
  }
}

if (rows.length < 80_000) {
  throw new Error(`Índice incompleto: solo se encontraron ${rows.length - 1} voces`);
}
fs.writeFileSync(outputFile, `${rows.join("\n")}\n`, "utf8");
process.stdout.write(`Índice del diccionario: ${rows.length - 1} voces en ${files.length} archivos\n`);
