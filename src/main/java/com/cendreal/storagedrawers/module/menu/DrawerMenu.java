package com.cendreal.storagedrawers.module.menu;

import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.module.storage.*;
import com.google.gson.*;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The same layout drives image generation and the server slot whitelist. */
public final class DrawerMenu {
    private static final JsonObject LAYOUT;
    static {try(var in=DrawerMenu.class.getResourceAsStream("/gui-layout.json")) {if(in==null)throw new IOException("Missing GUI layout");LAYOUT=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new ExceptionInInitializerError(e);}}
    public static int[] storageCells(DrawerController drawer) {String type=(drawer.behavior.layout.compacting()?"comp_":"")+drawer.size();return LAYOUT.getAsJsonObject("storage").getAsJsonArray(type).asList().stream().mapToInt(JsonElement::getAsInt).toArray();}
    public static int[] upgradeCells() {return LAYOUT.getAsJsonArray("upgrades").asList().stream().mapToInt(JsonElement::getAsInt).toArray();}
    private final Map<DrawerController,Set<Holder>> open=new IdentityHashMap<>();
    private final Set<DrawerController> dirty=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean scheduled;
    private static final class Holder implements InventoryHolder {
        final DrawerController drawer;final Inventory inventory;final int[] storage,upgrades;
        Holder(DrawerController drawer,Player player) {
            this.drawer=drawer;storage=storageCells(drawer);upgrades=upgradeCells();
            String type=(drawer.behavior.layout.compacting()?"comp_":"")+drawer.size();
            String title="<white><shift:-8><image:storagedrawers:gui_"+type+"><shift:-169><dark_gray>�������<shift:-36><font:storagedrawers:upgrade_label>����</font>";
            var parsed=AdventureHelper.deserialize(title,PlayerOptionalContext.of(BukkitAdaptor.adapt(player)));
            inventory=Bukkit.createInventory(this,LAYOUT.get("rows").getAsInt()*9,GsonComponentSerializer.gson().deserialize(AdventureHelper.componentToJson(parsed)));
        }
        @Override public Inventory getInventory(){return inventory;}
        int storage(int cell){for(int i=0;i<storage.length;i++)if(storage[i]==cell)return i;return -1;}
        int upgrade(int cell){for(int i=0;i<upgrades.length;i++)if(upgrades[i]==cell)return i;return -1;}
        void refresh() {
            inventory.clear();
            for(int i=0;i<storage.length;i++) {
                Item template=drawer.template(i);if(template.isEmpty())continue;
                ItemStack display=((ItemStack)template.platformItem()).clone();display.setAmount(Math.max(1,Math.min(drawer.count(i),display.getMaxStackSize())));
                var meta=display.getItemMeta();
                List<Component> lore=new ArrayList<>();if(meta.hasLore())lore.addAll(meta.lore());
                lore.add(Component.text("������"+drawer.count(i)+" / "+drawer.capacity(i),NamedTextColor.GRAY));
                lore.add(Component.text("������� / �Ҽ�����������ͬ����Ʒ",NamedTextColor.DARK_GRAY));
                if(drawer.count(i)==0)lore.add(Component.text(drawer.behavior.layout.compacting()?"��ǰ�����޿�ȡ������Ʒ":"��������������ǰΪ��",NamedTextColor.YELLOW));
                meta.lore(lore);display.setItemMeta(meta);inventory.setItem(storage[i],display);
            }
            var installed=drawer.upgrades();for(int i=0;i<upgrades.length;i++)if(!installed.get(i).isEmpty())inventory.setItem(upgrades[i],(ItemStack)drawer.upgradeItem(i).platformItem());
        }
    }
    public void open(Player player,DrawerController drawer){if(!DrawerInteraction.allowed(BukkitAdaptor.adapt(player),drawer)||!com.cendreal.storagedrawers.module.storage.StorageEvents.open(drawer,player,false))return;Holder holder=new Holder(drawer,player);holder.refresh();open.computeIfAbsent(drawer,k->Collections.newSetFromMap(new IdentityHashMap<>())).add(holder);player.openInventory(holder.inventory);}
    public void changed(DrawerController drawer){if(!open.containsKey(drawer))return;dirty.add(drawer);if(!scheduled){scheduled=true;Bukkit.getScheduler().runTask(StorageDrawersCE.instance(),()->{scheduled=false;var pending=new ArrayList<>(dirty);dirty.clear();for(var d:pending)for(var h:new ArrayList<>(open.getOrDefault(d,Set.of()))) {if(!d.blockEntity().isValid())for(var viewer:List.copyOf(h.inventory.getViewers()))viewer.closeInventory();else h.refresh();}});}}
    private boolean valid(Player player,Holder holder){var e=holder.drawer.blockEntity();return DrawerInteraction.allowed(BukkitAdaptor.adapt(player),holder.drawer) && player.getWorld()==e.world.world().platformWorld() && player.getLocation().distanceSquared(new org.bukkit.Location(player.getWorld(),e.pos.x+.5,e.pos.y+.5,e.pos.z+.5))<=64;}
    public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof Holder holder) || !(event.getWhoClicked() instanceof Player player))return;
        if(!valid(player,holder)){event.setCancelled(true);player.closeInventory();return;}
        int raw=event.getRawSlot();
        if(event.getAction()==InventoryAction.COLLECT_TO_CURSOR){event.setCancelled(true);return;}
        if(raw>=holder.inventory.getSize()) {
            if(event.isShiftClick()) {event.setCancelled(true);ItemStack source=event.getCurrentItem();if(source==null || source.isEmpty())return;Item input=BukkitAdaptor.adapt(source);String id=DrawerInteraction.tool(input);int used=0;
                if(Upgrades.isUpgrade(id)){if(holder.drawer.install(input))used=1;}
                else {int match=holder.drawer.find(input);if(match<0)for(int i=0;i<holder.drawer.size();i++)if(holder.drawer.template(i).isEmpty()){match=i;break;}if(match>=0)used=holder.drawer.put(match,input,source.getAmount(),BukkitAdaptor.adapt(player));}
                if(used>0){source.setAmount(source.getAmount()-used);event.setCurrentItem(source);}holder.refresh();}
            return;
        }
        if(raw<0)return;
        event.setCancelled(true);
        if(event.getClick()==ClickType.NUMBER_KEY){int hotbar=event.getHotbarButton();if(hotbar>=0 && hotbar<9)player.getInventory().setItem(hotbar,transfer(player,holder,raw,player.getInventory().getItem(hotbar),false));holder.refresh();return;}
        if(event.isShiftClick()){shiftOut(player,holder,raw);holder.refresh();return;}
        if(event.getClick()!=ClickType.LEFT && event.getClick()!=ClickType.RIGHT)return;
        event.setCursor(transfer(player,holder,raw,event.getCursor(),event.isRightClick()));holder.refresh();
    }
    private boolean canRemove(Player player,String id){return player.getGameMode()==org.bukkit.GameMode.CREATIVE || !id.startsWith("creative_");}
    private ItemStack transfer(Player player,Holder holder,int raw,ItemStack cursor,boolean right){
        if(cursor==null)cursor=new ItemStack(Material.AIR);else cursor=cursor.clone();
        int upgrade=holder.upgrade(raw),slot=holder.storage(raw);
        if(upgrade>=0){String current=holder.drawer.upgrades().get(upgrade);
            if(!canRemove(player,current))return cursor;
            if(cursor.isEmpty()){if(current.isEmpty())return cursor;Item removed=holder.drawer.uninstallItem(upgrade);return removed.isEmpty()?cursor:(ItemStack)removed.platformItem();}
            String id=DrawerInteraction.tool(BukkitAdaptor.adapt(cursor));if(!Upgrades.isUpgrade(id) || !current.isEmpty() && cursor.getAmount()!=1)return cursor;
            Item old=holder.drawer.upgradeItem(upgrade);if(!holder.drawer.replaceUpgrade(upgrade,BukkitAdaptor.adapt(cursor)))return cursor;
            if(!current.isEmpty())return (ItemStack)old.platformItem();cursor.setAmount(cursor.getAmount()-1);return cursor;
        }
        if(slot<0)return cursor;
        if(!cursor.isEmpty()) {Item input=BukkitAdaptor.adapt(cursor);int used=holder.drawer.put(slot,input,right?1:cursor.getAmount(),BukkitAdaptor.adapt(player));cursor.setAmount(cursor.getAmount()-used);return cursor;}
        return (ItemStack)holder.drawer.take(slot,right?1:holder.drawer.template(slot).maxStackSize(),BukkitAdaptor.adapt(player)).platformItem();
    }
    private void shiftOut(Player player,Holder holder,int raw){
        int slot=holder.storage(raw),upgrade=holder.upgrade(raw);ItemStack prototype;
        if(slot>=0){if(holder.drawer.count(slot)==0)return;prototype=(ItemStack)holder.drawer.template(slot).platformItem();}
        else if(upgrade>=0){String id=holder.drawer.upgrades().get(upgrade);if(id.isEmpty()||!canRemove(player,id))return;prototype=(ItemStack)holder.drawer.upgradeItem(upgrade).platformItem();}
        else return;
        int room=0;for(ItemStack stack:player.getInventory().getStorageContents())if(stack==null || stack.isEmpty())room+=prototype.getMaxStackSize();else if(stack.isSimilar(prototype))room+=Math.max(0,stack.getMaxStackSize()-stack.getAmount());
        if(room<=0)return;
        ItemStack result;
        if(slot>=0)result=(ItemStack)holder.drawer.take(slot,Math.min(room,prototype.getMaxStackSize()),BukkitAdaptor.adapt(player)).platformItem();else {String removed=holder.drawer.uninstall(upgrade);if(removed==null)return;result=prototype;}
        if(!result.isEmpty())player.getInventory().addItem(result);
    }
    public void drag(InventoryDragEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof Holder holder) || !(event.getWhoClicked() instanceof Player player))return;
        if(event.getRawSlots().stream().noneMatch(i->i<holder.inventory.getSize()))return;
        event.setCancelled(true);
        if(!valid(player,holder)){player.closeInventory();return;}
        // Mixed drags are rejected atomically. Valid single-slot drags use the same transaction as clicks.
        if(event.getNewItems().size()!=1)return;
        int raw=event.getNewItems().keySet().iterator().next();if(raw>=holder.inventory.getSize() || holder.storage(raw)<0 && holder.upgrade(raw)<0)return;
        ItemStack cursor=transfer(player,holder,raw,event.getOldCursor(),true);
        event.getView().setCursor(cursor);holder.refresh();
    }
    public void close(InventoryCloseEvent event){if(!(event.getInventory().getHolder() instanceof Holder holder))return;var holders=open.get(holder.drawer);if(holders!=null){holders.remove(holder);if(holders.isEmpty()){open.remove(holder.drawer);dirty.remove(holder.drawer);}}}
    public void closeAll(){for(var holders:new ArrayList<>(open.values()))for(var holder:new ArrayList<>(holders))for(var viewer:List.copyOf(holder.inventory.getViewers()))viewer.closeInventory();open.clear();dirty.clear();}
}
