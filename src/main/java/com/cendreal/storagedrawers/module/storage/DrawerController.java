package com.cendreal.storagedrawers.module.storage;

import com.cendreal.storagedrawers.StorageDrawersCE;
import com.cendreal.storagedrawers.compatibility.craftengine.DrawerBehavior;
import com.cendreal.storagedrawers.module.automation.DrawerContainer;
import com.cendreal.storagedrawers.module.automation.CollectionTicker;
import com.cendreal.storagedrawers.module.automation.RemoteBinding;
import com.cendreal.storagedrawers.render.DrawerDisplay;
import com.cendreal.storagedrawers.render.StatusDisplay;
import net.momirealms.craftengine.bukkit.block.behavior.DrawerBlockBehavior;
import net.momirealms.craftengine.bukkit.block.entity.DrawerBlockEntityController;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.libraries.nbt.*;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.Consumer;

public final class DrawerController extends NodeController {
    public static final String DATA_KEY="storagedrawers:contents";
    public static final int CONTENTS_VERSION=2;
    private final Item[] templates;
    private final int[] counts, factors;
    private long units;
    private boolean locked, quantity, concealed;
    private UUID owner;
    private boolean suspended;
    private int priority;
    private final boolean[] missing;
    private RemoteBinding remote;
    private final Item[] upgradeItems=new Item[Upgrades.SLOTS];
    private final CollectionTicker collector;
    private final Item[] previousTemplates;
    private final long[] previousAmounts;
    private int batchDepth;
    private boolean batchDirty;
    private final List<String> upgrades=new ArrayList<>(Upgrades.SLOTS);
    private final DrawerDisplay[] displays;
    private final StatusDisplay statusDisplay;
    private final DrawerContainer container;
    public UUID lastPlayer;
    public long lastClick;
    public int lastSlot=-1;
    public DrawerController(BlockEntity entity,DrawerBehavior behavior) {
        super(entity,behavior);
        quantity=StorageDrawersCE.instance().getConfig().getBoolean("display.front-quantity-default",true);
        for(int i=0;i<Upgrades.SLOTS;i++)upgrades.add("");
        int size=behavior.layout.drawers();
        missing=new boolean[size];Arrays.fill(upgradeItems,Item.empty());
        templates=new Item[size];Arrays.fill(templates,Item.empty());counts=new int[size];factors=new int[size];Arrays.fill(factors,1);
        previousTemplates=templates.clone();
        previousAmounts=new long[size];
        displays=new DrawerDisplay[size];
        for(int i=0;i<size;i++) {
            float x=behavior.layout.displayX(i), y=behavior.layout.displayY(i);
            float depth=behavior.layout.halfDepth()?-.025f:-.525f;
            // Reserve a separate strip below the largest flat item sprite. TextDisplay's
            // origin is below its glyphs; placing it only .15 lower overlaps the icon.
            float scale=size==1?.4f:.24f;
            float textY=y-(size==1?.30f:.18f);
            DrawerBlockBehavior displayBehavior=new DrawerBlockBehavior(behavior.block(),null,null,false,new Vector3f(x,y,depth),new Vector3f(x,textY,depth-.001f),new Vector3f(scale),new Vector3f(size==1?.3f:.24f),327680,true,true,behavior.facing,"storagedrawers:display_"+i,false);
            displays[i]=new DrawerDisplay(this,i,DrawerBlockEntityController.create(entity,displayBehavior));
        }
        container=new DrawerContainer(this);
        collector=new CollectionTicker(this);
        statusDisplay=new StatusDisplay(this);
    }
    public DrawerContainer container() {return container;}
    public int size() {return templates.length;}
    public Item template(int slot) {return templates[slot];}
    public int count(int slot) {if(templates[slot].isEmpty()||missing[slot])return 0;if(has("creative_vending_upgrade"))return Integer.MAX_VALUE;return behavior.layout.compacting()?(int)Math.min(Integer.MAX_VALUE,units/factors[slot]):counts[slot];}
    public boolean locked() {return locked;}
    public boolean quantity() {return quantity;}
    public boolean concealed() {return concealed;}
    public boolean claimed(){return owner!=null;}
    public UUID ownerId(){return owner;}
    /** Validated absolute write. Compacting cell identities are edited as a linked group. */
    public boolean replaceSlot(int slot,Item item,int amount){
        if(slot<0||slot>=size()||amount<0||missing[slot]||item.isEmpty()&&amount!=0||!blockEntity.isValid())return false;
        if(behavior.layout.compacting()){
            if(item.isEmpty())return replaceCompacting(item,0);
            if(templates[slot].isEmpty()||!templates[slot].isSimilar(item))return false;
            long value=(long)amount*factors[slot];if(value>maxUnits()||!StorageEvents.replace(this,slot,item,amount))return false;
            units=value;if(value==0&&!locked){Arrays.fill(templates,Item.empty());Arrays.fill(factors,1);}changed();return true;
        }
        Item next=item.isEmpty()?Item.empty():item.copyWithCount(1),old=templates[slot];
        templates[slot]=next;boolean fits=amount<=capacity(slot);templates[slot]=old;
        if(!fits||!StorageEvents.replace(this,slot,next,amount))return false;
        templates[slot]=amount==0&&!locked?Item.empty():next;counts[slot]=amount;changed();return true;
    }
    public boolean replaceCompacting(Item seed,int amount){
        if(!behavior.layout.compacting()||amount<0||seed.isEmpty()&&amount!=0||!blockEntity.isValid())return false;
        Item[] next=new Item[size()];Arrays.fill(next,Item.empty());int[] nextFactors=new int[size()];Arrays.fill(nextFactors,1);int seedSlot=0;
        if(!seed.isEmpty()){var chain=StorageDrawersCE.instance().compactingRules().chain(seed,size());if(chain==null)next[0]=seed.copyWithCount(1);else{System.arraycopy(chain.items(),0,next,0,size());System.arraycopy(chain.factors(),0,nextFactors,0,size());for(int i=0;i<size();i++)if(next[i].isSimilar(seed)){seedSlot=i;break;}}}
        long value=(long)amount*nextFactors[seedSlot];Item[] previous=templates.clone();int[] oldFactors=factors.clone();System.arraycopy(next,0,templates,0,size());System.arraycopy(nextFactors,0,factors,0,size());boolean fits=value<=maxUnits();System.arraycopy(previous,0,templates,0,size());System.arraycopy(oldFactors,0,factors,0,size());
        if(!fits||!StorageEvents.replace(this,seedSlot,seed,amount))return false;
        System.arraycopy(next,0,templates,0,size());System.arraycopy(nextFactors,0,factors,0,size());units=value;changed();return true;
    }
    public boolean setOwnerId(UUID value){if(Objects.equals(owner,value))return true;var before=StorageEvents.beforeState(this);owner=value;StorageDrawersCE.instance().networks().changed(this);changed();StorageEvents.state(this,before);return true;}
    public boolean setPriorityValue(int value){if(value< -2||value>2)return false;if(priority==value)return true;var before=StorageEvents.beforeState(this);priority=value;StorageDrawersCE.instance().networks().changed(this);changed();StorageEvents.state(this,before);return true;}
    public boolean setFlag(com.cendreal.storagedrawers.api.StorageDrawersApi.Setting setting,boolean value){var before=StorageEvents.beforeState(this);switch(setting){case LOCKED->{locked=value;if(!value){if(behavior.layout.compacting()&&units==0)Arrays.fill(templates,Item.empty());else if(!behavior.layout.compacting())for(int i=0;i<size();i++)if(counts[i]==0)templates[i]=Item.empty();}}case QUANTITY->quantity=value;case CONCEALED->concealed=value;case SUSPENDED->suspended=value;}StorageDrawersCE.instance().networks().changed(this);changed();for(var display:displays)display.refreshVisibility();StorageEvents.state(this,before);return true;}
    public boolean suspended() {return suspended;}
    public int priority() {return priority;}
    public boolean missing(int slot) {return missing[slot];}
    public RemoteBinding remote() {return remote;}
    public boolean remoteGroup() {return upgrades.stream().anyMatch(u->u.startsWith("remote_group"));}
    public Item upgradeItem(int slot) {return upgradeItems[slot].copy();}
    public int[] magnetRange() {int h=0,u=0;for(String id:upgrades) {if(id.equals("magnet_upgrade")){h++;u++;}else if(id.equals("magnet_upgrade_2")){h+=4;u+=2;}else if(id.equals("magnet_upgrade_3")){h+=8;u+=3;}}return new int[]{Math.min(24,h),Math.min(8,u),0};}
    public boolean canAccess(Player player) {return owner==null || player!=null && owner.equals(player.uuid());}
    public boolean has(String upgrade) {return upgrades.contains(upgrade);}
    public List<String> upgrades() {return List.copyOf(upgrades);}
    public int capacity(int slot) {
        if(behavior.layout.compacting())return (int)(maxUnits()/factors[slot]);
        if(has("creative_storage_upgrade")||has("creative_vending_upgrade"))return Integer.MAX_VALUE;
        long stacks=has("one_stack_upgrade")?1L:behavior.layout.baseStacks()*(long)Upgrades.multiplier(upgrades);
        int reference=behavior.layout.compacting()?0:slot;
        int maxStack=templates[reference].isEmpty()?64:templates[reference].maxStackSize();
        long capacity=stacks*maxStack;
        return (int)Math.min(Integer.MAX_VALUE,capacity);
    }
    public long room(int slot) {if(has("creative_vending_upgrade"))return Integer.MAX_VALUE;return behavior.layout.compacting()?Math.max(0,(maxUnits()-units)/factors[slot]):Math.max(0,(long)capacity(slot)-count(slot));}
    private long maxUnits() {if(has("creative_storage_upgrade")||has("creative_vending_upgrade"))return Integer.MAX_VALUE;long stacks=has("one_stack_upgrade")?1L:behavior.layout.baseStacks()*(long)Upgrades.multiplier(upgrades);int stackSize=templates[0].isEmpty()?64:templates[0].maxStackSize();return Math.min(Integer.MAX_VALUE,stacks*stackSize*factors[0]);}
    public boolean accepts(int slot,Item input) {
        return accepts(slot,input,false);
    }
    public boolean accepts(int slot,Item input,boolean manual) {
        if(input==null || input.isEmpty() || !blockEntity.isValid() || missing[slot]) return false;
        if(!StorageDrawersCE.instance().getConfig().getBoolean("filled-drawers.allow-in-drawers",false)&&com.cendreal.storagedrawers.module.portability.PortablePolicy.drawerPayload(input))return false;
        if(templates[slot].isEmpty()) return manual||!locked;
        return templates[slot].isSimilar(input) || has("conversion_upgrade") && StorageDrawersCE.instance().equivalence().equivalent(templates[slot],input);
    }
    public int put(int slot,Item input,int requested) {
        return put(slot,input,requested,null);
    }
    public int put(int slot,Item input,int requested,Player actor) {
        if(requested<=0 || input.isEmpty()) return 0;
        if(!StorageEvents.transfer(this,slot,input,requested,true,actor))return 0;
        Item previous=templates[slot];
        if(behavior.layout.compacting() && templates[0].isEmpty() && (!locked||actor!=null)) {
            var chain=StorageDrawersCE.instance().compactingRules().chain(input,size());
            if(chain==null) templates[0]=input.copyWithCount(1);
            else {System.arraycopy(chain.items(),0,templates,0,size());System.arraycopy(chain.factors(),0,factors,0,size());}
            slot=find(input); if(slot<0) return 0;
        }
        if(!accepts(slot,input,actor!=null)) return 0;
        if(templates[slot].isEmpty()) templates[slot]=input.copyWithCount(1);
        if(has("creative_vending_upgrade")){if(previous!=templates[slot])changed(actor);return requested;}
        long room=room(slot);
        int inserted=(int)Math.min(requested,Math.max(0,room));
        if(behavior.layout.compacting()) units+=(long)inserted*factors[slot];else counts[slot]+=inserted;
        if(inserted>0){BalanceService.balance(this,templates[slot],actor);changed(actor);}
        return has("void_upgrade")?requested:inserted;
    }
    public int find(Item input) {for(int i=0;i<size();i++) if(!templates[i].isEmpty() && templates[i].isSimilar(input)) return i;return -1;}
    public Item take(int slot,int requested) {
        return take(slot,requested,null);
    }
    public Item take(int slot,int requested,Player actor) {
        int amount=Math.min(Math.min(requested,count(slot)),templates[slot].isEmpty()?0:templates[slot].maxStackSize());
        if(amount<=0) return Item.empty();
        if(!StorageEvents.transfer(this,slot,templates[slot],amount,false,actor))return Item.empty();
        Item result=templates[slot].copyWithCount(amount);
        if(has("creative_vending_upgrade"))return result;
        if(behavior.layout.compacting()) {units-=(long)amount*factors[slot];if(units==0 && !locked) {Arrays.fill(templates,Item.empty());Arrays.fill(factors,1);}}
        else {counts[slot]-=amount;if(counts[slot]==0 && !locked) templates[slot]=Item.empty();}
        BalanceService.balance(this,result,actor);changed(actor);return result;
    }
    public boolean install(String id) {
        int slot=upgrades.indexOf("");return slot>=0 && replaceUpgrade(slot,id);
    }
    public boolean install(Item item) {int slot=upgrades.indexOf("");return slot>=0 && replaceUpgrade(slot,item);}
    public boolean rebind(Item bound){
        RemoteBinding binding=RemoteBinding.read(bound);
        if(binding==null||!binding.world().equals(blockEntity.world.uuid()))return false;
        for(int i=0;i<upgradeItems.length;i++)if(upgrades.get(i).startsWith("remote_")){
            Item replacement=upgradeItems[i].copy();replacement.setSparrowTag(binding.save(),RemoteBinding.KEY);
            return replaceUpgrade(i,replacement);
        }
        return false;
    }
    public boolean replaceUpgrade(int slot,String id) {
        return replaceUpgrade(slot,id.isEmpty()?Item.empty():Item.byId(net.momirealms.craftengine.core.util.Key.of("storagedrawers:"+id)));
    }
    public boolean replaceUpgrade(int slot,Item item) {
        String id=item.isEmpty()?"":item.customId().map(k->k.namespace().equals("storagedrawers")?k.value():"").orElse("");
        if(slot<0 || slot>=Upgrades.SLOTS || !id.isEmpty() && !Upgrades.isUpgrade(id)) return false;
        if((!id.isEmpty()||Upgrades.MULTIPLIERS.containsKey(upgrades.get(slot))) && StorageDrawersCE.instance().getConfig().getBoolean("detached-drawers.force-capacity-check",false))for(boolean absent:missing)if(absent)return false;
        if(!item.isEmpty() && id.isEmpty())return false;
        RemoteBinding binding=id.contains("remote_")?RemoteBinding.read(item):null;
        if(id.contains("remote_") && (binding==null || !binding.world().equals(blockEntity.world.uuid())))return false;
        List<String> proposed=new ArrayList<>(upgrades);proposed.set(slot,id);
        if(!validUpgrades(proposed))return false;
        var beforeState=StorageEvents.beforeState(this);Item previousUpgrade=upgradeItems[slot];
        String old=upgrades.set(slot,id);
        if(overCapacity()) {upgrades.set(slot,old);return false;}
        upgradeItems[slot]=item.copyWithCount(1);if(item.isEmpty())upgradeItems[slot]=Item.empty();
        remote=null;for(Item u:upgradeItems){var b=RemoteBinding.read(u);if(b!=null){remote=b;break;}}
        StorageDrawersCE.instance().networks().changed(this);
        changed();StorageEvents.slot(this,com.cendreal.storagedrawers.api.event.StorageSlotChangeEvent.Kind.UPGRADE,slot,previousUpgrade,previousUpgrade.isEmpty()?0:1,upgradeItems[slot],upgradeItems[slot].isEmpty()?0:1);StorageEvents.state(this,beforeState);return true;
    }
    private boolean validUpgrades(List<String> proposed) {
        return Upgrades.valid(proposed);
    }
    public String uninstall(int slot) {
        if(slot<0 || slot>=upgrades.size()) return null;
        String removed=upgrades.get(slot);return removed.isEmpty() || !replaceUpgrade(slot,"")?null:removed;
    }
    public Item uninstallItem(int slot){if(slot<0||slot>=Upgrades.SLOTS)return Item.empty();Item result=upgradeItems[slot].copy();return result.isEmpty()||!replaceUpgrade(slot,Item.empty())?Item.empty():result;}
    private boolean overCapacity() {if(behavior.layout.compacting()) return units>maxUnits();for(int i=0;i<size();i++) if(counts[i]>capacity(i)) return true;return false;}
    public boolean useKey(String id,Player player) {
        if(!canAccess(player) && !id.equals("personal_key_unlock")) return false;
        var beforeState=StorageEvents.beforeState(this);
        switch(id) {
            case "drawer_key" -> {locked=!locked;if(!locked) {if(behavior.layout.compacting() && units==0) Arrays.fill(templates,Item.empty());else if(!behavior.layout.compacting()) for(int i=0;i<size();i++) if(counts[i]==0) templates[i]=Item.empty();}}
            case "quantify_key" -> quantity=!quantity;
            case "shroud_key" -> concealed=!concealed;
            case "personal_key" -> {if(player==null)return false;owner=owner==null?player.uuid():null;}
            case "personal_key_unlock" -> owner=null;
            case "suspend_key" -> suspended=!suspended;
            case "priority_key" -> priority=switch(priority){case 0->1;case 1->2;case 2->-1;case -1->-2;default->0;};
            case "priority_key_p1" -> priority=priority==1?0:1;
            case "priority_key_p2" -> priority=priority==2?0:2;
            case "priority_key_n1" -> priority=priority==-1?0:-1;
            case "priority_key_n2" -> priority=priority==-2?0:-2;
            default -> {return false;}
        }
        StorageDrawersCE.instance().networks().changed(this);changed();for(var d:displays) d.refreshVisibility();StorageEvents.state(this,beforeState);return true;
    }
    public int signal() {
        if(!has("redstone_upgrade") && !has("min_redstone_upgrade") && !has("max_redstone_upgrade")) return 0;
        double fill=has("min_redstone_upgrade")?1:0;
        for(int i=0;i<size();i++) {double f=templates[i].isEmpty()?0:(double)count(i)/(behavior.layout.compacting()?Math.max(1,maxUnits()/factors[i]):capacity(i));
            if(has("min_redstone_upgrade")) fill=Math.min(fill,f);else if(has("max_redstone_upgrade")) fill=Math.max(fill,f);else fill+=f/size();}
        return fill<=0?0:Math.min(15,1+(int)Math.floor(fill*14));
    }
    private int previousSignal;
    public void changed(Player actor){if(actor==null)changed();else StorageContext.run((org.bukkit.entity.Player)actor.platformPlayer(),StorageContext.actor(actor).cause(),()->{changed();});}
    public void changed() {
        if(batchDepth>0){batchDirty=true;return;}
        java.util.List<com.cendreal.storagedrawers.api.event.StorageSlotChangeEvent> notifications=StorageEvents.slotsObserved()&&blockEntity.world!=null?new ArrayList<>():null;
        boolean identity=false;for(int i=0;i<size();i++){
            if(previousTemplates[i]!=templates[i])identity=true;
            long amount=behavior.layout.compacting()?(i==size()-1?units:0):counts[i];
            if(notifications!=null&&(previousAmounts[i]!=amount||previousTemplates[i]!=templates[i]&&!previousTemplates[i].isSimilar(templates[i])))notifications.add(StorageEvents.change(this,com.cendreal.storagedrawers.api.event.StorageSlotChangeEvent.Kind.CONTENTS,i,previousTemplates[i],previousAmounts[i],templates[i],amount));
            previousTemplates[i]=templates[i];previousAmounts[i]=amount;
        }
        StorageDrawersCE.instance().networks().contentsChanged(this,identity);
        collector.refresh();
        for(DrawerDisplay d:displays) d.sync(true);
        statusDisplay.sync(true);
        if(blockEntity.world==null) return;
        blockEntity.world.blockEntityChanged(blockEntity.pos);
        StorageDrawersCE.instance().menus().changed(this);
        int next=signal();if(next!=previousSignal) {previousSignal=next;
            var world=(net.minecraft.server.level.ServerLevel)blockEntity.world.world().minecraftWorld();
            var pos=new net.minecraft.core.BlockPos(blockEntity.pos.x,blockEntity.pos.y,blockEntity.pos.z);
            world.updateNeighbourForOutputSignal(pos,((net.minecraft.world.level.block.state.BlockState)blockEntity.blockState.customBlockState().minecraftState()).getBlock());
        }
        if(notifications!=null)for(var event:notifications)org.bukkit.Bukkit.getPluginManager().callEvent(event);
    }
    public void beginBatch(){batchDepth++;}
    public void endBatch(){if(--batchDepth==0 && batchDirty){batchDirty=false;changed();}}
    void balanceCount(int slot,int value){counts[slot]=value;if(value==0&&!locked)templates[slot]=Item.empty();}
    @Override public boolean hasElement() {return true;}
    @Override public void gatherElements(Consumer<BlockEntityElement> consumer) {super.gatherElements(consumer);consumer.accept(statusDisplay);for(var d:displays) consumer.accept(d);}
    @Override public void preBlockStateChange(ImmutableBlockState state) {super.preBlockStateChange(state);statusDisplay.stateChanged(state);for(var d:displays) d.stateChanged(state);}
    @Override public <C extends net.momirealms.craftengine.core.block.entity.BlockEntityController> net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker<C> createBlockEntityTicker(net.momirealms.craftengine.core.world.CEWorld world,ImmutableBlockState state){return createTickerHelper(collector.ticker());}
    @Override public void onRemove() {super.onRemove();}
    public CompoundTag contents() {
        CompoundTag data=new CompoundTag();data.putInt("version",CONTENTS_VERSION);data.putString("block",behavior.block().id().toString());data.putInt("data_version",VersionHelper.WORLD_VERSION);
        data.putBoolean("locked",locked);data.putBoolean("quantity",quantity);data.putBoolean("concealed",concealed);if(owner!=null)data.putString("owner",owner.toString());
        data.putBoolean("suspended",suspended);data.putInt("priority",priority);
        data.putLong("units",units);
        for(int i=0;i<size();i++) {CompoundTag slot=new CompoundTag();slot.putInt("count",counts[i]);slot.putInt("factor",factors[i]);slot.putBoolean("missing",missing[i]);if(!templates[i].isEmpty()) slot.put("item",ItemStackUtils.saveMinecraftItemStackAsTag(templates[i].minecraftItem()));data.put("slot"+i,slot);}
        for(int i=0;i<upgrades.size();i++) {data.putString("upgrade"+i,upgrades.get(i));if(!upgradeItems[i].isEmpty())data.put("upgradeItem"+i,ItemStackUtils.saveMinecraftItemStackAsTag(upgradeItems[i].minecraftItem()));}return data;
    }
    public boolean hasContents() {return locked || owner!=null || quantity!=StorageDrawersCE.instance().getConfig().getBoolean("display.front-quantity-default",true) || concealed || suspended || priority!=0 || upgrades.stream().anyMatch(u->!u.isEmpty()) || !Arrays.stream(templates).allMatch(Item::isEmpty) || java.util.stream.IntStream.range(0,size()).anyMatch(i->missing[i]);}
    @Override public void saveCustomData(CompoundTag root) {super.saveCustomData(root);root.put(DATA_KEY,contents());}
    @Override public void loadCustomData(CompoundTag root) {super.loadCustomData(root);CompoundTag data=root.getCompound(DATA_KEY);if(data!=null) loadContents(data);}
    @Override public void loadCustomDataFromItem(Item item) {super.loadCustomDataFromItem(item);if(item.getSparrowTag(DATA_KEY) instanceof CompoundTag data) loadContents(data);}
    private void loadContents(CompoundTag data) {
        int format=data.getInt("version",0);
        if((format!=1&&format!=CONTENTS_VERSION) || !data.getString("block","").equals(behavior.block().id().toString())) throw new IllegalArgumentException("Incompatible StorageDrawers contents");
        int version=data.getInt("data_version",VersionHelper.WORLD_VERSION);
        locked=data.getBoolean("locked",false);quantity=data.getBoolean("quantity",false);concealed=data.getBoolean("concealed",false);
        // Format 1 saved the old default (hidden) for every drawer. Adopt the new front
        // count default once; format 2 keeps later explicit quantify-key choices intact.
        if(format==1&&StorageDrawersCE.instance().getConfig().getBoolean("display.front-quantity-default",true))quantity=true;
        suspended=data.getBoolean("suspended",false);priority=data.getInt("priority",0);
        String user=data.getString("owner","");owner=user.isEmpty()?null:UUID.fromString(user);
        units=data.getLong("units",0);if(units<0) throw new IllegalArgumentException("Negative compacting contents");
        upgrades.clear();remote=null;for(int i=0;i<Upgrades.SLOTS;i++) {String u=data.getString("upgrade"+i,"");if(!u.isEmpty() && !Upgrades.isUpgrade(u)) throw new IllegalArgumentException("Unknown upgrade "+u);upgrades.add(u);Tag saved=data.get("upgradeItem"+i);upgradeItems[i]=saved!=null?ItemStackUtils.wrap(ItemStackUtils.parseCachedMinecraftItem(saved,version)).copyWithCount(1):u.isEmpty()?Item.empty():Item.byId(net.momirealms.craftengine.core.util.Key.of("storagedrawers:"+u));var binding=RemoteBinding.read(upgradeItems[i]);if(binding!=null)remote=binding;}
        for(int i=0;i<size();i++) {
            CompoundTag slot=data.getCompound("slot"+i);if(slot==null) throw new IllegalArgumentException("Missing drawer slot "+i);
            counts[i]=slot.getInt("count",0);factors[i]=slot.getInt("factor",1);
            missing[i]=slot.getBoolean("missing",false);
            if(counts[i]<0 || factors[i]<1 || factors[i]>81) throw new IllegalArgumentException("Invalid drawer amounts");
            Tag tag=slot.get("item");templates[i]=tag==null?Item.empty():ItemStackUtils.wrap(ItemStackUtils.parseCachedMinecraftItem(tag,version)).copyWithCount(1);
        }
        for(var d:displays)d.sync(false);statusDisplay.sync(false);collector.refresh();previousSignal=signal();
        for(int i=0;i<size();i++){previousTemplates[i]=templates[i];previousAmounts[i]=behavior.layout.compacting()?(i==size()-1?units:0):counts[i];}
    }
    public Item detach(int slot){
        if(behavior.layout.compacting() || missing[slot]||!StorageDrawersCE.instance().getConfig().getBoolean("detached-drawers.enabled",true))return Item.empty();
        boolean full=!templates[slot].isEmpty();Item result=Item.byId(net.momirealms.craftengine.core.util.Key.of("storagedrawers:"+(full?"detached_drawer_full":"detached_drawer")));
        var data=new CompoundTag();data.putInt("count",counts[slot]);data.putInt("capacity",capacity(slot));data.putInt("stacks",has("one_stack_upgrade")?1:behavior.layout.baseStacks()*Upgrades.multiplier(upgrades));data.putInt("stack_size",full?templates[slot].maxStackSize():64);data.putBoolean("heavy",!has("portability_upgrade"));data.putInt("data_version",VersionHelper.WORLD_VERSION);if(full)data.put("item",ItemStackUtils.saveMinecraftItemStackAsTag(templates[slot].minecraftItem()));result.setSparrowTag(data,"storagedrawers:detached");if(full)result.maxStackSize(1);
        templates[slot]=Item.empty();counts[slot]=0;missing[slot]=true;changed();return result;
    }
    public boolean attach(int slot,Item detached){return attach(slot,detached,null);}
    public boolean attach(int slot,Item detached,Player actor){
        if(behavior.layout.compacting() || !missing[slot])return false;
        Item prototype=Item.empty();int amount=0;
        if(detached.getSparrowTag("storagedrawers:detached") instanceof CompoundTag data){amount=data.getInt("count",0);if(data.get("item")!=null)prototype=ItemStackUtils.wrap(ItemStackUtils.parseCachedMinecraftItem(data.get("item"),data.getInt("data_version",VersionHelper.WORLD_VERSION))).copyWithCount(1);if(StorageDrawersCE.instance().getConfig().getBoolean("detached-drawers.force-capacity-check",false)&&data.getInt("stacks",32)<behavior.layout.baseStacks()*Upgrades.multiplier(upgrades))return false;}
        Item old=templates[slot];templates[slot]=prototype;
        if(amount<0 || amount>capacity(slot)){templates[slot]=old;return false;}
        counts[slot]=amount;missing[slot]=false;if(amount>0)BalanceService.balance(this,prototype,actor);changed();return true;
    }
}
