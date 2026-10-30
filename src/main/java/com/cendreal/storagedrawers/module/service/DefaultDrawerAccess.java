package com.cendreal.storagedrawers.module.service;
import com.cendreal.storagedrawers.api.*;
import com.cendreal.storagedrawers.module.storage.*;
import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;
import com.cendreal.storagedrawers.module.frame.FrameLibrary;
import com.cendreal.storagedrawers.compatibility.craftengine.ContentsLoot;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.item.Item;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.function.Function;
final class DefaultDrawerAccess implements DrawerAccess {
    private final DefaultStorageDrawersApi api;private final StoragePosition position;
    DefaultDrawerAccess(DefaultStorageDrawersApi api,StoragePosition position){this.api=api;this.position=position;}
    private DrawerController current(){api.thread();var location=position.location();return location.isPresent()&&api.node(location.get()) instanceof DrawerController drawer?drawer:null;}
    private DrawerController require(){var value=current();if(value==null)throw new IllegalStateException("Drawer unloaded or removed: "+position);return value;}
    private <T>T write(Player actor,T unavailable,Function<DrawerController,T> operation){var drawer=current();if(drawer==null||actor!=null&&!DrawerInteraction.allowed(BukkitAdaptor.adapt(actor),drawer))return unavailable;return StorageContext.run(actor,StorageCause.API,()->operation.apply(drawer));}
    private static Item item(ItemStack item){return item==null||item.isEmpty()?Item.empty():BukkitAdaptor.adapt(item.clone());}
    @Override public StoragePosition getPosition(){return position;}@Override public boolean isAvailable(){return current()!=null;}@Override public DrawerSnapshot getSnapshot(){return StorageEvents.snapshot(require());}
    @Override public NamespacedKey getBlockId(){return NamespacedKey.fromString(require().behavior.block().id().toString());}@Override public int getSlotCount(){return require().size();}
    @Override public ItemStack getItem(int slot){return StorageEvents.stack(require().template(slot));}@Override public int getCount(int slot){return require().count(slot);}@Override public int getCapacity(int slot){return require().capacity(slot);}@Override public boolean isMissing(int slot){return require().missing(slot);}
    @Override public ItemStack getUpgrade(int slot){return StorageEvents.stack(require().upgradeItem(slot));}@Override public Optional<UUID> getOwner(){return Optional.ofNullable(require().ownerId());}@Override public int getPriority(){return require().priority();}@Override public DrawerSnapshot.Frame getFrame(){return StorageEvents.state(require()).frame();}
    @Override public boolean isLocked(){return require().locked();}@Override public boolean isQuantityVisible(){return require().quantity();}@Override public boolean isConcealed(){return require().concealed();}@Override public boolean isSuspended(){return require().suspended();}
    @Override public boolean setItem(int slot,ItemStack item,int count,Player actor){return write(actor,false,d->d.replaceSlot(slot,item(item),count));}
    @Override public boolean setCount(int slot,int count,Player actor){return write(actor,false,d->slot>=0&&slot<d.size()&&d.replaceSlot(slot,d.template(slot),count));}
    @Override public boolean setCompactingContents(ItemStack seed,int count,Player actor){return write(actor,false,d->d.replaceCompacting(item(seed),count));}
    @Override public boolean setUpgrade(int slot,ItemStack upgrade,Player actor){return write(actor,false,d->d.replaceUpgrade(slot,item(upgrade)));}
    @Override public boolean setOwner(UUID owner,Player actor){return write(actor,false,d->d.setOwnerId(owner));}@Override public boolean setPriority(int priority,Player actor){return write(actor,false,d->d.setPriorityValue(priority));}
    @Override public boolean setFrame(ItemStack side,ItemStack trim,ItemStack front,Player actor){return write(actor,false,d->{if(!d.behavior.framed)return false;var result=FrameLibrary.frame(ContentsLoot.portable(d),item(side),item(trim),item(front));if(result.isEmpty())return false;var before=StorageEvents.beforeState(d);d.applyFrameFromItem(result);d.changed();StorageEvents.state(d,before);return true;});}
    @Override public boolean setSetting(StorageDrawersApi.Setting setting,boolean enabled,Player actor){return write(actor,false,d->{boolean active=switch(setting){case LOCKED->d.locked();case QUANTITY->d.quantity();case CONCEALED->d.concealed();case SUSPENDED->d.suspended();};if(active==enabled)return true;String key=switch(setting){case LOCKED->"drawer_key";case QUANTITY->"quantify_key";case CONCEALED->"shroud_key";case SUSPENDED->"suspend_key";};return actor!=null?d.useKey(key,BukkitAdaptor.adapt(actor)):d.setFlag(setting,enabled);});}
    @Override public ItemStack detach(int slot,Player actor){return write(actor,new ItemStack(Material.AIR),d->slot>=0&&slot<d.size()?StorageEvents.stack(d.detach(slot)):new ItemStack(Material.AIR));}
    @Override public boolean attach(int slot,ItemStack detached,Player actor){return write(actor,false,d->slot>=0&&slot<d.size()&&d.attach(slot,item(detached),actor==null?null:BukkitAdaptor.adapt(actor)));}
}
