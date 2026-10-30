package com.cendreal.storagedrawers.module.crafting;

import com.cendreal.storagedrawers.module.storage.*;
import com.cendreal.storagedrawers.module.interaction.*;
import com.cendreal.storagedrawers.module.automation.RemoteBinding;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.recipe.*;
import net.momirealms.craftengine.core.item.recipe.input.*;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.libraries.nbt.*;
import com.google.gson.JsonObject;
import java.util.*;

/** Native CE recipe matching/assembly/consumption; no fabricated Bukkit crafting result. */
public final class StorageRecipe extends CustomShapelessRecipe {
    private final String kind;
    private StorageRecipe(Key id,String kind,CustomShapelessRecipe base){super(id,false,base.result(),base.visualResult(),"storagedrawers",CraftingRecipeCategory.MISC,base.ingredientsInUse(),base.functions(),c->true,true,true);this.kind=kind;}
    public static final RecipeSerializer<StorageRecipe> SERIALIZER=new RecipeSerializer<>(){
        @Override public StorageRecipe readConfig(Key id,ConfigSection section){return new StorageRecipe(id,section.getString("kind",""),CustomShapelessRecipe.SERIALIZER.readConfig(id,section));}
        @Override @SuppressWarnings("unchecked")public StorageRecipe readJson(Key id,JsonObject json){return readConfig(id,ConfigSection.ofRoot(new com.google.gson.Gson().fromJson(json,Map.class)));}
    };
    @Override public boolean matches(RecipeInput input){return input instanceof CraftingInput crafting && super.matches(input) && !build(crafting).isEmpty();}
    @Override public boolean requiresInput(){return true;}
    @Override public Item assemble(RecipeInput input,ItemBuildContext context){return input instanceof CraftingInput crafting?build(crafting):Item.empty();}
    private Item build(CraftingInput input){return switch(kind){case "add_upgrade"->addUpgrade(input);case "add_detached_upgrade"->detached(input);case "keyring"->keyring(input);case "personal_key_cycle"->personal(input);case "remote_group_upgrade"->remoteGroup(input);default->Item.empty();};}
    private Item addUpgrade(CraftingInput input){Item target=null;List<Item> upgrades=new ArrayList<>();for(var entry:input){Item item=entry.item();if(item.isEmpty())continue;String id=DrawerInteraction.tool(item);if(Upgrades.isUpgrade(id)){if(id.equals("one_stack_upgrade"))return Item.empty();upgrades.add(item);}else{var definition=CraftEngineBlocks.byId(item.id());if(target!=null||definition==null)return Item.empty();var behavior=definition.defaultState().behavior().getFirst(com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior.class);if(behavior==null||!behavior.layout.storage())return Item.empty();target=item;}}
        if(target==null||upgrades.isEmpty())return Item.empty();var definition=CraftEngineBlocks.byId(target.id());var layout=definition.defaultState().behavior().getFirst(com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior.class).layout;
        CompoundTag data=target.getSparrowTag(DrawerController.DATA_KEY) instanceof CompoundTag old?old.copy():emptyContents(target.id(),layout.drawers());
        List<String> installed=new ArrayList<>();for(int i=0;i<Upgrades.SLOTS;i++)installed.add(data.getString("upgrade"+i,""));
        for(Item upgrade:upgrades){int slot=installed.indexOf("");if(slot<0)return Item.empty();String id=DrawerInteraction.tool(upgrade);if(id.startsWith("remote_")&&RemoteBinding.read(upgrade)==null)return Item.empty();installed.set(slot,id);if(!Upgrades.valid(installed))return Item.empty();data.putString("upgrade"+slot,id);data.put("upgradeItem"+slot,ItemStackUtils.saveMinecraftItemStackAsTag(upgrade.copyWithCount(1).minecraftItem()));}
        Item result=target.copyWithCount(1);result.setSparrowTag(data,DrawerController.DATA_KEY);result.maxStackSize(1);return result;
    }
    public static CompoundTag emptyContents(Key id,int slots){var data=new CompoundTag();data.putInt("version",DrawerController.CONTENTS_VERSION);data.putBoolean("quantity",com.cendreal.storagedrawers.StorageDrawersCE.instance().getConfig().getBoolean("display.front-quantity-default",true));data.putString("block",id.toString());data.putInt("data_version",VersionHelper.WORLD_VERSION);data.putLong("units",0);for(int i=0;i<slots;i++){var slot=new CompoundTag();slot.putInt("count",0);slot.putInt("factor",1);data.put("slot"+i,slot);}return data;}
    private Item detached(CraftingInput input){Item drawer=null;int multiplier=0;for(var entry:input){Item item=entry.item();if(item.isEmpty())continue;String id=DrawerInteraction.tool(item);if(id.equals("detached_drawer")||id.equals("detached_drawer_full")){if(drawer!=null)return Item.empty();drawer=item;}else if(Upgrades.MULTIPLIERS.containsKey(id))multiplier+=Upgrades.MULTIPLIERS.get(id);else return Item.empty();}if(drawer==null)return Item.empty();
        CompoundTag data=drawer.getSparrowTag("storagedrawers:detached") instanceof CompoundTag stored?stored.copy():new CompoundTag();int stackSize=data.getInt("stack_size",64),stacks=data.getInt("stacks",data.getInt("capacity",32*64)/stackSize);if(multiplier==0){Item result=Item.byId(Key.of("storagedrawers:detached_drawer"));var empty=new CompoundTag();empty.putInt("stacks",stacks);empty.putInt("stack_size",64);empty.putInt("capacity",(int)Math.min(Integer.MAX_VALUE,stacks*64L));empty.putBoolean("heavy",true);empty.putInt("count",0);result.setSparrowTag(empty,"storagedrawers:detached");return result;}stacks=Math.toIntExact(stacks+multiplier*32L);data.putInt("stacks",stacks);data.putInt("capacity",(int)Math.min(Integer.MAX_VALUE,(long)stacks*stackSize));Item result=drawer.copyWithCount(1);result.setSparrowTag(data,"storagedrawers:detached");return result;
    }
    private Item keyring(CraftingInput input){if(input.width()!=3||input.height()!=3)return Item.empty();for(int y=0;y<3;y++)for(int x=0;x<3;x++){Item item=input.getItem(x,y).item();if(x==1&&y==1){if(!RingTools.isKey(DrawerInteraction.tool(item)))return Item.empty();}else if(x==1||y==1){if(item.isEmpty()||!item.id().equals(Key.of("minecraft:iron_nugget")))return Item.empty();}else if(!item.isEmpty())return Item.empty();}return RingTools.create(List.of(input.getItem(1,1).item().copyWithCount(1)));}
    private Item personal(CraftingInput input){Item found=null;for(var entry:input)if(!entry.isEmpty()){if(found!=null||!DrawerInteraction.tool(entry.item()).equals("personal_key"))return Item.empty();found=entry.item();}return found==null?Item.empty():found.copyWithCount(1);}
    private Item remoteGroup(CraftingInput input){if(input.width()!=3)return Item.empty();Item bound=null;for(int y=0;y<input.height();y++){Item middle=input.getItem(1,y).item();if(middle.isEmpty())continue;if(bound!=null||!DrawerInteraction.tool(middle).equals("remote_upgrade_bound"))return Item.empty();if(!input.getItem(0,y).item().id().equals(Key.of("minecraft:ender_pearl"))||!input.getItem(2,y).item().id().equals(Key.of("minecraft:ender_pearl")))return Item.empty();bound=middle;}if(bound==null||input.ingredientCount()!=3)return Item.empty();var binding=RemoteBinding.read(bound);if(binding==null)return Item.empty();Item result=Item.byId(Key.of("storagedrawers:remote_group_upgrade_bound"));result.setSparrowTag(binding.save(),RemoteBinding.KEY);return result;}
}
