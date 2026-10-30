package com.cendreal.storagedrawers.compatibility.craftengine;

import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;
import com.cendreal.storagedrawers.module.interaction.RingTools;

import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.entity.player.*;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;

public final class ToolBehavior extends ItemBehavior {
    @Override public InteractionResult use(World world,Player player,InteractionHand hand){if(player==null||!player.isSecondaryUseActive())return InteractionResult.PASS;Item item=player.getItemInHand(hand);String id=DrawerInteraction.tool(item);if(id.startsWith("keyring")){player.setItemInHand(hand,RingTools.rotate(item));return InteractionResult.SUCCESS;}if(id.startsWith("priority_key")){player.setItemInHand(hand,Item.byId(Key.of("storagedrawers:"+RingTools.nextPriority(id))));return InteractionResult.SUCCESS;}return InteractionResult.PASS;}
}
