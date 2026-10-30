package com.cendreal.storagedrawers.render;

import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import java.util.*;

final class DisplayLighting {
    @SuppressWarnings({"unchecked","rawtypes"})static Object packet(int id,boolean bright,boolean text){List<Object> data=new ArrayList<>();DisplayData.BrightnessOverride.addEntityData(bright?new net.minecraft.util.Brightness(15,15).pack():-1,data);if(text)DisplayData.TextDisplayData.BackgroundColor.addEntityData(0,data);return new ClientboundSetEntityDataPacket(id,(List)data);}
    private DisplayLighting(){}
}
