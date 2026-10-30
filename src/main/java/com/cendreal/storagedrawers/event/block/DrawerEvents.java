package com.cendreal.storagedrawers.event.block;

import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;
import com.cendreal.storagedrawers.module.interaction.RingTools;

import com.cendreal.storagedrawers.module.storage.DrawerController;
import com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.event.*;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.util.Direction;
import org.bukkit.event.*;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public final class DrawerEvents implements Listener {
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void placementDefaults(CustomBlockPlaceEvent event){
        if(event.hand()!=InteractionHand.MAIN_HAND)return;var behavior=event.blockState().behavior().getFirst(DrawerBehavior.class);if(behavior==null||!behavior.layout.storage())return;
        String key=RingTools.effectiveKey(BukkitAdaptor.adapt(event.player().getInventory().getItemInOffHand()));if(!java.util.List.of("drawer_key","quantify_key","shroud_key","suspend_key").contains(key))return;
        var world=BukkitAdaptor.adapt(event.player().getWorld());var position=new net.momirealms.craftengine.core.world.BlockPos(event.bukkitBlock().getX(),event.bukkitBlock().getY(),event.bukkitBlock().getZ());var placed=behavior.at(world,position);if(!(placed instanceof DrawerController drawer))return;
        org.bukkit.Bukkit.getScheduler().runTask(com.cendreal.storagedrawers.StorageDrawersCE.instance(),()->{if(!drawer.blockEntity().isValid()||behavior.at(world,position)!=drawer)return;boolean active=switch(key){case "drawer_key"->drawer.locked();case "quantify_key"->drawer.quantity();case "shroud_key"->drawer.concealed();default->drawer.suspended();};if(!active)drawer.useKey(key,BukkitAdaptor.adapt(event.player()));});
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void leftClick(CustomBlockInteractEvent event) {
        if(event.action()!=CustomBlockInteractEvent.Action.LEFT_CLICK || event.hand()!=InteractionHand.MAIN_HAND || !(event.blockState().behavior().getFirst(DrawerBehavior.class) instanceof DrawerBehavior behavior))return;
        var node=behavior.at(BukkitAdaptor.adapt(event.player().getWorld()),new net.momirealms.craftengine.core.world.BlockPos(event.bukkitBlock().getX(),event.bukkitBlock().getY(),event.bukkitBlock().getZ()));
        if(!(node instanceof DrawerController drawer))return;
        if(com.cendreal.storagedrawers.StorageDrawersCE.instance().compatibility().inspect(event.player(),event.bukkitBlock().getLocation())){event.setCancelled(true);return;}
        var pos=event.bukkitBlock();var face=Direction.valueOf(event.clickedFace().name());
        if(face!=event.blockState().get(behavior.facing))return;
        event.setCancelled(true);
        if(!DrawerInteraction.allowed(BukkitAdaptor.adapt(event.player()),drawer))return;
        var hit=event.interactionPoint();
        if(hit==null){var ray=event.player().rayTraceBlocks(6,org.bukkit.FluidCollisionMode.NEVER);if(ray!=null&&pos.equals(ray.getHitBlock()))hit=ray.getHitPosition().toLocation(pos.getWorld());}
        int slot=hit==null?(drawer.size()==1?0:-1):behavior.layout.slot(event.blockState().get(behavior.facing),face,hit.getX()-pos.getX(),hit.getY()-pos.getY(),hit.getZ()-pos.getZ());
        if(slot<0)return;
        var player=event.player();var item=drawer.take(slot,player.isSneaking()?drawer.template(slot).maxStackSize():1,BukkitAdaptor.adapt(player));
        if(!item.isEmpty())player.getInventory().addItem((ItemStack)item.platformItem()).values().forEach(left->player.getWorld().dropItemNaturally(player.getLocation(),left));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void breakBlock(CustomBlockBreakEvent event) {
        DrawerBehavior behavior=event.blockState().behavior().getFirst(DrawerBehavior.class);
        if(behavior==null)return;
        var node=behavior.at(BukkitAdaptor.adapt(event.getPlayer().getWorld()),new net.momirealms.craftengine.core.world.BlockPos(event.bukkitBlock().getX(),event.bukkitBlock().getY(),event.bukkitBlock().getZ()));
        if(node instanceof DrawerController drawer && !drawer.canAccess(event.player()))event.setCancelled(true);
    }
}
