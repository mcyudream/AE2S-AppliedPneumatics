package com.wintercogs.appliedpneumatics.client.gui;

import ae2.client.gui.AEBaseGui;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.*;
import ae2.container.SlotSemantics;
import com.wintercogs.appliedpneumatics.common.menu.*;

/** Uses AE2S's backported upstream style, upgrade and toolbox widgets. */
public abstract class APUpgradeableScreen extends AEBaseGui<ContainerAP> {
    protected final ContainerAP menu;
    protected APUpgradeableScreen(ContainerAP menu,String stylePath){
        super(menu,menu.player.inventory,GuiStyleManager.loadStyleDoc(stylePath));
        this.menu=menu;
        widgets.add("upgrades",UpgradesPanel.create(widgets,menu.getSlots(SlotSemantics.UPGRADE),
            ()->ae2.api.upgrades.Upgrades.getTooltipLinesForInventory(menu.getUpgrades())));
        if(menu.toolbox.isPresent())widgets.add("toolbox",new ToolboxPanel(getStyle(),menu.toolbox.getName()));
    }
    protected final void send(int operation,int index,int count,String value){
        APNetwork.CHANNEL.sendToServer(new APNetwork.Action(menu.windowId,operation,index,count,value));
    }
    protected final void changeTarget(double delta){send(6,0,0,Double.toString(delta));}
}
