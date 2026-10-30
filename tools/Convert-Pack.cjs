const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const crypto = require('node:crypto');
const project = path.resolve(__dirname, '..');
const source = path.resolve(process.argv[2] || path.join(project, 'vendor/StorageDrawers'));
const pack = path.join(project, 'build/stage/CraftEngine/resources/storagedrawers');
const assets = path.join(source, 'common/src/main/resources/assets/storagedrawers');
const manifest = { source: 'https://github.com/jaquadro/StorageDrawers', commit: cp.execFileSync('git', ['rev-parse', 'HEAD'], { cwd: source, encoding: 'utf8' }).trim(), assets: 0, blocks: [], items: [], recipes: [], omittedRecipes: [] };
function files(dir) { return fs.readdirSync(dir, {withFileTypes:true}).flatMap(e => e.isDirectory() ? files(path.join(dir,e.name)) : [path.join(dir,e.name)]); }
function save(file, data) { fs.mkdirSync(path.dirname(file),{recursive:true}); fs.writeFileSync(file,typeof data==='string'?data:JSON.stringify(data,null,2)+'\n'); }
function yaml(v, d=0) {
  if (v===null || typeof v!=='object') return JSON.stringify(v);
  const pad='  '.repeat(d);
  if (!Object.keys(v).length) return Array.isArray(v)?'[]':'{}';
  const value=x=>x!==null && typeof x==='object' && Object.keys(x).length ? '\n'+yaml(x,d+1) : ' '+yaml(x,d+1);
  return Array.isArray(v) ? v.map(x=>pad+'-'+value(x)).join('\n') : Object.entries(v).map(([k,x])=>pad+JSON.stringify(k)+':'+value(x)).join('\n');
}
for(const file of files(assets)) {
  const relative=path.relative(assets,file);
  if (!/^(models|textures|lang)[\\/]/.test(relative)) continue;
  const dest=path.join(pack,'resourcepack/assets/storagedrawers',relative);
  fs.mkdirSync(path.dirname(dest),{recursive:true}); fs.copyFileSync(file,dest); manifest.assets++;
}
const names=JSON.parse(fs.readFileSync(path.join(assets,'lang/zh_cn.json'),'utf8'));
const woods=['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo','crimson','warped'];
const items={}, blocks={}, groups={wood:[],compacting:[],network:[],frames:[],upgrades:[],tools:[]};
function item(name,title,group) {
  const id='storagedrawers:'+name;
  items[id]={material:'minecraft:paper',model:'storagedrawers:item/'+name,data:{item_name:'<!i>'+title}};
  groups[group].push(id); manifest.items.push(id); return id;
}
function block(name,title,group,kind,count=0,half=false,framed=false) {
  const id=item(name,title,group);
  items[id].behavior={type:'block_item',block:id};
  const appearances={},variants={};
  const itemModel=JSON.parse(fs.readFileSync(path.join(assets,'models/item',name+'.json'),'utf8'));
  const modelPath=kind==='compacting'&&!framed?'storagedrawers:block/'+`${half?'half':'full'}_compdrawers_${count}_open${count}`:itemModel.parent;
  if(half&&!framed){
    // Native CE constant renderers share one invisible non-occluding carrier.
    // Giving all 140 half-drawer orientations separate leaf states exhausts the pool.
    const placed='storagedrawers:placed/'+name;
    save(path.join(pack,'resourcepack/assets/storagedrawers/models/placed',name+'.json'),{parent:modelPath,display:{fixed:{rotation:[0,0,0],translation:[0,0,0],scale:[.5,.5,.5]}}});
    items[id].model={type:'select',property:'display_context',cases:[{when:'fixed',model:{type:'minecraft:model',path:placed}}],fallback:{type:'minecraft:model',path:'storagedrawers:item/'+name}};
  }
  for(const [face,rotation] of [['north',0],['east',90],['south',180],['west',270]]) {
    // Half-depth models leave part of their voxel empty. A solid client carrier
    // would still cull the full face of the grass/stone below that empty half.
    appearances[face]=half&&!framed
      ?{auto_state:{type:'leaves',id:'storagedrawers:half_drawer_blank'},transparent:true,entity_renderer:[{item:id,display_context:'FIXED',scale:2,yaw:(180+rotation)%360}]}
      :{auto_state:framed?'leaves':'solid',model:{path:modelPath,...(rotation?{y:rotation}:{})}};
    variants['facing='+face]={appearance:face};
  }
  blocks[id]={settings:{item:id,hardness:5,resistance:5,push_reaction:'block',can_occlude:!half&&!framed,light_dampening:half?0:15,tags:['minecraft:mineable/axe'],sounds:{break:'minecraft:block.wood.break',place:'minecraft:block.wood.place',hit:'minecraft:block.wood.hit',step:'minecraft:block.wood.step',fall:'minecraft:block.wood.fall'}},
    behavior:{type:'storagedrawers:drawer',kind,drawers:count,half_depth:half,framed,base_stacks:kind==='compacting'?(half?16:32):(count?(half?16:32)/count:0)},
    loot:{pools:[{rolls:1,entries:[{type:'item',item:id,functions:[{type:'storagedrawers:contents'}]}]}]},
    states:{properties:{facing:{type:'horizontal_direction',default:'north'}},appearances,variants}};
  manifest.blocks.push({id,kind,drawers:count,half,framed});
}
for(const wood of woods) {
  const mat=names['block.storagedrawers.mat.'+wood];
  for(const half of [false,true]) for(const count of [1,2,4]) block(`${wood}_${half?'half':'full'}_drawers_${count}`,`${mat}${half?'С��':''}���� ${count===4?'2��2':'1��'+count}`,'wood','standard',count,half);
  block(wood+'_trim',mat+'�Žӷ���','network','trim');
}
for(const half of [false,true]) for(const count of [2,3]) block(`compacting_${half?'half_':''}drawers_${count}`,`${half?'С��':''}ѹ�����루${count}�㣩`,'compacting','compacting',count,half);
block('controller','���������','network','controller');
block('controller_io','�����������������','network','io');
for(const half of [false,true])for(const count of [1,2,4])block(`framed_${half?'half':'full'}_drawers_${count}`,`���${half?'С��':''}���� ${count}`,'frames','standard',count,half,true);
for(const half of [false,true])for(const count of [2,3])block(`framed_compacting_${half?'half_':''}drawers_${count}`,`���${half?'С��':''}ѹ������ ${count}`,'frames','compacting',count,half,true);
block('framed_trim','����Žӷ���','frames','trim',0,false,true);
block('framed_controller','�����������','frames','controller',0,false,true);
block('framed_controller_io','�����봫������','frames','io',0,false,true);
// The table and buttons retain every original placement/orientation state.
for(const [name,kind,title] of [['framing_table','table','�����̨'],['keybutton_drawer','button','��Կ�װ�ť'],['keybutton_quantify','button','����Կ�װ�ť'],['keybutton_concealment','button','����Կ�װ�ť']]){
  const id=item(name,title,kind==='table'?'frames':'network');items[id].behavior={type:'block_item',block:id};
  const original=JSON.parse(fs.readFileSync(path.join(assets,'blockstates',name+'.json'),'utf8'));
  const appearances={},variants={};let index=0;
  for(const [condition,model] of Object.entries(original.variants)){const appearance='v'+index++;const value=Array.isArray(model)?model[0]:model;appearances[appearance]={auto_state:kind==='table'?'leaves':'tripwire',model:{path:value.model,...(value.x?{x:value.x}:{}),...(value.y?{y:value.y}:{})}};variants[condition]={appearance};}
  const properties={facing:{type:'horizontal_direction',default:'north'}};
  if(kind==='table')properties.part={type:'string',values:['left','right'],default:'right'};
  else{properties.face={type:'string',values:['floor','wall','ceiling'],default:'wall'};properties.powered={type:'boolean',default:false};}
  blocks[id]={settings:{item:id,hardness:kind==='table'?2.5:.5,resistance:2.5,push_reaction:'block',can_occlude:false},behavior:{type:'storagedrawers:drawer',kind,drawers:0,half_depth:false,base_stacks:0},states:{properties,appearances,variants},loot:{pools:[{rolls:1,entries:[{type:'item',item:id}]}]}};
  manifest.blocks.push({id,kind,drawers:0,half:false});
}
for(const tier of ['obsidian','copper','iron','gold','emerald','diamond','netherite']) item(tier+'_storage_upgrade',names['item.storagedrawers.'+tier+'_storage_upgrade']||({obsidian:'����ʯ',copper:'ͭ',iron:'��',gold:'��',emerald:'�̱�ʯ',diamond:'��ʯ',netherite:'�½�Ͻ�'}[tier]+'��������'),'upgrades');
for(const name of ['void_upgrade','one_stack_upgrade','redstone_upgrade','min_redstone_upgrade','max_redstone_upgrade']) item(name,names['item.storagedrawers.'+name]||name,'upgrades');
item('upgrade_template','����ģ��','upgrades');
for(const name of ['drawer_key','quantify_key','shroud_key','personal_key','personal_key_unlock']) item(name,names['item.storagedrawers.'+name]||name,'tools');
const originalItems=fs.readFileSync(path.join(source,'common/src/main/java/com/jaquadro/minecraft/storagedrawers/core/ModItems.java'),'utf8');
const internalVariants=new Set([...originalItems.matchAll(/EXCLUDE_ITEMS_CREATIVE_TAB\.add\(([A-Z0-9_]+)\)/g)].map(m=>m[1].toLowerCase()));
for(const [,name] of originalItems.matchAll(/ITEMS\.register\("([^"]+)"/g)){
  const id='storagedrawers:'+name;if(items[id])continue;
  const title=names['item.storagedrawers.'+name]||name;
  const group=name.includes('upgrade')?'upgrades':'tools';item(name,title,group);
  if(internalVariants.has(name)||name==='personal_key_ftb'||name==='personal_key_cofh')groups[group]=groups[group].filter(x=>x!==id);
  if(!fs.existsSync(path.join(assets,'models/item',name+'.json')))throw new Error('Missing original item model: '+name);
}
for(const [id,definition] of Object.entries(items))if(/:(?:keyring|priority_key)/.test(id)){definition.behavior={type:'storagedrawers:tool'};definition.data.components={'minecraft:max_stack_size':1};}
const categories={};
const labels={wood:'ľ�ʳ���',compacting:'ѹ������',network:'�����������Ӽ�',frames:'����빤��̨',upgrades:'����',tools:'Կ�׹���'};
categories['storagedrawers:root']={priority:10,name:'<!i><gold>StorageDrawers �� �������',icon:'storagedrawers:oak_full_drawers_1',list:Object.keys(groups).map(k=>'#storagedrawers:'+k)};
for(const [group,ids] of Object.entries(groups)) categories['storagedrawers:'+group]={hidden:true,name:'<!i><white>'+labels[group],icon:ids[0],list:ids};
const recipes={};
function ingredient(x) {
  if(typeof x==='string') return x;
  if(x.item) { if(x.item.startsWith('storagedrawers:') && !items[x.item]) throw new Error('Unavailable item '+x.item); return x.item; }
  if(x.tag) {
    if(x.tag==='c:chests/wooden') return 'minecraft:chest';
    const vanillaTags={'c:rods/wooden':'stick','c:ingots/iron':'iron_ingot','c:ingots/gold':'gold_ingot','c:ingots/copper':'copper_ingot','c:ingots/netherite':'netherite_ingot','c:gems/diamond':'diamond','c:gems/emerald':'emerald','c:dusts/redstone':'redstone','c:dusts/glowstone':'glowstone_dust','c:obsidians':'obsidian'};
    if(vanillaTags[x.tag]) return 'minecraft:'+vanillaTags[x.tag];
    if(x.tag==='storagedrawers:drawers') return manifest.blocks.filter(b=>['standard','compacting'].includes(b.kind)).map(b=>b.id);
    if(x.tag==='c:stones') return '#minecraft:stone_crafting_materials';
    const tagFile=path.join(source,'common/src/main/resources/data',x.tag.split(':')[0],'tags/item',x.tag.split(':')[1]+'.json');
    if(fs.existsSync(tagFile)) {
      const values=JSON.parse(fs.readFileSync(tagFile,'utf8')).values;
      const mapped=values.filter(v=>typeof v==='string' && (!v.startsWith('storagedrawers:') || items[v]));
      if(mapped.length) return mapped;
    }
    if(x.tag.startsWith('minecraft:')) return '#'+x.tag;
  }
  throw new Error('Unmapped ingredient '+JSON.stringify(x));
}
for(const file of files(path.join(source,'fabric/src/main/resources/data/storagedrawers/recipe'))) {
  if(!file.endsWith('.json')) continue;
  const r=JSON.parse(fs.readFileSync(file,'utf8'));
  const result=typeof r.result==='string'?r.result:r.result?.id;
  if(!items[result]) continue;
  try {
    if(!['minecraft:crafting_shaped','minecraft:crafting_shapeless'].includes(r.type)) throw new Error('Requires special recipe serializer: '+r.type);
    const v={type:r.type.slice('minecraft:crafting_'.length),result:{id:result,count:r.result.count||1}};
    if(r.pattern) {v.pattern=r.pattern;v.ingredients=Object.fromEntries(Object.entries(r.key).map(([k,x])=>[k,ingredient(x)]));}
    else v.ingredients=r.ingredients.map(ingredient);
    const id='storagedrawers:'+path.basename(file,'.json'); recipes[id]=v;manifest.recipes.push(id);
  } catch(e) {manifest.omittedRecipes.push({file:path.basename(file),reason:e.message});}
}
const upgradeIds=Object.keys(items).filter(id=>id.includes('upgrade')&&!id.endsWith('upgrade_template'));
const drawerIds=manifest.blocks.filter(b=>b.drawers>0).map(b=>b.id);
function special(name,kind,ingredients,result){recipes['storagedrawers:'+name]={type:'storagedrawers:special',kind,ingredients,result:{id:result,count:1}};manifest.recipes.push('storagedrawers:'+name);}
for(let n=1;n<=7;n++)special('add_upgrade_'+n,'add_upgrade',[drawerIds,...Array(n).fill(upgradeIds)],'storagedrawers:oak_full_drawers_1');
const storageUpgrades=upgradeIds.filter(id=>id.endsWith('_storage_upgrade')&&!id.includes('creative'));
for(let n=0;n<=8;n++)special('add_detached_upgrade_'+n,'add_detached_upgrade',[['storagedrawers:detached_drawer','storagedrawers:detached_drawer_full'],...Array(n).fill(storageUpgrades)],'storagedrawers:detached_drawer');
const keyIds=Object.keys(items).filter(id=>/^storagedrawers:(?:drawer_key|quantify_key|shroud_key|personal_key(?:_[a-z]+)?|suspend_key|priority_key(?:_[np][12])?|drawer_puller)$/.test(id));
special('keyring','keyring',['minecraft:iron_nugget','minecraft:iron_nugget','minecraft:iron_nugget','minecraft:iron_nugget',keyIds],'storagedrawers:keyring');
special('personal_key_cycle','personal_key_cycle',['storagedrawers:personal_key'],'storagedrawers:personal_key');
special('remote_group_upgrade_bound','remote_group_upgrade',['minecraft:ender_pearl','storagedrawers:remote_upgrade_bound','minecraft:ender_pearl'],'storagedrawers:remote_group_upgrade_bound');
save(path.join(pack,'pack.yml'),yaml({author:'Justin Aquadro; Cendreal port',version:'26.3.0-dev',description:'StorageDrawers for CraftEngine'})+'\n');
require('./Generate-Frames.cjs')({project,assets,pack,manifest,items,save});
require('./Generate-Status.cjs')({assets,pack,save});
const framedIds=new Set(manifest.blocks.filter(b=>b.framed).map(b=>b.id));
save(path.join(pack,'configuration/items.yml'),yaml({items:Object.fromEntries(Object.entries(items).filter(([id])=>!framedIds.has(id)))})+'\n');
for(const id of framedIds)save(path.join(pack,'configuration/framed-items',id.split(':')[1]+'.yml'),yaml({items:{[id]:items[id]}})+'\n');
for(const [name,data] of Object.entries({blocks,categories,recipes})) save(path.join(pack,'configuration',name+'.yml'),yaml({[name]:data})+'\n');
const images={};
for(const type of ['1','2','4','comp_2','comp_3','framing']) images['storagedrawers:gui_'+type]={height:204,ascent:13,font:'storagedrawers:ui',file:'storagedrawers:font/gui/drawers_'+type+'.png'};
save(path.join(pack,'configuration/images.yml'),yaml({images})+'\n');
cp.execFileSync(process.execPath,[path.join(__dirname,'Generate-Gui.cjs'),pack],{stdio:'inherit'});
save(path.join(project,'build/manifest.json'),manifest);
save(path.join(project,'src/main/resources/content-index.json'),{blocks:manifest.blocks.map(x=>x.id),items:manifest.items,recipes:manifest.recipes});
for(const dest of [path.join(project,'src/main/resources/licenses/StorageDrawers-MIT.txt'),path.join(pack,'LICENSE')]) {fs.mkdirSync(path.dirname(dest),{recursive:true});fs.copyFileSync(path.join(source,'LICENSE'),dest);}
manifest.assetTreeSha256=crypto.createHash('sha256').update(files(assets).map(f=>path.relative(assets,f)+':'+crypto.createHash('sha256').update(fs.readFileSync(f)).digest('hex')).sort().join('\n')).digest('hex');
save(path.join(project,'build/manifest.json'),manifest);
console.log(JSON.stringify({blocks:manifest.blocks.length,items:manifest.items.length,recipes:manifest.recipes.length,omitted:manifest.omittedRecipes}));
