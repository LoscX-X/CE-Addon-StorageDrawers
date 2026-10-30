package com.cendreal.storagedrawers.render;

import com.cendreal.storagedrawers.module.storage.DrawerController;
import net.momirealms.craftengine.bukkit.block.entity.DrawerBlockEntityController;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;

/** CE owns tracking, virtual IDs, packets and culling. No Bukkit display entities or timers. */
public final class DrawerDisplay implements BlockEntityElement {
    private final DrawerController owner;
    private final int slot;
    private final DrawerBlockEntityController delegate;
    private boolean wasEmpty=true;
    private boolean bright;
    private int previousCount=-1;
    public DrawerDisplay(DrawerController owner,int slot,DrawerBlockEntityController delegate) {this.owner=owner;this.slot=slot;this.delegate=delegate;}
    public void sync(boolean notify) {
        Item template=owner.template(slot);int count=owner.count(slot);
        boolean empty=template.isEmpty();
        // The CE helper must hold a visible item even when a locked drawer contains zero.
        delegate.setItem(0,empty?Item.empty():template.copyWithCount(1));
        boolean item=delegate.refreshItemDisplayPacket(), text=count!=previousCount;
        if(text){previousCount=count;delegate.element.refreshChangeTextContentPacket(count);}
        boolean previouslyEmpty=wasEmpty; wasEmpty=empty;
        boolean lightingChanged=bright!=owner.has("illumination_upgrade");bright=owner.has("illumination_upgrade");
        if(!notify || owner.concealed()) return;
        if(empty) {if(!previouslyEmpty) delegate.refreshDynamicElement((e,p)->e.hide(p));}
        else if(previouslyEmpty) delegate.refreshDynamicElement((e,p)->show(p));
        else {
            if(item) delegate.refreshDynamicElement((e,p)->e.updateDisplayItem(p));
            if(text && owner.quantity()) delegate.refreshDynamicElement((e,p)->e.updateTextContent(p));
            if(lightingChanged)delegate.refreshDynamicElement((e,p)->lighting(p));
        }
    }
    @Override public void show(Player player) {
        if(owner.concealed()) return;
        delegate.element.show(player);
        if(!owner.quantity()) player.sendPacket(delegate.element.despawnTextPacket,false);
        lighting(player);
    }
    @Override public void hide(Player player) {delegate.element.hide(player);}
    public void refreshVisibility() {delegate.refreshDynamicElement((e,p)->{hide(p);show(p);});}
    public void stateChanged(net.momirealms.craftengine.core.block.ImmutableBlockState state) {delegate.preBlockStateChange(state);}
    private void lighting(Player player){player.sendPackets(java.util.List.of(DisplayLighting.packet(delegate.element.itemId,bright,false),DisplayLighting.packet(delegate.element.textId,bright,true)),false);}
}
