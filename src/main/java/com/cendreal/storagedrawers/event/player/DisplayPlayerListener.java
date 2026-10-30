package com.cendreal.storagedrawers.event.player;
import com.cendreal.storagedrawers.render.AimHologram;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.PlayerDeathEvent;
public final class DisplayPlayerListener implements Listener {
    private final AimHologram display;
    public DisplayPlayerListener(AimHologram display){this.display=display;}
    @EventHandler public void quit(PlayerQuitEvent event){display.hide(event.getPlayer());}
    @EventHandler public void world(PlayerChangedWorldEvent event){display.hide(event.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void teleport(PlayerTeleportEvent event){display.hide(event.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void mode(PlayerGameModeChangeEvent event){display.hide(event.getPlayer());}
    @EventHandler public void death(PlayerDeathEvent event){display.hide(event.getEntity());}
}
