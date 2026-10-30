package com.cendreal.storagedrawers.event.inventory;

import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;
import com.cendreal.storagedrawers.module.interaction.RingTools;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class RingEvents implements Listener {
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true)public void click(InventoryClickEvent event){if(!(event.getWhoClicked() instanceof Player player)||event.getClickedInventory()!=player.getInventory()||event.getClick()!=ClickType.RIGHT)return;ItemStack stack=event.getCurrentItem();if(stack==null||stack.getAmount()!=1)return;var ring=BukkitAdaptor.adapt(stack);if(!DrawerInteraction.tool(ring).startsWith("keyring"))return;
        var keys=RingTools.contents(ring);ItemStack cursor=event.getCursor();if(cursor.isEmpty()){if(keys.isEmpty())return;event.setCancelled(true);event.setCursor((ItemStack)keys.removeFirst().platformItem());event.setCurrentItem((ItemStack)RingTools.create(keys).platformItem());return;}
        var key=BukkitAdaptor.adapt(cursor);String id=DrawerInteraction.tool(key);if(!RingTools.isKey(id)||keys.stream().anyMatch(k->DrawerInteraction.tool(k).equals(id)))return;event.setCancelled(true);keys.add(key.copyWithCount(1));event.setCurrentItem((ItemStack)RingTools.create(keys).platformItem());cursor=cursor.clone();cursor.setAmount(cursor.getAmount()-1);event.setCursor(cursor);
    }
}
