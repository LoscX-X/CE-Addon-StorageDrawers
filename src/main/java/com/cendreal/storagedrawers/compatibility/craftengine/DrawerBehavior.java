package com.cendreal.storagedrawers.compatibility.craftengine;

import com.cendreal.storagedrawers.module.storage.*;
import com.cendreal.storagedrawers.module.automation.NetworkController;
import com.cendreal.storagedrawers.module.interaction.DrawerInteraction;
import com.cendreal.storagedrawers.module.frame.FramingController;
import net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehavior;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.block.behavior.*;
import net.momirealms.craftengine.core.block.entity.*;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.*;

public final class DrawerBehavior extends BukkitBlockBehavior implements EntityBlock,WorldlyContainerHolder {
    public final DrawerLayout layout;
    public final boolean framed;
    public final Property<Direction> facing;
    public final Property<String> part,face;
    public final Property<Boolean> powered;
    private int controllerId;
    public DrawerBehavior(BlockDefinition definition,ConfigSection config) {
        super(definition);
        framed=config.getBoolean("framed",false);
        layout=new DrawerLayout(config.getString("kind","standard"),config.getInt("drawers",1),config.getBoolean("half_depth",false),config.getInt("base_stacks",32));
        if(!java.util.List.of("standard","compacting","trim","controller","io","table","button").contains(layout.kind()) || layout.storage() && (layout.drawers()>4 || layout.baseStacks()<1))throw new IllegalArgumentException("Invalid StorageDrawers layout");
        facing=BlockBehaviorFactory.getProperty("storagedrawers:drawer",definition,"facing",Direction.class);
        part=BlockBehaviorFactory.getOptionalProperty(definition,"part",String.class);face=BlockBehaviorFactory.getOptionalProperty(definition,"face",String.class);powered=BlockBehaviorFactory.getOptionalProperty(definition,"powered",Boolean.class);
    }
    @Override public void initControllerId(int id) {controllerId=id;}
    @Override public BlockEntityController createBlockEntityController(BlockEntity entity) {return layout.storage()?new DrawerController(entity,this):layout.network()?new NetworkController(entity,this):layout.kind().equals("table")?new FramingController(entity,this):new NodeController(entity,this);}
    public NodeController controller(BlockEntity entity) {return entity==null?null:entity.controller.get(NodeController.class,controllerId);}
    public NodeController at(net.momirealms.craftengine.core.world.World world,BlockPos pos) {return controller(world.storageWorld().getBlockEntityAtIfLoaded(pos));}
    @Override public Object playerWillDestroy(Object self,Object[] args){
        if(args[0] instanceof net.minecraft.server.level.ServerLevel world&&args[1] instanceof net.minecraft.core.BlockPos pos&&args[3] instanceof net.minecraft.server.level.ServerPlayer player&&player.preventsBlockDrops()){
            var node=at(BukkitAdaptor.adapt(world.getWorld()),new BlockPos(pos.getX(),pos.getY(),pos.getZ()));
            if(node!=null&&(!node.frame().side().isEmpty()||node instanceof DrawerController drawer&&drawer.hasContents()))net.minecraft.world.level.block.Block.popResource(world,pos,(net.minecraft.world.item.ItemStack)ContentsLoot.portable(node).minecraftItem());
        }
        return super.playerWillDestroy(self,args);
    }
    @Override public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context,ImmutableBlockState state) {
        if(layout.kind().equals("button")){Direction clicked=context.getClickedFace();return state.with(facing,clicked==Direction.UP||clicked==Direction.DOWN?context.getHorizontalDirection().opposite():clicked).with(face,clicked==Direction.UP?"floor":clicked==Direction.DOWN?"ceiling":"wall");}
        if(layout.kind().equals("table")){
            state=state.with(facing,context.getHorizontalDirection()).with(part,"right");var second=context.getClickedPos().relative(state.get(facing).counterClockWise());
            if(!canPlaceMultiState(context.getWorld(),context.getClickedPos(),state))return null;
            if(context.getPlayer()!=null&&!net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player)context.getPlayer().platformPlayer(),net.momirealms.craftengine.libraries.antigrieflib.Flag.PLACE,new org.bukkit.Location((org.bukkit.World)context.getWorld().platformWorld(),second.x,second.y,second.z)))return null;
            return state;
        }
        return state.with(facing,context.getHorizontalDirection().opposite());
    }
    @Override public boolean hasMultiState(ImmutableBlockState state){return layout.kind().equals("table");}
    @Override public boolean canPlaceMultiState(net.momirealms.craftengine.core.world.WorldAccessor accessor,BlockPos pos,ImmutableBlockState state){if(!layout.kind().equals("table"))return true;if(!(accessor.minecraftWorld() instanceof net.minecraft.server.level.ServerLevel world))return false;var second=pos.relative(state.get(facing).counterClockWise());var nativePos=new net.minecraft.core.BlockPos(second.x,second.y,second.z);return world.hasChunkAt(nativePos)&&world.getWorldBorder().isWithinBounds(nativePos)&&world.getBlockState(nativePos).canBeReplaced();}
    @Override public void placeMultiState(Object self,Object[] args){if(!layout.kind().equals("table")||!(args[0] instanceof net.minecraft.server.level.ServerLevel world)||!(args[1] instanceof net.minecraft.core.BlockPos pos))return;var current=CraftEngineBlocksCompat.state(world,pos);if(current==null)return;var second=new BlockPos(pos.getX(),pos.getY(),pos.getZ()).relative(current.get(facing).counterClockWise());world.setBlock(new net.minecraft.core.BlockPos(second.x,second.y,second.z),(net.minecraft.world.level.block.state.BlockState)current.with(part,"left").customBlockState().minecraftState(),3);}
    @Override public void tick(Object self,Object[] args){if(!layout.kind().equals("button")||!(args[1] instanceof net.minecraft.server.level.ServerLevel world)||!(args[2] instanceof net.minecraft.core.BlockPos pos))return;var state=CraftEngineBlocksCompat.state(world,pos);if(state!=null)world.setBlock(pos,(net.minecraft.world.level.block.state.BlockState)state.with(powered,false).customBlockState().minecraftState(),3);}
    @Override public boolean isSignalSource(Object self,Object[] args){return layout.kind().equals("button");}
    @Override public int getSignal(Object self,Object[] args){if(!layout.kind().equals("button"))return 0;var state=net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);return state!=null&&state.get(powered)?15:0;}
    @Override public int getDirectSignal(Object self,Object[] args){if(!layout.kind().equals("button"))return 0;var state=net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);if(state==null)return 0;return vanillaButton(state).getDirectSignal((net.minecraft.world.level.BlockGetter)args[1],(net.minecraft.core.BlockPos)args[2],(net.minecraft.core.Direction)args[3]);}
    public net.minecraft.world.level.block.state.BlockState vanillaButton(ImmutableBlockState state){return net.minecraft.world.level.block.Blocks.STONE_BUTTON.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,net.minecraft.core.Direction.valueOf(state.get(facing).name())).setValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.valueOf(state.get(face).toUpperCase(java.util.Locale.ROOT))).setValue(net.minecraft.world.level.block.ButtonBlock.POWERED,state.get(powered));}
    @Override public boolean canSurvive(Object self,Object[] args){if(!layout.kind().equals("button"))return super.canSurvive(self,args);var state=net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);return state!=null&&vanillaButton(state).canSurvive((net.minecraft.world.level.LevelReader)args[1],(net.minecraft.core.BlockPos)args[2]);}
    @Override public Object updateShape(Object self,Object[] args){if(layout.kind().equals("button")){var state=net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);if(state!=null&&!vanillaButton(state).canSurvive((net.minecraft.world.level.LevelReader)args[1],(net.minecraft.core.BlockPos)args[3]))return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();}return super.updateShape(self,args);}
    private static final class CraftEngineBlocksCompat{static ImmutableBlockState state(net.minecraft.server.level.ServerLevel world,net.minecraft.core.BlockPos pos){return net.momirealms.craftengine.bukkit.api.CraftEngineBlocks.getCustomBlockState(world.getWorld().getBlockAt(pos.getX(),pos.getY(),pos.getZ()));}}
    @Override public InteractionResult useOnBlock(UseOnContext context,ImmutableBlockState state) {return DrawerInteraction.use(this,context,state);}
    @Override public InteractionResult useWithoutItem(UseOnContext context,ImmutableBlockState state) {return DrawerInteraction.use(this,context,state);}
    @Override public boolean canUseOnBlockIfSecondaryUseActive(UseOnContext context,ImmutableBlockState state) {return true;}
    @Override public Object getContainer(Object self,Object[] args) {
        if(!(args[1] instanceof net.minecraft.server.level.ServerLevel world) || !(args[2] instanceof net.minecraft.core.BlockPos pos)) return null;
        var node=at(BukkitAdaptor.adapt(world.getWorld()),new BlockPos(pos.getX(),pos.getY(),pos.getZ()));
        if(node instanceof DrawerController drawer)return drawer.container().nativeContainer();
        if(node instanceof NetworkController network)return network.container().nativeContainer();
        return null;
    }
    @Override public boolean hasAnalogOutputSignal(Object self,Object[] args) {return layout.storage();}
    @Override public int getAnalogOutputSignal(Object self,Object[] args) {
        if(!(args[1] instanceof net.minecraft.server.level.ServerLevel world) || !(args[2] instanceof net.minecraft.core.BlockPos pos))return 0;
        var node=at(BukkitAdaptor.adapt(world.getWorld()),new BlockPos(pos.getX(),pos.getY(),pos.getZ()));
        return node instanceof DrawerController drawer?drawer.signal():0;
    }
}
