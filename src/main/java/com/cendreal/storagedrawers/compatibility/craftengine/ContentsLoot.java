package com.cendreal.storagedrawers.compatibility.craftengine;

import com.cendreal.storagedrawers.module.storage.DrawerController;
import com.cendreal.storagedrawers.module.storage.NodeController;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.BlockPos;
import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.module.frame.FrameData;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

/** Snapshot only: removal and loot preview must not drain the live drawer. */
public final class ContentsLoot implements LootFunction {
    private record Address(Object world,BlockPos pos,String block) {}
    private record Snapshot(FrameData frame,CompoundTag contents) {
        void apply(Item item){if(!frame.side().isEmpty())frame.apply(item);if(contents!=null){item.setSparrowTag(contents.copy(),DrawerController.DATA_KEY);item.maxStackSize(1);}}
    }
    // Native player destruction removes CE's entity before evaluating its loot. Keep only
    // immutable item data for that synchronous removal/loot sequence, never a live inventory.
    private static final Map<Address,Snapshot> removed=new HashMap<>();
    private static boolean cleanupQueued;
    private static Snapshot snapshot(NodeController node){return new Snapshot(node.frame(),node instanceof DrawerController drawer&&drawer.hasContents()?drawer.contents():null);}
    public static void removed(NodeController node){
        var entity=node.blockEntity();var data=snapshot(node);if(data.contents()==null&&data.frame().side().isEmpty())return;
        removed.put(new Address(entity.world.world().minecraftWorld(),entity.pos,node.behavior.block().id().toString()),data);
        if(!cleanupQueued){cleanupQueued=true;org.bukkit.Bukkit.getScheduler().runTask(StorageDrawersCE.instance(),ContentsLoot::clear);}
    }
    public static void clear(){removed.clear();cleanupQueued=false;}
    public static Item portable(NodeController node){Item item=Item.byId(node.behavior.block().id());snapshot(node).apply(item);return item;}
    @Override public Item apply(Item item,LootContext context) {
        var position=context.contexts().getOrNull(DirectContextParameters.POSITION);
        if(position==null) return item;
        var pos=new BlockPos((int)Math.floor(position.x()),(int)Math.floor(position.y()),(int)Math.floor(position.z()));
        var state=context.contexts().getOrNull(DirectContextParameters.CUSTOM_BLOCK_STATE);
        var entity=context.world().storageWorld().getBlockEntityAtIfLoaded(pos);
        if(entity!=null&&(state==null||entity.blockState.owner().value()==state.owner().value()))entity.controller.let(NodeController.class,n->snapshot(n).apply(item));
        else if(state!=null){var data=removed.get(new Address(context.world().minecraftWorld(),pos,state.owner().value().id().toString()));if(data!=null)data.apply(item);}
        return item;
    }
}
