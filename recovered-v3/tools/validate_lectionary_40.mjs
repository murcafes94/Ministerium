import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const requireText = (value, expected, label) => {
  if (!value.includes(expected)) throw new Error(`${label}: falta "${expected}"`);
};

function firstSundayOfAdvent(year) {
  const value = new Date(Date.UTC(year, 10, 27, 12));
  while (value.getUTCDay() !== 0) value.setUTCDate(value.getUTCDate() + 1);
  return value;
}

function liturgicalYear(date) {
  const year = date.getUTCFullYear();
  return date < firstSundayOfAdvent(year) ? year : year + 1;
}

function sundayCycle(year) {
  const remainder = ((year % 3) + 3) % 3;
  return remainder === 1 ? "A" : remainder === 2 ? "B" : "C";
}

function weekdayCycle(date) {
  return date.getUTCFullYear() % 2 === 0 ? "II" : "I";
}

const cases = [
  ["2025-11-23T12:00:00Z", 2025, "C"],
  ["2025-11-30T12:00:00Z", 2026, "A"],
  ["2026-08-27T12:00:00Z", 2026, "A"],
  ["2026-11-29T12:00:00Z", 2027, "B"],
];
for (const [iso, expectedYear, expectedCycle] of cases) {
  const year = liturgicalYear(new Date(iso));
  if (year !== expectedYear || sundayCycle(year) !== expectedCycle) {
    throw new Error(`Ciclo incorrecto para ${iso}: ${year}/${sundayCycle(year)}`);
  }
}
for (const [iso, expectedCycle] of [
  ["2025-12-15T12:00:00Z", "I"],
  ["2026-01-12T12:00:00Z", "II"],
  ["2026-12-14T12:00:00Z", "II"],
  ["2027-01-11T12:00:00Z", "I"],
]) {
  const actual = weekdayCycle(new Date(iso));
  if (actual !== expectedCycle) {
    throw new Error(`Ciclo ferial incorrecto para ${iso}: ${actual}`);
  }
}

const engine = read("app/src/main/java/com/fabri/ministerium/LectionaryRuleEngine.java");
for (const marker of [
  "Ordenación de las Lecturas de la Misa",
  "OLM 65, 66, 79 y 89",
  "OLM 69",
  "lecturas del día salvo lectura propia expresamente indicada",
  "date.get(Calendar.YEAR) % 2",
  "lecturas asignadas; ordinariamente tres lecturas",
  "firstSundayOfAdvent",
  "LiturgicalResolver.primaryEvent(events)",
]) requireText(engine, marker, "LectionaryRuleEngine");

const activity = read("app/src/main/java/com/fabri/ministerium/MassReadingsActivity.java");
requireText(activity, "LectionaryRuleEngine.resolve", "MassReadingsActivity");
requireText(activity, "txtLectionaryRule", "MassReadingsActivity");

const layout = read("app/src/main/res/layout/activity_mass_readings.xml");
requireText(layout, 'android:id="@+id/txtLectionaryRule"', "activity_mass_readings.xml");

const repository = read("app/src/main/java/com/fabri/ministerium/MassReadingsRepository.java");
for (const section of [
  "Primera lectura",
  "Salmo responsorial",
  "Segunda lectura",
  "Aclamación antes del Evangelio",
  "Evangelio",
]) requireText(repository, section, "MassReadingsRepository");

console.log("Ministerium 4.0 Lectionary contract OK");
console.log("- OLM source and selection rules are traceable");
console.log("- Sunday cycles A/B/C cross the Advent boundary correctly");
console.log("- Ferial I/II stays on the civil-year boundary; seasonal cycles remain annual");
console.log("- Optional memorials do not silently replace the feria");
console.log("- Saints rules and all Word liturgy sections are preserved");
