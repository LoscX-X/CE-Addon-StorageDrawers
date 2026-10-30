package com.cendreal.storagedrawers.audit;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.module.automation.*;
import com.cendreal.storagedrawers.module.storage.DrawerController;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bukkit.api.*;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.*;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.function.Consumer;

/** Opt-in loaded-block pressure fixture. Timings describe this host/run, never a production TPS guarantee. */
final class PressureAudit {
    private final StorageDrawersCE plugin;
    private final World world;
    private final List<Location> hoppers=new ArrayList<>();
    private final List<DrawerController> targets=new ArrayList<>();
    private final JsonObject metrics=new JsonObject();
    PressureAudit(StorageDrawersCE plugin,World world,Consumer<String> passed){
        this.plugin=plugin;this.world=world;
        for(int x=20;x<=23;x++)for(int z=20;z<=22;z++)world.getChunkAt(x,z).addPluginChunkTicket(plugin);
        for(int x=0;x<16;x++)for(int y=0;y<8;y++)for(int z=0;z<16;z++){
            var at=at(320+x,10+y,320+z);place(x==0&&y==0&&z==0?"controller":"oak_full_drawers_1",at);
            if(y==7){var drawer=drawer(at);drawer.put(0,item(Material.STONE),16);targets.add(drawer);}
        }
        for(int x=336;x<370;x++)place("oak_trim",at(x,10,320));
        var boundary=at(370,10,320);place("oak_full_drawers_1",boundary);place("oak_trim",at(371,10,320));place("oak_full_drawers_1",at(372,10,320));
        var controller=(NetworkController)node(at(320,10,320));var index=plugin.networks();
        long start=System.nanoTime();var view=controller.container().view();metrics.addProperty("initialNetworkBuildNanos",System.nanoTime()-start);
        check(passed,"pressure network includes 2048 loaded drawers and the 50-block boundary",view.drawers().size()==2048&&view.drawers().contains(drawer(boundary))&&!view.drawers().contains(drawer(at(372,10,320))));
        var smaller=new YamlConfiguration();smaller.set("controller.range",12);index.configure(smaller);
        check(passed,"configured controller radius invalidates previous cached view",!controller.container().view().drawers().contains(drawer(boundary)));
        index.configure(plugin.getConfig());check(passed,"controller radius restore rebuilds the full loaded network",controller.container().view().drawers().size()==2048);
        var single=at(340,30,340);var neighbor=at(341,30,340);place("oak_full_drawers_1",single);place("oak_full_drawers_1",neighbor);
        var remote=drawer(single);remote.install(RemoteBinding.bind(Item.byId(Key.of("storagedrawers:remote_upgrade")),controller));
        check(passed,"single remote connects only its bound drawer",controller.container().view().drawers().contains(remote)&&!controller.container().view().drawers().contains(drawer(neighbor)));
        check(passed,"API resolves the controller of an isolated bound remote",plugin.getApi().network(single).isPresent());
        remote.replaceUpgrade(0,RemoteBinding.bind(Item.byId(Key.of("storagedrawers:remote_group_upgrade")),controller));
        check(passed,"group remote includes adjacent loaded drawers",controller.container().view().drawers().contains(drawer(neighbor)));
        var shorter=new YamlConfiguration();shorter.set("controller.range",50);shorter.set("controller.remote.max-group-range",15);index.configure(shorter);
        check(passed,"configured remote range excludes a more distant bound group",!controller.container().view().drawers().contains(remote));index.configure(plugin.getConfig());
        var container=controller.container();container.canPlaceItem(0,item(Material.STONE));container.getItem(1);long searches=index.traversals();
        start=System.nanoTime();for(int i=0;i<10000;i++){container.canPlaceItem(0,item(Material.STONE));container.getItem(1);}metrics.addProperty("cached10000RoutingPairsNanos",System.nanoTime()-start);
        check(passed,"pressure cached routing does not traverse the 2050-drawer graph",index.traversals()==searches);
        var target=targets.getFirst();int before=target.count(0);start=System.nanoTime();for(int i=0;i<1000;i++)if(target.put(0,item(Material.STONE),1)!=1||target.take(0,1).count()!=1)throw new AssertionError("Pressure transaction rejected");metrics.addProperty("committed1000InsertExtractPairsNanos",System.nanoTime()-start);
        check(passed,"pressure 1000 real insert/extract pairs conserve storage",target.count(0)==before);
        for(var drawer:targets){var pos=drawer.blockEntity().pos;var location=at(pos.x,pos.y+1,pos.z);location.getBlock().setType(Material.HOPPER);((Hopper)location.getBlock().getState()).getInventory().setItem(0,new ItemStack(Material.STONE,8));hoppers.add(location);}
        metrics.addProperty("drawersInNetwork",container.view().drawers().size());metrics.addProperty("nativeHoppers",hoppers.size());metrics.addProperty("nativeItemsToTransfer",hoppers.size()*8);metrics.addProperty("coreProtectEnabled",plugin.compatibility().coreProtectEnabled());
        metrics.addProperty("scope","Real CE loaded blocks, cached network calls, committed storage pairs, and running native hopper ticks. Server tick samples include fixture startup; no client/player-load simulation.");
    }
    private Location at(int x,int y,int z){return new Location(world,x,y,z);}
    private static Item item(Material material){return BukkitAdaptor.adapt(new ItemStack(material));}
    private com.cendreal.storagedrawers.module.storage.NodeController node(Location at){return BukkitAdaptor.adapt(world).storageWorld().getBlockEntityAtIfLoaded(new BlockPos(at.getBlockX(),at.getBlockY(),at.getBlockZ())).controller.get(com.cendreal.storagedrawers.module.storage.NodeController.class,0);}
    private DrawerController drawer(Location at){return (DrawerController)node(at);}
    private void place(String id,Location at){if(!CraftEngineBlocks.place(at,Key.of("storagedrawers:"+id),false))throw new AssertionError("Pressure fixture placement: "+at);}
    private static void check(Consumer<String> passed,String name,boolean condition){if(!condition)throw new AssertionError(name);passed.accept(name);}
    JsonObject finish(Consumer<String> passed){
        int remaining=hoppers.stream().map(location->((Hopper)location.getBlock().getState()).getInventory()).flatMap(inventory->Arrays.stream(inventory.getContents())).filter(Objects::nonNull).mapToInt(ItemStack::getAmount).sum();
        int received=targets.stream().mapToInt(drawer->drawer.count(0)-16).sum();metrics.addProperty("nativeRemaining",remaining);metrics.addProperty("nativeReceived",received);
        check(passed,"pressure 256 simultaneous native hopper inputs conserve all 2048 items",remaining==0&&received==2048&&targets.stream().allMatch(drawer->drawer.count(0)==24));
        long[] ticks=Bukkit.getServer().getTickTimes().clone();Arrays.sort(ticks);metrics.addProperty("observedTickMeanMillis",Arrays.stream(ticks).average().orElse(0)/1_000_000d);metrics.addProperty("observedTickP95Millis",ticks[(int)(ticks.length*.95)]/1_000_000d);metrics.addProperty("maxHeapBytes",Runtime.getRuntime().maxMemory());return metrics;
    }
}
