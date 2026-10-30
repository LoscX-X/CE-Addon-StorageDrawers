package com.cendreal.storagedrawers.api;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;
/** Position-bound access; no retained controller or forced chunk load. Absolute setters
 * deliberately overwrite storage, never the caller's inventory. A null setter actor is
 * a trusted server operation; supplied players retain ownership/protection checks.
 * Getters throw when unavailable; setters return false. */
public interface DrawerAccess {
    StoragePosition getPosition();boolean isAvailable();DrawerSnapshot getSnapshot();
    NamespacedKey getBlockId();int getSlotCount();ItemStack getItem(int slot);int getCount(int slot);int getCapacity(int slot);boolean isMissing(int slot);
    ItemStack getUpgrade(int slot);Optional<UUID> getOwner();int getPriority();DrawerSnapshot.Frame getFrame();
    boolean isLocked();boolean isQuantityVisible();boolean isConcealed();boolean isSuspended();
    boolean setItem(int slot,ItemStack item,int count,Player actor);boolean setCount(int slot,int count,Player actor);
    /** Rebuild the compacting group from one seed denomination; setItem only edits its existing chain. */
    boolean setCompactingContents(ItemStack seed,int count,Player actor);
    boolean setUpgrade(int slot,ItemStack upgrade,Player actor);boolean setOwner(UUID owner,Player actor);boolean setPriority(int priority,Player actor);
    boolean setFrame(ItemStack side,ItemStack trim,ItemStack front,Player actor);
    boolean setSetting(StorageDrawersApi.Setting setting,boolean enabled,Player actor);
    ItemStack detach(int slot,Player actor);
    /** On success the caller consumes one detached item; this argument is not mutated. */
    boolean attach(int slot,ItemStack detached,Player actor);
    default boolean setItem(int slot,ItemStack item,int count){return setItem(slot,item,count,null);}
    default boolean setCount(int slot,int count){return setCount(slot,count,null);}
    default boolean setCompactingContents(ItemStack seed,int count){return setCompactingContents(seed,count,null);}
    default boolean setUpgrade(int slot,ItemStack upgrade){return setUpgrade(slot,upgrade,null);}
    default boolean setOwner(UUID owner){return setOwner(owner,null);}
    default boolean setPriority(int priority){return setPriority(priority,null);}
    default boolean setFrame(ItemStack side,ItemStack trim,ItemStack front){return setFrame(side,trim,front,null);}
    default boolean setLocked(boolean value){return setSetting(StorageDrawersApi.Setting.LOCKED,value,null);}
    default boolean setQuantityVisible(boolean value){return setSetting(StorageDrawersApi.Setting.QUANTITY,value,null);}
    default boolean setConcealed(boolean value){return setSetting(StorageDrawersApi.Setting.CONCEALED,value,null);}
    default boolean setSuspended(boolean value){return setSetting(StorageDrawersApi.Setting.SUSPENDED,value,null);}
}
