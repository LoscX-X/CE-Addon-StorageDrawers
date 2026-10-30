package com.cendreal.storagedrawers.event.inventory;
import com.cendreal.storagedrawers.module.storage.DrawerController;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import org.bukkit.event.*;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
public final class DrawerCraftingListener implements Listener {
    @EventHandler(priority=EventPriority.HIGHEST) public void prepare(PrepareItemCraftEvent event){
        if(event.getRecipe() instanceof org.bukkit.Keyed keyed&&keyed.getKey().getNamespace().equals("storagedrawers")&&keyed.getKey().getKey().startsWith("add_upgrade_"))return;
        for(var item:event.getInventory().getMatrix())if(item!=null&&!item.isEmpty()&&BukkitAdaptor.adapt(item).hasTag(DrawerController.DATA_KEY)){event.getInventory().setResult(null);return;}
    }
}
