package com.cendreal.storagedrawers.module.automation;

import java.util.UUID;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;

public record RemoteBinding(UUID world,long position) {
    public static final String KEY="storagedrawers:remote";
    public CompoundTag save(){var data=new CompoundTag();data.putString("world",world.toString());data.putLong("position",position);return data;}
    public static RemoteBinding read(Item item){if(!(item.getSparrowTag(KEY) instanceof CompoundTag tag))return null;return read(tag);}
    public static RemoteBinding read(CompoundTag tag){return new RemoteBinding(UUID.fromString(tag.getString("world","")),tag.getLong("position",0));}
    public static Item bind(Item original,NetworkController controller){String name=original.customId().orElseThrow().value();boolean group=name.startsWith("remote_group");Item result=Item.byId(Key.of("storagedrawers:"+(group?"remote_group_upgrade_bound":"remote_upgrade_bound"))).count(original.count());result.setSparrowTag(new RemoteBinding(controller.blockEntity().world.uuid(),controller.blockEntity().pos.asLong()).save(),KEY);return result;}
}
