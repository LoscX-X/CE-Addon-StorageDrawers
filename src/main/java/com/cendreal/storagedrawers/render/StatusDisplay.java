package com.cendreal.storagedrawers.render;

import com.cendreal.storagedrawers.module.storage.DrawerController;
import net.momirealms.craftengine.bukkit.block.behavior.DrawerBlockBehavior;
import net.momirealms.craftengine.bukkit.block.entity.DrawerBlockEntityController;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import org.joml.Vector3f;
import java.util.*;

/** One composite virtual mesh for all locks, flags, missing drawers and fill bars. */
public final class StatusDisplay implements BlockEntityElement {
    private final DrawerController owner;private final DrawerBlockEntityController delegate;private String signature="";
    public StatusDisplay(DrawerController owner){this.owner=owner;var b=owner.behavior;delegate=DrawerBlockEntityController.create(owner.blockEntity(),new DrawerBlockBehavior(b.block(),null,null,false,new Vector3f(),new Vector3f(),new Vector3f(1),new Vector3f(0),1,false,false,b.facing,"storagedrawers:status",false));}
    public void sync(boolean notify){List<String> flags=new ArrayList<>();flags.add(owner.locked()?owner.claimed()?"both":"locked":owner.claimed()?"claimed":"");flags.add(owner.has("void_upgrade")?"true":"");flags.add(owner.concealed()?"true":"");flags.add(owner.suspended()?"true":"");flags.add(owner.magnetRange()[0]>0?"true":"");flags.add(owner.priority()==0?"":String.valueOf(owner.priority()));for(int i=0;i<owner.size();i++)flags.add(owner.missing(i)?"true":"");List<Float> levels=new ArrayList<>();for(int i=0;i<owner.size();i++)levels.add(owner.has("fill_level_upgrade")?(float)(Math.min(16,Math.floor(16.0*owner.count(i)/Math.max(1,owner.capacity(i))))/16.0):-1f);String next=flags+"/"+levels;if(next.equals(signature))return;signature=next;
        boolean present=flags.stream().anyMatch(s->!s.isEmpty())||owner.has("fill_level_upgrade");Item model=Item.empty();if(present){model=Item.byId(Key.of("minecraft:paper")).itemModel("storagedrawers:status/"+(owner.behavior.layout.halfDepth()?"half":"full")+"_"+owner.size());model.setJavaComponent("minecraft:custom_model_data",Map.of("strings",flags,"floats",levels));}delegate.setItem(0,model);delegate.refreshItemDisplayPacket();delegate.refreshTextContentPacket();if(notify)delegate.refreshDynamicElement((e,p)->{hide(p);show(p);});
    }
    @Override public void show(Player player){delegate.element.show(player);player.sendPacket(delegate.element.despawnTextPacket,false);}
    @Override public void hide(Player player){delegate.element.hide(player);}
    public void stateChanged(ImmutableBlockState state){delegate.preBlockStateChange(state);}
}
