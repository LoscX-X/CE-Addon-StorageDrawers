package com.cendreal.storagedrawers.event.server;
import com.cendreal.storagedrawers.module.automation.NetworkIndex;
import org.bukkit.event.*;
import org.bukkit.event.world.WorldUnloadEvent;
public final class StorageWorldListener implements Listener {
    private final NetworkIndex networks;
    public StorageWorldListener(NetworkIndex networks){this.networks=networks;}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void unload(WorldUnloadEvent event){networks.unloadWorld(event.getWorld().getUID());}
}
