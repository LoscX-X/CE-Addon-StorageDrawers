package com.cendreal.storagedrawers.module.menu;

import com.cendreal.storagedrawers.module.frame.FramingController;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import com.google.gson.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class FramingMenu {
    private static final JsonObject LAYOUT;
    static{try(var in=FramingMenu.class.getResourceAsStream("/gui-layout.json")){LAYOUT=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("framing");}catch(IOException e){throw new ExceptionInInitializerError(e);}}
    private static final int[] INPUTS=LAYOUT.getAsJsonArray("inputs").asList().stream().mapToInt(JsonElement::getAsInt).toArray();
    private static final int OUTPUT=LAYOUT.get("result").getAsInt();
    private final Map<FramingController,Set<Holder>> open=new IdentityHashMap<>();
    private final Set<FramingController> dirty=Collections.newSetFromMap(new IdentityHashMap<>());private boolean scheduled;
    private static final class Holder implements InventoryHolder{
        final FramingController table;final Inventory inventory;
        Holder(FramingController table,Player player){this.table=table;String title="<white><shift:-8><image:storagedrawers:gui_framing><shift:-169><dark_gray>�����̨<shift:-27><font:storagedrawers:framing_labels>Ŀ��</font><shift:18><font:storagedrawers:framing_labels>����߿�����</font><shift:18><font:storagedrawers:framing_labels>��Ʒ</font>";inventory=Bukkit.createInventory(this,45,GsonComponentSerializer.gson().deserialize(AdventureHelper.componentToJson(AdventureHelper.deserialize(title,PlayerOptionalContext.of(BukkitAdaptor.adapt(player))))));}
        @Override public Inventory getInventory(){return inventory;}
        void refresh(){inventory.clear();for(int i=0;i<4;i++)inventory.setItem(INPUTS[i],(ItemStack)table.input(i).platformItem());inventory.setItem(OUTPUT,(ItemStack)table.result().platformItem());}
        int input(int raw){for(int i=0;i<INPUTS.length;i++)if(raw==INPUTS[i])return i;return -1;}
    }
    public void open(Player player,FramingController part){var table=part.primaryController();if(table==null||!DrawerInteraction.allowed(BukkitAdaptor.adapt(player),table)||!com.cendreal.storagedrawers.module.storage.StorageEvents.open(table,player,true))return;Holder holder=new Holder(table,player);holder.refresh();open.computeIfAbsent(table,k->Collections.newSetFromMap(new IdentityHashMap<>())).add(holder);player.openInventory(holder.inventory);}
    public void changed(FramingController table){if(!open.containsKey(table))return;dirty.add(table);if(scheduled)return;scheduled=true;Bukkit.getScheduler().runTask(StorageDrawersCE.instance(),()->{scheduled=false;var pending=new ArrayList<>(dirty);dirty.clear();for(var entry:pending)for(var h:new ArrayList<>(open.getOrDefault(entry,Set.of())))if(!entry.blockEntity().isValid())for(var viewer:List.copyOf(h.inventory.getViewers()))viewer.closeInventory();else h.refresh();});}
    private boolean valid(Player player,Holder holder){var e=holder.table.blockEntity();return DrawerInteraction.allowed(BukkitAdaptor.adapt(player),holder.table)&&player.getWorld()==e.world.world().platformWorld()&&player.getLocation().distanceSquared(new Location(player.getWorld(),e.pos.x+.5,e.pos.y+.5,e.pos.z+.5))<=64;}
    private ItemStack transfer(Holder h,int raw,ItemStack cursor,boolean right){cursor=cursor==null?new ItemStack(Material.AIR):cursor.clone();if(raw==OUTPUT){Item result=h.table.result();if(result.isEmpty())return cursor;ItemStack preview=(ItemStack)result.platformItem();if(!cursor.isEmpty()&&!cursor.isSimilar(preview))return cursor;int room=cursor.isEmpty()?preview.getMaxStackSize():cursor.getMaxStackSize()-cursor.getAmount();int amount=Math.min(room,right?1:preview.getAmount());if(amount<=0)return cursor;ItemStack crafted=(ItemStack)h.table.craft(amount).platformItem();if(cursor.isEmpty())return crafted;cursor.setAmount(cursor.getAmount()+crafted.getAmount());return cursor;}
        int input=h.input(raw);if(input<0)return cursor;Item old=h.table.input(input);
        if(cursor.isEmpty()){if(old.isEmpty())return cursor;int amount=right?(old.count()+1)/2:old.count();Item result=old.copyWithCount(amount);old.shrink(amount);h.table.set(input,old);return (ItemStack)result.platformItem();}
        Item held=BukkitAdaptor.adapt(cursor);if(!h.table.accepts(input,held))return cursor;
        if(old.isEmpty()||old.isSimilar(held)){int amount=Math.min(right?1:cursor.getAmount(),old.isEmpty()?held.maxStackSize():old.maxStackSize()-old.count());if(amount<=0)return cursor;Item next=held.copyWithCount((old.isEmpty()?0:old.count())+amount);if(h.table.set(input,next))cursor.setAmount(cursor.getAmount()-amount);return cursor;}
        if(h.table.set(input,held))return (ItemStack)old.platformItem();return cursor;
    }
    public void click(InventoryClickEvent event){if(!(event.getView().getTopInventory().getHolder() instanceof Holder h)||!(event.getWhoClicked() instanceof Player player))return;if(!valid(player,h)){event.setCancelled(true);player.closeInventory();return;}int raw=event.getRawSlot();if(event.getAction()==InventoryAction.COLLECT_TO_CURSOR){event.setCancelled(true);return;}
        if(raw>=45){if(event.isShiftClick()){event.setCancelled(true);ItemStack current=event.getCurrentItem();if(current==null||current.isEmpty())return;Item input=BukkitAdaptor.adapt(current);for(int i=0;i<4;i++)if(h.table.accepts(i,input)&&(h.table.input(i).isEmpty()||h.table.input(i).isSimilar(input))){ItemStack remainder=transfer(h,INPUTS[i],current,false);event.setCurrentItem(remainder);break;}h.refresh();}return;}
        if(raw<0)return;event.setCancelled(true);if(event.getClick()==ClickType.NUMBER_KEY){int key=event.getHotbarButton();if(key>=0&&key<9)player.getInventory().setItem(key,transfer(h,raw,player.getInventory().getItem(key),false));h.refresh();return;}
        if(event.isShiftClick()){Item source=raw==OUTPUT?h.table.result():h.input(raw)>=0?h.table.input(h.input(raw)):Item.empty();if(source.isEmpty())return;ItemStack stack=(ItemStack)source.platformItem();int room=0;for(ItemStack part:player.getInventory().getStorageContents())if(part==null||part.isEmpty())room+=stack.getMaxStackSize();else if(part.isSimilar(stack))room+=Math.max(0,part.getMaxStackSize()-part.getAmount());int amount=Math.min(room,stack.getAmount());if(amount<=0)return;Item taken;if(raw==OUTPUT)taken=h.table.craft(amount);else{taken=source.copyWithCount(amount);source.shrink(amount);h.table.set(h.input(raw),source);}player.getInventory().addItem((ItemStack)taken.platformItem());h.refresh();return;}
        if(event.getClick()!=ClickType.LEFT&&event.getClick()!=ClickType.RIGHT)return;event.setCursor(transfer(h,raw,event.getCursor(),event.isRightClick()));h.refresh();
    }
    public void drag(InventoryDragEvent event){if(!(event.getView().getTopInventory().getHolder() instanceof Holder h)||!(event.getWhoClicked() instanceof Player player)||event.getRawSlots().stream().noneMatch(i->i<45))return;event.setCancelled(true);if(!valid(player,h)||event.getNewItems().size()!=1)return;int raw=event.getNewItems().keySet().iterator().next();if(h.input(raw)<0)return;event.getView().setCursor(transfer(h,raw,event.getOldCursor(),true));h.refresh();}
    public void close(InventoryCloseEvent event){if(!(event.getInventory().getHolder() instanceof Holder h))return;var holders=open.get(h.table);if(holders!=null){holders.remove(h);if(holders.isEmpty()){open.remove(h.table);dirty.remove(h.table);}}}
    public void closeAll(){for(var group:new ArrayList<>(open.values()))for(var holder:new ArrayList<>(group))for(var viewer:List.copyOf(holder.inventory.getViewers()))viewer.closeInventory();open.clear();dirty.clear();}
}
