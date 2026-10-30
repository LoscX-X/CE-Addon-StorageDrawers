package com.cendreal.storagedrawers.module.automation;

import com.cendreal.storagedrawers.module.storage.*;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import java.util.*;

/** Index only loaded CE block entities. A cache miss is bounded and never requests a chunk. */
public final class NetworkIndex {
    private int range=50,remoteRange=50,remoteGroupRange=50;
    private static final Direction[] DIRECTIONS=Direction.values();
    private static final class WorldNodes {long revision,contents,templates;final Map<Long,NodeController> nodes=new HashMap<>();final Map<Long,Set<DrawerController>> remotes=new HashMap<>();final Map<DrawerController,Long> bindings=new IdentityHashMap<>();final Map<NodeController,View> views=new IdentityHashMap<>();}
    public record View(NodeController controller,List<DrawerController> drawers,long revision) {}
    private final Map<UUID,WorldNodes> worlds=new HashMap<>();
    private long traversals;
    public void configure(org.bukkit.configuration.ConfigurationSection config){
        int next=Math.max(1,config.getInt("controller.range",50));int single=config.getInt("controller.remote.max-range",0),group=config.getInt("controller.remote.max-group-range",0);
        int nextRemote=single>0?Math.min(next,single):next,nextGroup=group>0?Math.min(next,group):next;
        if(range==next&&remoteRange==nextRemote&&remoteGroupRange==nextGroup)return;range=next;remoteRange=nextRemote;remoteGroupRange=nextGroup;
        for(var world:worlds.values()){world.revision++;world.views.clear();}
    }
    public void add(NodeController node) {var e=node.blockEntity();var w=worlds.computeIfAbsent(e.world.uuid(),k->new WorldNodes());if(w.nodes.put(e.pos.asLong(),node)!=node){w.revision++;w.views.clear();bind(w,node);}}
    public void remove(NodeController node) {var e=node.blockEntity();if(e.world==null)return;var w=worlds.get(e.world.uuid());if(w!=null && w.nodes.remove(e.pos.asLong(),node)) {w.revision++;w.views.clear();unbind(w,node);if(w.nodes.isEmpty())worlds.remove(e.world.uuid());}}
    private void unbind(WorldNodes w,NodeController node){if(node instanceof DrawerController d){Long old=w.bindings.remove(d);if(old!=null){var set=w.remotes.get(old);if(set!=null){set.remove(d);if(set.isEmpty())w.remotes.remove(old);}}}}
    private void bind(WorldNodes w,NodeController node){unbind(w,node);if(node instanceof DrawerController d && d.remote()!=null && d.remote().world().equals(node.blockEntity().world.uuid())){long pos=d.remote().position();w.bindings.put(d,pos);w.remotes.computeIfAbsent(pos,k->Collections.newSetFromMap(new IdentityHashMap<>())).add(d);}}
    public void changed(NodeController node){var e=node.blockEntity();if(e.world==null)return;var w=worlds.get(e.world.uuid());if(w!=null && w.nodes.get(e.pos.asLong())==node){w.revision++;w.views.clear();bind(w,node);}}
    public void contentsChanged(NodeController node,boolean identity){var e=node.blockEntity();if(e.world==null)return;var w=worlds.get(e.world.uuid());if(w!=null && w.nodes.get(e.pos.asLong())==node){w.contents++;if(identity)w.templates++;}}
    public long contentsRevision(NodeController node){var w=worlds.get(node.blockEntity().world.uuid());return w==null?-1:w.contents;}
    public long templateRevision(NodeController node){var w=worlds.get(node.blockEntity().world.uuid());return w==null?-1:w.templates;}
    public long revision(NodeController node) {var w=worlds.get(node.blockEntity().world.uuid());return w==null?-1:w.revision;}
    public View view(NodeController start) {
        var w=worlds.get(start.blockEntity().world.uuid());
        if(w==null||w.nodes.get(start.blockEntity().pos.asLong())!=start)return new View(null,List.of(),-1);
        View cached=w.views.get(start);if(cached!=null)return cached;
        List<NodeController> nodes=walk(w,start);
        NodeController root=start.behavior.layout.kind().equals("controller")?start:nodes.stream().filter(n->n.behavior.layout.kind().equals("controller")).min(Comparator.comparingLong(n->n.blockEntity().pos.asLong())).orElse(null);
        if(root==null&&start instanceof DrawerController drawer&&drawer.remote()!=null){var bound=w.nodes.get(drawer.remote().position());if(bound!=null&&bound.behavior.layout.kind().equals("controller")&&within(start.blockEntity().pos,bound.blockEntity().pos,drawer.remoteGroup()?remoteGroupRange:remoteRange))root=bound;}
        if(root==null){View result=new View(null,List.of(),w.revision);w.views.put(start,result);return result;}
        if(root!=start) nodes=walk(w,root);
        List<DrawerController> drawers=new ArrayList<>();for(var n:nodes) if(n instanceof DrawerController d) drawers.add(d);
        drawers.sort(Comparator.<DrawerController>comparingInt(DrawerController::priority).reversed().thenComparingLong(d->d.blockEntity().pos.asLong()));
        View result=new View(root,List.copyOf(drawers),w.revision);w.views.put(start,result);return result;
    }
    private List<NodeController> walk(WorldNodes world,NodeController root) {
        traversals++;
        BlockPos origin=root.blockEntity().pos;
        Map<Long,NodeController> result=new LinkedHashMap<>();Set<Long> visited=new HashSet<>();
        walkFrom(world,root,origin,range,visited,result);
        for(DrawerController remote:world.remotes.getOrDefault(origin.asLong(),Set.of())){var pos=remote.blockEntity().pos;if(!remote.blockEntity().isValid()||!within(pos,origin,remote.remoteGroup()?remoteGroupRange:remoteRange))continue;if(remote.remoteGroup())walkFrom(world,remote,origin,remoteGroupRange,visited,result);else result.put(pos.asLong(),remote);}
        return new ArrayList<>(result.values());
    }
    private static boolean within(BlockPos pos,BlockPos origin,int range){return Math.abs(pos.x-origin.x)<=range&&Math.abs(pos.y-origin.y)<=range&&Math.abs(pos.z-origin.z)<=range;}
    private void walkFrom(WorldNodes world,NodeController root,BlockPos origin,int limit,Set<Long> visited,Map<Long,NodeController> result){
        ArrayDeque<NodeController> queue=new ArrayDeque<>();if(visited.add(root.blockEntity().pos.asLong()))queue.add(root);
        while(!queue.isEmpty()) {
            NodeController current=queue.removeFirst();result.put(current.blockEntity().pos.asLong(),current);
            for(Direction direction:DIRECTIONS) {
                BlockPos pos=current.blockEntity().pos.relative(direction);
                if(!within(pos,origin,limit)||!visited.add(pos.asLong()))continue;
                NodeController next=world.nodes.get(pos.asLong());
                if(next!=null && next.blockEntity().isValid())queue.addLast(next);
            }
        }
    }
    public int loadedNodes() {return worlds.values().stream().mapToInt(w->w.nodes.size()).sum();}
    public List<DrawerController> loadedDrawers(UUID world){var nodes=worlds.get(world);return nodes==null?List.of():nodes.nodes.values().stream().filter(n->n instanceof DrawerController&&n.blockEntity().isValid()).map(n->(DrawerController)n).toList();}
    public long traversals() {return traversals;}
    public void clear() {worlds.clear();}
    public void unloadWorld(UUID world){worlds.remove(world);}
}
