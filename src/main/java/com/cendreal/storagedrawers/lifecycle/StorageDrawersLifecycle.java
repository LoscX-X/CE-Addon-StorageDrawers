package com.cendreal.storagedrawers.lifecycle;
import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.api.StorageDrawersApi;
import com.cendreal.storagedrawers.module.automation.NetworkIndex;
import com.cendreal.storagedrawers.module.storage.*;
import com.cendreal.storagedrawers.module.menu.*;
import com.cendreal.storagedrawers.module.portability.PortablePolicy;
import com.cendreal.storagedrawers.module.service.DefaultStorageDrawersApi;
import com.cendreal.storagedrawers.compatibility.CompatibilityManager;
import com.cendreal.storagedrawers.compatibility.craftengine.*;
import com.cendreal.storagedrawers.event.block.DrawerEvents;
import com.cendreal.storagedrawers.event.inventory.*;
import com.cendreal.storagedrawers.event.player.DisplayPlayerListener;
import com.cendreal.storagedrawers.event.player.PortablePlayerListener;
import com.cendreal.storagedrawers.event.server.StorageWorldListener;
import com.cendreal.storagedrawers.event.server.CraftEngineReloadListener;
import com.cendreal.storagedrawers.render.AimHologram;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.loot.function.LootFunctions;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.nio.file.*;
/** Owns startup order, registrations, reload and matching cleanup. */
public final class StorageDrawersLifecycle {
    private final StorageDrawersCE plugin;
    private final NetworkIndex networks=new NetworkIndex();
    private final DrawerMenu menus=new DrawerMenu();private final FramingMenu framingMenus=new FramingMenu();
    private final AimHologram aim;private final CompatibilityManager compatibility;
    private final List<BukkitTask> tasks=new ArrayList<>();
    private CompactingRules compacting;private ItemEquivalence equivalence;private StorageDrawersApi api;private boolean stopped;
    public StorageDrawersLifecycle(StorageDrawersCE plugin){this.plugin=plugin;aim=new AimHologram(plugin);compatibility=new CompatibilityManager(plugin);}
    public void load(){BlockBehaviors.register(Key.of("storagedrawers:drawer"),DrawerBehavior::new);LootFunctions.register(Key.of("storagedrawers:contents"),config->new ContentsLoot());net.momirealms.craftengine.core.item.behavior.ItemBehaviors.register(Key.of("storagedrawers:tool"),(pack,path,key,config)->new ToolBehavior());net.momirealms.craftengine.core.item.recipe.RecipeSerializers.register(Key.of("storagedrawers:special"),com.cendreal.storagedrawers.module.crafting.StorageRecipe.SERIALIZER);}
    public void start(){
        importLegacyConfig();plugin.saveDefaultConfig();networks.configure(plugin.getConfig());equivalence=new ItemEquivalence(plugin.getConfig());compacting=new CompactingRules();
        register(new CraftEngineReloadListener(this));register(new DrawerEvents());register(new DrawerCraftingListener());register(new DrawerMenuListener(menus));register(new FramingMenuListener(framingMenus));register(new RingEvents());register(new DisplayPlayerListener(aim));register(compatibility);
        var portable=new PortablePolicy();register(new PortableInventoryListener(portable));register(new PortablePlayerListener(plugin,portable));register(new StorageWorldListener(networks));
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,aim::tick,4,4));tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,()->{for(var player:Bukkit.getOnlinePlayers())portable.maintain(player);},60,60));
        api=new DefaultStorageDrawersApi(plugin);Bukkit.getServicesManager().register(StorageDrawersApi.class,api,plugin,ServicePriority.Normal);
        tasks.add(Bukkit.getScheduler().runTask(plugin,this::loaded));
    }
    private void register(Listener listener){Bukkit.getPluginManager().registerEvents(listener,plugin);}
    private void importLegacyConfig(){var target=plugin.getDataFolder().toPath().resolve("config.yml");var legacy=plugin.getDataFolder().toPath().getParent().resolve("StorageDrawers-CE/config.yml");if(Files.exists(target)||!Files.isRegularFile(legacy))return;try{Files.createDirectories(target.getParent());Files.copy(legacy,target);plugin.getLogger().info("Imported existing StorageDrawers-CE configuration");}catch(java.io.IOException failure){throw new IllegalStateException("Cannot import legacy configuration",failure);}}
    public void reload(){if(stopped)return;if(Bukkit.isPrimaryThread())loaded();else tasks.add(Bukkit.getScheduler().runTask(plugin,this::loaded));}
    private void loaded(){if(stopped)return;plugin.reloadConfig();networks.configure(plugin.getConfig());equivalence=new ItemEquivalence(plugin.getConfig());aim.reload();compacting=new CompactingRules();int states=DrawerShapes.install();compatibility.reload();plugin.getLogger().info("StorageDrawers CE: "+states+" block states; "+compacting.size()+" compacting rules; idle collection tickers sleep");String manual=System.getProperty("storagedrawers.ce.manualWorld","");if(!manual.isBlank())new WorldCreator(manual).createWorld();else if(Boolean.getBoolean("storagedrawers.ce.audit"))com.cendreal.storagedrawers.audit.RuntimeAudit.schedule(plugin);}
    public void stop(){if(stopped)return;stopped=true;Bukkit.getServicesManager().unregisterAll(plugin);Bukkit.getScheduler().cancelTasks(plugin);tasks.clear();compatibility.close();aim.clear();menus.closeAll();framingMenus.closeAll();networks.clear();ContentsLoot.clear();HandlerList.unregisterAll(plugin);api=null;}
    public NetworkIndex networks(){return networks;}public DrawerMenu menus(){return menus;}public FramingMenu framingMenus(){return framingMenus;}public AimHologram aim(){return aim;}public ItemEquivalence equivalence(){return equivalence;}public CompactingRules compacting(){return compacting;}public StorageDrawersApi api(){return api;}public CompatibilityManager compatibility(){return compatibility;}
}
