package com.cendreal.storagedrawers.module.interaction;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior;
import com.cendreal.storagedrawers.module.storage.*;
import com.cendreal.storagedrawers.module.automation.NetworkController;
import com.cendreal.storagedrawers.module.automation.RemoteBinding;
import com.cendreal.storagedrawers.module.frame.FramingController;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.*;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.libraries.antigrieflib.Flag;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.PlayerInventory;

public final class DrawerInteraction {
    private DrawerInteraction() {}
    public static boolean allowed(Player player,NodeController node) {
        if(player==null || !(player.platformPlayer() instanceof org.bukkit.entity.Player bukkit) || bukkit.getGameMode()==GameMode.SPECTATOR)return false;
        var e=node.blockEntity();
        return e.isValid() && (!(node instanceof DrawerController drawer) || drawer.canAccess(player) || RingTools.effectiveKey(player.getItemInHand(InteractionHand.MAIN_HAND)).equals("personal_key_unlock")) && BukkitCraftEngine.instance().antiGriefProvider().test(bukkit,Flag.OPEN_CONTAINER,new Location((org.bukkit.World)e.world.world().platformWorld(),e.pos.x,e.pos.y,e.pos.z));
    }
    public static String tool(Item item) {if(item==null || item.isEmpty() || item.customId().isEmpty())return "";var id=item.customId().get();return id.namespace().equals("storagedrawers")?id.value():"";}
    public static InteractionResult use(DrawerBehavior behavior,UseOnContext context,ImmutableBlockState state) {
        var actor=context.getPlayer();return com.cendreal.storagedrawers.module.storage.StorageContext.run(actor==null?null:(org.bukkit.entity.Player)actor.platformPlayer(),com.cendreal.storagedrawers.api.StorageCause.PLAYER,()->useInternal(behavior,context,state));
    }
    private static InteractionResult useInternal(DrawerBehavior behavior,UseOnContext context,ImmutableBlockState state) {
        NodeController node=behavior.at(context.getWorld(),context.getClickedPos());
        if(node==null || behavior.layout.kind().equals("trim"))return InteractionResult.PASS;
        if(behavior.layout.kind().equals("button"))return ButtonInteraction.use(behavior,context,state);
        Player player=context.getPlayer();if(player!=null&&StorageDrawersCE.instance().compatibility().inspect((org.bukkit.entity.Player)player.platformPlayer(),new Location((org.bukkit.World)context.getWorld().platformWorld(),context.getClickedPos().x,context.getClickedPos().y,context.getClickedPos().z)))return InteractionResult.SUCCESS_AND_CANCEL;
        if(!allowed(player,node))return InteractionResult.FAIL;
        if(context.getHand()!=InteractionHand.MAIN_HAND)return InteractionResult.SUCCESS_AND_CANCEL;
        Item hand=player.getItemInHand(InteractionHand.MAIN_HAND);String raw=tool(hand),id=RingTools.effectiveKey(hand);
        if(node instanceof FramingController table){StorageDrawersCE.instance().framingMenus().open((org.bukkit.entity.Player)player.platformPlayer(),table);return InteractionResult.SUCCESS_AND_CANCEL;}
        if(raw.startsWith("keyring")&&context.isSecondaryUseActive()){player.setItemInHand(InteractionHand.MAIN_HAND,RingTools.rotate(hand));return InteractionResult.SUCCESS_AND_CANCEL;}
        if(node instanceof NetworkController network) {
            if(raw.startsWith("remote_")&&network.container().view().controller() instanceof NetworkController root){player.setItemInHand(InteractionHand.MAIN_HAND,RemoteBinding.bind(hand,root));return InteractionResult.SUCCESS_AND_CANCEL;}
            if(RingTools.isKey(id)) {for(var d:network.container().view().drawers())if(allowed(player,d))d.useKey(id,player);return InteractionResult.SUCCESS_AND_CANCEL;}
            if(context.isSecondaryUseActive())return InteractionResult.PASS;
            for(var drawer:network.container().view().drawers()) if(allowed(player,drawer))depositMatching(drawer,player,hand);
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        DrawerController drawer=(DrawerController)node;
        var hit=context.getClickLocation();var pos=context.getClickedPos();
        int slot=behavior.layout.slot(state.get(behavior.facing),context.getClickedFace(),hit.x-pos.x,hit.y-pos.y,hit.z-pos.z);
        if(id.equals("drawer_puller")){if(slot>=0){Item pulled=drawer.detach(slot);if(!pulled.isEmpty())((org.bukkit.entity.Player)player.platformPlayer()).getInventory().addItem((org.bukkit.inventory.ItemStack)pulled.platformItem()).values().forEach(left->((org.bukkit.entity.Player)player.platformPlayer()).getWorld().dropItemNaturally(((org.bukkit.entity.Player)player.platformPlayer()).getLocation(),left));}return InteractionResult.SUCCESS_AND_CANCEL;}
        if(raw.equals("detached_drawer")||raw.equals("detached_drawer_full")){if(slot>=0&&drawer.attach(slot,hand,player))hand.shrink(1);return InteractionResult.SUCCESS_AND_CANCEL;}
        if(RingTools.isKey(id)) {drawer.useKey(id,player);return InteractionResult.SUCCESS_AND_CANCEL;}
        if(Upgrades.isUpgrade(id) && !context.isSecondaryUseActive()) {
            if(id.startsWith("remote_")&&drawer.remote()!=null&&drawer.rebind(hand))return InteractionResult.SUCCESS_AND_CANCEL;
            if(drawer.install(hand) && ((org.bukkit.entity.Player)player.platformPlayer()).getGameMode()!=GameMode.CREATIVE)hand.shrink(1);
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        if(context.isSecondaryUseActive()) {
            if(hand.isEmpty()) {StorageDrawersCE.instance().menus().open((org.bukkit.entity.Player)player.platformPlayer(),drawer);return InteractionResult.SUCCESS_AND_CANCEL;}
            if(slot>=0 && DrawerTransform.apply(drawer,hand,player)){if(((org.bukkit.entity.Player)player.platformPlayer()).getGameMode()!=GameMode.CREATIVE)hand.shrink(1);return InteractionResult.SUCCESS_AND_CANCEL;}
            return InteractionResult.PASS;
        }
        if(slot<0)return InteractionResult.PASS;
        long now=System.nanoTime();
        if(player.uuid().equals(drawer.lastPlayer) && drawer.lastSlot==slot && now-drawer.lastClick<500_000_000L && !drawer.template(slot).isEmpty()) {
            depositInventory(drawer,slot,player);drawer.lastPlayer=null;
        } else {if(!hand.isEmpty())hand.shrink(drawer.put(slot,hand,hand.count(),player));drawer.lastPlayer=player.uuid();drawer.lastClick=now;drawer.lastSlot=slot;}
        return InteractionResult.SUCCESS_AND_CANCEL;
    }
    private static void depositInventory(DrawerController drawer,int slot,Player player) {
        PlayerInventory inventory=((org.bukkit.entity.Player)player.platformPlayer()).getInventory();
        drawer.beginBatch();try{for(int i=0;i<36;i++) {var stack=inventory.getItem(i);if(stack==null || stack.isEmpty())continue;Item item=BukkitAdaptor.adapt(stack);if(!drawer.accepts(slot,item))continue;int used=drawer.put(slot,item,item.count(),player);if(used>0){stack.setAmount(stack.getAmount()-used);inventory.setItem(i,stack);}}}finally{drawer.endBatch();}
    }
    private static void depositMatching(DrawerController drawer,Player player,Item hand) {
        long now=System.nanoTime();
        boolean twice=player.uuid().equals(drawer.lastPlayer) && now-drawer.lastClick<500_000_000L;
        if(twice) {for(int i=0;i<drawer.size();i++)if(!drawer.template(i).isEmpty())depositInventory(drawer,i,player);}
        else if(!hand.isEmpty()) {int slot=drawer.find(hand);if(slot>=0)hand.shrink(drawer.put(slot,hand,hand.count(),player));}
        drawer.lastPlayer=twice?null:player.uuid();drawer.lastClick=now;
    }
}
