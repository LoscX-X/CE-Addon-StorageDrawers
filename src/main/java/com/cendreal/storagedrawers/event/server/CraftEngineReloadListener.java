package com.cendreal.storagedrawers.event.server;
import com.cendreal.storagedrawers.lifecycle.StorageDrawersLifecycle;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import org.bukkit.event.*;
public final class CraftEngineReloadListener implements Listener {
    private final StorageDrawersLifecycle lifecycle;
    public CraftEngineReloadListener(StorageDrawersLifecycle lifecycle){this.lifecycle=lifecycle;}
    @EventHandler public void reload(CraftEngineReloadEvent event){lifecycle.reload();}
}
