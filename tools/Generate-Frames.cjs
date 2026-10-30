const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
module.exports=function({project,assets,pack,manifest,items,save}) {
  const vanilla=JSON.parse(fs.readFileSync(path.join(project,'build/vanilla-models.json'),'utf8'));
  const models=vanilla.models;
  const resolved=new Map();
  function model(id,visiting=new Set()) {
    id=id.replace(/^minecraft:/,'');if(resolved.has(id))return resolved.get(id);if(visiting.has(id))return null;visiting.add(id);
    const raw=models[id];if(!raw)return null;
    const parent=raw.parent?model(raw.parent,visiting):{};
    const value={...(parent||{}),...raw,textures:{...(parent?.textures||{}),...raw.textures}};resolved.set(id,value);return value;
  }
  function texture(value,textures){const visited=new Set();while(typeof value==='string'&&value.startsWith('#')){if(visited.has(value))return null;visited.add(value);value=textures[value.slice(1)];}return typeof value==='string'?value:value?.sprite;}
  const materials={};
  for(const [id,state]of Object.entries(vanilla.states)) {
    if(!state.variants)continue;let first=Object.values(state.variants)[0];if(Array.isArray(first))first=first[0];if(!first?.model)continue;
    const data=model(first.model);if(!data?.elements||data.elements.length!==1)continue;
    const e=data.elements[0];if(JSON.stringify(e.from)!=='[0,0,0]'||JSON.stringify(e.to)!=='[16,16,16]'||Object.keys(e.faces||{}).length!==6)continue;
    const faces=Object.fromEntries(Object.entries(e.faces).map(([face,v])=>[face,texture(v.texture,data.textures)]));if(Object.values(faces).some(x=>!x))continue;
    materials['minecraft:'+id]=faces;
  }
  function partTemplate(block,role){
    const {drawers,half,kind}=block;
    if((kind==='trim'||kind==='io')&&role==='front')return null;
    const type=kind==='compacting'?`compdrawers_${drawers}`:kind==='standard'?'drawers':kind==='io'?'controller_io':kind;
    const meta=JSON.parse(fs.readFileSync(path.join(assets,'blockstates',`meta_framed_${type}_${role}.json`),'utf8'));
    const selected=Object.entries(meta.variants).find(([condition])=>condition.split(',').filter(Boolean).every(pair=>{const [key,value]=pair.split('=');return value===({facing:'north',half:String(half),slots:kind==='compacting'?'open'+drawers:String(drawers)})[key];}));
    if(!selected)throw new Error('Unresolved framed geometry: '+type+'/'+role);
    let source=JSON.parse(fs.readFileSync(path.join(assets,'models',selected[1].model.split(':')[1]+'.json'),'utf8'));
    if(source.parent&&!source.elements){const parent=path.join(assets,'models',source.parent.split(':')[1]+'.json');if(fs.existsSync(parent))source={...JSON.parse(fs.readFileSync(parent,'utf8')),...source};}
    return source;
  }
  const meshes=new Map();
  for(const block of manifest.blocks.filter(x=>x.framed)) {
    const name=block.id.split(':')[1];const nodes=[];
    for(const [index,role]of ['side','trim','front'].entries()) {
      const source=partTemplate(block,role);if(!source?.elements)continue;
      const geometry=structuredClone(source.elements);for(const element of geometry)for(const [face,value]of Object.entries(element.faces||{})){value.texture=face;delete value.cullface;}
      const mesh=crypto.createHash('sha256').update(JSON.stringify(geometry)).digest('hex').slice(0,20);
      const cases=[];
      for(const [material,faces]of Object.entries(materials)){
        const data=structuredClone(source);delete data.textures;data.parent='minecraft:block/block';
        for(const element of data.elements)for(const [face,value]of Object.entries(element.faces||{})){value.texture='#'+face;delete value.cullface;}
        data.textures={...faces,particle:faces.north};
        // Keep normal held/item-frame transforms; the server applies full world scale and yaw to facade displays.
        delete data.display;
        const key=`frame/mesh_${mesh}/${material.split(':')[1]}`;
        if(!meshes.has(key)){save(path.join(pack,'resourcepack/assets/storagedrawers/models',key+'.json'),data);meshes.set(key,true);}
        cases.push({when:material,model:{type:'minecraft:model',path:'storagedrawers:'+key}});
      }
      nodes.push({type:'select',property:'custom_model_data',index,cases,fallback:{type:'empty'}});
      save(path.join(pack,'resourcepack/assets/storagedrawers/items/frame',name,role+'.json'),{model:{type:'minecraft:select',property:'minecraft:custom_model_data',index:0,cases:cases.map(c=>({when:c.when,model:{type:'minecraft:model',model:c.model.path}})),fallback:{type:'minecraft:empty'}}});
    }
    const raw=JSON.parse(fs.readFileSync(path.join(assets,'models/item',name+'.json'),'utf8')).parent;
    // The raw shell is a fallback for an unframed item. Drawing it below the
    // material meshes produces coplanar faces in hand, GUI and item frames.
    nodes.unshift({type:'select',property:'custom_model_data',index:0,
      cases:[{when:Object.keys(materials),model:{type:'empty'}}],
      fallback:{type:'minecraft:model',path:raw}});
    items[block.id].model={type:'composite',models:nodes};
  }
  save(path.join(project,'src/main/resources/frame-materials.json'),Object.keys(materials));
  manifest.frameMaterials=Object.keys(materials).length;
  manifest.uniqueFrameMeshes=meshes.size;
};
