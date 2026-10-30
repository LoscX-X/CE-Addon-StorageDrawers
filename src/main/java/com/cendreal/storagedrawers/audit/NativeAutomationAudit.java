package com.cendreal.storagedrawers.audit;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.api.*;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.*;
import java.util.*;
import java.util.function.Consumer;

/** Exercises Paper's running hopper tick and its cancellation path, rather than dispatching a mock event. */
final class NativeAutomationAudit implements Listener {
    private final Location input,output;
    private final DrawerAccess inputDrawer,outputDrawer;
    private int inputAttempts,outputAttempts;
    private int committedWhileCancelled;
    NativeAutomationAudit(StorageDrawersCE plugin,World world){
        world.getChunkAt(7,5).addPluginChunkTicket(plugin);
        input=new Location(world,120,10,80);output=new Location(world,122,10,80);
        for(var at:List.of(input,output))if(!CraftEngineBlocks.place(at,Key.of("storagedrawers:oak_full_drawers_1"),false))throw new AssertionError("Native cancellation fixture placement");
        var api=StorageDrawersApi.get();inputDrawer=api.getDrawer(input).orElseThrow();outputDrawer=api.getDrawer(output).orElseThrow();outputDrawer.setItem(0,new ItemStack(Material.DIAMOND),8);
        input.clone().add(0,1,0).getBlock().setType(Material.HOPPER);((Hopper)input.clone().add(0,1,0).getBlock().getState()).getInventory().setItem(0,new ItemStack(Material.COAL,8));
        output.clone().subtract(0,1,0).getBlock().setType(Material.HOPPER);output.clone().subtract(0,2,0).getBlock().setType(Material.CHEST);
        Bukkit.getPluginManager().registerEvents(this,plugin);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void move(InventoryMoveItemEvent event){
        if(input.equals(event.getDestination().getLocation())){inputAttempts++;event.setCancelled(true);}
        if(output.equals(event.getSource().getLocation())){outputAttempts++;event.setCancelled(true);}
    }
    @EventHandler public void changed(com.cendreal.storagedrawers.api.event.StorageSlotChangeEvent event){if(event.getPosition().equals(StoragePosition.of(input))||event.getPosition().equals(StoragePosition.of(output)))committedWhileCancelled++;}
    private static int count(Inventory inventory){return Arrays.stream(inventory.getContents()).filter(Objects::nonNull).mapToInt(ItemStack::getAmount).sum();}
    private Inventory upper(){return ((Hopper)input.clone().add(0,1,0).getBlock().getState()).getInventory();}
    private int lower(){return count(((Hopper)output.clone().subtract(0,1,0).getBlock().getState()).getInventory())+count(((Chest)output.clone().subtract(0,2,0).getBlock().getState()).getBlockInventory());}
    private static void check(Consumer<String> passed,String name,boolean condition){if(!condition)throw new AssertionError(name);passed.accept(name);}
    void blocked(Consumer<String> passed){
        try{
            check(passed,"native cancelled hopper insertion preserves both inventories",inputAttempts>0&&inputDrawer.getCount(0)==0&&count(upper())==8);
            check(passed,"native cancelled hopper extraction preserves both inventories",outputAttempts>0&&outputDrawer.getCount(0)==8&&lower()==0);
            check(passed,"native cancelled hopper operations publish no committed item changes",committedWhileCancelled==0);
        }finally{HandlerList.unregisterAll(this);}
    }
    void resumed(Consumer<String> passed){
        check(passed,"native hopper insertion resumes after cancellation removed",inputDrawer.getCount(0)==8&&count(upper())==0);
        check(passed,"native hopper extraction resumes without loss or duplication",outputDrawer.getCount(0)==0&&lower()==8);
    }
}
