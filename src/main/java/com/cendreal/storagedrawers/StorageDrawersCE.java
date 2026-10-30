package com.cendreal.storagedrawers;
import com.cendreal.storagedrawers.lifecycle.StorageDrawersLifecycle;
import com.cendreal.storagedrawers.api.StorageDrawersApi;
import com.cendreal.storagedrawers.module.automation.NetworkIndex;
import com.cendreal.storagedrawers.module.storage.*;
import com.cendreal.storagedrawers.module.menu.*;
import com.cendreal.storagedrawers.render.AimHologram;
import com.cendreal.storagedrawers.compatibility.CompatibilityManager;
import org.bukkit.plugin.java.JavaPlugin;
/** Paper entry point. The lifecycle owns feature wiring and cleanup. */
public final class StorageDrawersCE extends JavaPlugin {
    private static StorageDrawersCE instance;
    private final StorageDrawersLifecycle lifecycle=new StorageDrawersLifecycle(this);
    public static StorageDrawersCE instance(){return instance;}
    @Override public void onLoad(){instance=this;lifecycle.load();}
    @Override public void onEnable(){lifecycle.start();}
    @Override public void onDisable(){lifecycle.stop();}
    public StorageDrawersApi getApi(){return lifecycle.api();}
    public NetworkIndex networks(){return lifecycle.networks();}public DrawerMenu menus(){return lifecycle.menus();}public FramingMenu framingMenus(){return lifecycle.framingMenus();}public CompactingRules compactingRules(){return lifecycle.compacting();}public ItemEquivalence equivalence(){return lifecycle.equivalence();}public AimHologram aimHologram(){return lifecycle.aim();}public CompatibilityManager compatibility(){return lifecycle.compatibility();}
    public String artifactHash(){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(getFile().toPath())));}catch(Exception failure){throw new IllegalStateException(failure);}}
}
