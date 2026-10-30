package com.cendreal.storagedrawers.event.inventory;
import com.cendreal.storagedrawers.module.menu.DrawerMenu;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
/** Converts Bukkit inventory events into the menu's validated transactions. */
public final class DrawerMenuListener implements Listener {
    private final DrawerMenu menus;
    public DrawerMenuListener(DrawerMenu menus){this.menus=menus;}
    @EventHandler public void click(InventoryClickEvent event){com.cendreal.storagedrawers.module.storage.StorageContext.run(event.getWhoClicked() instanceof org.bukkit.entity.Player player?player:null,com.cendreal.storagedrawers.api.StorageCause.PLAYER,()->menus.click(event));}
    @EventHandler public void drag(InventoryDragEvent event){com.cendreal.storagedrawers.module.storage.StorageContext.run(event.getWhoClicked() instanceof org.bukkit.entity.Player player?player:null,com.cendreal.storagedrawers.api.StorageCause.PLAYER,()->menus.drag(event));}
    @EventHandler public void close(InventoryCloseEvent event){menus.close(event);}
}
