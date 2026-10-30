const fs=require('node:fs'),path=require('node:path');
module.exports=function({assets,pack,save}){
  const fixed={rotation:[0,0,0],translation:[0,0,0],scale:[1,1,1]};
  function original(meta,half){const state=JSON.parse(fs.readFileSync(path.join(assets,'blockstates',meta+'.json'),'utf8'));const selected=Object.entries(state.variants).find(([condition])=>condition.split(',').filter(Boolean).every(pair=>{const[k,v]=pair.split('=');return v===({facing:'north',half:String(half)})[k];}));if(!selected)throw new Error('Missing status model '+meta);const name='status/world_'+selected[1].model.split('/').pop();save(path.join(pack,'resourcepack/assets/storagedrawers/models',name+'.json'),{parent:selected[1].model,display:{fixed}});return {type:'minecraft:model',model:'storagedrawers:'+name};}
  for(const half of [false,true])for(const count of [1,2,3,4]){
    const depth=half?'half':'full',nodes=[];
    const select=(index,cases)=>({type:'minecraft:select',property:'minecraft:custom_model_data',index,cases,fallback:{type:'minecraft:empty'}});
    nodes.push(select(0,[{when:'locked',model:original('meta_locked',half)},{when:'claimed',model:original('meta_claimed',half)},{when:'both',model:original('meta_locked_claimed',half)}]));
    for(const [index,meta]of [[1,'meta_void_icon'],[2,'meta_shroud_icon'],[3,'meta_suspend_icon'],[4,'meta_magnet_icon']])nodes.push(select(index,[{when:'true',model:original(meta,half)}]));
    nodes.push(select(5,[-2,-1,1,2].map(level=>({when:String(level),model:original('meta_priority_'+(level>0?'p':'n')+Math.abs(level)+'_icon',half)}))));
    for(let slot=0;slot<count;slot++){
      if(count!==3)nodes.push(select(6+slot,[{when:'true',model:original(`meta_missing_slot_${count}_${slot+1}`,half)}]));
      const entries=[];
      for(let level=0;level<=16;level++){
        let cx=8,cy=count===1?8:count===2?(slot===0?12:4):count===3?(slot===0?12:4):(slot<2?12:4);
        if(count>=3&&!(count===3&&slot===0))cx=(count===3?slot===1:slot%2===0)?12:4;
        const width=count===1||count===2||count===3&&slot===0?12:5;
        const x=cx-width/2,y=cy-3,z=half?8.96:.96;
        const elements=[{from:[x,y,z],to:[x+width,y+.4,z+.01],faces:{north:{texture:'#background'}}}];
        if(level>0)elements.push({from:[x,y,z-.01],to:[x+width*level/16,y+.4,z],faces:{north:{texture:'#fill'}}});
        const name=`status/${depth}_${count}_${slot}_${level}`;
        save(path.join(pack,'resourcepack/assets/storagedrawers/models',name+'.json'),{parent:'minecraft:block/block',display:{fixed},textures:{background:'minecraft:block/gray_concrete',fill:'minecraft:block/lime_concrete',particle:'minecraft:block/lime_concrete'},elements});
        entries.push({threshold:level/16,model:{type:'minecraft:model',model:'storagedrawers:'+name}});
      }
      nodes.push({type:'minecraft:range_dispatch',property:'minecraft:custom_model_data',index:slot,entries,fallback:{type:'minecraft:empty'}});
    }
    save(path.join(pack,'resourcepack/assets/storagedrawers/items/status',depth+'_'+count+'.json'),{model:{type:'minecraft:composite',models:nodes}});
  }
};
