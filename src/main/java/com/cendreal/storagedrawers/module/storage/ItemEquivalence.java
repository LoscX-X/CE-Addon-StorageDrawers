package com.cendreal.storagedrawers.module.storage;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;

/** Opt-in conversion groups and allowlisted tags. Player-modified components never disappear in conversion. */
public final class ItemEquivalence {
    private final List<Set<Key>> groups=new ArrayList<>();
    private final Set<Key> tags=new HashSet<>();
    public ItemEquivalence(FileConfiguration config){
        for(String group:config.getStringList("conversion.item-equivalence-groups")){Set<Key> members=new HashSet<>();for(String id:group.split(";"))members.add(Key.of(id.trim()));if(members.size()>1)groups.add(Set.copyOf(members));}
        for(String id:config.getStringList("conversion.tag-allow-list"))tags.add(Key.of(id));
        for(String type:config.getStringList("conversion.ore-types"))for(String material:config.getStringList("conversion.ore-materials"))tags.add(Key.of(type+"/"+material));
        for(String id:config.getStringList("conversion.tag-deny-list"))tags.remove(Key.of(id));
    }
    private boolean plain(Item item){return item.isSimilar(Item.byId(item.id()));}
    public boolean equivalent(Item left,Item right){if(left.isEmpty() || right.isEmpty() || !plain(left) || !plain(right))return false;for(Set<Key> group:groups)if(group.contains(left.id()) && group.contains(right.id()))return true;for(Key tag:tags)if((left.hasPluginTag(tag)||left.hasVanillaTag(tag)) && (right.hasPluginTag(tag)||right.hasVanillaTag(tag)))return true;return false;}
}
