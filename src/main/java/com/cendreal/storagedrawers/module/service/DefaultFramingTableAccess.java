package com.cendreal.storagedrawers.module.service;
import com.cendreal.storagedrawers.api.*;
import com.cendreal.storagedrawers.module.frame.FramingController;
import com.cendreal.storagedrawers.module.storage.*;
import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.item.Item;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
final class DefaultFramingTableAccess implements FramingTableAccess {
    private final DefaultStorageDrawersApi api;private final StoragePosition position;
    DefaultFramingTableAccess(DefaultStorageDrawersApi api,StoragePosition position){this.api=api;this.position=position;}
    private FramingController current(){api.thread();var location=position.location();return location.isPresent()&&api.node(location.get()) instanceof FramingController table&&table.primary()?table:null;}
    private FramingController require(){var result=current();if(result==null)throw new IllegalStateException("Framing table unloaded or removed");return result;}
    @Override public StoragePosition getPosition(){return position;}@Override public boolean isAvailable(){return current()!=null;}
    @Override public ItemStack getInput(int slot){if(slot<0||slot>3)throw new IndexOutOfBoundsException(slot);return StorageEvents.stack(require().input(slot));}@Override public ItemStack getResult(){return StorageEvents.stack(require().result());}
    @Override public boolean setInput(int slot,ItemStack item,Player actor){var table=current();if(table==null||slot<0||slot>3||actor!=null&&!DrawerInteraction.allowed(BukkitAdaptor.adapt(actor),table)||item!=null&&item.getAmount()>item.getMaxStackSize())return false;return StorageContext.run(actor,StorageCause.API,()->table.set(slot,item==null||item.isEmpty()?Item.empty():BukkitAdaptor.adapt(item.clone())));}
    @Override public ItemStack craft(int amount,Player actor){var table=current();if(table==null||amount<=0||actor!=null&&!DrawerInteraction.allowed(BukkitAdaptor.adapt(actor),table))return new ItemStack(Material.AIR);return StorageContext.run(actor,StorageCause.API,()->StorageEvents.stack(table.craft(amount)));}
}
