package com.cendreal.storagedrawers.api;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import java.util.*;
/** A stable block address. Resolving its world never requests a chunk. */
public record StoragePosition(UUID worldId,int x,int y,int z) {
    public StoragePosition{Objects.requireNonNull(worldId,"worldId");}
    public static StoragePosition of(Location location){Objects.requireNonNull(location.getWorld(),"location.world");return new StoragePosition(location.getWorld().getUID(),location.getBlockX(),location.getBlockY(),location.getBlockZ());}
    public Optional<Location> location(){var world=Bukkit.getWorld(worldId);return world==null?Optional.empty():Optional.of(new Location(world,x,y,z));}
}
