package com.cendreal.storagedrawers.module.frame;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.module.storage.NodeController;
import com.cendreal.storagedrawers.module.storage.DrawerController;
import com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import java.util.Arrays;

public final class FramingController extends NodeController {
    private final Item[] input={Item.empty(),Item.empty(),Item.empty(),Item.empty()};
    private static boolean removingPair;
    public FramingController(BlockEntity entity,DrawerBehavior behavior){super(entity,behavior);}
    public boolean primary(){return blockEntity.blockState.get(behavior.part).equals("right");}
    public FramingController primaryController(){if(primary())return this;var partner=blockEntity.pos.relative(blockEntity.blockState.get(behavior.facing).clockWise());var found=blockEntity.world.getBlockEntityAtIfLoaded(partner);return found==null?null:found.controller.get(FramingController.class,0);}
    public Item input(int slot){return input[slot].copy();}
    public boolean accepts(int slot,Item item){return item.isEmpty() || slot==0?item.isEmpty()||FrameLibrary.target(item)!=null:FrameLibrary.isMaterial(item);}
    public boolean set(int slot,Item item){if(slot<0||slot>3||!accepts(slot,item))return false;Item[] before=beforeInputs();
        if(slot==0&&!item.isEmpty()){FrameData frame=FrameData.read(item);if(!frame.side().isEmpty()){
            if(!input[1].isEmpty()||!input[2].isEmpty()||!input[3].isEmpty())return false;
            Item base=frame.baseItem()==null?Item.byId(Key.of(frame.base())):ItemStackUtils.wrap(ItemStackUtils.parseCachedMinecraftItem(frame.baseItem(),frame.dataVersion()));base.count(item.count());if(item.getSparrowTag(DrawerController.DATA_KEY) instanceof CompoundTag stored){var copy=stored.copy();copy.putString("block",base.id().toString());base.setSparrowTag(copy,DrawerController.DATA_KEY);base.maxStackSize(1);}input[0]=base;
            String[] parts={frame.side(),frame.trim(),frame.front()};for(int i=0;i<3;i++)input[i+1]=parts[i].isEmpty()?Item.empty():Item.byId(Key.of(parts[i])).count(item.count());changed();publishInputs(before);return true;
        }}
        input[slot]=item.copy();changed();publishInputs(before);return true;
    }
    public Item result(){if(input[0].isEmpty()||input[1].isEmpty())return Item.empty();Item result=FrameLibrary.frame(input[0],input[1],input[2],input[3]);if(result.isEmpty())return result;int amount=Math.min(input[0].count(),result.maxStackSize());for(int i=1;i<4;i++)if(!input[i].isEmpty())amount=Math.min(amount,input[i].count());return result.count(amount);}
    public Item craft(int requested){Item result=result();if(result.isEmpty())return result;int amount=Math.min(requested,result.count());if(amount<=0)return Item.empty();Item[] before=beforeInputs();for(int i=0;i<4;i++)if(!input[i].isEmpty()){input[i].shrink(amount);if(input[i].isEmpty())input[i]=Item.empty();}changed();publishInputs(before);return result.count(amount);}
    private Item[] beforeInputs(){if(!com.cendreal.storagedrawers.module.storage.StorageEvents.slotsObserved())return null;return Arrays.stream(input).map(Item::copy).toArray(Item[]::new);}
    private void publishInputs(Item[] before){if(before==null)return;for(int i=0;i<4;i++)com.cendreal.storagedrawers.module.storage.StorageEvents.slot(this,com.cendreal.storagedrawers.api.event.StorageSlotChangeEvent.Kind.FRAMING_INPUT,i,before[i],before[i].count(),input[i],input[i].count());}
    public void changed(){if(blockEntity.world!=null)blockEntity.world.blockEntityChanged(blockEntity.pos);StorageDrawersCE.instance().framingMenus().changed(this);}
    @Override public void saveCustomData(CompoundTag root){super.saveCustomData(root);var data=new CompoundTag();data.putInt("data_version",VersionHelper.WORLD_VERSION);for(int i=0;i<4;i++)if(!input[i].isEmpty())data.put("input"+i,ItemStackUtils.saveMinecraftItemStackAsTag(input[i].minecraftItem()));root.put("storagedrawers:framing_table",data);}
    @Override public void loadCustomData(CompoundTag root){super.loadCustomData(root);var data=root.getCompound("storagedrawers:framing_table");if(data==null)return;for(int i=0;i<4;i++){var tag=data.get("input"+i);input[i]=tag==null?Item.empty():ItemStackUtils.wrap(ItemStackUtils.parseCachedMinecraftItem(tag,data.getInt("data_version",VersionHelper.WORLD_VERSION)));}}
    @Override public void onRemove(){
        if(primary()){for(Item item:input)if(!item.isEmpty())blockEntity.world.world().dropItemNaturally(new net.momirealms.craftengine.core.world.Vec3d(blockEntity.pos.x+.5,blockEntity.pos.y+.5,blockEntity.pos.z+.5),item);Arrays.fill(input,Item.empty());}
        StorageDrawersCE.instance().framingMenus().changed(this);
        if(!removingPair){removingPair=true;try{var direction=blockEntity.blockState.get(behavior.facing);var position=blockEntity.pos.relative(primary()?direction.counterClockWise():direction.clockWise());var other=blockEntity.world.getBlockEntityAtIfLoaded(position);if(other!=null&&other.controller.get(FramingController.class,0) instanceof FramingController pair&&pair.primary()!=primary()&&other.blockState.get(behavior.facing)==direction)CraftEngineBlocks.remove(((org.bukkit.World)blockEntity.world.world().platformWorld()).getBlockAt(position.x,position.y,position.z));}finally{removingPair=false;}}
        super.onRemove();
    }
}
