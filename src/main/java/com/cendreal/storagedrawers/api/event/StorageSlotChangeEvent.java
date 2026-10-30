package com.cendreal.storagedrawers.api.event;
import com.cendreal.storagedrawers.api.*;
import org.bukkit.event.*;
import org.bukkit.inventory.ItemStack;
/** Committed storage change, including automation and balancing. Counts are separate from item stack limits.
 * Compacting contents account their shared inventory once, in smallest units at the last slot;
 * other denomination slots report prototype changes. Use DrawerAccess for visible denomination counts. */
public final class StorageSlotChangeEvent extends Event {
    public enum Kind{CONTENTS,UPGRADE,FRAMING_INPUT}
    private static final HandlerList HANDLERS=new HandlerList();
    private final StoragePosition position;private final StorageActor actor;private final Kind kind;private final int slot;private final ItemStack before,after;private final long beforeCount,afterCount;
    public StorageSlotChangeEvent(StoragePosition position,StorageActor actor,Kind kind,int slot,ItemStack before,long beforeCount,ItemStack after,long afterCount){this.position=position;this.actor=actor;this.kind=kind;this.slot=slot;this.before=before.clone();this.after=after.clone();this.beforeCount=beforeCount;this.afterCount=afterCount;}
    public StoragePosition getPosition(){return position;}public StorageActor getActor(){return actor;}public Kind getKind(){return kind;}public int getSlot(){return slot;}public ItemStack getBeforeItem(){return before.clone();}public ItemStack getAfterItem(){return after.clone();}public long getBeforeCount(){return beforeCount;}public long getAfterCount(){return afterCount;}
    @Override public HandlerList getHandlers(){return HANDLERS;}public static HandlerList getHandlerList(){return HANDLERS;}
}
