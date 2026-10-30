package com.cendreal.storagedrawers.module.storage;
import com.cendreal.storagedrawers.api.*;
import com.cendreal.storagedrawers.api.event.*;
import net.momirealms.craftengine.core.item.Item;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
/** No Bukkit item snapshots are allocated on the transfer path when nobody listens. */
public final class StorageEvents {
    public static StoragePosition position(NodeController node){var e=node.blockEntity();return new StoragePosition(e.world.uuid(),e.pos.x,e.pos.y,e.pos.z);}
    public static DrawerSnapshot.State state(DrawerController drawer){var f=drawer.frame();return new DrawerSnapshot.State(drawer.locked(),drawer.quantity(),drawer.concealed(),drawer.suspended(),drawer.priority(),drawer.ownerId(),drawer.upgrades(),new DrawerSnapshot.Frame(f.base(),f.side(),f.trim(),f.front()));}
    public static DrawerSnapshot snapshot(DrawerController drawer){var cells=new java.util.ArrayList<DrawerSnapshot.Slot>();for(int i=0;i<drawer.size();i++)cells.add(new DrawerSnapshot.Slot(stack(drawer.template(i)),drawer.count(i),drawer.capacity(i),drawer.missing(i)));return new DrawerSnapshot(position(drawer),NamespacedKey.fromString(drawer.behavior.block().id().toString()),cells,state(drawer));}
    public static ItemStack stack(Item item){return item==null||item.isEmpty()?new ItemStack(Material.AIR):((ItemStack)item.platformItem()).clone();}
    public static boolean transfer(DrawerController drawer,int slot,Item item,int amount,boolean insert,net.momirealms.craftengine.core.entity.player.Player actor){
        if(actor==null&&!StorageContext.scoped()||drawer.blockEntity().world==null||DrawerTransferEvent.getHandlerList().getRegisteredListeners().length==0)return true;
        var event=new DrawerTransferEvent(position(drawer),StorageContext.actor(actor),slot,stack(item),amount,insert?DrawerTransferEvent.Direction.INSERT:DrawerTransferEvent.Direction.EXTRACT);Bukkit.getPluginManager().callEvent(event);return !event.isCancelled();
    }
    public static boolean slotsObserved(){return StorageSlotChangeEvent.getHandlerList().getRegisteredListeners().length!=0;}
    public static boolean replace(DrawerController drawer,int slot,Item item,int amount){if(drawer.blockEntity().world==null||DrawerTransferEvent.getHandlerList().getRegisteredListeners().length==0)return true;var event=new DrawerTransferEvent(position(drawer),StorageContext.actor(null),slot,stack(item),amount,DrawerTransferEvent.Direction.SET);Bukkit.getPluginManager().callEvent(event);return !event.isCancelled();}
    public static StorageSlotChangeEvent change(NodeController node,StorageSlotChangeEvent.Kind kind,int slot,Item before,long beforeCount,Item after,long afterCount){return new StorageSlotChangeEvent(position(node),StorageContext.actor(null),kind,slot,stack(before),beforeCount,stack(after),afterCount);}
    public static void slot(NodeController node,StorageSlotChangeEvent.Kind kind,int slot,Item before,long beforeCount,Item after,long afterCount){
        if(node.blockEntity().world==null||!slotsObserved()||beforeCount==afterCount&&(before==after||before.isSimilar(after)))return;
        Bukkit.getPluginManager().callEvent(change(node,kind,slot,before,beforeCount,after,afterCount));
    }
    public static DrawerSnapshot.State beforeState(DrawerController drawer){return DrawerStateChangeEvent.getHandlerList().getRegisteredListeners().length==0?null:state(drawer);}
    public static void state(DrawerController drawer,DrawerSnapshot.State before){if(before!=null&&drawer.blockEntity().world!=null){var after=state(drawer);if(!before.equals(after))Bukkit.getPluginManager().callEvent(new DrawerStateChangeEvent(position(drawer),StorageContext.actor(null),before,after));}}
    public static boolean open(NodeController node,Player player,boolean framing){if(DrawerOpenEvent.getHandlerList().getRegisteredListeners().length==0)return true;var event=new DrawerOpenEvent(position(node),player,framing?DrawerOpenEvent.Kind.FRAMING_TABLE:DrawerOpenEvent.Kind.DRAWER);Bukkit.getPluginManager().callEvent(event);return !event.isCancelled();}
    public static void lifecycle(NodeController node,DrawerLifecycleEvent.Action action){if(node.blockEntity().world!=null&&DrawerLifecycleEvent.getHandlerList().getRegisteredListeners().length!=0)Bukkit.getPluginManager().callEvent(new DrawerLifecycleEvent(position(node),NamespacedKey.fromString(node.behavior.block().id().toString()),action));}
    private StorageEvents(){}
}
