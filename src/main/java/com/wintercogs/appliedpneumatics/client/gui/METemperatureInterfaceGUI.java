package com.wintercogs.appliedpneumatics.client.gui;

import com.wintercogs.appliedpneumatics.common.menu.ContainerAP;
import net.minecraft.util.text.*;
import java.util.Locale;

/** Port of upstream METemperatureInterfaceGUI, including Celsius presentation and Shift x10. */
public final class METemperatureInterfaceGUI extends APUpgradeableScreen {
    public METemperatureInterfaceGUI(ContainerAP menu){
        super(menu,"/screens/me_temperature_interface.json");
        widgets.addButton("add_button_10",new TextComponentString("+10"),()->changeTarget(isShiftKeyDown()?100:10));
        widgets.addButton("add_button_1",new TextComponentString("+1"),()->changeTarget(isShiftKeyDown()?10:1));
        widgets.addButton("reduce_button_1",new TextComponentString("-1"),()->changeTarget(isShiftKeyDown()?-10:-1));
        widgets.addButton("reduce_button_10",new TextComponentString("-10"),()->changeTarget(isShiftKeyDown()?-100:-10));
    }
    @Override protected void updateBeforeRender(){
        super.updateBeforeRender();var state=menu.state;
        setTextContent("temperature",new TextComponentTranslation("menu.label.ae2s-appliedpneumatics.me_temperature_interface.temperature",String.format(Locale.ROOT,"%.1f",state.getDouble("value")-273)+"℃"));
        setTextContent("heat_cap",new TextComponentTranslation("menu.label.ae2s-appliedpneumatics.me_temperature_interface.heat_cap",String.format(Locale.ROOT,"%.0f",state.getDouble("heat_capacity"))));
        setTextContent("expected_temperature",new TextComponentString(String.format(Locale.ROOT,"%.1f",state.getDouble("target")-273)));
        setTextContent("expected_temperature_text",new TextComponentTranslation("menu.label.ae2s-appliedpneumatics.me_temperature_interface.expected_temperature_text"));
        setTextContent("expected_temperature_change_mult_text",new TextComponentTranslation("menu.label.ae2s-appliedpneumatics.me_temperature_interface.expected_temperature_change_mult_text"));
    }
}
