package com.cendreal.storagedrawers.audit;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.module.storage.*;
import com.cendreal.storagedrawers.module.automation.*;
import com.cendreal.storagedrawers.module.menu.DrawerMenu;
import com.google.gson.*;
import net.momirealms.craftengine.bukkit.api.*;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.world.context.*;
import net.momirealms.craftengine.core.world.*;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.block.Hopper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import io.netty.channel.embedded.EmbeddedChannel;
import com.mojang.authlib.GameProfile;
import java.util.*;
import java.nio.file.*;

/** Opt-in server-side integration fixture. It does not certify client rendering or real input packets. */
public final class RuntimeAudit {
    private static boolean scheduled;
    private final StorageDrawersCE plugin;
    private final JsonObject report=new JsonObject();
    private final JsonArray checks=new JsonArray();
    private org.bukkit.World world;
    private org.bukkit.entity.Player player;
    private EmbeddedChannel playerChannel;
    private Location hopperDrawer;
    private DrawerController collected,networkLeaf;
    private DrawerController hopperCollected;
    private DrawerController offhandFixture;
    private Location buttonLocation;
    private org.bukkit.entity.Item magneticDrop;
    private NativeAutomationAudit nativeAutomation;
    private CoreProtectAudit coreProtectAudit;
    private PressureAudit pressureAudit;
    private RuntimeAudit(StorageDrawersCE plugin){this.plugin=plugin;report.add("checks",checks);report.addProperty("artifactSha256",plugin.artifactHash());report.addProperty("scope","Real Paper/CE blocks, persistence, native hopper execution, CE fake-player interactions and Bukkit inventory events; no real client rendering/input verification");}
    public static void schedule(StorageDrawersCE plugin){if(scheduled)return;scheduled=true;Bukkit.getScheduler().runTaskLater(plugin,()->new RuntimeAudit(plugin).start(),40);}
    private void check(String name,boolean ok){JsonObject c=new JsonObject();c.addProperty("name",name);c.addProperty("passed",ok);checks.add(c);if(!ok)throw new AssertionError(name);}
    private Location pos(int x,int y,int z){return new Location(world,x,y,z);}
    private DrawerController drawer(Location location){var entity=BukkitAdaptor.adapt(world).storageWorld().getBlockEntityAtIfLoaded(new BlockPos(location.getBlockX(),location.getBlockY(),location.getBlockZ()));return entity==null?null:entity.controller.get(DrawerController.class,0);}
    private NodeController node(Location location){var entity=BukkitAdaptor.adapt(world).storageWorld().getBlockEntityAtIfLoaded(new BlockPos(location.getBlockX(),location.getBlockY(),location.getBlockZ()));return entity==null?null:entity.controller.get(NodeController.class,0);}
    private DrawerController place(String id,Location at){CraftEngineBlocks.remove(at.getBlock());check("place "+id+" "+at.getBlockX(),CraftEngineBlocks.place(at,Key.of("storagedrawers:"+id),false));return drawer(at);}
    private Item item(Material material){return BukkitAdaptor.adapt(new ItemStack(material));}
    private void start(){try{
        if(Boolean.getBoolean("storagedrawers.ce.recovery")){recover();return;}
        check("100 registered blocks",CraftEngineBlocks.loadedBlocks().keySet().stream().filter(k->k.namespace().equals("storagedrawers")).count()==100);
        check("159 registered items",CraftEngineItems.loadedItems().keySet().stream().filter(k->k.namespace().equals("storagedrawers")).count()==159);
        int hollowStates=0;boolean floorVisible=true,sideVisible=true;
        for(var definition:CraftEngineBlocks.loadedBlocks().values()){
            var behavior=definition.defaultState().behavior().getFirst(com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior.class);
            if(behavior==null||!behavior.framed&&!behavior.layout.kind().equals("table"))continue;
            for(var state:definition.variantProvider().states()){
                var visual=(net.minecraft.world.level.block.state.BlockState)state.visualBlockState().minecraftState();hollowStates++;
                floorVisible&=net.minecraft.world.level.block.Block.shouldRenderFace(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),visual,net.minecraft.core.Direction.UP);
                sideVisible&=net.minecraft.world.level.block.Block.shouldRenderFace(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),visual,net.minecraft.core.Direction.NORTH);
            }
        }
        report.addProperty("hollowVisualStates",hollowStates);check("hollow models do not cull the floor face",hollowStates==60&&floorVisible);check("hollow models do not cull the adjacent side face",sideVisible);
        var browser=BukkitCraftEngine.instance().itemBrowserManager();for(String group:List.of("root","wood","compacting","network","frames","upgrades","tools"))check("category "+group,browser.byId(Key.of("storagedrawers:"+group)).isPresent());
        for(String type:List.of("1","2","4","comp_2","comp_3","framing"))check("GUI image "+type,CraftEngineImages.byId(Key.of("storagedrawers:gui_"+type))!=null);
        world=new WorldCreator("drawer_audit_"+System.currentTimeMillis()).type(WorldType.FLAT).generateStructures(false).createWorld();report.addProperty("world",world.getName());world.getChunkAt(4,4).addPluginChunkTicket(plugin);world.getChunkAt(5,4).addPluginChunkTicket(plugin);
        var d=place("oak_full_drawers_4",pos(65,10,65));check("CE block entity created",d!=null);
        check("base capacity and four independent cells",d.put(0,item(Material.STONE),600)==512 && d.put(1,item(Material.DIRT),10)==10 && d.count(0)==512 && d.count(1)==10 && d.count(2)==0);
        check("slot rejects different item",d.put(0,item(Material.DIRT),1)==0);
        check("capacity upgrade",d.install("obsidian_storage_upgrade") && d.capacity(0)==1024 && d.put(0,item(Material.STONE),512)==512);
        check("reject capacity-removing upgrade",d.uninstall(0)==null && d.count(0)==1024);
        check("one item output",d.take(0,1).count()==1 && d.count(0)==1023);
        d.useKey("drawer_key",null);while(d.count(1)>0)d.take(1,64);
        check("locked empty slot retains type",d.count(1)==0 && d.template(1).isSimilar(item(Material.DIRT)) && d.put(1,item(Material.STONE),1)==0);
        ItemStack named=new ItemStack(Material.DIAMOND);named.editMeta(m->m.displayName(net.kyori.adventure.text.Component.text("Audit")));
        d.useKey("drawer_key",null);
        check("metadata-sensitive identity",d.put(2,BukkitAdaptor.adapt(named),3)==3 && d.put(2,item(Material.DIAMOND),1)==0);
        d.useKey("drawer_key",null);
        var copy=new DrawerController(d.blockEntity(),d.behavior);var tag=new net.momirealms.craftengine.libraries.nbt.CompoundTag();d.saveCustomData(tag);copy.loadCustomData(tag);
        check("NBT round trip preserves identity, lock, upgrades",copy.count(0)==1023 && copy.count(2)==3 && copy.locked() && copy.upgrades().get(0).equals("obsidian_storage_upgrade") && copy.template(2).isSimilar(BukkitAdaptor.adapt(named)));
        quantityTests(d);
        for(int i=1;i<Upgrades.SLOTS;i++)check("upgrade slot "+i,d.replaceUpgrade(i,"obsidian_storage_upgrade"));check("seven upgrade slots",!d.install("obsidian_storage_upgrade"));
        var compact=place("compacting_drawers_3",pos(67,10,65));
        report.addProperty("compactingRuleCount",plugin.compactingRules().size());
        JsonArray recipes=new JsonArray();var iterator=Bukkit.recipeIterator();while(iterator.hasNext()){Recipe r=iterator.next();if(r.getResult().getType()==Material.IRON_BLOCK || r.getResult().getType()==Material.IRON_INGOT){JsonObject x=new JsonObject();x.addProperty("type",r.getClass().getName());x.addProperty("result",r.getResult().toString());if(r instanceof ShapedRecipe shaped)x.addProperty("choices",shaped.getChoiceMap().toString());if(r instanceof ShapelessRecipe shapeless)x.addProperty("choices",shapeless.getChoiceList().toString());recipes.add(x);}}report.add("ironRecipes",recipes);
        check("compacting iron chain",compact.put(0,item(Material.IRON_INGOT),9)==9 && compact.template(0).isSimilar(item(Material.IRON_BLOCK)) && compact.count(0)==1 && compact.count(1)==9 && compact.count(2)==81);
        check("compacting conserves base units",compact.take(2,1).count()==1 && compact.count(0)==0 && compact.count(1)==8 && compact.count(2)==80);
        check("compacting capacities reflect conversion factors",compact.capacity(0)==2048&&compact.capacity(1)==18432&&compact.capacity(2)==165888);
        var leaf=place("oak_full_drawers_1",pos(70,10,65));leaf.put(0,item(Material.STONE),32);
        CraftEngineBlocks.place(pos(71,10,65),Key.of("storagedrawers:oak_trim"),false);CraftEngineBlocks.place(pos(72,10,65),Key.of("storagedrawers:controller"),false);
        var network=(NetworkController)node(pos(72,10,65));check("loaded network index",network!=null && network.container().view().drawers().contains(leaf));
        long searches=plugin.networks().traversals(),start=System.nanoTime();for(int i=0;i<10000;i++){network.container().containerSize();network.container().getItem(1);}
        report.addProperty("cached10000LookupsNanos",System.nanoTime()-start);check("10000 cached accesses do not traverse graph",plugin.networks().traversals()==searches);
        CraftEngineBlocks.remove(pos(71,10,65).getBlock());check("removal invalidates network",!network.container().view().drawers().contains(leaf));
        // CE's actual loot/removal path, followed by its item placement behavior below.
        Location broken=pos(74,10,65);var portable=place("birch_full_drawers_1",broken);portable.put(0,item(Material.DIAMOND),127);portable.replaceUpgrade(3,"iron_storage_upgrade");portable.useKey("drawer_key",null);
        check("CE break succeeds",CraftEngineBlocks.remove(broken.getBlock(),null,false,true,false));
        var drop=world.getNearbyEntities(broken.clone().add(.5,.5,.5),2,2,2).stream().filter(e->e instanceof org.bukkit.entity.Item).map(e->((org.bukkit.entity.Item)e).getItemStack()).filter(i->BukkitAdaptor.adapt(i).hasTag(DrawerController.DATA_KEY)).findFirst().orElse(null);
        check("actual break drops portable contents",drop!=null && drop.getMaxStackSize()==1);
        createPlayer(pos(74,10,63));
        nativeBreakTests();
        breakPolicyTests();
        Location replacement=pos(74,10,67);replacement.getBlock().setType(Material.AIR);replacement.clone().subtract(0,1,0).getBlock().setType(Material.STONE);
        var cePlayer=BukkitAdaptor.adapt(player);var portableItem=BukkitAdaptor.adapt(drop.clone());cePlayer.setItemInHand(InteractionHand.MAIN_HAND,portableItem);
        var hit=new BlockHitResult(new Vec3d(74.5,10,67.5),Direction.UP,new BlockPos(74,9,67),false);
        portableItem.getBehavior().orElseThrow().getFirst(net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior.class).place(new BlockPlaceContext(cePlayer.world(),cePlayer,InteractionHand.MAIN_HAND,portableItem,hit));
        check("CE item placement restores contents",drawer(replacement)!=null && drawer(replacement).count(0)==127 && drawer(replacement).locked() && drawer(replacement).upgrades().get(3).equals("iron_storage_upgrade"));
        menuTests(leaf);
        aimTests(leaf);
        advancedTests();
        if(!System.getProperty("storagedrawers.ce.apiProbe","").isBlank()){
            world.getChunkAt(6,5).addPluginChunkTicket(plugin);var apiNormal=pos(100,10,80);var apiCompact=pos(102,10,80);var apiFrame=pos(104,10,80);var apiTable=pos(106,10,80);
            place("oak_full_drawers_1",apiNormal);place("compacting_drawers_3",apiCompact);placeItem(com.cendreal.storagedrawers.module.frame.FrameLibrary.frame(custom("oak_full_drawers_1"),item(Material.STONE),item(Material.OAK_PLANKS),item(Material.GOLD_BLOCK)),apiFrame);drawer(apiFrame).put(0,item(Material.DIAMOND),23);placeItem(custom("framing_table"),apiTable);
            ((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle().setPos(100.5,10,77.5);
            for(String name:ApiAudit.run(plugin,world,player,apiNormal,apiCompact,apiFrame,apiTable,pos(84,10,65)))check(name,true);
            var persisted=place("oak_full_drawers_4",pos(108,10,80));var handle=plugin.getApi().getDrawer(pos(108,10,80)).orElseThrow();ItemStack persistedItem=new ItemStack(Material.DIAMOND);persistedItem.editMeta(meta->meta.displayName(net.kyori.adventure.text.Component.text("Persistent SDK item")));
            check("API persistence fixture writes complete state",handle.setItem(0,persistedItem,23)&&handle.setUpgrade(0,plugin.getApi().createItem(new NamespacedKey("storagedrawers","iron_storage_upgrade"),1).orElseThrow())&&handle.setOwner(UUID.fromString("ef5aa35c-0829-44d5-bfb4-02dc54a4b082"))&&handle.setPriority(2)&&handle.setLocked(true)&&handle.setQuantityVisible(false)&&handle.setConcealed(true)&&handle.setSuspended(true));
            report.addProperty("apiPersistence",true);
        }
        var diamondDisplay=place("oak_full_drawers_1",pos(84,10,78));diamondDisplay.put(0,item(Material.DIAMOND),41);
        var mixedDisplay=place("oak_full_drawers_4",pos(86,10,78));mixedDisplay.install("creative_storage_upgrade");mixedDisplay.put(0,item(Material.STONE),Integer.MAX_VALUE);mixedDisplay.put(1,item(Material.DIAMOND),64);mixedDisplay.put(2,item(Material.IRON_INGOT),12);mixedDisplay.put(3,item(Material.EGG),1);
        check("front render fixtures contain flat icons and a ten-digit quantity",diamondDisplay.count(0)==41&&mixedDisplay.count(0)==Integer.MAX_VALUE&&mixedDisplay.count(1)==64);
        var fill=place("oak_full_drawers_1",pos(78,10,65));hopperDrawer=pos(78,10,65);var above=pos(78,11,65).getBlock();above.setType(Material.HOPPER);var upper=(Hopper)above.getState();upper.getInventory().setItem(0,new ItemStack(Material.COBBLESTONE,8));
        var below=pos(78,9,65).getBlock();below.setType(Material.HOPPER);var hopper=(org.bukkit.block.data.type.Hopper)below.getBlockData();hopper.setFacing(org.bukkit.block.BlockFace.DOWN);below.setBlockData(hopper);pos(78,8,65).getBlock().setType(Material.CHEST);
        nativeAutomation=new NativeAutomationAudit(plugin,world);
        if(Boolean.getBoolean("storagedrawers.ce.coreProtectAudit")){
            var cpSimple=pos(112,10,80);var cpMulti=pos(114,10,80);var cpLarge=pos(116,10,80);
            place("oak_full_drawers_1",cpSimple);place("oak_full_drawers_4",cpMulti);place("oak_full_drawers_1",cpLarge);
            coreProtectAudit=new CoreProtectAudit(plugin,player,cpSimple,cpMulti,cpLarge);
        }
        if(Boolean.getBoolean("storagedrawers.ce.pressure"))pressureAudit=new PressureAudit(plugin,world,name->check(name,true));
        Bukkit.getScheduler().runTaskLater(plugin,()->afterHoppers(fill),180);
    }catch(Throwable t){fail(t);}}
    private void createPlayer(Location at){
        var server=MinecraftServer.getServer();var level=((CraftWorld)world).getHandle();var profile=new GameProfile(UUID.randomUUID(),"DrawerAudit");var nativePlayer=new ServerPlayer(server,level,profile,ClientInformation.createDefault());nativePlayer.setPos(at.getX(),at.getY(),at.getZ());
        var connection=new Connection(PacketFlow.SERVERBOUND);connection.channel=playerChannel=new EmbeddedChannel();connection.address=new java.net.InetSocketAddress("127.0.0.1",0);nativePlayer.connection=new ServerGamePacketListenerImpl(server,connection,nativePlayer,CommonListenerCookie.createInitial(profile,false));
        player=nativePlayer.getBukkitEntity();BukkitCraftEngine.instance().networkManager().addFakePlayer(player);
    }
    private void click(org.bukkit.inventory.InventoryView view,int raw,ClickType type,InventoryAction action){Bukkit.getPluginManager().callEvent(new InventoryClickEvent(view,raw<45?org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER:org.bukkit.event.inventory.InventoryType.SlotType.QUICKBAR,raw,type,action));}
    private List<Object> packets(){List<Object> result=new ArrayList<>();for(Object packet;(packet=playerChannel.readOutbound())!=null;)result.add(packet);return result;}
    private void nativeBreakTests(){
        JsonArray results=new JsonArray();boolean survival=false,creative=false;var previousMode=player.getGameMode();
        try{for(var mode:List.of(GameMode.SURVIVAL,GameMode.CREATIVE)){
            player.setGameMode(mode);player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
            var at=pos(mode==GameMode.SURVIVAL?85:87,10,75);Item framed=com.cendreal.storagedrawers.module.frame.FrameLibrary.frame(custom("oak_full_drawers_1"),item(Material.STONE),item(Material.OAK_PLANKS),item(Material.GOLD_BLOCK));placeItem(framed,at);var source=drawer(at);source.put(0,item(Material.DIAMOND),23);source.replaceUpgrade(1,"iron_storage_upgrade");
            var known=world.getNearbyEntities(at,2,2,2).stream().map(org.bukkit.entity.Entity::getUniqueId).collect(java.util.stream.Collectors.toSet());
            boolean destroyed=((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle().gameMode.destroyBlock(new net.minecraft.core.BlockPos(at.getBlockX(),at.getBlockY(),at.getBlockZ()));
            var drops=world.getNearbyEntities(at,2,2,2).stream().filter(e->!known.contains(e.getUniqueId())&&e instanceof org.bukkit.entity.Item).map(e->BukkitAdaptor.adapt(((org.bukkit.entity.Item)e).getItemStack())).toList();
            boolean kept=drops.stream().anyMatch(i->i.getSparrowTag(DrawerController.DATA_KEY) instanceof net.momirealms.craftengine.libraries.nbt.CompoundTag tag&&tag.getCompound("slot0").getInt("count",0)==23&&tag.getString("upgrade1","").equals("iron_storage_upgrade")&&com.cendreal.storagedrawers.module.frame.FrameData.read(i).side().equals("minecraft:stone"));
            JsonObject result=new JsonObject();result.addProperty("mode",mode.name());result.addProperty("destroyed",destroyed);result.addProperty("kept",kept);result.addProperty("dropCount",drops.size());result.addProperty("dropDetails",drops.toString());results.add(result);
            if(mode==GameMode.SURVIVAL)survival=destroyed&&kept;else creative=destroyed&&kept;
        }}finally{player.setGameMode(previousMode);report.add("nativeBreakResults",results);}
        check("native survival destruction preserves contents, upgrades and frame",survival);check("native creative destruction preserves contents, upgrades and frame",creative);
    }
    private void aimLook(float yaw,float pitch){var handle=((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle();handle.setPos(70.5,10,62.5);handle.setYRot(yaw);handle.setYHeadRot(yaw);handle.setXRot(pitch);}
    private void breakPolicyTests(){
        var previousMode=player.getGameMode();
        try{
            for(var mode:List.of(GameMode.SURVIVAL,GameMode.CREATIVE))for(boolean cancel:List.of(true,false)){
                player.setGameMode(mode);var at=pos(89,10,75);placeItem(custom("oak_full_drawers_1"),at);var source=drawer(at);source.put(0,item(Material.DIAMOND),7);
                var listener=new org.bukkit.event.Listener(){};
                Bukkit.getPluginManager().registerEvent(org.bukkit.event.block.BlockBreakEvent.class,listener,org.bukkit.event.EventPriority.HIGHEST,(l,e)->{var event=(org.bukkit.event.block.BlockBreakEvent)e;if(event.getBlock().equals(at.getBlock())){if(cancel)event.setCancelled(true);else event.setDropItems(false);}},plugin,true);
                var known=world.getNearbyEntities(at,2,2,2).stream().map(org.bukkit.entity.Entity::getUniqueId).collect(java.util.stream.Collectors.toSet());boolean destroyed;
                try{destroyed=((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle().gameMode.destroyBlock(new net.minecraft.core.BlockPos(at.getBlockX(),at.getBlockY(),at.getBlockZ()));}finally{org.bukkit.event.HandlerList.unregisterAll(listener);}
                boolean noDrops=world.getNearbyEntities(at,2,2,2).stream().noneMatch(e->!known.contains(e.getUniqueId())&&e instanceof org.bukkit.entity.Item);
                check("native "+mode+" respects "+(cancel?"cancelled break":"disabled drops"),noDrops&&(cancel?!destroyed&&drawer(at)==source&&source.count(0)==7:destroyed&&drawer(at)==null));
                CraftEngineBlocks.remove(at.getBlock());
            }
            player.setGameMode(GameMode.CREATIVE);var at=pos(89,10,75);placeItem(custom("oak_full_drawers_1"),at);var known=world.getNearbyEntities(at,2,2,2).stream().map(org.bukkit.entity.Entity::getUniqueId).collect(java.util.stream.Collectors.toSet());
            ((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle().gameMode.destroyBlock(new net.minecraft.core.BlockPos(at.getBlockX(),at.getBlockY(),at.getBlockZ()));
            check("native creative empty default drawer has no drop",world.getNearbyEntities(at,2,2,2).stream().noneMatch(e->!known.contains(e.getUniqueId())&&e instanceof org.bukkit.entity.Item));
            for(String id:List.of("oak_full_drawers_1","oak_full_drawers_4")){
                placeItem(custom(id),at);var source=drawer(at);source.put(0,item(Material.DIAMOND),7);var face=org.bukkit.block.BlockFace.valueOf(source.blockEntity().blockState.get(source.behavior.facing).name());
                var event=new net.momirealms.craftengine.bukkit.api.event.CustomBlockInteractEvent(player,at,null,source.blockEntity().blockState,at.getBlock(),face,InteractionHand.MAIN_HAND,net.momirealms.craftengine.bukkit.api.event.CustomBlockInteractEvent.Action.LEFT_CLICK,player.getInventory().getItemInMainHand());
                Bukkit.getPluginManager().callEvent(event);
                check(id+" null-hit front click is cancelled safely",event.isCancelled()&&source.count(0)==(source.size()==1?6:7));CraftEngineBlocks.remove(at.getBlock());
            }
        }finally{player.setGameMode(previousMode);}
    }
    private void aimTests(DrawerController drawer){
        var aim=plugin.aimHologram();var mode=player.getGameMode();var permission=player.addAttachment(plugin,"storagedrawers.aim",true);
        boolean aimSetting=plugin.getConfig().getBoolean("aim-hologram.enabled",false);
        var wall=pos(70,10,64).getBlock();var original=wall.getBlockData();
        try{
            report.addProperty("aimCameraInitiallyLoaded",world.isChunkLoaded(4,3));world.getChunkAt(4,3).addPluginChunkTicket(plugin);
            player.closeInventory();player.setGameMode(GameMode.CREATIVE);aimLook(0,24);packets();int entities=world.getEntities().size();
            plugin.getConfig().set("aim-hologram.enabled",false);aim.reload();aim.refresh(player);
            check("disabled aim config sends no overhead display",aim.viewers()==0&&packets().isEmpty());
            plugin.getConfig().set("aim-hologram.enabled",true);aim.reload();
            aim.refresh(player);var first=packets();
            JsonObject probe=new JsonObject();probe.addProperty("viewers",aim.viewers());probe.addProperty("location",player.getLocation().toString());probe.addProperty("eye",player.getEyeLocation().toString());probe.addProperty("inventoryType",player.getOpenInventory().getType().name());probe.addProperty("permission",player.hasPermission("storagedrawers.aim"));probe.addProperty("allowed",com.cendreal.storagedrawers.module.interaction.DrawerInteraction.allowed(BukkitAdaptor.adapt(player),drawer));var ray=player.rayTraceBlocks(6,FluidCollisionMode.NEVER);probe.addProperty("hit",ray==null?"none":String.valueOf(ray.getHitBlock()));probe.addProperty("packets",first.toString());report.add("aimProbe",probe);
            var nativeWorld=((CraftWorld)world).getHandle();var nativePos=new net.minecraft.core.BlockPos(70,10,65);var nativeState=nativeWorld.getBlockState(nativePos);probe.addProperty("bukkitBlock",world.getBlockAt(70,10,65).getType().name());probe.addProperty("nativeState",nativeState.toString());probe.addProperty("nativeShape",nativeState.getShape(nativeWorld,nativePos).toAabbs().toString());probe.addProperty("customShape",((net.minecraft.world.level.block.state.BlockState)drawer.blockEntity().blockState.customBlockState().minecraftState()).getShape(nativeWorld,nativePos).toAabbs().toString());
            check("aim ray trace spawns private item and text above drawer",aim.viewers()==1&&first.stream().filter(p->p instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket add&&add.getY()>11).count()==2);
            check("aim displays are not world entities",world.getEntities().size()==entities);
            var spawns=first.stream().filter(p->p instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket).map(p->(net.minecraft.network.protocol.game.ClientboundAddEntityPacket)p).toList();
            check("aim icon and text share their world anchor",spawns.size()==2&&spawns.get(0).getX()==spawns.get(1).getX()&&spawns.get(0).getY()==spawns.get(1).getY()&&spawns.get(0).getZ()==spawns.get(1).getZ());
            check("aim layout offsets are in billboard local space",first.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket data&&data.packedItems().stream().anyMatch(v->v.value() instanceof org.joml.Vector3fc value&&Math.abs(value.x()+.65f)<.00001f&&Math.abs(value.y()-.09f)<.00001f))&&first.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket data&&data.packedItems().stream().anyMatch(v->v.value() instanceof org.joml.Vector3fc value&&Math.abs(value.x()-.13f)<.00001f&&Math.abs(value.y())<.00001f)));
            aim.refresh(player);check("unchanged aim sends no display packets",packets().isEmpty());
            aimLook(2,26);aim.refresh(player);check("camera turn needs no aim movement packets",aim.viewers()==1&&packets().isEmpty());
            ((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle().setPos(70.65,10,62.4);aim.refresh(player);check("camera movement needs no aim movement packets",aim.viewers()==1&&packets().isEmpty());aimLook(0,24);
            int before=drawer.count(0);drawer.put(0,item(Material.STONE),1);packets();aim.refresh(player);var changed=packets();
            check("aim count change sends metadata without respawning",changed.stream().noneMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket)&&changed.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket data&&data.packedItems().stream().anyMatch(v->v.value() instanceof net.minecraft.network.chat.Component text&&text.getString().contains("�� "+(before+1)))));
            drawer.take(0,1);aim.refresh(player);packets();
            wall.setType(Material.STONE);aim.refresh(player);check("aim respects solid line of sight",aim.viewers()==0&&packets().stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket));wall.setBlockData(original);
            aim.refresh(player);packets();drawer.useKey("shroud_key",null);aim.refresh(player);check("shrouded drawer has no aim disclosure",aim.viewers()==0);drawer.useKey("shroud_key",null);packets();
            aim.refresh(player);permission.setPermission("storagedrawers.aim",false);aim.refresh(player);check("aim permission disables the viewer",aim.viewers()==0);permission.setPermission("storagedrawers.aim",true);
            aim.refresh(player);packets();aimLook(180,0);aim.refresh(player);check("looking away removes aim displays",aim.viewers()==0&&packets().stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket));
        }finally{wall.setBlockData(original);aim.hide(player);player.removeAttachment(permission);player.setGameMode(mode);plugin.getConfig().set("aim-hologram.enabled",aimSetting);aim.reload();}
    }
    private void quantityTests(DrawerController source){
        check("new drawer shows front quantity by default",source.quantity());
        var old=source.contents();old.putInt("version",1);old.putBoolean("quantity",false);var root=new net.momirealms.craftengine.libraries.nbt.CompoundTag();root.put(DrawerController.DATA_KEY,old);
        var migrated=new DrawerController(source.blockEntity(),source.behavior);migrated.loadCustomData(root);
        check("legacy hidden quantity adopts front default without changing stored items",migrated.quantity()&&migrated.count(0)==source.count(0)&&migrated.template(2).isSimilar(source.template(2))&&migrated.upgrades().equals(source.upgrades()));
        migrated.useKey("quantify_key",null);root=new net.momirealms.craftengine.libraries.nbt.CompoundTag();migrated.saveCustomData(root);var restored=new DrawerController(source.blockEntity(),source.behavior);restored.loadCustomData(root);
        check("explicit front quantity off survives a new-format save",!restored.quantity()&&restored.contents().getInt("version",0)==DrawerController.CONTENTS_VERSION);
        boolean original=plugin.getConfig().getBoolean("display.front-quantity-default",true);
        try{plugin.getConfig().set("display.front-quantity-default",false);root=new net.momirealms.craftengine.libraries.nbt.CompoundTag();root.put(DrawerController.DATA_KEY,old);var disabled=new DrawerController(source.blockEntity(),source.behavior);disabled.loadCustomData(root);check("front quantity default can be configured off",!disabled.quantity());}finally{plugin.getConfig().set("display.front-quantity-default",original);}
        var crafted=com.cendreal.storagedrawers.module.crafting.StorageRecipe.emptyContents(source.behavior.block().id(),source.size());check("upgraded recipe contents carry the front quantity default",crafted.getBoolean("quantity",false)&&crafted.getInt("version",0)==DrawerController.CONTENTS_VERSION);
    }
    private void menuTests(DrawerController d){
        player.teleport(pos(70,10,64));plugin.menus().open(player,d);var view=player.getOpenInventory();check("custom menu has exact slot layout",view.getTopInventory().getSize()==45 && DrawerMenu.storageCells(d)[0]==13 && DrawerMenu.upgradeCells().length==7);
        player.setItemOnCursor(new ItemStack(Material.DIRT,8));click(view,0,ClickType.LEFT,InventoryAction.PLACE_ALL);check("decorative cells reject input",view.getTopInventory().getItem(0)==null && player.getItemOnCursor().getAmount()==8);
        click(view,37,ClickType.LEFT,InventoryAction.PLACE_ALL);check("upgrade cell rejects ordinary item",d.upgrades().get(0).isEmpty() && player.getItemOnCursor().getAmount()==8);
        player.setItemOnCursor((ItemStack)Item.byId(Key.of("storagedrawers:iron_storage_upgrade")).platformItem());click(view,40,ClickType.LEFT,InventoryAction.PLACE_ALL);check("allowed upgrade cell accepts one upgrade",d.upgrades().get(3).equals("iron_storage_upgrade") && player.getItemOnCursor().isEmpty());
        player.setItemOnCursor(new ItemStack(Material.STONE,8));int before=d.count(0);click(view,13,ClickType.RIGHT,InventoryAction.PLACE_ONE);check("storage cursor transaction",d.count(0)==before+1 && player.getItemOnCursor().getAmount()==7);
        player.setItemOnCursor(new ItemStack(Material.AIR));click(view,13,ClickType.RIGHT,InventoryAction.PICKUP_ONE);check("storage extraction transaction",d.count(0)==before && player.getItemOnCursor().getAmount()==1);
        var event=new InventoryClickEvent(view,org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,13,ClickType.DOUBLE_CLICK,InventoryAction.COLLECT_TO_CURSOR);Bukkit.getPluginManager().callEvent(event);check("double collect cannot duplicate previews",event.isCancelled() && d.count(0)==before);
        player.getInventory().setItem(0,new ItemStack(Material.STONE,8));var number=new InventoryClickEvent(view,InventoryType.SlotType.CONTAINER,0,ClickType.NUMBER_KEY,InventoryAction.HOTBAR_SWAP,0);Bukkit.getPluginManager().callEvent(number);check("number key cannot insert into decoration",number.isCancelled()&&player.getInventory().getItem(0).getAmount()==8&&view.getTopInventory().getItem(0)==null);
        number=new InventoryClickEvent(view,InventoryType.SlotType.CONTAINER,13,ClickType.NUMBER_KEY,InventoryAction.HOTBAR_SWAP,0);Bukkit.getPluginManager().callEvent(number);check("number key uses storage transaction",d.count(0)==before+8&&(player.getInventory().getItem(0)==null||player.getInventory().getItem(0).isEmpty()));before=d.count(0);
        var drag=new InventoryDragEvent(view,new ItemStack(Material.STONE,6),new ItemStack(Material.STONE,8),false,Map.of(0,new ItemStack(Material.STONE),13,new ItemStack(Material.STONE)));Bukkit.getPluginManager().callEvent(drag);check("mixed decoration drag rejected atomically",drag.isCancelled()&&d.count(0)==before&&view.getTopInventory().getItem(0)==null);
        player.setItemOnCursor(new ItemStack(Material.STONE,8));drag=new InventoryDragEvent(view,new ItemStack(Material.STONE,7),new ItemStack(Material.STONE,8),true,Map.of(13,new ItemStack(Material.STONE)));Bukkit.getPluginManager().callEvent(drag);check("single storage drag transacts exactly one",drag.isCancelled()&&d.count(0)==before+1&&player.getItemOnCursor().getAmount()==7);
        player.closeInventory();
    }
    private void afterHoppers(DrawerController d){try{
        var top=(Hopper)hopperDrawer.clone().add(0,1,0).getBlock().getState();var bottom=(Hopper)hopperDrawer.clone().subtract(0,1,0).getBlock().getState();var chest=(org.bukkit.block.Chest)hopperDrawer.clone().subtract(0,2,0).getBlock().getState();
        int output=Arrays.stream(chest.getBlockInventory().getContents()).filter(Objects::nonNull).mapToInt(ItemStack::getAmount).sum();int remaining=Arrays.stream(top.getInventory().getContents()).filter(Objects::nonNull).mapToInt(ItemStack::getAmount).sum()+Arrays.stream(bottom.getInventory().getContents()).filter(Objects::nonNull).mapToInt(ItemStack::getAmount).sum();
        report.addProperty("hopperChestOutput",output);report.addProperty("hopperDrawerCount",d.count(0));report.addProperty("hopperRemaining",remaining);check("real hopper input/output conserves items",output==8 && remaining==0 && d.count(0)==0);
        check("real magnet entity collection conserves items",collected.count(0)==7&&!magneticDrop.isValid());
        check("real hopper-upgrade entity collection",hopperCollected.count(0)==5);
        check("offhand key initializes actual placed drawer",offhandFixture.locked());
        var released=CraftEngineBlocks.getCustomBlockState(buttonLocation.getBlock());var buttonBehavior=released.behavior().getFirst(com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior.class);check("button scheduled release",!released.get(buttonBehavior.powered));CraftEngineBlocks.remove(pos(94,10,80).getBlock());check("button drops when support removed",buttonLocation.getBlock().getType()==Material.AIR);
        var networkTop=(Hopper)pos(84,11,65).getBlock().getState();int networkRemaining=Arrays.stream(networkTop.getInventory().getContents()).filter(Objects::nonNull).mapToInt(ItemStack::getAmount).sum();check("native hopper routes through aggregate controller",networkRemaining==0&&networkLeaf.count(0)==9);
        nativeAutomation.blocked(name->check(name,true));unloadTest();
    }catch(Throwable t){fail(t);}}
    private void unloadTest(){
        world.getChunkAt(10,10).addPluginChunkTicket(plugin);var fixture=place("oak_full_drawers_1",pos(160,10,160));fixture.put(0,item(Material.EMERALD),3);int loaded=plugin.networks().loadedNodes();world.removePluginChunkTicket(10,10,plugin);
        Bukkit.getScheduler().runTaskLater(plugin,()->{try{check("chunk unload request queued",world.unloadChunkRequest(10,10));Bukkit.getScheduler().runTaskLater(plugin,()->{try{check("chunk really unloaded",!world.isChunkLoaded(10,10));check("unloaded node removed from live index",plugin.networks().loadedNodes()==loaded-1);check("unloaded block entity lookup does not load a chunk",drawer(pos(160,10,160))==null&&!world.isChunkLoaded(10,10));world.getChunkAt(10,10).addPluginChunkTicket(plugin);Bukkit.getScheduler().runTaskLater(plugin,()->{try{var restored=drawer(pos(160,10,160));check("chunk reload restores contents",restored!=null&&restored.count(0)==3&&restored.template(0).isSimilar(item(Material.EMERALD)));check("reloaded node returns to index",plugin.networks().loadedNodes()==loaded);completeIntegration();}catch(Throwable t){fail(t);}},20);}catch(Throwable t){fail(t);}},20);}catch(Throwable t){fail(t);}},40);
    }
    private void completeIntegration(){if(nativeAutomation!=null)nativeAutomation.resumed(name->check(name,true));if(pressureAudit!=null)report.add("pressure",pressureAudit.finish(name->check(name,true)));if(coreProtectAudit!=null)coreProtectAudit.verify(names->{for(var name:names)check(name,true);saveAndBuildPack();},this::fail);else saveAndBuildPack();}
    private void saveAndBuildPack(){world.save();BukkitAdaptor.adapt(world).storageWorld().saveChunks();Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{BukkitCraftEngine.instance().packManager().runWorkflow("default");report.addProperty("resourcePackWorkflow",true);report.addProperty("passed",true);Bukkit.getScheduler().runTask(plugin,this::finish);}catch(Throwable t){Bukkit.getScheduler().runTask(plugin,()->fail(t));}});}
    private Item custom(String id){return Item.byId(Key.of("storagedrawers:"+id));}
    private void recover() throws java.io.IOException {
        var previous=JsonParser.parseString(Files.readString(plugin.getDataFolder().toPath().resolve("runtime-audit.json"))).getAsJsonObject();check("initial runtime audit passed",previous.get("passed").getAsBoolean());check("restart uses the exact tested plugin artifact",previous.get("artifactSha256").getAsString().equals(plugin.artifactHash()));String name=previous.get("world").getAsString();world=new WorldCreator(name).createWorld();report.addProperty("world",name);world.getChunkAt(4,4).addPluginChunkTicket(plugin);world.getChunkAt(5,4).addPluginChunkTicket(plugin);if(previous.has("apiPersistence"))world.getChunkAt(6,5).addPluginChunkTicket(plugin);
        Bukkit.getScheduler().runTaskLater(plugin,()->{try{var restored=drawer(pos(74,10,67));check("full server restart restores portable contents",restored!=null&&restored.count(0)==127&&restored.locked()&&restored.has("iron_storage_upgrade"));var framed=drawer(pos(92,10,74));check("full server restart restores frame and contents",framed!=null&&framed.behavior.framed&&framed.count(0)==23&&framed.frame().side().equals("minecraft:stone"));var table=(com.cendreal.storagedrawers.module.frame.FramingController)node(pos(90,10,74));check("full server restart restores framing inputs",table!=null&&table.input(0).id().equals(Key.of("storagedrawers:oak_full_drawers_1"))&&table.input(1).id().equals(Key.of("minecraft:stone")));var partition=drawer(pos(82,10,70));check("full server restart restores repartition",partition!=null&&partition.count(0)+partition.count(1)+partition.count(2)+partition.count(3)==127);check("full server restart restores collection",drawer(pos(90,10,70)).count(0)==7);if(previous.has("apiPersistence"))recoverApi();report.addProperty("passed",true);finish();}catch(Throwable t){fail(t);}},40);
    }
    private void recoverApi(){
        var api=plugin.getApi();var handle=api.getDrawer(pos(108,10,80)).orElseThrow();ItemStack named=new ItemStack(Material.DIAMOND);named.editMeta(meta->meta.displayName(net.kyori.adventure.text.Component.text("Persistent SDK item")));
        check("restart preserves API item metadata and exact count",handle.getCount(0)==23&&handle.getItem(0).isSimilar(named));
        check("restart preserves API upgrades and ownership",!handle.getUpgrade(0).isEmpty()&&handle.getOwner().orElseThrow().equals(UUID.fromString("ef5aa35c-0829-44d5-bfb4-02dc54a4b082")));
        check("restart preserves API settings and priority",handle.isLocked()&&!handle.isQuantityVisible()&&handle.isConcealed()&&handle.isSuspended()&&handle.getPriority()==2);
        var compact=api.getDrawer(pos(102,10,80)).orElseThrow();check("restart preserves API compacting base units",compact.getCount(0)==2&&compact.getCount(1)==18&&compact.getCount(2)==162);
        var frame=api.getDrawer(pos(104,10,80)).orElseThrow();check("restart preserves API frame replacement and contents",frame.getCount(0)==23&&frame.getFrame().side().equals("minecraft:deepslate"));
    }
    private void advancedTests(){
        var actor=BukkitAdaptor.adapt(player);var a=place("oak_full_drawers_1",pos(80,10,70));
        var manual=place("oak_full_drawers_2",pos(81,10,72));manual.useKey("drawer_key",actor);check("locked empty slot rejects automation",manual.put(0,item(Material.STONE),1)==0);check("manual input can seed a locked empty slot",manual.put(0,item(Material.STONE),1,actor)==1);manual.take(0,1,actor);check("locked prototype still rejects different manual type",manual.put(0,item(Material.DIRT),1,actor)==0&&manual.template(0).isSimilar(item(Material.STONE)));
        actor.setItemInHand(InteractionHand.OFF_HAND,custom("drawer_key"));placeItem(custom("oak_full_drawers_1"),pos(95,10,72));offhandFixture=drawer(pos(95,10,72));actor.setItemInHand(InteractionHand.OFF_HAND,Item.empty());
        a.put(0,item(Material.DIAMOND),100);check("detached drawer retains count",a.detach(0).getSparrowTag("storagedrawers:detached") instanceof net.momirealms.craftengine.libraries.nbt.CompoundTag detached&&detached.getInt("count",0)==100);
        var force=place("oak_full_drawers_2",pos(79,10,74));force.install("iron_storage_upgrade");force.install("void_upgrade");Item detachedEmpty=force.detach(0);boolean originalForce=plugin.getConfig().getBoolean("detached-drawers.force-capacity-check",false);plugin.getConfig().set("detached-drawers.force-capacity-check",true);try{check("missing drawer strict capacity rejects added upgrades",!force.install("illumination_upgrade"));check("missing drawer strict capacity retains capacity upgrades",force.uninstallItem(0).isEmpty());check("missing drawer strict capacity permits removing utility",!force.uninstallItem(1).isEmpty());}finally{plugin.getConfig().set("detached-drawers.force-capacity-check",originalForce);}check("strict-capacity fixture restores detached drawer",force.attach(0,detachedEmpty,actor));
        check("missing drawer rejects insertion",a.missing(0)&&a.put(0,item(Material.DIAMOND),1)==0);check("empty detached drawer attaches",a.attach(0,custom("detached_drawer"))&&!a.missing(0)&&a.count(0)==0);
        a.put(0,item(Material.STONE),64);a.useKey("drawer_key",actor);a.take(0,64);Item emptyLocked=a.detach(0);check("locked empty detached prototype attaches",a.attach(0,emptyLocked)&&a.template(0).isSimilar(item(Material.STONE))&&a.count(0)==0);
        check("one-stack capacity",a.install("one_stack_upgrade")&&a.put(0,item(Material.STONE),100)==64&&a.count(0)==64);check("void consumes overflow",a.install("void_upgrade")&&a.put(0,item(Material.STONE),13)==13&&a.count(0)==64);
        check("comparator full signal",a.install("redstone_upgrade")&&a.signal()==15);a.useKey("suspend_key",actor);check("suspended automation rejects both directions",!a.container().canPlaceItem(0,item(Material.STONE))&&!a.container().canTakeItemThroughFace(1,item(Material.STONE),Direction.DOWN));a.useKey("suspend_key",actor);
        check("creative vending exposes unlimited output",a.install("creative_vending_upgrade")&&a.take(0,64).count()==64&&a.count(0)==Integer.MAX_VALUE);
        var vendor=place("oak_full_drawers_1",pos(80,10,72));vendor.put(0,item(Material.DIAMOND),1);vendor.install("creative_vending_upgrade");check("one seed vends full stacks repeatedly",vendor.take(0,64).count()==64&&vendor.take(0,64).count()==64&&vendor.count(0)==Integer.MAX_VALUE);check("vending inputs do not mutate the stored seed",vendor.put(0,item(Material.DIAMOND),64)==64&&vendor.uninstallItem(0).count()==1&&vendor.count(0)==1);
        var dense=place("compacting_drawers_3",pos(79,10,72));for(int i=0;i<7;i++)dense.install("netherite_storage_upgrade");int blocks=dense.put(0,item(Material.IRON_BLOCK),Integer.MAX_VALUE);check("maximum compacting upgrades cannot overflow base units",blocks==Integer.MAX_VALUE/81&&dense.capacity(0)==Integer.MAX_VALUE/81&&dense.capacity(2)==Integer.MAX_VALUE);int remainder=Integer.MAX_VALUE%81;check("compacting accepts exact remaining base units",dense.put(2,item(Material.IRON_NUGGET),remainder)==remainder&&dense.count(2)==Integer.MAX_VALUE&&dense.put(2,item(Material.IRON_NUGGET),1)==0);
        a.useKey("personal_key",actor);check("personal claim",a.claimed()&&a.canAccess(actor)&&!a.canAccess(null));a.useKey("personal_key",actor);check("personal key toggles claim off",!a.claimed());
        var transform=place("oak_full_drawers_1",pos(82,10,70));transform.put(0,item(Material.STONE),127);transform.install("iron_storage_upgrade");check("wood retrim preserves storage",com.cendreal.storagedrawers.module.interaction.DrawerTransform.apply(transform,custom("birch_trim"),actor));transform=drawer(pos(82,10,70));check("retrim identity and upgrades",transform.count(0)==127&&transform.behavior.block().id().value().startsWith("birch_")&&transform.has("iron_storage_upgrade"));check("repartition transforms",com.cendreal.storagedrawers.module.interaction.DrawerTransform.apply(transform,custom("oak_full_drawers_4"),actor));transform=drawer(pos(82,10,70));check("repartition conserves odd remainder",transform.size()==4&&transform.count(0)==32&&transform.count(3)==31);
        var remote=place("oak_full_drawers_1",pos(88,10,65));CraftEngineBlocks.place(pos(84,10,65),Key.of("storagedrawers:controller"),false);var root=(NetworkController)node(pos(84,10,65));Item bound=RemoteBinding.bind(custom("remote_upgrade"),root);check("remote binding installs",remote.install(bound)&&root.container().view().drawers().contains(remote));CraftEngineBlocks.place(pos(86,10,65),Key.of("storagedrawers:controller"),false);var rebound=(NetworkController)node(pos(86,10,65));check("bound remote updates target without changing installed type",remote.rebind(RemoteBinding.bind(custom("remote_group_upgrade"),rebound))&&!remote.remoteGroup()&&!root.container().view().drawers().contains(remote)&&rebound.container().view().drawers().contains(remote));Item removed=remote.uninstallItem(0);check("remote removal retains binding and disconnects",RemoteBinding.read(removed)!=null&&!rebound.container().view().drawers().contains(remote));
        networkLeaf=place("oak_full_drawers_1",pos(83,10,65));networkLeaf.put(0,item(Material.STONE),1);pos(84,11,65).getBlock().setType(Material.HOPPER);((Hopper)pos(84,11,65).getBlock().getState()).getInventory().setItem(0,new ItemStack(Material.STONE,8));check("aggregate controller exposes constant two slots",root.container().containerSize()==2);
        var balance1=place("oak_full_drawers_1",pos(86,10,70));var balance2=place("oak_full_drawers_1",pos(87,10,70));CraftEngineBlocks.place(pos(85,10,70),Key.of("storagedrawers:controller"),false);balance1.put(0,item(Material.COBBLESTONE),10);balance2.put(0,item(Material.COBBLESTONE),2);balance1.install("balance_fill_upgrade");balance2.install("balance_fill_upgrade");balance1.put(0,item(Material.COBBLESTONE),4,actor);check("balanced fill conserves contents",balance1.count(0)==8&&balance2.count(0)==8);
        collected=place("oak_full_drawers_1",pos(90,10,70));check("magnet installs",collected.install("magnet_upgrade"));pos(91,9,70).getBlock().setType(Material.STONE);magneticDrop=world.dropItem(new Location(world,91.2,10.1,70.5),new ItemStack(Material.EMERALD,7));magneticDrop.setVelocity(new org.bukkit.util.Vector(0,0,0));
        hopperCollected=place("oak_full_drawers_1",pos(93,10,70));hopperCollected.install("hopper_upgrade");var hopperItem=world.dropItem(new Location(world,93.5,11.2,70.5),new ItemStack(Material.COAL,5));hopperItem.setVelocity(new org.bukkit.util.Vector(0,0,0));
        buttonTests();customCompactingTest();
        Item balanced=balance1.detach(0);balance2.put(0,item(Material.COBBLESTONE),16,actor);check("reattaching a balanced drawer redistributes conserved contents",balance1.attach(0,balanced,actor)&&balance1.count(0)==16&&balance2.count(0)==16);
        frameTests();craftTests();
    }
    private void customCompactingTest(){var compress=new ShapedRecipe(new NamespacedKey("storagedrawers","audit_compact"),(ItemStack)custom("obsidian_storage_upgrade").platformItem()).shape("AA","AA");compress.setIngredient('A',new RecipeChoice.ExactChoice((ItemStack)custom("upgrade_template").platformItem()));var reverse=new ShapelessRecipe(new NamespacedKey("storagedrawers","audit_unpack"),(ItemStack)custom("upgrade_template").count(4).platformItem());reverse.addIngredient(new RecipeChoice.ExactChoice((ItemStack)custom("obsidian_storage_upgrade").platformItem()));Bukkit.addRecipe(compress);Bukkit.addRecipe(reverse);try{var chain=new CompactingRules().chain(custom("upgrade_template"),2);check("custom CE compacting uses reversible exact recipes",chain!=null&&chain.items()[0].id().equals(Key.of("storagedrawers:obsidian_storage_upgrade"))&&chain.factors()[0]==4);}finally{Bukkit.removeRecipe(compress.getKey());Bukkit.removeRecipe(reverse.getKey());}}
    private void buttonTests(){CraftEngineBlocks.place(pos(94,10,80),Key.of("storagedrawers:controller"),false);var leaf=place("oak_full_drawers_1",pos(94,10,79));buttonLocation=pos(94,10,81);var definition=CraftEngineBlocks.byId(Key.of("storagedrawers:keybutton_drawer"));var behavior=definition.defaultState().behavior().getFirst(com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior.class);var state=definition.defaultState().with(behavior.facing,Direction.SOUTH).with(behavior.face,"wall");check("button supported placement",CraftEngineBlocks.place(buttonLocation,state,false));var actor=BukkitAdaptor.adapt(player);var hit=new BlockHitResult(new Vec3d(94.5,10.5,81.9),Direction.SOUTH,new BlockPos(94,10,81),false);behavior.useWithoutItem(new UseOnContext(actor,InteractionHand.MAIN_HAND,Item.empty(),hit),state);state=CraftEngineBlocks.getCustomBlockState(buttonLocation.getBlock());check("button toggles attached controller group",state.get(behavior.powered)&&leaf.locked());var nativeState=(net.minecraft.world.level.block.state.BlockState)state.customBlockState().minecraftState();var level=((CraftWorld)world).getHandle();var position=new net.minecraft.core.BlockPos(94,10,81);check("button has no collision and directional strong signal",nativeState.getCollisionShape(level,position).isEmpty()&&nativeState.getDirectSignal(level,position,net.minecraft.core.Direction.SOUTH)==15&&nativeState.getDirectSignal(level,position,net.minecraft.core.Direction.NORTH)==0);}
    private void placeItem(Item item,Location at){at.getBlock().setType(Material.AIR);at.clone().subtract(0,1,0).getBlock().setType(Material.STONE);var actor=BukkitAdaptor.adapt(player);actor.setItemInHand(InteractionHand.MAIN_HAND,item);var hit=new BlockHitResult(new Vec3d(at.getX()+.5,at.getY(),at.getZ()+.5),Direction.UP,new BlockPos(at.getBlockX(),at.getBlockY()-1,at.getBlockZ()),false);item.getBehavior().orElseThrow().getFirst(net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior.class).place(new BlockPlaceContext(actor.world(),actor,InteractionHand.MAIN_HAND,item,hit));}
    private void frameTests(){
        player.teleport(pos(90,10,72));placeItem(custom("framing_table"),pos(90,10,74));var table=(com.cendreal.storagedrawers.module.frame.FramingController)node(pos(90,10,74));check("native framing table places both halves",table!=null&&node(pos(91,10,74)) instanceof com.cendreal.storagedrawers.module.frame.FramingController||table!=null&&node(pos(89,10,74)) instanceof com.cendreal.storagedrawers.module.frame.FramingController);
        Item base=custom("oak_full_drawers_1");base.customNameJson(JsonParser.parseString("{\"text\":\"Framed audit\"}"));var contents=com.cendreal.storagedrawers.module.crafting.StorageRecipe.emptyContents(base.id(),1);var slot=contents.getCompound("slot0");slot.putInt("count",23);slot.put("item",net.momirealms.craftengine.bukkit.util.ItemStackUtils.saveMinecraftItemStackAsTag(item(Material.DIAMOND).minecraftItem()));base.setSparrowTag(contents,DrawerController.DATA_KEY);base.maxStackSize(1);
        check("framing accepts real material inputs",table.set(0,base)&&table.set(1,item(Material.STONE))&&table.set(2,item(Material.OAK_PLANKS))&&table.set(3,item(Material.GOLD_BLOCK)));Item framed=table.craft(1);check("framing consumes inputs once",!framed.isEmpty()&&table.result().isEmpty()&&table.input(0).isEmpty());
        placeItem(framed.copy(),pos(92,10,74));var drawer=drawer(pos(92,10,74));check("framed CE placement restores contents and materials",drawer!=null&&drawer.behavior.framed&&drawer.count(0)==23&&drawer.frame().side().equals("minecraft:stone"));check("unframing preserves base metadata",table.set(0,framed)&&table.input(0).customNameJson().equals(base.customNameJson())&&table.input(0).id().equals(base.id())&&table.input(1).id().equals(Key.of("minecraft:stone")));
    }
    private ItemStack craft(ItemStack[] matrix){
        player.setItemOnCursor(null);player.openWorkbench(player.getLocation(),true);var inventory=(CraftingInventory)player.getOpenInventory().getTopInventory();inventory.setMatrix(matrix);var nativePlayer=((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle();var menu=(net.minecraft.world.inventory.CraftingMenu)nativePlayer.containerMenu;menu.slotsChanged(menu.craftSlots);ItemStack preview=inventory.getResult();if(preview==null||preview.isEmpty())return new ItemStack(Material.AIR);menu.clicked(0,0,net.minecraft.world.inventory.ContainerInput.PICKUP,nativePlayer);return player.getItemOnCursor().clone();
    }
    private void craftTests(){
        ItemStack[] grid=new ItemStack[9];grid[0]=(ItemStack)custom("oak_full_drawers_1").platformItem();grid[1]=(ItemStack)custom("iron_storage_upgrade").platformItem();ItemStack upgraded=craft(grid);check("native special upgrade crafting assembles contents",!upgraded.isEmpty()&&BukkitAdaptor.adapt(upgraded).hasTag(DrawerController.DATA_KEY));var inventory=(CraftingInventory)player.getOpenInventory().getTopInventory();check("native special crafting consumes each input",Arrays.stream(inventory.getMatrix()).allMatch(s->s==null||s.isEmpty()));
        grid=new ItemStack[9];for(int i:new int[]{1,3,5,7})grid[i]=new ItemStack(Material.IRON_NUGGET);grid[4]=(ItemStack)custom("drawer_key").platformItem();ItemStack ring=craft(grid);check("native keyring recipe",!ring.isEmpty()&&com.cendreal.storagedrawers.module.interaction.RingTools.effectiveKey(BukkitAdaptor.adapt(ring)).equals("drawer_key"));
        grid=new ItemStack[9];grid[0]=upgraded;grid[1]=new ItemStack(Material.OAK_PLANKS);ItemStack invalid=craft(grid);check("ordinary recipes reject filled portable items",invalid.isEmpty());player.closeInventory();
    }
    private void fail(Throwable t){report.addProperty("passed",false);report.addProperty("error",t.toString());plugin.getLogger().log(java.util.logging.Level.SEVERE,"StorageDrawers runtime audit",t);finish();}
    private void finish(){try{if(player!=null){player.closeInventory();BukkitCraftEngine.instance().networkManager().removeFakePlayer(player);}Files.createDirectories(plugin.getDataFolder().toPath());Files.writeString(plugin.getDataFolder().toPath().resolve(Boolean.getBoolean("storagedrawers.ce.recovery")?"recovery-audit.json":"runtime-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new IllegalStateException(e);}finally{if(Boolean.getBoolean("storagedrawers.ce.auditStop"))Bukkit.getScheduler().runTaskLater(plugin,Bukkit::shutdown,10);}}
}
