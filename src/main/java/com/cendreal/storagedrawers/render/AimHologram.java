package com.cendreal.storagedrawers.render;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior;
import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;
import com.cendreal.storagedrawers.module.storage.DrawerController;
import com.mojang.math.Transformation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryType;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/** Per-viewer packet displays. They are never added to a world or its entity ticker. */
public final class AimHologram {
    private final StorageDrawersCE plugin;
    private final Map<UUID,View> views=new HashMap<>();
    private boolean enabled;
    private double distance=6;
    public AimHologram(StorageDrawersCE plugin){this.plugin=plugin;}
    public void reload(){clear();enabled=plugin.getConfig().getBoolean("aim-hologram.enabled",false);distance=Math.clamp(plugin.getConfig().getDouble("aim-hologram.distance",6),1,16);}
    public void tick(){if(enabled)for(Player player:Bukkit.getOnlinePlayers())refresh(player);}
    public void refresh(Player player){
        DrawerController target=target(player);View current=views.get(player.getUniqueId());
        if(target==null){hide(player);return;}
        if(current==null||current.drawer!=target){hide(player);current=new View(target,player);views.put(player.getUniqueId(),current);}
        current.refresh(player);
    }
    private DrawerController target(Player player){
        var inventoryType=player.getOpenInventory().getType();
        if(!enabled||player.isDead()||!player.hasPermission("storagedrawers.aim")||inventoryType!=InventoryType.CRAFTING&&inventoryType!=InventoryType.CREATIVE)return null;
        var hit=player.rayTraceBlocks(distance,FluidCollisionMode.NEVER);
        if(hit==null||hit.getHitBlock()==null)return null;
        var block=hit.getHitBlock();if(!block.getWorld().isChunkLoaded(block.getX()>>4,block.getZ()>>4))return null;
        var state=CraftEngineBlocks.getCustomBlockState(block);if(state==null)return null;
        var behavior=state.behavior().getFirst(DrawerBehavior.class);if(behavior==null||!behavior.layout.storage())return null;
        var node=behavior.at(BukkitAdaptor.adapt(block.getWorld()),new BlockPos(block.getX(),block.getY(),block.getZ()));
        return node instanceof DrawerController drawer&&!drawer.concealed()&&DrawerInteraction.allowed(BukkitAdaptor.adapt(player),drawer)?drawer:null;
    }
    public void hide(Player player){View previous=views.remove(player.getUniqueId());if(previous!=null)previous.hide(player);}
    public void clear(){for(var entry:views.entrySet()){Player player=Bukkit.getPlayer(entry.getKey());if(player!=null)entry.getValue().hide(player);}views.clear();}
    public int viewers(){return views.size();}

    private static Transformation transform(float scale,float x,float y){return new Transformation(new Vector3f(x,y,0),new Quaternionf(),new Vector3f(scale),new Quaternionf());}
    private static void send(Player player,net.minecraft.network.protocol.Packet<?> packet){((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle().connection.send(packet);}
    private static void spawn(Player player,Display display){
        send(player,new ClientboundAddEntityPacket(display.getId(),display.getUUID(),display.getX(),display.getY(),display.getZ(),0,0,display.getType(),0,net.minecraft.world.phys.Vec3.ZERO,0));
        var metadata=display.getEntityData().getNonDefaultValues();if(metadata!=null)send(player,new ClientboundSetEntityDataPacket(display.getId(),metadata));
        display.getEntityData().packDirty();
    }
    private static void update(Player player,Display display){var metadata=display.getEntityData().packDirty();if(metadata!=null&&!metadata.isEmpty())send(player,new ClientboundSetEntityDataPacket(display.getId(),metadata));}
    private static void prepare(Display display,float size,float x,float y){display.setBillboardConstraints(Display.BillboardConstraints.CENTER);display.setTransformation(transform(size,x,y));display.setBrightnessOverride(new net.minecraft.util.Brightness(15,15));display.setShadowRadius(0);display.setViewRange(1);}
    private static final class Row {
        final Display.ItemDisplay icon;
        final Display.TextDisplay text;
        Item previous;
        int count=Integer.MIN_VALUE;
        boolean previousMissing,infinite;
        boolean spawned;
        Row(org.bukkit.World world,float rowY){var level=((CraftWorld)world).getHandle();icon=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);text=new Display.TextDisplay(EntityTypes.TEXT_DISPLAY,level);prepare(icon,.27f,-.65f,rowY+.09f);prepare(text,.7f,.13f,rowY);icon.setItemTransform(ItemDisplayContext.FIXED);text.setFlags((byte)0);text.getEntityData().set(Display.TextDisplay.DATA_BACKGROUND_COLOR_ID,0);text.getEntityData().set(Display.TextDisplay.DATA_LINE_WIDTH_ID,200);}
        void refresh(Player player,Item template,int next,boolean missing,boolean vending){
            boolean itemChanged=previous!=template;
            if(itemChanged){previous=template;icon.setItemStack(template.isEmpty()?net.minecraft.world.item.ItemStack.EMPTY:((net.minecraft.world.item.ItemStack)template.minecraftItem()).copyWithCount(1));}
            if(itemChanged||count!=next||previousMissing!=missing||infinite!=vending){
                count=next;previousMissing=missing;infinite=vending;
                Component label=missing?Component.literal("未安装抽屉"):template.isEmpty()?Component.literal("空抽屉"):((net.minecraft.world.item.ItemStack)template.minecraftItem()).getHoverName().copy().append(Component.literal(" × "+(vending?"∞":Integer.toString(next))).withStyle(net.minecraft.ChatFormatting.GOLD));
                text.setText(label);
            }
            if(!spawned){spawn(player,icon);spawn(player,text);spawned=true;}else{update(player,icon);update(player,text);}
        }
        void position(double x,double y,double z){icon.setPos(x,y,z);text.setPos(x,y,z);}
    }
    private static final class View {
        final DrawerController drawer;
        final Row[] rows;
        // Minecraft rotates the local translations with CENTER every rendered frame.
        // All parts share an anchor; camera movement needs no server position updates.
        View(DrawerController drawer,Player player){this.drawer=drawer;rows=new Row[drawer.size()];var pos=drawer.blockEntity().pos;for(int i=0;i<rows.length;i++){rows[i]=new Row(player.getWorld(),(rows.length-1-i)*.28f);rows[i].position(pos.x+.5,pos.y+1.3,pos.z+.5);}}
        void refresh(Player player){
            for(int i=0;i<rows.length;i++)rows[i].refresh(player,drawer.template(i),drawer.count(i),drawer.missing(i),drawer.has("creative_vending_upgrade"));
        }
        void hide(Player player){int[] ids=new int[rows.length*2];for(int i=0;i<rows.length;i++){ids[i*2]=rows[i].icon.getId();ids[i*2+1]=rows[i].text.getId();}send(player,new ClientboundRemoveEntitiesPacket(ids));}
    }
}
