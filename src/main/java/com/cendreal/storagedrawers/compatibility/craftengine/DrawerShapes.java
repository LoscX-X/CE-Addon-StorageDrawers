package com.cendreal.storagedrawers.compatibility.craftengine;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.util.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class DrawerShapes {
    private record Shape(VoxelShape value,VoxelShape collision,VoxelShape support) implements BlockShape {
        Shape(VoxelShape value){this(value,value,value);}
        @Override public Object getShape(Object a,Object b,Object c,Object d,Object e) {return value;}
        @Override public Object getCollisionShape(Object a,Object b,Object c,Object d,Object e) {return collision;}
        @Override public Object getSupportShape(Object a,Object b,Object c,Object d) {return support;}
    }
    public static int install() {
        int count=0;
        for(var definition:CraftEngineBlocks.loadedBlocks().values()) {
            if(!definition.id().namespace().equals("storagedrawers"))continue;
            var behavior=definition.defaultState().behavior().getFirst(DrawerBehavior.class);
            if(behavior==null)continue;
            for(var state:definition.variantProvider().states()) {
                Direction facing=state.get(behavior.facing);
                VoxelShape shape=Shapes.block();
                if(behavior.layout.storage()) {
                    VoxelShape recess=switch(facing) {case SOUTH->Block.box(1,1,15,15,15,16);case WEST->Block.box(0,1,1,1,15,15);case EAST->Block.box(15,1,1,16,15,15);default->Block.box(1,1,0,15,15,1);};
                    shape=behavior.layout.halfDepth()?switch(facing) {case SOUTH->Block.box(0,0,0,16,16,8);case WEST->Block.box(8,0,0,16,16,16);case EAST->Block.box(0,0,0,8,16,16);default->Block.box(0,0,8,16,16,16);}:Shapes.join(Shapes.block(),recess,BooleanOp.ONLY_FIRST);
                }
                if(behavior.layout.kind().equals("table")){Direction edge=state.get(behavior.part).equals("left")?facing.counterClockWise():facing.clockWise();VoxelShape bottom=switch(edge){case NORTH->Block.box(1,0,0,15,16,15);case SOUTH->Block.box(1,0,1,15,16,16);case WEST->Block.box(1,0,1,16,16,15);default->Block.box(0,0,1,15,16,15);};shape=Shapes.or(Block.box(0,14,0,16,16,16),bottom);}
                Shape delegate=new Shape(shape);
                if(behavior.layout.kind().equals("button")){var button=behavior.vanillaButton(state);shape=button.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO);delegate=new Shape(shape,Shapes.empty(),Shapes.empty());}
                BlockState nativeState=(BlockState)state.customBlockState().minecraftState();
                ((DelegatingBlock)nativeState.getBlock()).shapeDelegate().bindValue(delegate);nativeState.initCache();count++;
            }
        }
        return count;
    }
    private DrawerShapes() {}
}
