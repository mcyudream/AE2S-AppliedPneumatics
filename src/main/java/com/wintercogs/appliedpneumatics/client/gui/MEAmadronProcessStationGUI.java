package com.wintercogs.appliedpneumatics.client.gui;

import com.wintercogs.appliedpneumatics.common.menu.ContainerAP;
import com.wintercogs.appliedpneumatics.client.gui.widgets.AE2TinyButton;
import ae2.container.SlotSemantics;
import net.minecraft.util.text.TextComponentTranslation;

/** Upstream common/extended station layouts, with AE2S's native priority submenu. */
public final class MEAmadronProcessStationGUI extends APUpgradeableScreen {
    public MEAmadronProcessStationGUI(ContainerAP menu){
        super(menu,menu.getSlots(SlotSemantics.ENCODED_PATTERN).size()>9?"/screens/me_amadron_extended_process_station.json":"/screens/me_amadron_process_station.json");
        var cancel=new AE2TinyButton(new TextComponentTranslation("menu.ae2s-appliedpneumatics.button.cancel_jobs"),()->send(5,0,0,""));
        cancel.setTooltip(new TextComponentTranslation("menu.ae2s-appliedpneumatics.button.cancel_jobs.tooltip"));
        widgets.add("cancel_jobs_button",cancel);widgets.addOpenPriorityButton();
    }
    @Override protected void updateBeforeRender(){
        super.updateBeforeRender();setTextContent("job_amount",new TextComponentTranslation("menu.label.ae2s-appliedpneumatics.me_amadron_process_station.job_amount",menu.state.getInteger("queued")));
    }
}
