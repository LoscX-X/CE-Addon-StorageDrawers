package com.cendreal.storagedrawers.event.player;
import com.cendreal.storagedrawers.module.portability.PortablePolicy;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
/** Coalesces inventory maintenance after successful player inventory transitions. */
public final class PortablePlayerListener implements Listener {
    private final JavaPlugin plugin;private final PortablePolicy policy;private final Set<UUID> pending=new HashSet<>();
    public PortablePlayerListener(JavaPlugin plugin,PortablePolicy policy){this.plugin=plugin;this.policy=policy;}
    private void queue(Player player){UUID id=player.getUniqueId();if(!pending.add(id))return;Bukkit.getScheduler().runTask(plugin,()->{pending.remove(id);var online=Bukkit.getPlayer(id);if(online!=null)policy.maintain(online);});}
    @EventHandler public void join(PlayerJoinEvent event){queue(event.getPlayer());}
    @EventHandler public void respawn(PlayerRespawnEvent event){queue(event.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void held(PlayerItemHeldEvent event){queue(event.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void swap(PlayerSwapHandItemsEvent event){queue(event.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void pickup(EntityPickupItemEvent event){if(event.getEntity() instanceof Player player)queue(player);}
    @EventHandler public void close(InventoryCloseEvent event){if(event.getPlayer() instanceof Player player)queue(player);}
    @EventHandler public void quit(PlayerQuitEvent event){pending.remove(event.getPlayer().getUniqueId());}
}
