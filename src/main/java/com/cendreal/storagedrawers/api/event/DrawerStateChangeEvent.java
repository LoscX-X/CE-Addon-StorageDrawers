package com.cendreal.storagedrawers.api.event;
import com.cendreal.storagedrawers.api.*;
import org.bukkit.event.*;
public final class DrawerStateChangeEvent extends Event {
    private static final HandlerList HANDLERS=new HandlerList();private final StoragePosition position;private final StorageActor actor;private final DrawerSnapshot.State before,after;
    public DrawerStateChangeEvent(StoragePosition position,StorageActor actor,DrawerSnapshot.State before,DrawerSnapshot.State after){this.position=position;this.actor=actor;this.before=before;this.after=after;}
    public StoragePosition getPosition(){return position;}public StorageActor getActor(){return actor;}public DrawerSnapshot.State getBefore(){return before;}public DrawerSnapshot.State getAfter(){return after;}
    @Override public HandlerList getHandlers(){return HANDLERS;}public static HandlerList getHandlerList(){return HANDLERS;}
}
