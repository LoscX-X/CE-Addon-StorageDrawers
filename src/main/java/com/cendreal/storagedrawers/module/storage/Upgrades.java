package com.cendreal.storagedrawers.module.storage;

import java.util.Map;
import java.util.List;

public final class Upgrades {
    public static final int SLOTS = 7;
    public static final Map<String,Integer> MULTIPLIERS = Map.of("obsidian_storage_upgrade",2,"copper_storage_upgrade",4,"iron_storage_upgrade",8,"gold_storage_upgrade",16,"emerald_storage_upgrade",64,"diamond_storage_upgrade",256,"netherite_storage_upgrade",2048);
    public static boolean isUpgrade(String id) { return MULTIPLIERS.containsKey(id) || List.of("void_upgrade","one_stack_upgrade","redstone_upgrade","min_redstone_upgrade","max_redstone_upgrade","creative_storage_upgrade","creative_vending_upgrade","conversion_upgrade","illumination_upgrade","fill_level_upgrade","balance_fill_upgrade","portability_upgrade","hopper_upgrade","magnet_upgrade","magnet_upgrade_2","magnet_upgrade_3","remote_upgrade","remote_upgrade_bound","remote_group_upgrade","remote_group_upgrade_bound").contains(id); }
    public static int multiplier(List<String> installed) { int result=0; for(String id:installed) result+=MULTIPLIERS.getOrDefault(id,0); return Math.max(1,result); }
    public static boolean valid(List<String> proposed){java.util.Set<String> unique=new java.util.HashSet<>();int redstone=0,remotes=0;for(String id:proposed){if(id.isEmpty())continue;if(!isUpgrade(id))return false;if(!MULTIPLIERS.containsKey(id)&&!id.startsWith("magnet_upgrade")&&!unique.add(id))return false;if(id.endsWith("redstone_upgrade")&&++redstone>1)return false;if(id.startsWith("remote_")&&++remotes>1)return false;}return true;}
    private Upgrades() {}
}
