package com.cendreal.storagedrawers.api.event;
import com.cendreal.storagedrawers.api.StoragePosition;
import org.bukkit.NamespacedKey;
import org.bukkit.event.*;
public final class DrawerLifecycleEvent extends Event {
    public enum Action{LOAD,UNLOAD,REMOVE}
    private static final HandlerList HANDLERS=new HandlerList();private final StoragePosition position;private final NamespacedKey blockId;private final Action action;
    public DrawerLifecycleEvent(StoragePosition position,NamespacedKey blockId,Action action){this.position=position;this.blockId=blockId;this.action=action;}
    public StoragePosition getPosition(){return position;}public NamespacedKey getBlockId(){return blockId;}public Action getAction(){return action;}
    @Override public HandlerList getHandlers(){return HANDLERS;}public static HandlerList getHandlerList(){return HANDLERS;}
}
