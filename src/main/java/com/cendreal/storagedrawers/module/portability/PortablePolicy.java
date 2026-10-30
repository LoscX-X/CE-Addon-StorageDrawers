package com.cendreal.storagedrawers.module.portability;

import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.module.storage.DrawerController;
import com.cendreal.storagedrawers.module.automation.*;
import net.momirealms.craftengine.bukkit.api.*;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

/** Ports the mod's container nesting rules and its three-second player inventory maintenance. */
public final class PortablePolicy {
    public static boolean storedItems(Item item){if(item.getSparrowTag(DrawerController.DATA_KEY) instanceof CompoundTag data){if(data.getLong("units",0)>0)return true;for(int i=0;i<4;i++){var slot=data.getCompound("slot"+i);if(slot!=null&&slot.getInt("count",0)>0)return true;}}return false;}
    public static boolean drawerPayload(Item item){if(item.customId().isEmpty()||!item.hasTag(DrawerController.DATA_KEY)&&!item.hasTag("storagedrawers:frame"))return false;var definition=CraftEngineBlocks.byId(item.id());var behavior=definition==null?null:definition.defaultState().behavior().getFirst(com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior.class);return behavior!=null&&behavior.layout.storage();}
    public static boolean forbiddenInContainerItem(Item item){String id=DrawerInteraction.tool(item);var config=StorageDrawersCE.instance().getConfig();if(id.startsWith("detached_drawer"))return !config.getBoolean("detached-drawers.allow-in-container-items",false);if(item.customId().isEmpty())return false;var block=CraftEngineBlocks.byId(item.id());var behavior=block==null?null:block.defaultState().behavior().getFirst(com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior.class);return behavior!=null&&behavior.layout.storage()&&!config.getBoolean("filled-drawers.allow-in-container-items",false);}
    private boolean forbidden(ItemStack stack){return stack!=null&&!stack.isEmpty()&&forbiddenInContainerItem(BukkitAdaptor.adapt(stack));}
    public void click(InventoryClickEvent event){var top=event.getView().getTopInventory();if(top.getType()==InventoryType.SHULKER_BOX){boolean insert=event.getRawSlot()>=0&&event.getRawSlot()<top.getSize()&&forbidden(event.getCursor())||event.isShiftClick()&&event.getRawSlot()>=top.getSize()&&forbidden(event.getCurrentItem());if(event.getClick()==ClickType.NUMBER_KEY&&event.getRawSlot()>=0&&event.getRawSlot()<top.getSize())insert|=forbidden(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));if(insert){event.setCancelled(true);return;}}
        ItemStack current=event.getCurrentItem(),cursor=event.getCursor();if(bundle(current)&&forbidden(cursor)||bundle(cursor)&&forbidden(current))event.setCancelled(true);
    }
    private boolean bundle(ItemStack item){return item!=null&&(item.getType()==Material.BUNDLE||item.getType().name().endsWith("_BUNDLE"));}
    public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getType()!=InventoryType.SHULKER_BOX)return;for(var entry:event.getNewItems().entrySet())if(entry.getKey()<event.getView().getTopInventory().getSize()&&forbidden(entry.getValue())){event.setCancelled(true);return;}}
    public void move(InventoryMoveItemEvent event){if(event.getDestination().getType()==InventoryType.SHULKER_BOX&&forbidden(event.getItem()))event.setCancelled(true);}
    public static boolean heavy(Item item){var config=StorageDrawersCE.instance().getConfig();if(config.getBoolean("filled-drawers.heavy",false)&&item.getSparrowTag(DrawerController.DATA_KEY) instanceof CompoundTag data){for(int i=0;i<7;i++)if(data.getString("upgrade"+i,"").equals("portability_upgrade"))return false;return storedItems(item);}
        if(config.getBoolean("detached-drawers.heavy",false)&&item.getSparrowTag("storagedrawers:detached") instanceof CompoundTag data)return data.getBoolean("heavy",false)&&data.getInt("count",0)>data.getInt("stack_size",64);return false;}
    public void maintain(Player player){boolean heavy=false;var inventory=player.getInventory();for(int i=0;i<inventory.getSize();i++){ItemStack stack=inventory.getItem(i);if(stack==null||stack.isEmpty()||stack.getType()!=Material.PAPER)continue;Item item=BukkitAdaptor.adapt(stack);String id=DrawerInteraction.tool(item);
            if(id.startsWith("remote_")&&id.endsWith("_bound")){RemoteBinding binding=RemoteBinding.read(item);if(binding!=null){var world=Bukkit.getWorld(binding.world());var pos=net.momirealms.craftengine.core.world.BlockPos.of(binding.position());if(world!=null&&world.isChunkLoaded(pos.x>>4,pos.z>>4)){var entity=BukkitAdaptor.adapt(world).storageWorld().getBlockEntityAtIfLoaded(pos);if(entity==null||!(entity.controller.get(NetworkController.class,0) instanceof NetworkController network)||!network.behavior.layout.kind().equals("controller")){inventory.setItem(i,(ItemStack)Item.byId(Key.of("storagedrawers:"+id.replace("_bound",""))).count(stack.getAmount()).platformItem());}}}}
            heavy|=heavy(item);
        }
        if(heavy)player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS,100,3,true,true));
    }
}
