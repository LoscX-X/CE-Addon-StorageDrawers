package com.cendreal.storagedrawers.api.event;
import com.cendreal.storagedrawers.api.*;
import org.bukkit.event.*;
import org.bukkit.inventory.ItemStack;
/** Player/API attempt hook before mutation. Native automation uses Bukkit inventory move/pickup events.
 * Cancellation accepts/extracts zero; it does not undo earlier transactions. */
public final class DrawerTransferEvent extends Event implements Cancellable {
    public enum Direction{INSERT,EXTRACT,SET}
    private static final HandlerList HANDLERS=new HandlerList();private boolean cancelled;
    private final StoragePosition position;private final StorageActor actor;private final int slot,amount;private final ItemStack item;private final Direction direction;
    public DrawerTransferEvent(StoragePosition position,StorageActor actor,int slot,ItemStack item,int amount,Direction direction){this.position=position;this.actor=actor;this.slot=slot;this.item=item.clone();this.amount=amount;this.direction=direction;}
    public StoragePosition getPosition(){return position;}public StorageActor getActor(){return actor;}public int getSlot(){return slot;}public ItemStack getItem(){return item.clone();}public int getAmount(){return amount;}public Direction getDirection(){return direction;}
    @Override public boolean isCancelled(){return cancelled;}@Override public void setCancelled(boolean value){cancelled=value;}@Override public HandlerList getHandlers(){return HANDLERS;}public static HandlerList getHandlerList(){return HANDLERS;}
}
