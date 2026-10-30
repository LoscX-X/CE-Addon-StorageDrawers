package com.cendreal.storagedrawers.api;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
/** Bound to the primary half. Inputs 0..3: target, side, trim, front. */
public interface FramingTableAccess {
    StoragePosition getPosition();boolean isAvailable();ItemStack getInput(int slot);ItemStack getResult();
    boolean setInput(int slot,ItemStack item,Player actor);
    /** Consumes table inputs and returns the crafted stack, never inserts it into a player inventory. */
    ItemStack craft(int amount,Player actor);
    default boolean setInput(int slot,ItemStack item){return setInput(slot,item,null);}
    default ItemStack craft(int amount){return craft(amount,null);}
}
