package com.cendreal.storagedrawers.event.inventory;
import com.cendreal.storagedrawers.module.portability.PortablePolicy;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
public final class PortableInventoryListener implements Listener {
    private final PortablePolicy policy;
    public PortableInventoryListener(PortablePolicy policy){this.policy=policy;}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void click(InventoryClickEvent event){policy.click(event);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void drag(InventoryDragEvent event){policy.drag(event);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void move(InventoryMoveItemEvent event){policy.move(event);}
}
