package com.wintercogs.appliedpneumatics.common.menu;

import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MachineTile;
import com.wintercogs.appliedpneumatics.common.items.AmadronWirelessTerminalItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.*;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;

public final class APNetwork {
    public static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("apneumatics");
    public static void init(){
        CHANNEL.registerMessage(ActionHandler.class,Action.class,0,Side.SERVER);
        CHANNEL.registerMessage(StateHandler.class,State.class,1,Side.CLIENT);
        CHANNEL.registerMessage(BasketHandler.class,Basket.class,2,Side.SERVER);
        NetworkRegistry.INSTANCE.registerGuiHandler(AppliedPneumatics.INSTANCE,new IGuiHandler(){
            private ContainerAP create(int id,EntityPlayer player,World world,int x,int y,int z){
                if(id==1){
                    var hand=x==0?EnumHand.MAIN_HAND:EnumHand.OFF_HAND;
                    var stack=player.getHeldItem(hand);
                    var station=world.isRemote?null:AmadronWirelessTerminalItem.station(stack);
                    if(!world.isRemote&&(station==null||!(stack.getItem() instanceof AmadronWirelessTerminalItem)))return null;
                    return new ContainerAP(player,station,hand);
                }
                var tile=world.getTileEntity(new BlockPos(x,y,z));
                return tile instanceof MachineTile machine?new ContainerAP(player,machine,null):null;
            }
            @Override public Object getServerGuiElement(int id,EntityPlayer p,World w,int x,int y,int z){return create(id,p,w,x,y,z);}
            @Override public Object getClientGuiElement(int id,EntityPlayer p,World w,int x,int y,int z){return AppliedPneumatics.proxy.createScreen(create(id,p,w,x,y,z));}
        });
    }
    public static final class Action implements IMessage {
        public int window,operation,index,count;public String text="";
        public Action(){}
        public Action(int window,int op,int index,int count,String text){this.window=window;this.operation=op;this.index=index;this.count=count;this.text=text;}
        @Override public void toBytes(ByteBuf b){b.writeInt(window);b.writeInt(operation);b.writeInt(index);b.writeInt(count);ByteBufUtils.writeUTF8String(b,text);}
        @Override public void fromBytes(ByteBuf b){window=b.readInt();operation=b.readInt();index=b.readInt();count=b.readInt();text=ByteBufUtils.readUTF8String(b);if(text.length()>128)throw new IllegalArgumentException("Oversized action text");}
    }
    public static final class ActionHandler implements IMessageHandler<Action,IMessage> {
        @Override public IMessage onMessage(Action m,MessageContext c){var p=c.getServerHandler().player;p.getServerWorld().addScheduledTask(()->{
            if(p.openContainer instanceof ContainerAP menu&&menu.windowId==m.window&&menu.canInteractWith(p))menu.action(m);
        });return null;}
    }
    /** A bounded cart references the fixed, server-issued offer snapshot. */
    public static final class Basket implements IMessage {
        public int window;public long revision;public int[] indices=new int[0],counts=new int[0];
        public Basket(){}
        public Basket(int window,long revision,java.util.Map<Integer,Integer> entries){
            this.window=window;this.revision=revision;indices=new int[entries.size()];counts=new int[entries.size()];
            int i=0;for(var entry:entries.entrySet()){indices[i]=entry.getKey();counts[i++]=entry.getValue();}
        }
        @Override public void toBytes(ByteBuf b){b.writeInt(window);b.writeLong(revision);b.writeInt(indices.length);for(int i=0;i<indices.length;i++){b.writeInt(indices[i]);b.writeInt(counts[i]);}}
        @Override public void fromBytes(ByteBuf b){
            window=b.readInt();revision=b.readLong();int size=b.readInt();
            if(size<0||size>512||b.readableBytes()!=size*8)throw new IllegalArgumentException("Invalid Amadron cart");
            indices=new int[size];counts=new int[size];for(int i=0;i<size;i++){indices[i]=b.readInt();counts[i]=b.readInt();}
        }
    }
    public static final class BasketHandler implements IMessageHandler<Basket,IMessage> {
        @Override public IMessage onMessage(Basket m,MessageContext c){var p=c.getServerHandler().player;p.getServerWorld().addScheduledTask(()->{
            if(p.openContainer instanceof ContainerAP menu&&menu.windowId==m.window&&menu.canInteractWith(p))menu.submit(m);
        });return null;}
    }
    public static final class State implements IMessage {
        public int window;public NBTTagCompound tag;
        public State(){}
        public State(int window,NBTTagCompound tag){this.window=window;this.tag=tag;}
        @Override public void toBytes(ByteBuf b){b.writeInt(window);ByteBufUtils.writeTag(b,tag);}
        @Override public void fromBytes(ByteBuf b){window=b.readInt();tag=ByteBufUtils.readTag(b);}
    }
    public static final class StateHandler implements IMessageHandler<State,IMessage> {
        @Override public IMessage onMessage(State m,MessageContext c){AppliedPneumatics.proxy.handleState(m.window,m.tag);return null;}
    }
}
