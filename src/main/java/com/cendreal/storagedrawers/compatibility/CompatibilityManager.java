package com.cendreal.storagedrawers.compatibility;
import com.cendreal.storagedrawers.compatibility.coreprotect.CoreProtectCompatibility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.server.*;
import org.bukkit.plugin.java.JavaPlugin;
public final class CompatibilityManager implements Listener {
    private final JavaPlugin plugin;private CoreProtectCompatibility coreProtect;
    public CompatibilityManager(JavaPlugin plugin){this.plugin=plugin;}
    public void reload(){close();var candidate=Bukkit.getPluginManager().getPlugin("CoreProtect");if(!plugin.getConfig().getBoolean("compatibility.coreprotect.enabled",true)||candidate==null||!candidate.isEnabled())return;
        try{coreProtect=new CoreProtectCompatibility((net.coreprotect.CoreProtect)candidate);Bukkit.getPluginManager().registerEvents(coreProtect,plugin);plugin.getLogger().info("CoreProtect container logging connected");}catch(LinkageError|RuntimeException failure){coreProtect=null;plugin.getLogger().warning("CoreProtect integration unavailable: "+failure);}
    }
    public boolean coreProtectEnabled(){return coreProtect!=null&&coreProtect.enabled();}
    public boolean inspecting(Player player){return coreProtect!=null&&coreProtect.inspecting(player);}
    public boolean inspect(Player player,Location location){return coreProtect!=null&&coreProtect.inspect(player,location);}
    @EventHandler public void enabled(PluginEnableEvent event){if(event.getPlugin().getName().equals("CoreProtect"))reload();}
    @EventHandler public void disabled(PluginDisableEvent event){if(event.getPlugin().getName().equals("CoreProtect"))close();}
    public void close(){if(coreProtect!=null){HandlerList.unregisterAll(coreProtect);coreProtect=null;}}
}
