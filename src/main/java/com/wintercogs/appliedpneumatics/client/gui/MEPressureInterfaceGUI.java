package com.wintercogs.appliedpneumatics.client.gui;

import com.wintercogs.appliedpneumatics.common.menu.ContainerAP;
import net.minecraft.util.text.*;
import java.util.Locale;

/** Port of upstream MEPressureInterfaceGUI; layout and button steps are unchanged. */
public final class MEPressureInterfaceGUI extends APUpgradeableScreen {
    public MEPressureInterfaceGUI(ContainerAP menu){
        super(menu,"/screens/me_pressure_interface.json");
        widgets.addButton("add_button_1",new TextComponentString("+1"),()->changeTarget(isShiftKeyDown()?5:1));
        widgets.addButton("add_button_01",new TextComponentString("+0.1"),()->changeTarget(isShiftKeyDown()?.5:.1));
        widgets.addButton("reduce_button_01",new TextComponentString("-0.1"),()->changeTarget(isShiftKeyDown()?-.5:-.1));
        widgets.addButton("reduce_button_1",new TextComponentString("-1"),()->changeTarget(isShiftKeyDown()?-5:-1));
    }
    @Override protected void updateBeforeRender(){
        super.updateBeforeRender();var state=menu.state;
        String air=String.format(Locale.ROOT,"%.1f",state.getInteger("air")/1000f);
        String volume=String.format(Locale.ROOT,"%.0f",state.getInteger("volume")/1000f);
        String pressure=String.format(Locale.ROOT,"%.0f",state.getDouble("value"));
        setTextContent("air_amount",new TextComponentTranslation("menu.label.ae2s-appliedpneumatics.me_pressure_interface.air_amount",air,volume,pressure));
        setTextContent("max_pressure",new TextComponentTranslation("menu.label.ae2s-appliedpneumatics.me_pressure_interface.max_pressure",state.getDouble("danger")));
        setTextContent("current_pressure",new TextComponentString(String.format(Locale.ROOT,"%.1f",state.getDouble("target"))));
        setTextContent("expected_pressure_text",new TextComponentTranslation("menu.label.ae2s-appliedpneumatics.me_pressure_interface.expected_pressure_text"));
        setTextContent("expected_pressure_change_mult_text",new TextComponentTranslation("menu.label.ae2s-appliedpneumatics.me_pressure_interface.expected_pressure_change_mult_text"));
    }
}
