package com.wintercogs.appliedpneumatics.compat;

import com.wintercogs.appliedpneumatics.common.blocks.entitis.*;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Load machines saved with the previous mod ID, and write the current ID on the next save. */
public final class PreviousTileIds {
    public static void register(){
        GameRegistry.registerTileEntity(Pressure.class,new ResourceLocation("appliedpneumatics","me_pressure_interface_block"));
        GameRegistry.registerTileEntity(Temperature.class,new ResourceLocation("appliedpneumatics","me_temperature_interface"));
        GameRegistry.registerTileEntity(Station.class,new ResourceLocation("appliedpneumatics","me_amadron_process_station"));
        GameRegistry.registerTileEntity(ExtendedStation.class,new ResourceLocation("appliedpneumatics","me_amadron_extended_process_station"));
    }
    public static final class Pressure extends MEPressureInterfaceBlockEntity {
        @Override public void saveAdditional(NBTTagCompound tag){super.saveAdditional(tag);tag.setString("id",APContent.id("me_pressure_interface_block").toString());}
    }
    public static final class Temperature extends METemperatureInterfaceBlockEntity {
        @Override public void saveAdditional(NBTTagCompound tag){super.saveAdditional(tag);tag.setString("id",APContent.id("me_temperature_interface").toString());}
    }
    public static final class Station extends MEAmadronProcessStationBlockEntity {
        @Override public void saveAdditional(NBTTagCompound tag){super.saveAdditional(tag);tag.setString("id",APContent.id("me_amadron_process_station").toString());}
    }
    public static final class ExtendedStation extends MEAmadronProcessStationBlockEntity.Extended {
        @Override public void saveAdditional(NBTTagCompound tag){super.saveAdditional(tag);tag.setString("id",APContent.id("me_amadron_extended_process_station").toString());}
    }
}
