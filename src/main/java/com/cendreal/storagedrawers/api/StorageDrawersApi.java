package com.cendreal.storagedrawers.api;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;
/**
 * Bukkit-only integration boundary. Every operation runs on the server thread and
 * accesses loaded chunks only. Transfers do not modify the caller's inventory.
 */
public interface StorageDrawersApi {
    int API_VERSION=1;
    static StorageDrawersApi get(){return Bukkit.getServicesManager().load(StorageDrawersApi.class);}
    Optional<DrawerSnapshot> drawer(Location location);
    Optional<DrawerAccess> getDrawer(Location location);
    Optional<FramingTableAccess> getFramingTable(Location location);
    Optional<ItemStack> createItem(NamespacedKey id,int amount);
    List<NamespacedKey> getItemIds();
    List<DrawerSnapshot> loadedDrawers(World world);
    Optional<NetworkSnapshot> network(Location location);
    /** actor==null uses automation rules; player access uses existing ownership/protection checks. */
    int insert(Location location,int slot,ItemStack item,int amount,Player actor);
    ItemStack extract(Location location,int slot,int amount,Player actor);
    boolean open(Player player,Location location);
    boolean setSetting(Location location,Setting setting,boolean enabled,Player actor);
    enum Setting { LOCKED, QUANTITY, CONCEALED, SUSPENDED }
}
