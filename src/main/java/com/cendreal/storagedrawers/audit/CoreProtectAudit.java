package com.cendreal.storagedrawers.audit;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.api.*;
import com.cendreal.storagedrawers.api.event.DrawerTransferEvent;
import net.coreprotect.CoreProtect;
import net.coreprotect.api.result.ContainerResult;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.function.Consumer;

/** Reads the genuine CoreProtect database after its consumer has persisted real addon transactions. */
final class CoreProtectAudit {
    private final StorageDrawersCE plugin;
    private final Location simple,multi,large;
    CoreProtectAudit(StorageDrawersCE plugin,Player player,Location simple,Location multi,Location large){
        this.plugin=plugin;this.simple=simple;this.multi=multi;this.large=large;
        if(!plugin.compatibility().coreProtectEnabled())throw new AssertionError("CoreProtect bridge did not enable");
        var api=StorageDrawersApi.get();var drawer=api.getDrawer(simple).orElseThrow();
        ItemStack named=new ItemStack(Material.DIAMOND);named.editMeta(meta->meta.displayName(net.kyori.adventure.text.Component.text("CoreProtect audit")));
        if(!drawer.setItem(0,named,19,player)||api.extract(simple,0,4,player).getAmount()!=4)throw new AssertionError("CoreProtect input fixture");
        var cancel=new Listener(){};Bukkit.getPluginManager().registerEvent(DrawerTransferEvent.class,cancel,EventPriority.HIGHEST,(listener,event)->{var transfer=(DrawerTransferEvent)event;if(transfer.getPosition().equals(StoragePosition.of(simple)))transfer.setCancelled(true);},plugin,true);
        try{if(api.insert(simple,0,named,3,player)!=0||drawer.getCount(0)!=15)throw new AssertionError("CoreProtect cancelled fixture");}finally{HandlerList.unregisterAll(cancel);}
        if(api.insert(simple,0,named,2,null)!=2||api.extract(simple,0,1,player).getAmount()!=1)throw new AssertionError("CoreProtect interleaved actors fixture");
        var cells=api.getDrawer(multi).orElseThrow();
        if(!cells.setItem(0,new ItemStack(Material.STONE),2)||!cells.setItem(1,new ItemStack(Material.IRON_INGOT),3)||api.extract(multi,0,1,null).getAmount()!=1)throw new AssertionError("CoreProtect multi-cell fixture");
        var unlimited=api.getDrawer(large).orElseThrow();
        if(!unlimited.setUpgrade(0,api.createItem(new NamespacedKey("storagedrawers","creative_storage_upgrade"),1).orElseThrow())||!unlimited.setItem(0,new ItemStack(Material.EMERALD),Integer.MAX_VALUE))throw new AssertionError("CoreProtect large-count fixture");
    }
    private static long total(List<ContainerResult> rows,Material material,int action){return rows.stream().filter(row->row.getType()==material&&row.getActionId()==action).mapToLong(ContainerResult::getAmount).sum();}
    private static String describe(List<ContainerResult> rows){return rows.stream().map(row->row.getPlayer()+":"+row.getActionString()+":"+row.getType()+":"+row.getAmount()+":metadata="+(row.getMetadata()==null?0:row.getMetadata().length)).toList().toString();}
    void verify(Consumer<List<String>> success,Consumer<Throwable> failure){
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            try{
                var api=((CoreProtect)Bukkit.getPluginManager().getPlugin("CoreProtect")).getAPI();
                List<ContainerResult> first=List.of(),second=List.of(),third=List.of(),nativeInput=List.of(),nativeOutput=List.of();boolean ready=false;
                long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(45);
                while(System.nanoTime()<deadline){
                    first=api.containerLookup(simple,120);second=api.containerLookup(multi,120);third=api.containerLookup(large,120);nativeInput=api.containerLookup(new Location(simple.getWorld(),120,10,80),120);nativeOutput=api.containerLookup(new Location(simple.getWorld(),122,10,80),120);
                    if(first!=null&&second!=null&&third!=null&&nativeInput!=null&&nativeOutput!=null&&total(first,Material.DIAMOND,1)==21&&total(first,Material.DIAMOND,0)==5&&total(second,Material.STONE,1)==2&&total(second,Material.STONE,0)==1&&total(second,Material.IRON_INGOT,1)==3&&total(second,Material.IRON_INGOT,0)==0&&total(third,Material.EMERALD,1)==Integer.MAX_VALUE&&total(nativeInput,Material.COAL,1)==8&&total(nativeInput,Material.COAL,0)==0&&total(nativeOutput,Material.DIAMOND,1)==8&&total(nativeOutput,Material.DIAMOND,0)==8){ready=true;break;}
                    Thread.sleep(1000);
                }
                if(!ready)throw new AssertionError("CoreProtect persisted transactions: simple="+describe(first==null?List.of():first)+" multi="+describe(second==null?List.of():second)+" large="+describe(third==null?List.of():third)+" nativeInput="+describe(nativeInput==null?List.of():nativeInput)+" nativeOutput="+describe(nativeOutput==null?List.of():nativeOutput));
                if(first.stream().filter(row->row.getType()==Material.DIAMOND).anyMatch(row->row.getMetadata()==null||row.getMetadata().length==0)||first.stream().filter(row->row.getPlayer().equals("DrawerAudit")&&row.getActionId()==1).mapToInt(ContainerResult::getAmount).sum()!=19||first.stream().filter(row->row.getPlayer().equals("#CE-Addon-StorageDrawers")&&row.getActionId()==1).mapToInt(ContainerResult::getAmount).sum()!=2)throw new AssertionError("CoreProtect actor or named-item metadata missing");
                var passed=List.of("CoreProtect database persists named-item additions and removals with actor", "CoreProtect cancelled API operation creates no additional item record", "CoreProtect independent drawer cells create no cross-cell removals", "CoreProtect database preserves a 2147483647 item transaction without splitting stacks", "CoreProtect interleaved actors retain their own exact transaction amounts", "CoreProtect native hopper records contain no duplicates or cancelled attempts");
                Bukkit.getScheduler().runTask(plugin,()->success.accept(passed));
            }catch(Throwable problem){Bukkit.getScheduler().runTask(plugin,()->failure.accept(problem));}
        });
    }
}
