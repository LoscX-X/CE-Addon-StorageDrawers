package com.cendreal.storagedrawers.compatibility.coreprotect;
import com.cendreal.storagedrawers.api.event.StorageSlotChangeEvent;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.coreprotect.config.Config;
import net.coreprotect.config.ConfigHandler;
import net.coreprotect.listener.player.PlayerInteractEntityListener;
import net.coreprotect.consumer.Queue;
import net.coreprotect.utility.HopperTransactionUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.inventory.ItemStack;
/** Uses the specified-inventory protocol employed by CE's own CoreProtect bridge. */
public final class CoreProtectCompatibility implements Listener {
    private final CoreProtectAPI api;
    public CoreProtectCompatibility(CoreProtect plugin){api=plugin.getAPI();if(api.APIVersion()<10)throw new IllegalStateException("CoreProtect API 10 or later is required");}
    public boolean enabled(){return api.isEnabled();}
    public boolean inspecting(Player player){return enabled()&&player.hasPermission("coreprotect.inspect")&&Boolean.TRUE.equals(ConfigHandler.inspecting.get(player.getName()));}
    public boolean inspect(Player player,Location location){if(!inspecting(player))return false;new net.coreprotect.listener.player.inspector.ContainerInspector().performContainerLookup(player,location);return true;}
    @EventHandler(priority=EventPriority.MONITOR) public void changed(StorageSlotChangeEvent event){
        if(!enabled()||event.getBeforeCount()==0&&event.getAfterCount()==0)return;var optional=event.getPosition().location();if(optional.isEmpty())return;var location=optional.get();if(!Config.getConfig(location.getWorld()).ITEM_TRANSACTIONS)return;
        ItemStack before=amount(event.getBeforeItem(),event.getBeforeCount()),after=amount(event.getAfterItem(),event.getAfterCount());
        // CoreProtect normally coalesces same-location container snapshots. A drawer
        // event describes one committed cell, so retain each exact before/after pair.
        // Its own per-location lock also excludes the asynchronous consumer here.
        String user=event.getActor().name();
        HopperTransactionUtils.synchronizeTransaction(HopperTransactionUtils.getTransactionId(location),()->{
            Queue.addForceContainer(HopperTransactionUtils.getLoggingId(user,location),new ItemStack[]{after});
            PlayerInteractEntityListener.queueContainerSpecifiedItems(user,Material.JUKEBOX,new Object[]{new ItemStack[]{before},new ItemStack[]{after}},location,false);
        });
    }
    private static ItemStack amount(ItemStack item,long count){if(count<=0||item.isEmpty())return new ItemStack(Material.AIR);item.setAmount(Math.toIntExact(count));return item;}
}
