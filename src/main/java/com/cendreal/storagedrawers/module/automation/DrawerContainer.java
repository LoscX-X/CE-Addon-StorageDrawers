package com.cendreal.storagedrawers.module.automation;

import com.cendreal.storagedrawers.module.storage.DrawerController;
import com.cendreal.storagedrawers.module.storage.NodeController;
import net.momirealms.craftengine.bukkit.world.BukkitContainer;
import net.momirealms.craftengine.core.world.WorldlyContainer;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Direction;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.*;

/** One empty insertion cell and one single-item extraction cell per drawer, independent of capacity. */
public class DrawerContainer implements BukkitContainer,WorldlyContainer,InventoryHolder {
    public record Access(DrawerController drawer,int slot) {}
    protected final NodeController node;
    private final Object nativeContainer;
    private final Inventory inventory;
    protected Access[] access;
    protected int[] cells;
    protected DrawerContainer(NodeController node) {
        this.node=node;
        nativeContainer=CraftEngine.instance().platform().createContainer(this);
        inventory=new CraftInventory((net.minecraft.world.Container)nativeContainer);
    }
    public DrawerContainer(DrawerController drawer) {this((NodeController)drawer);Access[] slots=new Access[drawer.size()];for(int i=0;i<slots.length;i++)slots[i]=new Access(drawer,i);bind(slots);}
    protected final void bind(Access[] slots) {access=slots;cells=new int[slots.length*2];for(int i=0;i<cells.length;i++)cells[i]=i;}
    protected void refresh() {}
    protected Access at(int cell) {refresh();return cell<0 || cell/2>=access.length?null:access[cell/2];}
    public Object nativeContainer() {refresh();return nativeContainer;}
    @Override public int containerSize() {refresh();return cells==null?0:cells.length;}
    @Override public int[] getSlotsForFace(Direction direction) {refresh();return cells;}
    @Override public boolean isEmpty() {refresh();for(Access a:access)if(a.drawer().count(a.slot())>0)return false;return true;}
    @Override public Item getItem(int cell) {Access a=at(cell);return a==null || cell%2==0 || a.drawer().count(a.slot())==0?Item.empty():a.drawer().template(a.slot()).copyWithCount(1);}
    @Override public Item removeItem(int cell,int amount) {Access a=at(cell);return a==null || cell%2==0 || amount<=0?Item.empty():a.drawer().take(a.slot(),1);}
    @Override public Item removeItemNoUpdate(int cell) {return removeItem(cell,1);}
    @Override public void setItem(int cell,Item item) {
        Access a=at(cell);if(a==null)return;
        if(cell%2==0) {if(!item.isEmpty() && canPlaceItem(cell,item)) a.drawer().put(a.slot(),item,Math.min(1,item.count()));}
        else if(item.isEmpty() && a.drawer().count(a.slot())>0) a.drawer().take(a.slot(),1);
    }
    @Override public boolean canPlaceItem(int cell,Item item) {
        Access a=at(cell);if(a==null || a.drawer().suspended() || cell%2!=0 || item.isEmpty() || !a.drawer().accepts(a.slot(),item)) return false;
        if(node.behavior.layout.network() && a.drawer().template(a.slot()).isEmpty()) return false;
        if(a.drawer().has("void_upgrade"))return true;
        return a.drawer().room(a.slot())>0;
    }
    @Override public boolean canPlaceItemThroughFace(int cell,Item item,Direction direction) {return canPlaceItem(cell,item);}
    @Override public boolean canTakeItemThroughFace(int cell,Item item,Direction direction) {Access a=at(cell);return a!=null && !a.drawer().suspended() && cell%2==1 && a.drawer().count(a.slot())>0;}
    @Override public int maxStackSize() {return 1;}
    @Override public void setMaxStackSize(int size) {}
    @Override public void setChanged() {if(node.blockEntity().world!=null)node.blockEntity().world.blockEntityChanged(node.blockEntity().pos);}
    @Override public boolean stillValid(Player player) {return node.blockEntity().isValid();}
    @Override public void clearContent() {throw new UnsupportedOperationException("Use drawer extraction; this inventory exposes transfer cells");}
    @Override public List<Item> contents() {List<Item> result=new ArrayList<>(containerSize());for(int i=0;i<containerSize();i++)result.add(getItem(i));return result;}
    @Override public WorldPosition position() {var e=node.blockEntity();return new WorldPosition(e.world.world(),e.pos.x,e.pos.y,e.pos.z);}
    @Override public void onOpen(HumanEntity player) {}
    @Override public void onClose(HumanEntity player) {}
    @Override public List<HumanEntity> getViewers() {return List.of();}
    @Override public InventoryHolder getOwner() {return this;}
    @Override public Inventory getInventory() {return inventory;}
}
