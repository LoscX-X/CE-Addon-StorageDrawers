package com.cendreal.storagedrawers.event.inventory;
import com.cendreal.storagedrawers.module.menu.FramingMenu;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
public final class FramingMenuListener implements Listener {
    private final FramingMenu menus;
    public FramingMenuListener(FramingMenu menus){this.menus=menus;}
    @EventHandler public void click(InventoryClickEvent event){com.cendreal.storagedrawers.module.storage.StorageContext.run(event.getWhoClicked() instanceof org.bukkit.entity.Player player?player:null,com.cendreal.storagedrawers.api.StorageCause.PLAYER,()->menus.click(event));}
    @EventHandler public void drag(InventoryDragEvent event){com.cendreal.storagedrawers.module.storage.StorageContext.run(event.getWhoClicked() instanceof org.bukkit.entity.Player player?player:null,com.cendreal.storagedrawers.api.StorageCause.PLAYER,()->menus.drag(event));}
    @EventHandler public void close(InventoryCloseEvent event){menus.close(event);}
}
