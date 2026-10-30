package com.cendreal.storagedrawers.module.automation;

import com.cendreal.storagedrawers.module.storage.DrawerController;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.block.entity.tick.*;
import net.momirealms.craftengine.core.util.Direction;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.util.BoundingBox;

/** Enabled only by collection upgrades; CE removes it from active ticking when asleep/unloaded. */
public final class CollectionTicker {
    private final DrawerController drawer;
    private int countdown;
    private final SleepingBlockEntityTicker<DrawerController> ticker;
    public CollectionTicker(DrawerController drawer){this.drawer=drawer;ticker=new SleepingBlockEntityTicker<>((world,pos,state,c)->tick());ticker.sleep();}
    public SleepingBlockEntityTicker<DrawerController> ticker(){return ticker;}
    public void refresh(){if(drawer.suspended() || drawer.magnetRange()[0]==0 && !drawer.has("hopper_upgrade"))ticker.sleep();else ticker.wakeUp();}
    private void tick(){
        if(--countdown>0)return;
        int[] range=drawer.magnetRange();boolean magnet=range[0]>0;
        countdown=20;
        var e=drawer.blockEntity();var world=(org.bukkit.World)e.world.world().platformWorld();var pos=e.pos;
        if(!magnet){var above=world.getBlockAt(pos.x,pos.y+1,pos.z);if(above.isSolid())return;}
        BoundingBox box=magnet?new BoundingBox(pos.x-range[0],pos.y-range[2],pos.z-range[0],pos.x+range[0]+1,pos.y+range[1]+1,pos.z+range[0]+1):new BoundingBox(pos.x,pos.y+11.0/16,pos.z,pos.x+1,pos.y+2,pos.z+1);
        for(var entity:world.getNearbyEntities(box,x->x instanceof org.bukkit.entity.Item && !x.isDead())) {
            var dropped=(org.bukkit.entity.Item)entity;var stack=dropped.getItemStack();
            ItemTransfer: {
                var input=BukkitAdaptor.adapt(stack);boolean match=false;
                for(int i=0;i<drawer.size();i++)if(drawer.accepts(i,input)){match=true;break;}
                if(!match)break ItemTransfer;
                var event=new InventoryPickupItemEvent(drawer.container().getInventory(),dropped);org.bukkit.Bukkit.getPluginManager().callEvent(event);if(event.isCancelled())break ItemTransfer;
                int remaining=stack.getAmount();for(int i=0;i<drawer.size() && remaining>0;i++)remaining-=drawer.put(i,input,remaining);
                if(remaining==stack.getAmount())break ItemTransfer;
                if(remaining==0)dropped.remove();else{stack.setAmount(remaining);dropped.setItemStack(stack);}
                countdown=drawer.has("magnet_upgrade_3")?5:drawer.has("magnet_upgrade_2")?10:20;return;
            }
        }
    }
}
