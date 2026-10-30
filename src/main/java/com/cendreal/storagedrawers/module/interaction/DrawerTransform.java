package com.cendreal.storagedrawers.module.interaction;

import com.cendreal.storagedrawers.module.storage.*;
import com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior;
import net.momirealms.craftengine.bukkit.api.*;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.libraries.antigrieflib.Flag;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;

/** In-place wood changes and repartitioning. Validate the complete result before replacing the block. */
public final class DrawerTransform {
    public static boolean apply(DrawerController source,Item held,Player player){
        if(source.behavior.framed||!source.behavior.layout.kind().equals("standard"))return false;
        var definition=CraftEngineBlocks.byId(held.id());if(definition==null)return false;
        var behavior=definition.defaultState().behavior().getFirst(DrawerBehavior.class);if(behavior==null||behavior.framed)return false;
        boolean trim=behavior.layout.kind().equals("trim");
        if(trim){String wood=definition.id().value().replaceFirst("_trim$","");String depth=source.behavior.layout.halfDepth()?"half":"full";definition=CraftEngineBlocks.byId(Key.of("storagedrawers:"+wood+"_"+depth+"_drawers_"+source.size()));if(definition==null)return false;behavior=definition.defaultState().behavior().getFirst(DrawerBehavior.class);}
        else if(!behavior.layout.kind().equals("standard")||behavior.layout.halfDepth()!=source.behavior.layout.halfDepth()||behavior.layout.drawers()==source.size())return false;
        if(definition.id().equals(source.behavior.block().id()))return false;
        for(int i=0;i<source.size();i++)if(source.missing(i))return false;
        CompoundTag data=source.contents();data.putString("block",definition.id().toString());
        if(!trim){Item prototype=Item.empty();long total=0;for(int i=0;i<source.size();i++){Item next=source.template(i);if(!next.isEmpty()){if(!prototype.isEmpty()&&!prototype.isSimilar(next))return false;prototype=next;}total+=source.count(i);}
            int count=behavior.layout.drawers();long stacks=source.has("one_stack_upgrade")?1:(long)behavior.layout.baseStacks()*Upgrades.multiplier(source.upgrades());long capacity=source.has("creative_storage_upgrade")?Integer.MAX_VALUE:Math.min(Integer.MAX_VALUE,stacks*(prototype.isEmpty()?64:prototype.maxStackSize()));
            if((total+count-1)/count>capacity)return false;
            for(int i=0;i<4;i++)data.remove("slot"+i);
            for(int i=0;i<count;i++){var slot=new CompoundTag();slot.putInt("count",(int)(total/count+(i<total%count?1:0)));slot.putInt("factor",1);if(!prototype.isEmpty())slot.put("item",ItemStackUtils.saveMinecraftItemStackAsTag(prototype.minecraftItem()));data.put("slot"+i,slot);}
        }
        var entity=source.blockEntity();var world=(org.bukkit.World)entity.world.world().platformWorld();var location=new org.bukkit.Location(world,entity.pos.x,entity.pos.y,entity.pos.z);
        if(!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player)player.platformPlayer(),Flag.PLACE,location))return false;
        var state=definition.defaultState().with(behavior.facing,entity.blockState.get(source.behavior.facing));
        if(!CraftEngineBlocks.place(location,state,false))return false;
        var target=behavior.at(entity.world.world(),entity.pos);if(!(target instanceof DrawerController drawer))throw new IllegalStateException("Missing transformed drawer");
        var root=new CompoundTag();root.put(DrawerController.DATA_KEY,data);drawer.loadCustomData(root);drawer.changed();world.playSound(location,org.bukkit.Sound.BLOCK_WOOD_PLACE,.2f,2);return true;
    }
    private DrawerTransform(){}
}
