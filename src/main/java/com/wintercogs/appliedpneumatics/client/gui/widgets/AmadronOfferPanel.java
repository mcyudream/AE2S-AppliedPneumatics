package com.wintercogs.appliedpneumatics.client.gui.widgets;

import ae2.client.gui.style.Blitter;
import ae2.client.gui.style.FluidBlitter;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.common.menu.AmadronOfferSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Upstream's 79x22 card, UVs, scaled counts, tooltips, and mouse-button cart rules. */
public final class AmadronOfferPanel extends GuiButton {
    private static final ResourceLocation BG=APContent.renderId("textures/gui/amadron_wireless_terminal.png");
    private static final int PANEL_W=79,PANEL_H=22,IN_ICON_X=3,OUT_ICON_X=60,ICON_Y=3;
    private static final float TEXT_SCALE=.666f,TEXT_Z=300;
    private final AmadronOfferSnapshot offer;
    private int wantedStock;
    private AmadronOfferPanel(int x,int y,AmadronOfferSnapshot offer){super(0,x,y,PANEL_W,PANEL_H,"");this.offer=offer;}
    public static AmadronOfferPanel fromOffer(int x,int y,AmadronOfferSnapshot offer){return new AmadronOfferPanel(x,y,offer);}
    public AmadronOfferSnapshot getOffer(){return offer;}
    public AmadronOfferSnapshot getOfferType(){return offer;}
    public int getOfferId(){return offer.index();}
    public int getWantedStock(){return wantedStock;}
    public int getX(){return x;}
    public int getY(){return y;}
    public void setX(int value){x=value;}
    public void setY(int value){y=value;}
    public boolean isMouseOver(double mx,double my){return enabled&&visible&&mx>=x&&my>=y&&mx<x+width&&my<y+height;}
    public boolean isMouseOverForTooltip(double mx,double my){return enabled&&visible&&mx>=x&&my>=y&&mx<x+width+1&&my<y+height+1;}
    public void onClicked(double mx,double my,int button,boolean shift){
        if(button==0||button==1){
            if(shift){wantedStock=(int)Math.max(0,wantedStock*(button==0?.5:2));if(offer.stock()>-1)wantedStock=Math.min(offer.stock(),wantedStock);wantedStock=Math.min(256,wantedStock);}
            else{wantedStock=Math.max(0,wantedStock+(button==0?-1:1));if(offer.stock()>-1)wantedStock=Math.min(offer.stock(),wantedStock);wantedStock=Math.min(64,wantedStock);}
        }else if(button==2)wantedStock=0;
    }
    @Override public void drawButton(Minecraft mc,int mouseX,int mouseY,float partialTicks){
        if(!visible)return;hovered=isMouseOver(mouseX,mouseY);
        GlStateManager.color(1,1,1,1);GlStateManager.enableBlend();GlStateManager.enableDepth();
        Blitter.texture(BG,512,512).src(1,hovered?294:271,PANEL_W,PANEL_H).dest(x,y).blit();
        if(!offer.valid()){mc.fontRenderer.drawString("…",x+4,y+7,0x777777);return;}
        drawResource(mc,offer.offer().getInput(),x+IN_ICON_X,y+ICON_Y);
        drawResource(mc,offer.offer().getOutput(),x+OUT_ICON_X,y+ICON_Y);
        if(offer.isStatic()&&!offer.isPlayer())Blitter.texture(BG,512,512).src(95,282,8,11).dest(x+35,y+9).zOffset(300).blit();
        else if(offer.isPlayer())Blitter.texture(BG,512,512).src(107,283,8,8).dest(x+35,y+10).zOffset(300).blit();
        GlStateManager.pushMatrix();GlStateManager.translate(x,y,TEXT_Z);GlStateManager.scale(.5,.5,1);
        String wanted=offer.stock()>-1?wantedStock+" / "+offer.stock():wantedStock>0?Integer.toString(wantedStock):"";
        mc.fontRenderer.drawStringWithShadow(wanted,77-mc.fontRenderer.getStringWidth(wanted)/2f,5,0xffffff);
        GlStateManager.popMatrix();GlStateManager.color(1,1,1,1);
    }
    private static void drawResource(Minecraft mc,Object resource,int x,int y){
        String count;
        if(resource instanceof ItemStack stack){
            RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(stack,x,y);RenderHelper.disableStandardItemLighting();count=Integer.toString(stack.getCount());
        }else if(resource instanceof FluidStack fluid){
            GlStateManager.disableLighting();FluidBlitter.create(fluid).dest(x,y,16,16).blit();count=String.format(Locale.ROOT,"%.1fB",fluid.amount/1000.0);
        }else return;
        GlStateManager.pushMatrix();GlStateManager.translate(0,0,TEXT_Z);GlStateManager.scale(TEXT_SCALE,TEXT_SCALE,TEXT_SCALE);
        int tx=(int)((x-1+16f+2-mc.fontRenderer.getStringWidth(count)*TEXT_SCALE)/TEXT_SCALE);
        int ty=(int)((y-1+16f-5f*TEXT_SCALE)/TEXT_SCALE);
        mc.fontRenderer.drawStringWithShadow(count,tx,ty,0xffffff);GlStateManager.popMatrix();
    }
    private static String name(Object resource){return resource instanceof ItemStack item?item.getDisplayName():((FluidStack)resource).getLocalizedName();}
    private static ITextComponent line(String key,TextFormatting color,String value){return new TextComponentTranslation(key).setStyle(new Style().setColor(color)).appendSibling(new TextComponentString(value).setStyle(new Style().setColor(TextFormatting.WHITE)));}
    public List<ITextComponent> getTooltipMessage(){
        var lines=new ArrayList<ITextComponent>();
        lines.add(line("tooltip.ae2s-appliedpneumatics.amadron_trade.supplier",TextFormatting.YELLOW,offer.supplier()));
        lines.add(line("tooltip.ae2s-appliedpneumatics.amadron_trade.output",TextFormatting.YELLOW,name(offer.offer().getOutput())));
        lines.add(line("tooltip.ae2s-appliedpneumatics.amadron_trade.input",TextFormatting.YELLOW,name(offer.offer().getInput())));
        lines.add(line("tooltip.ae2s-appliedpneumatics.amadron_trade.in_basket",TextFormatting.AQUA,Integer.toString(Math.max(0,wantedStock))));
        if(Minecraft.getMinecraft().gameSettings.advancedItemTooltips)lines.add(new TextComponentString("Offer: "+offer.index()).setStyle(new Style().setColor(TextFormatting.DARK_GRAY)));
        return lines;
    }
}
