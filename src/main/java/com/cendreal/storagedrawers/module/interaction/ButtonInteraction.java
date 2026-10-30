package com.cendreal.storagedrawers.module.interaction;

import com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior;
import com.cendreal.storagedrawers.module.automation.NetworkController;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.libraries.antigrieflib.Flag;

public final class ButtonInteraction {
    public static InteractionResult use(DrawerBehavior behavior,UseOnContext context,ImmutableBlockState state){var player=context.getPlayer();if(player==null||state.get(behavior.powered))return InteractionResult.SUCCESS_AND_CANCEL;var pos=context.getClickedPos();var bukkit=(org.bukkit.entity.Player)player.platformPlayer();if(player.isSpectatorMode()||!BukkitCraftEngine.instance().antiGriefProvider().test(bukkit,Flag.USE_BUTTON,new org.bukkit.Location(bukkit.getWorld(),pos.x,pos.y,pos.z)))return InteractionResult.FAIL;
        var attachment=switch(state.get(behavior.face)){case "floor"->Direction.DOWN;case "ceiling"->Direction.UP;default->state.get(behavior.facing).opposite();};var target=pos.relative(attachment);var entity=context.getWorld().storageWorld().getBlockEntityAtIfLoaded(target);if(entity!=null&&entity.controller.get(NetworkController.class,0) instanceof NetworkController network){String id=switch(behavior.block().id().value()){case "keybutton_quantify"->"quantify_key";case "keybutton_concealment"->"shroud_key";default->"drawer_key";};for(var drawer:network.container().view().drawers())if(DrawerInteraction.allowed(player,drawer))drawer.useKey(id,player);}
        var world=(net.minecraft.server.level.ServerLevel)context.getWorld().minecraftWorld();var nativePos=new net.minecraft.core.BlockPos(pos.x,pos.y,pos.z);var next=(net.minecraft.world.level.block.state.BlockState)state.with(behavior.powered,true).customBlockState().minecraftState();world.setBlock(nativePos,next,3);world.scheduleTick(nativePos,next.getBlock(),20);bukkit.getWorld().playSound(new org.bukkit.Location(bukkit.getWorld(),pos.x+.5,pos.y+.5,pos.z+.5),org.bukkit.Sound.BLOCK_STONE_BUTTON_CLICK_ON,1,1);return InteractionResult.SUCCESS_AND_CANCEL;}
    private ButtonInteraction(){}
}
