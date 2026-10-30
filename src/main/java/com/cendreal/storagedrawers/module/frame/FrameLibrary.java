package com.cendreal.storagedrawers.module.frame;

import com.google.gson.*;
import com.cendreal.storagedrawers.module.storage.DrawerController;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Material;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class FrameLibrary {
    private static final Set<String> MATERIALS;
    static {try(var input=FrameLibrary.class.getResourceAsStream("/frame-materials.json")){if(input==null)throw new IOException("Missing frame materials");MATERIALS=new HashSet<>();for(var id:JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonArray())MATERIALS.add(id.getAsString());}catch(IOException e){throw new ExceptionInInitializerError(e);}}
    public static boolean isMaterial(Item item){if(item.isEmpty()||item.isCustomItem()||!MATERIALS.contains(item.vanillaId().toString()))return false;Material type=Material.matchMaterial(item.vanillaId().toString());return type!=null&&type.isOccluding()&&item.isSimilar(Item.byId(item.id()));}
    public static String target(Item item){if(item.customId().isEmpty())return null;String id=item.customId().get().toString();if(!id.startsWith("storagedrawers:"))return null;String name=id.substring("storagedrawers:".length());if(name.startsWith("framed_"))return id;if(name.contains("_drawers_")&&!name.startsWith("compacting_")){int split=name.indexOf("_full_drawers_");if(split<0)split=name.indexOf("_half_drawers_");if(split>=0)return "storagedrawers:framed"+name.substring(split);}if(name.startsWith("compacting_"))return "storagedrawers:framed_"+name;if(name.endsWith("_trim"))return "storagedrawers:framed_trim";if(name.equals("controller")||name.equals("controller_io"))return "storagedrawers:framed_"+name;return null;}
    public static Item frame(Item original,Item side,Item trim,Item front){String target=target(original);if(target==null||!isMaterial(side)||!trim.isEmpty()&&!isMaterial(trim)||!front.isEmpty()&&!isMaterial(front))return Item.empty();Item definition=Item.byId(Key.of(target));Item result=original.copyWithCount(1);result.customId(Key.of(target));definition.itemModel().ifPresent(result::itemModel);definition.itemNameJson().ifPresent(result::itemNameJson);
        if(original.getSparrowTag(DrawerController.DATA_KEY) instanceof net.momirealms.craftengine.libraries.nbt.CompoundTag contents){var copy=contents.copy();copy.putString("block",target);result.setSparrowTag(copy,DrawerController.DATA_KEY);result.maxStackSize(1);}
        FrameData previous=FrameData.read(original);String base=previous.base();if(base.isEmpty())base=original.id().toString();var saved=previous.baseItem()==null?(net.momirealms.craftengine.libraries.nbt.CompoundTag)net.momirealms.craftengine.bukkit.util.ItemStackUtils.saveMinecraftItemStackAsTag(original.copyWithCount(1).minecraftItem()):previous.baseItem();new FrameData(base,side.id().toString(),trim.isEmpty()?"":trim.id().toString(),front.isEmpty()?"":front.id().toString(),saved,previous.baseItem()==null?net.momirealms.craftengine.core.util.VersionHelper.WORLD_VERSION:previous.dataVersion()).apply(result);return result;}
    public static int materialCount(){return MATERIALS.size();}
    private FrameLibrary(){}
}
