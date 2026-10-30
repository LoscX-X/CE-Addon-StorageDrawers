const fs=require('node:fs'),path=require('node:path');
const sharp=require('sharp');
const root=path.resolve(__dirname,'..');
const layout=JSON.parse(fs.readFileSync(path.join(root,'src/main/resources/gui-layout.json'),'utf8'));
const pack=process.argv[2];
async function main() {
  require('node:child_process').execFileSync(process.env.STORAGEDRAWERS_PYTHON || 'python',[path.join(__dirname,'Export-Gui-Font.py'),pack],{stdio:'inherit'});
  for(const [type,storage] of Object.entries({...layout.storage,framing:[...layout.framing.inputs,layout.framing.result]})) {
    const shapes=[];
    const rect=(x,y,w,h,fill,stroke)=>shapes.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${fill}"${stroke?` stroke="${stroke}"`:''}/>`);
    rect(0,0,176,204,'#202020');rect(1,1,174,202,'#f6f6f6');rect(3,3,170,198,'#555555');rect(3,3,168,196,'#c6c6c6');
    if(type==='framing'){
      for(const [x,w]of [[22,26],[58,60],[130,26]]){rect(x,30,w,30,'#827d5d');rect(x+2,32,w-4,26,'#e5e3da');rect(x+3,33,w-6,24,'#c6c6c6');}
      shapes.push('<path d="M120 42h4v-3l5 6-5 6v-3h-4z" fill="#555555"/>');
    }else{rect(57,14,62,64,'#827d5d');rect(59,16,58,60,'#37352d');rect(61,18,54,56,'#e5e3da');rect(63,20,50,52,'#c6c6c6');}
    const slot=(x,y,upgrade=false)=>{rect(x-1,y-1,18,18,'#373737');rect(x,y,17,17,'#ffffff');rect(x,y,16,16,upgrade?'#8d8978':'#8b8b8b');rect(x,y,16,1,'#626262');rect(x,y,1,16,'#626262');};
    for(const index of storage)slot(8+index%9*18,18+Math.floor(index/9)*18);
    if(type!=='framing')for(const index of layout.upgrades)slot(8+index%9*18,18+Math.floor(index/9)*18,true);
    for(let row=0;row<3;row++)for(let col=0;col<9;col++)slot(8+col*18,121+row*18);
    for(let col=0;col<9;col++)slot(8+col*18,179);
    const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="176" height="204" shape-rendering="crispEdges">${shapes.join('')}</svg>`;
    const dest=path.join(pack,'resourcepack/assets/storagedrawers/textures/font/gui',`drawers_${type}.png`);
    fs.mkdirSync(path.dirname(dest),{recursive:true});
    await sharp(Buffer.from(svg)).png().toFile(dest);
    const source=path.join(root,'build/gui-source',`drawers_${type}.svg`);fs.mkdirSync(path.dirname(source),{recursive:true});fs.writeFileSync(source,svg);
  }
}
main().catch(e=>{console.error(e);process.exitCode=1;});
