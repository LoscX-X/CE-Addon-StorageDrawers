package com.cendreal.storagedrawers.module.storage;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.*;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.item.Item;

/** Immutable recipe index; recipe enumeration happens on reload, never on a transfer. */
public final class CompactingRules {
    public record Edge(Material large, Material small, int ratio) {}
    public record Chain(Item[] items, int[] factors) {}
    private final Map<Material,Edge> up = new EnumMap<>(Material.class), down = new EnumMap<>(Material.class);
    private record CustomEdge(Item large,Item small,int ratio){}
    private final Map<net.momirealms.craftengine.core.util.Key,CustomEdge> customUp=new HashMap<>(),customDown=new HashMap<>();
    private static Material single(RecipeChoice choice) {
        if(choice instanceof RecipeChoice.MaterialChoice material && material.getChoices().size()==1)return material.getChoices().getFirst();
        if(choice instanceof RecipeChoice.ExactChoice exact && exact.getChoices().size()==1) {ItemStack stack=exact.getChoices().getFirst();if(stack.isSimilar(new ItemStack(stack.getType())))return stack.getType();}
        return null;
    }
    public CompactingRules() {
        List<Edge> candidates=new ArrayList<>();
        Set<String> reverse=new HashSet<>();
        Iterator<Recipe> iterator=Bukkit.recipeIterator();
        while(iterator.hasNext()) {
            Recipe r=iterator.next(); ItemStack result=r.getResult();
            if(!result.isSimilar(new ItemStack(result.getType()))) continue;
            if(r instanceof ShapelessRecipe sr && sr.getChoiceList().size()==1 && result.getAmount()>1 && single(sr.getChoiceList().getFirst())!=null)
                reverse.add(single(sr.getChoiceList().getFirst())+":"+result.getType()+":"+result.getAmount());
            if(r instanceof ShapedRecipe reverseShape && reverseShape.getShape().length==1 && reverseShape.getShape()[0].length()==1 && result.getAmount()>1 && single(reverseShape.getChoiceMap().get(reverseShape.getShape()[0].charAt(0)))!=null)
                reverse.add(single(reverseShape.getChoiceMap().get(reverseShape.getShape()[0].charAt(0)))+":"+result.getType()+":"+result.getAmount());
            if(!(r instanceof ShapedRecipe shaped) || result.getAmount()!=1) continue;
            String[] pattern=shaped.getShape();
            if(pattern.length!=2 && pattern.length!=3) continue;
            if(Arrays.stream(pattern).anyMatch(row->row.length()!=pattern.length || row.indexOf(' ')>=0)) continue;
            Material material=null; boolean valid=true;
            for(String row:pattern) for(char key:row.toCharArray()) {
                Material selected=single(shaped.getChoiceMap().get(key));
                if(selected==null) {valid=false;break;}
                if(material==null) material=selected;
                else if(material!=selected) {valid=false;break;}
            }
            if(valid && material!=null) candidates.add(new Edge(result.getType(),material,pattern.length*pattern.length));
        }
        candidates.sort(Comparator.comparing(e->e.large().name()));
        for(Edge e:candidates) if(reverse.contains(e.large()+":"+e.small()+":"+e.ratio())) add(e);
        // These are the default StorageDrawers extra rules, including its explicit clay rule.
        add(new Edge(Material.CLAY,Material.CLAY_BALL,4));
        add(new Edge(Material.BRICKS,Material.BRICK,4));
        add(new Edge(Material.QUARTZ_BLOCK,Material.QUARTZ,4));
        loadCustomRules();
    }
    private Item exact(RecipeChoice choice){Item item=null;if(choice instanceof RecipeChoice.ExactChoice exact&&exact.getChoices().size()==1)item=BukkitAdaptor.adapt(exact.getChoices().getFirst()).copyWithCount(1);else if(choice instanceof RecipeChoice.MaterialChoice materials&&materials.getChoices().size()==1)item=BukkitAdaptor.adapt(new ItemStack(materials.getChoices().getFirst()));return item!=null&&item.isSimilar(Item.byId(item.id()))?item:null;}
    private void loadCustomRules(){
        Map<String,Boolean> reverse=new HashMap<>();List<CustomEdge> candidates=new ArrayList<>();var recipes=Bukkit.recipeIterator();
        while(recipes.hasNext()){Recipe recipe=recipes.next();ItemStack result=recipe.getResult();if(result==null||result.isEmpty())continue;Item output=BukkitAdaptor.adapt(result).copyWithCount(1);if(!output.isSimilar(Item.byId(output.id())))continue;
            RecipeChoice single=null;if(recipe instanceof ShapelessRecipe r&&r.getChoiceList().size()==1)single=r.getChoiceList().getFirst();else if(recipe instanceof ShapedRecipe r&&r.getShape().length==1&&r.getShape()[0].length()==1)single=r.getChoiceMap().get(r.getShape()[0].charAt(0));
            if(single!=null&&result.getAmount()>1){Item from=exact(single);if(from!=null)reverse.put(from.id()+"/"+output.id()+"/"+result.getAmount(),true);}
            if(!(recipe instanceof ShapedRecipe shaped)||result.getAmount()!=1)continue;String[] shape=shaped.getShape();if(shape.length!=2&&shape.length!=3||Arrays.stream(shape).anyMatch(row->row.length()!=shape.length||row.indexOf(' ')>=0))continue;Item material=null;boolean valid=true;for(String row:shape)for(char cell:row.toCharArray()){Item selected=exact(shaped.getChoiceMap().get(cell));if(selected==null||material!=null&&!material.isSimilar(selected)){valid=false;break;}material=selected;}if(valid&&material!=null)candidates.add(new CustomEdge(output,material,shape.length*shape.length));
        }
        for(Edge edge:down.values())addCustom(new CustomEdge(BukkitAdaptor.adapt(new ItemStack(edge.large())),BukkitAdaptor.adapt(new ItemStack(edge.small())),edge.ratio()));
        candidates.sort(Comparator.comparing(e->e.large().id().toString()));for(CustomEdge edge:candidates)if(reverse.containsKey(edge.large().id()+"/"+edge.small().id()+"/"+edge.ratio()))addCustom(edge);
    }
    private void addCustom(CustomEdge edge){if(!edge.large().id().equals(edge.small().id())){customUp.putIfAbsent(edge.small().id(),edge);customDown.putIfAbsent(edge.large().id(),edge);}}
    private Chain customChain(Item input,int slots){if(!input.isSimilar(Item.byId(input.id())))return null;Item largest=input.copyWithCount(1);Set<net.momirealms.craftengine.core.util.Key> visited=new HashSet<>();visited.add(largest.id());for(int i=1;i<slots;i++){CustomEdge edge=customUp.get(largest.id());if(edge==null||!visited.add(edge.large().id()))break;largest=edge.large();}List<Item> types=new ArrayList<>();List<Integer> ratios=new ArrayList<>();types.add(largest);visited.clear();visited.add(largest.id());while(types.size()<slots){CustomEdge edge=customDown.get(types.getLast().id());if(edge==null||!visited.add(edge.small().id()))break;ratios.add(edge.ratio());types.add(edge.small());}Item[] items=new Item[slots];Arrays.fill(items,Item.empty());int[] factors=new int[slots];Arrays.fill(factors,1);for(int i=0;i<types.size();i++)items[i]=types.get(i).copyWithCount(1);for(int i=types.size()-2;i>=0;i--)factors[i]=Math.multiplyExact(factors[i+1],ratios.get(i));return new Chain(items,factors);}
    private void add(Edge edge) { if(edge.large()!=edge.small()) {up.putIfAbsent(edge.small(),edge);down.putIfAbsent(edge.large(),edge);} }
    public Chain chain(Item input,int slots) {
        if(input.isCustomItem())return customChain(input,slots);
        Object platform=input.platformItem();
        if(!(platform instanceof ItemStack stack) || !stack.isSimilar(new ItemStack(stack.getType()))) return null;
        Material largest=stack.getType();
        Set<Material> visited=EnumSet.noneOf(Material.class); visited.add(largest);
        for(int i=1;i<slots;i++) {Edge e=up.get(largest);if(e==null || !visited.add(e.large())) break;largest=e.large();}
        List<Material> types=new ArrayList<>();List<Integer> ratios=new ArrayList<>();types.add(largest);
        visited.clear();visited.add(largest);
        while(types.size()<slots) {Edge e=down.get(types.getLast());if(e==null || !visited.add(e.small())) break;ratios.add(e.ratio());types.add(e.small());}
        Item[] items=new Item[slots]; Arrays.fill(items,Item.empty());int[] factors=new int[slots];Arrays.fill(factors,1);
        for(int i=0;i<types.size();i++) items[i]=BukkitAdaptor.adapt(new ItemStack(types.get(i)));
        for(int i=types.size()-2;i>=0;i--) factors[i]=Math.multiplyExact(factors[i+1],ratios.get(i));
        return new Chain(items,factors);
    }
    public int size() { return down.size(); }
}
