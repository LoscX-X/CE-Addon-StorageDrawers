package com.cendreal.storagedrawers.module.storage;
import com.cendreal.storagedrawers.api.*;
import org.bukkit.entity.Player;
import java.util.function.Supplier;
/** Scoped attribution for nested synchronous transactions, including network balancing. */
public final class StorageContext {
    private static final ThreadLocal<StorageActor> CURRENT=new ThreadLocal<>();
    public static boolean scoped(){return CURRENT.get()!=null;}
    public static StorageActor actor(net.momirealms.craftengine.core.entity.player.Player explicit){var current=CURRENT.get();if(explicit!=null)return new StorageActor(explicit.uuid(),explicit.name(),current==null?StorageCause.PLAYER:current.cause());return current==null?new StorageActor(null,"#CE-Addon-StorageDrawers",StorageCause.AUTOMATION):current;}
    public static <T>T run(Player player,StorageCause cause,Supplier<T> task){var previous=CURRENT.get();CURRENT.set(new StorageActor(player==null?null:player.getUniqueId(),player==null?"#CE-Addon-StorageDrawers":player.getName(),cause));try{return task.get();}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}}
    public static void run(Player player,StorageCause cause,Runnable task){run(player,cause,()->{task.run();return null;});}
    private StorageContext(){}
}
