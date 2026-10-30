package com.cendreal.storagedrawers.module.storage;

import net.momirealms.craftengine.core.util.Direction;

/** Coordinates are measured looking at the north face, matching StorageDrawers. */
public record DrawerLayout(String kind, int drawers, boolean halfDepth, int baseStacks) {
    public boolean storage() { return drawers > 0; }
    public boolean compacting() { return kind.equals("compacting"); }
    public boolean network() { return kind.equals("controller") || kind.equals("io"); }
    public int slot(Direction facing, Direction face, double x, double y, double z) {
        if (!storage() || face != facing || y < .0625 || y > .9375) return -1;
        double horizontal = switch (face) { case NORTH -> 1-x; case SOUTH -> x; case WEST -> z; case EAST -> 1-z; default -> -1; };
        if (horizontal < .0625 || horizontal > .9375) return -1;
        if (drawers == 1) return 0;
        if (drawers == 2) return y >= .5 ? 0 : 1;
        if (drawers == 3) return y >= .5 ? 0 : horizontal < .5 ? 1 : 2;
        return (y >= .5 ? 0 : 2) + (horizontal < .5 ? 0 : 1);
    }
    public float displayX(int slot) {
        if (drawers < 3 || drawers == 3 && slot == 0) return 0;
        return (drawers == 3 ? slot == 1 : slot % 2 == 0) ? -.25f : .25f;
    }
    public float displayY(int slot) { return drawers == 1 ? 0 : (drawers == 3 ? slot == 0 : slot < (drawers == 4 ? 2 : 1)) ? .25f : -.25f; }
}
