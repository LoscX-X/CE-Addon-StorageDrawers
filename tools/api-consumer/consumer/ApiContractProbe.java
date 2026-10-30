package consumer;

import com.cendreal.storagedrawers.api.*;
import com.cendreal.storagedrawers.api.event.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Compiled against the published API JAR and Paper only: no CE, NMS or addon internals. */
public final class ApiContractProbe {
    private static void check(List<String> passed,String name,boolean condition){if(!condition)throw new AssertionError(name);passed.add(name);}
    public static final class Capture implements Listener {
        final StoragePosition position;boolean cancelTransfer,cancelOpen;final List<StorageSlotChangeEvent> changes=new ArrayList<>();int removed,settings;
        Capture(Location location){position=StoragePosition.of(location);}
        @EventHandler public void before(DrawerTransferEvent event){if(cancelTransfer&&event.getPosition().equals(position))event.setCancelled(true);}
        @EventHandler public void changed(StorageSlotChangeEvent event){if(event.getPosition().equals(position))changes.add(event);}
        @EventHandler public void opened(DrawerOpenEvent event){if(cancelOpen&&event.getPosition().equals(position))event.setCancelled(true);}
        @EventHandler public void state(DrawerStateChangeEvent event){if(event.getPosition().equals(position))settings++;}
        @EventHandler public void lifecycle(DrawerLifecycleEvent event){if(event.getPosition().equals(position)&&event.getAction()==DrawerLifecycleEvent.Action.REMOVE)removed++;}
    }
    public static List<String> run(JavaPlugin plugin,World world,Player player,Location normal,Location compact,Location framed,Location table,Location controller)throws Exception{
        List<String> passed=new ArrayList<>();StorageDrawersApi api=StorageDrawersApi.get();
        check(passed,"SDK service is registered",api!=null);
        DrawerAccess drawer=api.getDrawer(normal).orElseThrow();Capture capture=new Capture(normal);Bukkit.getPluginManager().registerEvents(capture,plugin);
        try{
            check(passed,"SDK item catalog and item factory",api.getItemIds().size()==159&&api.createItem(new NamespacedKey("storagedrawers","iron_storage_upgrade"),1).isPresent());
            ItemStack diamond=new ItemStack(Material.DIAMOND);diamond.editMeta(meta->meta.displayName(net.kyori.adventure.text.Component.text("API probe")));
            check(passed,"SDK setItem stores quantity and metadata",drawer.setItem(0,diamond,23)&&drawer.getCount(0)==23&&drawer.getItem(0).isSimilar(diamond));
            ItemStack copy=drawer.getItem(0);copy.setAmount(64);copy.editMeta(meta->meta.displayName(net.kyori.adventure.text.Component.text("mutated")));
            check(passed,"SDK item getters are defensive copies",drawer.getCount(0)==23&&drawer.getItem(0).isSimilar(diamond));
            var snapshot=drawer.getSnapshot();snapshot.slots().getFirst().item().setAmount(64);
            check(passed,"SDK snapshots cannot mutate live storage",drawer.getCount(0)==23);
            int baseCapacity=drawer.getCapacity(0);
            check(passed,"SDK rejected absolute writes preserve contents",!drawer.setCount(0,baseCapacity+1)&&!drawer.setCount(0,-1)&&drawer.getCount(0)==23);
            int committed=capture.changes.size();capture.cancelTransfer=true;
            check(passed,"SDK cancelled SET commits nothing",!drawer.setCount(0,20)&&drawer.getCount(0)==23&&capture.changes.size()==committed);
            capture.cancelTransfer=false;check(passed,"SDK setCount commits exact value",drawer.setCount(0,20)&&drawer.getCount(0)==20);
            ItemStack supplied=diamond.clone();supplied.setAmount(5);
            check(passed,"SDK insert preserves caller stack",api.insert(normal,0,supplied,5,player)==5&&supplied.getAmount()==5&&drawer.getCount(0)==25);
            ItemStack extracted=api.extract(normal,0,3,player);
            check(passed,"SDK extraction conserves count and metadata",extracted.getAmount()==3&&extracted.isSimilar(diamond)&&drawer.getCount(0)==22);
            committed=capture.changes.size();capture.cancelTransfer=true;
            check(passed,"SDK cancelled transfer creates no committed event",api.insert(normal,0,supplied,1,player)==0&&api.extract(normal,0,1,player).isEmpty()&&drawer.getCount(0)==22&&capture.changes.size()==committed);
            capture.cancelTransfer=false;
            check(passed,"SDK committed changes carry API attribution",capture.changes.stream().anyMatch(e->e.getActor().cause()==StorageCause.API&&e.getBeforeCount()==20&&e.getAfterCount()==25));
            UUID owner=UUID.randomUUID();check(passed,"SDK ownership get/set",drawer.setOwner(owner)&&drawer.getOwner().orElseThrow().equals(owner));
            check(passed,"SDK player transfers honor ownership",api.insert(normal,0,supplied,1,player)==0);drawer.setOwner(null);
            check(passed,"SDK priority range and network invalidation",drawer.setPriority(2)&&drawer.getPriority()==2&&!drawer.setPriority(3)&&drawer.getPriority()==2);drawer.setPriority(0);
            check(passed,"SDK upgrade getter and capacity change",drawer.setUpgrade(0,api.createItem(new NamespacedKey("storagedrawers","iron_storage_upgrade"),1).orElseThrow())&&!drawer.getUpgrade(0).isEmpty()&&drawer.getCapacity(0)>baseCapacity);
            drawer.setCount(0,baseCapacity+1);
            check(passed,"SDK capacity-removing upgrade write is rejected atomically",!drawer.setUpgrade(0,new ItemStack(Material.AIR))&&drawer.getCount(0)==baseCapacity+1&&!drawer.getUpgrade(0).isEmpty());
            drawer.setCount(0,22);drawer.setUpgrade(0,new ItemStack(Material.AIR));
            check(passed,"SDK flag getters and setters",drawer.setLocked(true)&&drawer.isLocked()&&drawer.setQuantityVisible(false)&&!drawer.isQuantityVisible()&&drawer.setConcealed(true)&&drawer.isConcealed()&&drawer.setSuspended(true)&&drawer.isSuspended());
            check(passed,"SDK automation respects suspension",api.insert(normal,0,supplied,1,null)==0&&api.extract(normal,0,1,null).isEmpty());
            drawer.setLocked(false);drawer.setQuantityVisible(true);drawer.setConcealed(false);drawer.setSuspended(false);
            check(passed,"SDK top-level settings support trusted server actor",api.setSetting(normal,StorageDrawersApi.Setting.QUANTITY,false,null)&&!drawer.isQuantityVisible()&&api.setSetting(normal,StorageDrawersApi.Setting.QUANTITY,true,null)&&drawer.isQuantityVisible());
            check(passed,"SDK settings changes are observable",capture.settings>=8);
            ItemStack detached=drawer.detach(0,null);
            check(passed,"SDK detached drawer preserves contents",!detached.isEmpty()&&drawer.isMissing(0)&&drawer.getCount(0)==0&&!drawer.setItem(0,diamond,1));
            check(passed,"SDK attachment restores contents without mutating argument",drawer.attach(0,detached,null)&&!drawer.isMissing(0)&&drawer.getCount(0)==22&&detached.getAmount()==1);
            DrawerAccess compacting=api.getDrawer(compact).orElseThrow();
            check(passed,"SDK compacting seed maintains all denominations",compacting.setCompactingContents(new ItemStack(Material.IRON_INGOT),9)&&compacting.getCount(0)==1&&compacting.getCount(1)==9&&compacting.getCount(2)==81);
            check(passed,"SDK compacting absolute count preserves base units",compacting.setCount(1,18)&&compacting.getCount(0)==2&&compacting.getCount(2)==162);
            check(passed,"SDK incompatible compacting identities are rejected",!compacting.setItem(1,new ItemStack(Material.DIAMOND),3)&&compacting.getCount(1)==18);
            DrawerAccess frame=api.getDrawer(framed).orElseThrow();int frameCount=frame.getCount(0);
            check(passed,"SDK framing setter preserves stored items",frame.setFrame(new ItemStack(Material.DEEPSLATE),new ItemStack(Material.OAK_PLANKS),new ItemStack(Material.GOLD_BLOCK))&&frame.getFrame().side().equals("minecraft:deepslate")&&frame.getCount(0)==frameCount);
            check(passed,"SDK unsupported frame materials are rejected",!frame.setFrame(new ItemStack(Material.GLASS),new ItemStack(Material.AIR),new ItemStack(Material.AIR))&&frame.getFrame().side().equals("minecraft:deepslate"));
            FramingTableAccess workbench=api.getFramingTable(table).orElseThrow();
            check(passed,"SDK framing input setters",workbench.setInput(0,api.createItem(new NamespacedKey("storagedrawers","oak_full_drawers_1"),1).orElseThrow())&&workbench.setInput(1,new ItemStack(Material.STONE))&&workbench.setInput(2,new ItemStack(Material.OAK_PLANKS))&&workbench.setInput(3,new ItemStack(Material.GOLD_BLOCK)));
            ItemStack preview=workbench.getResult(),crafted=workbench.craft(1);
            check(passed,"SDK crafting consumes table inputs exactly once",!crafted.isEmpty()&&crafted.isSimilar(preview)&&workbench.getInput(0).isEmpty()&&workbench.getInput(1).isEmpty()&&workbench.getResult().isEmpty());
            check(passed,"SDK network lookup uses loaded index",api.network(controller).isPresent()&&api.loadedDrawers(world).stream().anyMatch(d->d.position().equals(StoragePosition.of(normal))));
            check(passed,"SDK framing tables are excluded from drawer networks",api.network(table).isEmpty());
            capture.cancelOpen=true;check(passed,"SDK menu opening can be cancelled",!api.open(player,normal));capture.cancelOpen=false;
            check(passed,"SDK menu opening uses normal validation",api.open(player,normal)&&player.getOpenInventory().getTopInventory().getSize()==45);player.closeInventory();
            AtomicBoolean rejected=new AtomicBoolean();Thread worker=new Thread(()->{try{drawer.getCount(0);}catch(IllegalStateException expected){rejected.set(true);}},"drawer-sdk-thread-probe");worker.start();worker.join(2000);
            check(passed,"SDK rejects off-thread world access",rejected.get());
            check(passed,"SDK lookup never loads an absent chunk",!world.isChunkLoaded(62500,62500)&&api.getDrawer(new Location(world,1000000,10,1000000)).isEmpty()&&!world.isChunkLoaded(62500,62500));
            normal.getBlock().setType(Material.AIR);
            check(passed,"SDK handle invalidates on block removal",!drawer.isAvailable()&&!drawer.setCount(0,1)&&capture.removed==1);
            return passed;
        }finally{HandlerList.unregisterAll(capture);player.closeInventory();}
    }
}
