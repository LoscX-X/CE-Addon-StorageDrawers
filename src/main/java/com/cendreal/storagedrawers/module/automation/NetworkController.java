package com.cendreal.storagedrawers.module.automation;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.module.storage.NodeController;
import com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import java.util.*;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Direction;

public final class NetworkController extends NodeController {
    private final NetworkContainer container=new NetworkContainer(this);
    public NetworkController(BlockEntity entity,DrawerBehavior behavior) {super(entity,behavior);}
    public NetworkContainer container() {return container;}
    public static final class NetworkContainer extends DrawerContainer {
        private NetworkIndex.View view;
        private static final int[] TRANSFER_CELLS={0,1};
        private long templateVersion=-1,outputVersion=-1;
        private Map<Key,List<Access>> routes=Map.of();
        private List<Access> conversions=List.of();
        private long inputVersion=-1;private Item previousInput;private Access previousDestination;
        private Access selectedOutput;
        private int nextOutput;
        NetworkContainer(NodeController node) {super(node);bind(new Access[0]);}
        @Override protected void refresh() {
            var index=StorageDrawersCE.instance().networks();
            if(view!=null && view.revision()==index.revision(node))return;
            view=index.view(node);
            templateVersion=-1;outputVersion=-1;inputVersion=-1;selectedOutput=null;
            List<Access> slots=new ArrayList<>();
            for(var drawer:view.drawers())for(int i=0;i<drawer.size();i++)slots.add(new Access(drawer,i));
            bind(slots.toArray(Access[]::new));
        }
        public NetworkIndex.View view() {refresh();return view;}
        @Override public int containerSize(){return 2;}
        @Override public int[] getSlotsForFace(Direction face){return TRANSFER_CELLS;}
        private void routeIndex(){refresh();var index=StorageDrawersCE.instance().networks();long version=index.templateRevision(node);if(templateVersion==version)return;Map<Key,List<Access>> grouped=new HashMap<>();List<Access> converted=new ArrayList<>();for(Access a:access)if(!a.drawer().template(a.slot()).isEmpty()){grouped.computeIfAbsent(a.drawer().template(a.slot()).id(),k->new ArrayList<>()).add(a);if(a.drawer().has("conversion_upgrade"))converted.add(a);}routes=grouped;conversions=converted;templateVersion=version;inputVersion=-1;}
        private Access input(Item item){routeIndex();long revision=StorageDrawersCE.instance().networks().contentsRevision(node);if(inputVersion==revision&&previousInput!=null&&previousInput.isSimilar(item))return previousDestination;Access result=null;for(Access a:routes.getOrDefault(item.id(),List.of()))if(!a.drawer().suspended()&&a.drawer().accepts(a.slot(),item)&&(a.drawer().room(a.slot())>0||a.drawer().has("void_upgrade"))){result=a;break;}if(result==null)for(Access a:conversions)if(!a.drawer().suspended()&&a.drawer().accepts(a.slot(),item)&&(a.drawer().room(a.slot())>0||a.drawer().has("void_upgrade"))){result=a;break;}inputVersion=revision;previousInput=item.copyWithCount(1);previousDestination=result;return result;}
        private Access output(){refresh();long version=StorageDrawersCE.instance().networks().contentsRevision(node);if(outputVersion==version)return selectedOutput;selectedOutput=null;outputVersion=version;for(int i=0;i<access.length;i++){int slot=(nextOutput+i)%access.length;Access a=access[slot];if(!a.drawer().suspended() && a.drawer().blockEntity().isValid() && a.drawer().count(a.slot())>0){selectedOutput=a;nextOutput=(slot+1)%access.length;break;}}return selectedOutput;}
        @Override public boolean isEmpty(){return output()==null;}
        @Override public Item getItem(int cell){Access a=cell==1?output():null;return a==null?Item.empty():a.drawer().template(a.slot()).copyWithCount(1);}
        @Override public boolean canPlaceItem(int cell,Item item){return cell==0 && !item.isEmpty() && input(item)!=null;}
        @Override public boolean canPlaceItemThroughFace(int cell,Item item,Direction face){return canPlaceItem(cell,item);}
        @Override public boolean canTakeItemThroughFace(int cell,Item item,Direction face){return cell==1 && output()!=null;}
        @Override public Item removeItem(int cell,int count){Access a=cell==1?output():null;if(a==null || count<=0)return Item.empty();Item result=a.drawer().take(a.slot(),1);outputVersion=-1;return result;}
        @Override public Item removeItemNoUpdate(int cell){return removeItem(cell,1);}
        @Override public void setItem(int cell,Item item){if(cell==0 && !item.isEmpty()){Access a=input(item);if(a!=null)a.drawer().put(a.slot(),item,Math.min(item.count(),1));}else if(cell==1 && item.isEmpty())removeItem(cell,1);}
    }
}
