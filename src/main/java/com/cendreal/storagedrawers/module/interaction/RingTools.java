package com.cendreal.storagedrawers.module.interaction;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import java.util.*;

public final class RingTools {
    private static final String KEY="storagedrawers:keyring";
    public static boolean isKey(String id){return List.of("drawer_key","quantify_key","shroud_key","personal_key","personal_key_unlock","personal_key_ftb","personal_key_cofh","suspend_key","priority_key","priority_key_p1","priority_key_p2","priority_key_n1","priority_key_n2","drawer_puller").contains(id);}
    public static List<Item> contents(Item ring){List<Item> keys=new ArrayList<>();if(ring.getSparrowTag(KEY) instanceof CompoundTag tag){int n=tag.getInt("size",0);for(int i=0;i<n;i++){var saved=tag.get("key"+i);if(saved!=null)keys.add(ItemStackUtils.wrap(ItemStackUtils.parseCachedMinecraftItem(saved,tag.getInt("data_version",VersionHelper.WORLD_VERSION))).copyWithCount(1));}}return keys;}
    public static String effectiveKey(Item item){String id=DrawerInteraction.tool(item);if(!id.startsWith("keyring"))return id;var keys=contents(item);if(!keys.isEmpty())return DrawerInteraction.tool(keys.getFirst());if(id.equals("keyring"))return "";String suffix=id.substring("keyring_".length());if(suffix.equals("puller"))return "drawer_puller";int underscore=suffix.indexOf('_');return underscore<0?suffix+"_key":suffix.substring(0,underscore)+"_key"+suffix.substring(underscore);}
    public static Item create(List<Item> keys){if(keys.isEmpty())return Item.byId(Key.of("storagedrawers:keyring"));String id=DrawerInteraction.tool(keys.getFirst());String suffix=id.equals("drawer_puller")?"puller":id.replace("_key","");Item ring=Item.byId(Key.of("storagedrawers:keyring_"+suffix));var tag=new CompoundTag();tag.putInt("size",keys.size());tag.putInt("data_version",VersionHelper.WORLD_VERSION);for(int i=0;i<keys.size();i++)tag.put("key"+i,ItemStackUtils.saveMinecraftItemStackAsTag(keys.get(i).copyWithCount(1).minecraftItem()));ring.setSparrowTag(tag,KEY);ring.maxStackSize(1);return ring;}
    public static Item rotate(Item ring){var keys=contents(ring);if(keys.size()<2)return ring;keys.add(keys.removeFirst());return create(keys);}
    public static String nextPriority(String id){return switch(id){case "priority_key"->"priority_key_p1";case "priority_key_p1"->"priority_key_p2";case "priority_key_p2"->"priority_key_n1";case "priority_key_n1"->"priority_key_n2";default->"priority_key";};}
    private RingTools(){}
}
