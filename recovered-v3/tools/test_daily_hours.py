#!/usr/bin/env python3
"""Corpus checks: dated offices, variants, accents and no cross-date substitutions."""
import datetime, hashlib, json, re
from pathlib import Path
assets=Path('app/src/main/assets');base=assets/'hours-daily';manifest=json.loads((base/'manifest.json').read_text())
expected={'office':'oficio','lauds':'laudes','terce':'tercia','sext':'sexta','none':'nona','vespers':'visperas','compline':'completas','invitatory':'oficio'}
count=0
for date,day in manifest['days'].items():
 datetime.date.fromisoformat(date);assert date==day['date'];assert day['options']
 for option in day['options']:
  assert option['title'].strip();assert option['hours']
  for hour,asset in option['hours'].items():
   assert re.fullmatch(r'\d{4}-\d{2}-\d{2}/(?:day|\d+)/[a-z]+\.json',asset)
   doc=json.loads((base/asset).read_text());assert doc['date']==date and doc['hour']==hour
   assert doc['sourcePath']==option['id']+'/'+expected[hour]+'.htm'
   assert len(doc['sourceSha256'])==64
   text='\n\n'.join(doc['paragraphs']);assert len(text)>200
   assert '<script' not in text.lower() and 'tamaño:' not in text.lower()
   assert not re.search(r'\bOf La Tr Sx Nn Vs Cm\b',text)
   assert '\ufffd' not in text and 'SeÃ±or' not in text
   count+=1
assert count==manifest['officeCount']
assert '2027-01-01' not in manifest['days']
saturday=manifest['days']['2026-10-10']['options'][0]
text='\n'.join(json.loads((base/saturday['hours']['compline']).read_text())['paragraphs'])
assert 'celebración del domingo' in text and 'Ahora, Señor' in text
assert saturday['hours']['terce']!=saturday['hours']['sext']!=saturday['hours']['none']
variants=manifest['days']['2026-10-12']['options'];assert len(variants)>=2
assert len({o['id'] for o in variants})==len(variants)
print(f'Validated {len(manifest["days"])} exact dates and {count} offices, including Saturday Compline and celebration variants.')
