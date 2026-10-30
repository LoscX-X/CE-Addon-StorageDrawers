"""Use installed vanilla pixel glyphs for labels at a second container-title baseline."""
import json,sys,zipfile,os
from pathlib import Path
from PIL import Image

if not os.environ.get('STORAGEDRAWERS_ASSETS'):
    raise SystemExit('Set STORAGEDRAWERS_ASSETS to the Minecraft assets directory')
installed=Path(os.environ['STORAGEDRAWERS_ASSETS'])
asset_index=os.environ.get('STORAGEDRAWERS_ASSET_INDEX','34')
index=json.loads((installed/'indexes'/(asset_index+'.json')).read_text(encoding='utf-8'))['objects']
digest=index['minecraft/font/unifont.zip']['hash']
source=installed/'objects'/digest[:2]/digest
chars='Ŀ�����߿�����Ʒ����'
glyphs={}
with zipfile.ZipFile(source) as archive:
    for name in archive.namelist():
        if not name.endswith('.hex'):continue
        for line in archive.read(name).decode('ascii').splitlines():
            code,pixels=line.split(':',1)
            char=chr(int(code,16))
            if char in chars:glyphs[char]=pixels
assert set(glyphs)==set(chars),set(chars)-set(glyphs)
atlas=Image.new('RGBA',(len(chars)*16,16))
for cell,char in enumerate(chars):
    raw=bytes.fromhex(glyphs[char]);assert len(raw)==32,(char,len(raw))
    for y in range(16):
        row=int.from_bytes(raw[y*2:y*2+2],'big')
        for x in range(16):
            if row&(1<<(15-x)):atlas.putpixel((cell*16+x,y),(255,255,255,255))
pack=Path(sys.argv[1])/'resourcepack/assets/storagedrawers'
texture=pack/'textures/font/gui/labels.png';texture.parent.mkdir(parents=True,exist_ok=True);atlas.save(texture)
for name,ascent in [('framing_labels',-49),('upgrade_label',-66)]:
    font=pack/'font'/(name+'.json');font.parent.mkdir(parents=True,exist_ok=True)
    font.write_text(json.dumps({'providers':[{'type':'bitmap','file':'storagedrawers:font/gui/labels.png','height':8,'ascent':ascent,'chars':[chars]}]},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
