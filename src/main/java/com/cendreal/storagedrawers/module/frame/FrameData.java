package com.cendreal.storagedrawers.module.frame;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import java.util.*;

public record FrameData(String base,String side,String trim,String front,CompoundTag baseItem,int dataVersion) {
    public static final String KEY="storagedrawers:frame";
    public static final FrameData EMPTY=new FrameData("","","","",null,net.momirealms.craftengine.core.util.VersionHelper.WORLD_VERSION);
    public String material(int role){String value=role==0?side:role==1?trim:front;return value.isEmpty()?side:value;}
    public CompoundTag save(){var tag=new CompoundTag();tag.putString("base",base);tag.putString("side",side);tag.putString("trim",trim);tag.putString("front",front);if(baseItem!=null)tag.put("base_item",baseItem.copy());tag.putInt("data_version",dataVersion);return tag;}
    public static FrameData read(CompoundTag tag){return new FrameData(tag.getString("base",""),tag.getString("side",""),tag.getString("trim",""),tag.getString("front",""),tag.getCompound("base_item"),tag.getInt("data_version",net.momirealms.craftengine.core.util.VersionHelper.WORLD_VERSION));}
    public static FrameData read(Item item){return item.getSparrowTag(KEY) instanceof CompoundTag tag?read(tag):EMPTY;}
    public void apply(Item item){item.setSparrowTag(save(),KEY);item.setJavaComponent("minecraft:custom_model_data",Map.of("strings",List.of(material(0),material(1),material(2))));}
}
