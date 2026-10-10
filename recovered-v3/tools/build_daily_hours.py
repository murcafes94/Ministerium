#!/usr/bin/env python3
"""Import dated offices without inventing dates or combining celebrations."""
import argparse, datetime, hashlib, json, re, subprocess
from html.parser import HTMLParser
from pathlib import Path
REPO='https://github.com/liturgiadelashoras/liturgiadelashoras.github.io.git'
COMMIT='45ea5e0c73c6df1b312f75c7f79cd5889e81d4cd'
MONTHS=dict(zip('ene feb mar abr may jun jul ago sep oct nov dic'.split(),range(1,13)))
HOURS={'office':'oficio','lauds':'laudes','terce':'tercia','sext':'sexta','none':'nona','vespers':'visperas','compline':'completas'}
class Text(HTMLParser):
 def __init__(self,index=False):
  super().__init__(convert_charrefs=True);self.active=False;self.skip=0;self.parts=[];self.index=index
 def handle_starttag(self,tag,attrs):
  attrs=dict(attrs)
  if tag=='div' and attrs.get('id')=='cuerpo':self.active=True
  if self.active and self.index and tag=='table':self.active=False
  if tag in ('a','script','style'):self.skip+=1
  if self.active and not self.skip and tag=='br':self.parts.append('\n')
 def handle_endtag(self,tag):
  if tag in ('a','script','style'):self.skip=max(0,self.skip-1)
  if tag=='body':self.active=False
 def handle_data(self,data):
  if self.active and not self.skip:self.parts.append(re.sub(r'\s+',' ',data))
 def text(self):
  s=''.join(self.parts).replace('\xa0',' ')
  s='\n'.join(re.sub(r'[ \t]+',' ',x).strip() for x in s.split('\n'))
  return re.sub(r'\n{3,}','\n\n',s).strip()
def extract(file,index=False):
 raw=file.read_bytes()
 try:s=raw.decode('utf-8')
 except UnicodeDecodeError:s=raw.decode('iso-8859-1')
 p=Text(index);p.feed(s);return p.text()
def build(source,output):
 output.mkdir(parents=True,exist_ok=True);days={};count=0
 for month in sorted((source/'sync/2026').iterdir()):
  if month.name not in MONTHS:continue
  for folder in sorted(month.iterdir()):
   if not folder.is_dir() or not folder.name.isdigit():continue
   try:date=datetime.date(2026,MONTHS[month.name],int(folder.name)).isoformat()
   except ValueError:continue
   options=[]
   for choice in [folder]+sorted(x for x in folder.iterdir() if x.is_dir() and x.name.isdigit()):
    hours={};title=extract(choice/'index.htm',True) if (choice/'index.htm').is_file() else ''
    for key,stem in HOURS.items():
     file=choice/(stem+'.htm')
     if not file.is_file():continue
     text=extract(file)
     if len(text)<200:raise ValueError('Office body missing: '+str(file))
     asset=f'{date}/{choice.name if choice!=folder else "day"}/{key}.json'
     target=output/asset;target.parent.mkdir(parents=True,exist_ok=True)
     payload={'date':date,'hour':key,'sourcePath':file.relative_to(source).as_posix(),'sourceSha256':hashlib.sha256(file.read_bytes()).hexdigest(),'paragraphs':text.split('\n\n')}
     target.write_text(json.dumps(payload,ensure_ascii=False,separators=(',',':'))+'\n')
     hours[key]=asset;count+=1
     if key=='office' and 'INVITATORIO' in text:
      stop=re.search(r'(?i)\bHimno\s*:',text)
      if stop:
       invitatory=text[:stop.start()].strip()
       if len(invitatory)>200:
        invasset=asset.replace('office.json','invitatory.json');payload=dict(payload,hour='invitatory',paragraphs=invitatory.split('\n\n'));(output/invasset).write_text(json.dumps(payload,ensure_ascii=False,separators=(',',':'))+'\n');hours['invitatory']=invasset;count+=1
    if hours:options.append({'id':choice.relative_to(source).as_posix(),'title':title or 'Oficio publicado para '+date,'hours':hours})
   if options:days[date]={'date':date,'options':options}
 manifest={'schema':1,'repository':REPO,'commit':COMMIT,'year':2026,'firstDate':min(days),'lastDate':max(days),'officeCount':count,'days':days}
 (output/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,separators=(',',':'))+'\n');print(f'Daily offices: {len(days)} dates, {count} offices ({min(days)} — {max(days)})')
if __name__=='__main__':
 a=argparse.ArgumentParser();a.add_argument('--source',type=Path);a.add_argument('--output',type=Path,default=Path('app/src/main/assets/hours-daily'));args=a.parse_args()
 source=args.source or Path('tools/cache/daily-hours-source')
 if not args.source:
  if not (source/'.git').exists():subprocess.run(['git','clone','--no-checkout',REPO,str(source)],check=True)
  subprocess.run(['git','-C',str(source),'checkout','--detach',COMMIT],check=True)
 build(source,args.output)
