package com.cendreal.storagedrawers.module.service;
import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.api.*;
import com.cendreal.storagedrawers.module.storage.*;
import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;
import com.cendreal.storagedrawers.module.automation.NetworkController;
import com.cendreal.storagedrawers.module.frame.FramingController;
import com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior;
import net.momirealms.craftengine.bukkit.api.*;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

/** All external operations resolve live storage through the same loaded-node boundary. */
public final class DefaultStorageDrawersApi implements StorageDrawersApi {
    private final StorageDrawersCE plugin;
    public DefaultStorageDrawersApi(StorageDrawersCE plugin){this.plugin=plugin;}
    void thread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("StorageDrawers API requires the server thread");if(!plugin.isEnabled())throw new IllegalStateException("CE-Addon-StorageDrawers is not enabled");}
    NodeController node(Location location){thread();Objects.requireNonNull(location,"location");var world=location.getWorld();if(world==null||!world.isChunkLoaded(location.getBlockX()>>4,location.getBlockZ()>>4))return null;var state=CraftEngineBlocks.getCustomBlockState(location.getBlock());if(state==null)return null;var behavior=state.behavior().getFirst(DrawerBehavior.class);return behavior==null?null:behavior.at(BukkitAdaptor.adapt(world),new BlockPos(location.getBlockX(),location.getBlockY(),location.getBlockZ()));}
    @Override public Optional<DrawerSnapshot> drawer(Location location){return node(location) instanceof DrawerController drawer?Optional.of(StorageEvents.snapshot(drawer)):Optional.empty();}
    @Override public Optional<DrawerAccess> getDrawer(Location location){return node(location) instanceof DrawerController?Optional.of(new DefaultDrawerAccess(this,StoragePosition.of(location))):Optional.empty();}
    @Override public Optional<FramingTableAccess> getFramingTable(Location location){if(!(node(location) instanceof FramingController part))return Optional.empty();var table=part.primaryController();return table==null?Optional.empty():Optional.of(new DefaultFramingTableAccess(this,StorageEvents.position(table)));}
    @Override public Optional<ItemStack> createItem(NamespacedKey id,int amount){thread();if(id==null||!id.getNamespace().equals("storagedrawers")||amount<=0)return Optional.empty();var key=net.momirealms.craftengine.core.util.Key.of(id.toString());if(!CraftEngineItems.loadedItems().containsKey(key))return Optional.empty();var item=net.momirealms.craftengine.core.item.Item.byId(key);return amount>item.maxStackSize()?Optional.empty():Optional.of(StorageEvents.stack(item.count(amount)));}
    @Override public List<NamespacedKey> getItemIds(){thread();return CraftEngineItems.loadedItems().keySet().stream().filter(k->k.namespace().equals("storagedrawers")).map(k->NamespacedKey.fromString(k.toString())).sorted(java.util.Comparator.comparing(NamespacedKey::toString)).toList();}
    @Override public List<DrawerSnapshot> loadedDrawers(World world){thread();return plugin.networks().loadedDrawers(world.getUID()).stream().map(StorageEvents::snapshot).toList();}
    @Override public Optional<NetworkSnapshot> network(Location location){var node=node(location);if(node==null||node.behavior.layout.kind().equals("table")||node.behavior.layout.kind().equals("button"))return Optional.empty();var view=plugin.networks().view(node);return view.controller()==null?Optional.empty():Optional.of(new NetworkSnapshot(StorageEvents.position(view.controller()),view.drawers().stream().map(StorageEvents::position).toList(),view.revision()));}
    private boolean allowed(DrawerController drawer,Player actor){return actor==null?!drawer.suspended():DrawerInteraction.allowed(BukkitAdaptor.adapt(actor),drawer);}
    @Override public int insert(Location location,int slot,ItemStack item,int amount,Player actor){var node=node(location);if(!(node instanceof DrawerController drawer)||slot<0||slot>=drawer.size()||item==null||item.isEmpty()||amount<=0||!allowed(drawer,actor))return 0;return StorageContext.run(actor,StorageCause.API,()->drawer.put(slot,BukkitAdaptor.adapt(item.clone()),amount,actor==null?null:BukkitAdaptor.adapt(actor)));}
    @Override public ItemStack extract(Location location,int slot,int amount,Player actor){var node=node(location);if(!(node instanceof DrawerController drawer)||slot<0||slot>=drawer.size()||amount<=0||!allowed(drawer,actor))return new ItemStack(Material.AIR);return StorageContext.run(actor,StorageCause.API,()->StorageEvents.stack(drawer.take(slot,amount,actor==null?null:BukkitAdaptor.adapt(actor))));}
    @Override public boolean open(Player player,Location location){var node=node(location);if(node==null||player==null||player.getWorld()!=location.getWorld()||player.getLocation().distanceSquared(location.clone().add(.5,.5,.5))>64||!DrawerInteraction.allowed(BukkitAdaptor.adapt(player),node))return false;var before=player.getOpenInventory().getTopInventory();if(node instanceof DrawerController drawer)plugin.menus().open(player,drawer);else if(node instanceof FramingController table)plugin.framingMenus().open(player,table);else return false;return player.getOpenInventory().getTopInventory()!=before&&player.getOpenInventory().getTopInventory().getSize()==45;}
    @Override public boolean setSetting(Location location,Setting setting,boolean enabled,Player actor){Objects.requireNonNull(setting,"setting");return getDrawer(location).map(drawer->drawer.setSetting(setting,enabled,actor)).orElse(false);}
}
