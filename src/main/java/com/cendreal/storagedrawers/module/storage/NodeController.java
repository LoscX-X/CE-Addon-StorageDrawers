package com.cendreal.storagedrawers.module.storage;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import com.cendreal.storagedrawers.module.frame.FrameData;
import com.cendreal.storagedrawers.render.FrameDisplay;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.momirealms.craftengine.core.item.Item;
import java.util.function.Consumer;

public class NodeController extends BlockEntityController {
    public final DrawerBehavior behavior;
    private FrameData frame=FrameData.EMPTY;
    private final FrameDisplay[] facades;
    public NodeController(BlockEntity entity,DrawerBehavior behavior) {super(entity);this.behavior=behavior;facades=new FrameDisplay[behavior.framed?3:0];for(int i=0;i<facades.length;i++)facades[i]=new FrameDisplay(this,i);}
    public FrameData frame(){return frame;}
    @Override public boolean hasElement(){return facades.length>0;}
    @Override public void gatherElements(Consumer<BlockEntityElement> consumer){for(var display:facades)consumer.accept(display);}
    @Override public void preBlockStateChange(ImmutableBlockState state){for(var display:facades)display.stateChanged(state);}
    @Override public void saveCustomData(CompoundTag root){if(!frame.side().isEmpty())root.put(FrameData.KEY,frame.save());}
    @Override public void loadCustomData(CompoundTag root){if(root.getCompound(FrameData.KEY) instanceof CompoundTag data){frame=FrameData.read(data);for(var display:facades)display.sync(false);}}
    @Override public void loadCustomDataFromItem(Item item){frame=FrameData.read(item);for(var display:facades)display.sync(false);}
    public void writeFrame(Item item){if(!frame.side().isEmpty())frame.apply(item);}
    public void applyFrameFromItem(Item item){frame=FrameData.read(item);for(var display:facades)display.sync(true);if(blockEntity.world!=null)blockEntity.world.blockEntityChanged(blockEntity.pos);}
    @Override public void onLoad() {if(!behavior.layout.kind().equals("button")&&!behavior.layout.kind().equals("table"))StorageDrawersCE.instance().networks().add(this);StorageEvents.lifecycle(this,com.cendreal.storagedrawers.api.event.DrawerLifecycleEvent.Action.LOAD);}
    @Override public void onUnload() {StorageEvents.lifecycle(this,com.cendreal.storagedrawers.api.event.DrawerLifecycleEvent.Action.UNLOAD);StorageDrawersCE.instance().networks().remove(this);}
    @Override public void onRemove() {StorageEvents.lifecycle(this,com.cendreal.storagedrawers.api.event.DrawerLifecycleEvent.Action.REMOVE);com.cendreal.storagedrawers.compatibility.craftengine.ContentsLoot.removed(this);StorageDrawersCE.instance().networks().remove(this);}
}
