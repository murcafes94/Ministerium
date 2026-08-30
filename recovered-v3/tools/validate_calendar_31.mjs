import fs from 'node:fs';

const resolver = fs.readFileSync(
  'app/src/main/java/com/fabri/ministerium/LiturgicalResolver.java', 'utf8');

const failures = [];
if (!resolver.includes('Calendar baptism = baptismOfLord(year);')) {
  failures.push('LiturgicalResolver no usa baptismOfLord(year)');
}
if (!resolver.includes('day >= 7 ? 1 : 7')) {
  failures.push('No está la regla de Bautismo del Señor tras Epifanía 7/8 enero');
}
if (resolver.includes('addDays(epiphanySunday(year), 7)')) {
  failures.push('Permanece la fórmula antigua Epifanía + 7');
}

const atNoon = (y, m, d) => new Date(Date.UTC(y, m - 1, d, 12));
const addDays = (date, n) => new Date(date.getTime() + n * 86400000);
const daysBetween = (a, b) => Math.round((b - a) / 86400000);
const sunday = 0;

function adventStart(year) {
  const d = atNoon(year, 12, 3);
  while (d.getUTCDay() !== sunday) d.setUTCDate(d.getUTCDate() - 1);
  return d;
}
function epiphanySunday(year) {
  const d = atNoon(year, 1, 2);
  while (d.getUTCDay() !== sunday) d.setUTCDate(d.getUTCDate() + 1);
  return d;
}
function baptism(year) {
  const e = epiphanySunday(year);
  return addDays(e, e.getUTCDate() >= 7 ? 1 : 7);
}
function easter(year) {
  const a = year % 19, b = Math.floor(year / 100), c = year % 100;
  const d = Math.floor(b / 4), e = b % 4, f = Math.floor((b + 8) / 25);
  const g = Math.floor((b - f + 1) / 3);
  const h = (19 * a + b - d - g + 15) % 30;
  const i = Math.floor(c / 4), k = c % 4;
  const l = (32 + 2 * e + 2 * i - h - k) % 7;
  const m = Math.floor((a + 11 * h + 22 * l) / 451);
  const month = Math.floor((h + l - 7 * m + 114) / 31);
  const day = ((h + l - 7 * m + 114) % 31) + 1;
  return atNoon(year, month, day);
}
function ordinaryWeek(date) {
  const year = date.getUTCFullYear();
  const easterDate = easter(year);
  const ash = addDays(easterDate, -46);
  const pentecost = addDays(easterDate, 49);
  const advent = adventStart(year);
  const bapt = baptism(year);
  if (date > bapt && date < ash) {
    const firstMonday = addDays(bapt, 1);
    let week = Math.floor(daysBetween(firstMonday, date) / 7) + 1;
    if (date.getUTCDay() === sunday) week += 1;
    return Math.max(1, week);
  }
  if (date > pentecost && date < advent) {
    const christKing = addDays(advent, -7);
    const remaining = daysBetween(date, christKing);
    return 34 - Math.floor((Math.max(0, remaining) + 6) / 7);
  }
  return 0;
}

const cases = [
  [2026, 8, 24, 21, '24-08-2026 debe ser semana XXI'],
  [2023, 1, 9, 0, 'Bautismo 2023 cae lunes 9; ese día todavía no se cuenta como feria ordinaria'],
  [2023, 1, 10, 1, '10-01-2023 inicia semana I'],
  [2024, 1, 8, 0, 'Bautismo 2024 cae lunes 8'],
  [2024, 1, 9, 1, '09-01-2024 inicia semana I'],
];
for (const [y,m,d,expected,label] of cases) {
  const actual = ordinaryWeek(atNoon(y,m,d));
  if (actual !== expected) failures.push(`${label}: esperado ${expected}, obtenido ${actual}`);
}

if (failures.length) {
  console.error('Calendario 3.1: FALLÓ');
  failures.forEach((x) => console.error(`- ${x}`));
  process.exit(1);
}
console.log('Calendario 3.1: OK — semana XXI y casos Epifanía/Bautismo cubiertos.');
