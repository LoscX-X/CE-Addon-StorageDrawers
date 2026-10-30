package com.cendreal.storagedrawers.render;

import com.cendreal.storagedrawers.module.storage.NodeController;
import net.momirealms.craftengine.bukkit.block.behavior.DrawerBlockBehavior;
import net.momirealms.craftengine.bukkit.block.entity.DrawerBlockEntityController;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import org.joml.Vector3f;

/** Three batched facade meshes at most, culled/tracked by CE, updated only when framing changes. */
public final class FrameDisplay implements BlockEntityElement {
    private final NodeController owner;private final int role;private final DrawerBlockEntityController delegate;
    public FrameDisplay(NodeController owner,int role){this.owner=owner;this.role=role;var behavior=owner.behavior;delegate=DrawerBlockEntityController.create(owner.blockEntity(),new DrawerBlockBehavior(behavior.block(),null,null,false,new Vector3f(),new Vector3f(),new Vector3f(2.002f),new Vector3f(0),1,false,false,behavior.facing,"storagedrawers:facade_"+role,false));stateChanged(owner.blockEntity().blockState);}
    public void sync(boolean notify){String material=owner.frame().material(role);boolean supported=role<2||!owner.behavior.layout.kind().equals("trim")&&!owner.behavior.layout.kind().equals("io");Item model=Item.empty();if(supported&&!material.isEmpty()){model=Item.byId(Key.of("minecraft:paper"));model.itemModel("storagedrawers:frame/"+owner.behavior.block().id().value()+"/"+new String[]{"side","trim","front"}[role]);model.setJavaComponent("minecraft:custom_model_data",java.util.Map.of("strings",java.util.List.of(material)));}delegate.setItem(0,model);delegate.refreshItemDisplayPacket();delegate.refreshTextContentPacket();if(notify)delegate.refreshDynamicElement((e,p)->{hide(p);show(p);});}
    @Override public void show(Player player){delegate.element.show(player);player.sendPacket(delegate.element.despawnTextPacket,false);}
    @Override public void hide(Player player){delegate.element.hide(player);}
    public void stateChanged(ImmutableBlockState state){var e=owner.blockEntity();var center=new net.momirealms.craftengine.core.world.WorldPosition(null,e.pos.x+.5,e.pos.y+.5,e.pos.z+.5);delegate.element.refreshSpawnItemAndTextPacket(center,center,delegate.calculateYRot(state));if(e.world!=null)delegate.refreshDynamicElement((element,player)->element.updateElementPos(player));}
}
