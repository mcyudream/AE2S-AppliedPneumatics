package com.wintercogs.appliedpneumatics.client.gui.widgets;

import ae2.client.gui.style.Blitter;
import ae2.client.gui.widgets.AE2Button;
import ae2.client.gui.widgets.ITooltip;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import java.awt.Rectangle;
import java.util.List;

/** Upstream sprites, including the normal/highlighted nine-slice metadata. */
public final class AE2TinyButton extends AE2Button implements ITooltip {
    private ITextComponent tooltip;
    public AE2TinyButton(ITextComponent message,Runnable action){super(message,action);}
    public void setTooltip(ITextComponent text){tooltip=text;}
    @Override public List<ITextComponent> getTooltipMessage(){return tooltip==null?List.of():java.util.Arrays.stream(tooltip.getFormattedText().replace("\\n","\n").split("\n")).map(net.minecraft.util.text.TextComponentString::new).map(ITextComponent.class::cast).toList();}
    @Override public Rectangle getTooltipArea(){return new Rectangle(x,y,width,height);}
    @Override public boolean isTooltipAreaVisible(){return visible;}
    @Override public void drawButton(Minecraft mc,int mouseX,int mouseY,float partialTicks){
        if(!visible)return;
        hovered=mouseX>=x&&mouseY>=y&&mouseX<x+width&&mouseY<y+height;
        GlStateManager.color(1,1,1,1);GlStateManager.enableBlend();GlStateManager.enableDepth();
        if(!enabled)renderButtonBackground(x,y,width,height,false,false);
        else{
            var texture=Blitter.texture(APContent.renderId("textures/gui/sprites/ae_tiny_button"+(hovered?"_highlighted":"")+".png"),89,18);
            int border=hovered?3:4;
            int[] sourceX={0,border,89-border},sourceY={0,border,18-border},sourceW={border,89-2*border,border},sourceH={border,18-2*border,border};
            int[] destX={x,x+border,x+width-border},destY={y,y+border,y+height-border},destW={border,width-2*border,border},destH={border,height-2*border,border};
            for(int row=0;row<3;row++)for(int col=0;col<3;col++)texture.copy().src(sourceX[col],sourceY[row],sourceW[col],sourceH[row]).dest(destX[col],destY[row],destW[col],destH[row]).blit();
        }
        renderButtonText(mc.fontRenderer,0xff000000|(!enabled?0x413f54:hovered?0x517497:0xf2f2f2),!enabled?-1:hovered?0:1);
    }
}
