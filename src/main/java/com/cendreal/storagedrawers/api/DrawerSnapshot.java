package com.cendreal.storagedrawers.api;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import java.util.*;
/** Detached read model; mutating a returned item never changes storage. */
public record DrawerSnapshot(StoragePosition position,NamespacedKey blockId,List<Slot> slots,State state) {
    public DrawerSnapshot{slots=List.copyOf(slots);}
    public record Slot(ItemStack item,int count,int capacity,boolean missing){
        public Slot{item=item.clone();}
        @Override public ItemStack item(){return item.clone();}
    }
    public record State(boolean locked,boolean quantity,boolean concealed,boolean suspended,int priority,UUID owner,List<String> upgrades,Frame frame){public State{upgrades=List.copyOf(upgrades);}}
    public record Frame(String base,String side,String trim,String front){}
}
