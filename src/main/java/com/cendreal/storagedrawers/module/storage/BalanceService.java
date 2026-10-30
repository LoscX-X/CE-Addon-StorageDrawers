package com.cendreal.storagedrawers.module.storage;

import com.cendreal.storagedrawers.StorageDrawersCE;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.entity.player.Player;
import java.util.*;

final class BalanceService {
    private record Slot(DrawerController drawer,int index){}
    static void balance(DrawerController source,Item prototype,Player actor){if(prototype.isEmpty()||source.behavior.layout.compacting()||source.suspended()||!source.has("balance_fill_upgrade")||source.has("creative_vending_upgrade"))return;
        var view=StorageDrawersCE.instance().networks().view(source);List<DrawerController> scope=view.controller()==null?List.of(source):view.drawers();List<Slot> slots=new ArrayList<>();
        for(var d:scope)if(!d.behavior.layout.compacting()&&!d.suspended()&&d.has("balance_fill_upgrade")&&!d.has("creative_vending_upgrade")&&(actor==null||d.canAccess(actor)))for(int i=0;i<d.size();i++)if(!d.missing(i)&&d.template(i).isSimilar(prototype))slots.add(new Slot(d,i));
        if(slots.size()<2)return;long total=0;for(var slot:slots)total+=slot.drawer.count(slot.index);long[] assigned=new long[slots.size()];
        while(total>0){int available=0;for(int i=0;i<slots.size();i++)if(assigned[i]<slots.get(i).drawer.capacity(slots.get(i).index))available++;if(available==0)throw new IllegalStateException("Balance capacities below stored contents");long each=total/available,remainder=total%available;for(int i=0;i<slots.size();i++){var slot=slots.get(i);long room=(long)slot.drawer.capacity(slot.index)-assigned[i];if(room<=0)continue;long amount=Math.min(room,each+(remainder-->0?1:0));assigned[i]+=amount;total-=amount;}}
        Set<DrawerController> changed=Collections.newSetFromMap(new IdentityHashMap<>());for(int i=0;i<slots.size();i++){var slot=slots.get(i);if(slot.drawer.count(slot.index)!=(int)assigned[i]){slot.drawer.balanceCount(slot.index,(int)assigned[i]);changed.add(slot.drawer);}}
        for(var drawer:changed)if(drawer!=source)drawer.changed(actor);
    }
    private BalanceService(){}
}
