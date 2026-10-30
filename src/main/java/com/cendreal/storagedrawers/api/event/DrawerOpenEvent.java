package com.cendreal.storagedrawers.api.event;
import com.cendreal.storagedrawers.api.StoragePosition;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
/** Fired after normal access checks and before either drawer or framing inventory opens. */
public final class DrawerOpenEvent extends Event implements Cancellable {
    public enum Kind{DRAWER,FRAMING_TABLE}
    private static final HandlerList HANDLERS=new HandlerList();private boolean cancelled;private final StoragePosition position;private final Player player;private final Kind kind;
    public DrawerOpenEvent(StoragePosition position,Player player,Kind kind){this.position=position;this.player=player;this.kind=kind;}
    public StoragePosition getPosition(){return position;}public Player getPlayer(){return player;}public Kind getKind(){return kind;}
    @Override public boolean isCancelled(){return cancelled;}@Override public void setCancelled(boolean value){cancelled=value;}@Override public HandlerList getHandlers(){return HANDLERS;}public static HandlerList getHandlerList(){return HANDLERS;}
}
