package com.wintercogs.appliedpneumatics.common.init;

import ae2.core.definitions.AEBlocks;
import ae2.core.definitions.AEItems;
import ae2.recipes.game.StorageCellDisassemblyRecipe;
import com.google.gson.*;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.items.IAirStorageCell;
import com.wintercogs.appliedpneumatics.common.me.storage.AirCellInventory;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.recipe.ItemIngredient;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid=AppliedPneumatics.MODID)
public final class APRecipes {
    public static int machineRecipeCount;
    public static ItemStack stack(JsonObject object){
        String id=object.has("item")?object.get("item").getAsString():object.get("id").getAsString();
        var item=Item.REGISTRY.getObject(new ResourceLocation(id));
        if(item==null||item==net.minecraft.init.Items.AIR)throw new IllegalStateException("Missing recipe item: "+id);
        return new ItemStack(item,object.has("count")?object.get("count").getAsInt():1,object.has("data")?object.get("data").getAsInt():0);
    }
    public static void integrate(){
        var api=PneumaticRegistry.getInstance().getRecipeRegistry();
        try(var input=Objects.requireNonNull(APRecipes.class.getResourceAsStream("/assets/ae2s-appliedpneumatics/data/machine-recipes.json"));var reader=new InputStreamReader(input,StandardCharsets.UTF_8)){
            var recipes=new JsonParser().parse(reader).getAsJsonArray();
            for(var element:recipes){
                var recipe=element.getAsJsonObject();String type=recipe.get("type").getAsString();
                if(recipe.get("source").getAsString().contains("mega_air_cell_shell")&&!AppliedPneumatics.enableLargeCells)continue;
                switch(type){
                    case "pneumaticcraft:amadron" -> api.registerDefaultStaticAmadronOffer(stack(recipe.getAsJsonObject("input").getAsJsonObject("resource")),stack(recipe.getAsJsonObject("output").getAsJsonObject("resource")));
                    case "pneumaticcraft:assembly_drill" -> api.addAssemblyDrillRecipe(stack(recipe.getAsJsonObject("input")),stack(recipe.getAsJsonObject("result")));
                    case "pneumaticcraft:assembly_laser" -> api.addAssemblyLaserRecipe(stack(recipe.getAsJsonObject("input")),stack(recipe.getAsJsonObject("result")));
                    case "pneumaticcraft:pressure_chamber" -> {
                        var inputs=new ArrayList<ItemIngredient>();for(var ingredient:recipe.getAsJsonArray("inputs"))inputs.add(new ItemIngredient(stack(ingredient.getAsJsonObject())));
                        var results=new ArrayList<ItemStack>();for(var result:recipe.getAsJsonArray("results"))results.add(stack(result.getAsJsonObject()));
                        api.registerPressureChamberRecipe(inputs.toArray(ItemIngredient[]::new),recipe.get("pressure").getAsFloat(),results.toArray(ItemStack[]::new));
                    }
                    default -> throw new IllegalArgumentException("Unsupported migrated recipe: "+type);
                }
                machineRecipeCount++;
            }
        }catch(IOException error){throw new UncheckedIOException(error);}
        for(int i=0;i<APContent.TIERS.length;i++){
            var normal=new ArrayList<ItemStack>();
            if(i<5){normal.add(new ItemStack(APContent.AIR_CELL_SHELL));normal.add(component(i));}
            else {normal.add(new ItemStack(APContent.MEGA_AIR_CELL_SHELL));for(int j=0;j<4;j++)normal.add(new ItemStack(APContent.CELLS.get(i-1)));}
            StorageCellDisassemblyRecipe.register(new StorageCellDisassemblyRecipe(APContent.CELLS.get(i),normal));
            var portable=new ArrayList<ItemStack>();
            if(i<5)portable.addAll(normal);else portable.add(new ItemStack(APContent.CELLS.get(i)));
            portable.add(AEBlocks.ME_CHEST.stack());portable.add(AEBlocks.ENERGY_CELL.stack());
            StorageCellDisassemblyRecipe.register(new StorageCellDisassemblyRecipe(APContent.PORTABLE_CELLS.get(i),portable));
        }
    }
    private static ItemStack component(int i){return switch(i){case 0->AEItems.CELL_COMPONENT_1K.stack();case 1->AEItems.CELL_COMPONENT_4K.stack();case 2->AEItems.CELL_COMPONENT_16K.stack();case 3->AEItems.CELL_COMPONENT_64K.stack();default->AEItems.CELL_COMPONENT_256K.stack();};}
    @SubscribeEvent public static void recipes(RegistryEvent.Register<IRecipe> event){
        if(AppliedPneumatics.enableLargeCells)for(int i=5;i<APContent.TIERS.length;i++){
            var in=NonNullList.<Ingredient>create();for(int j=0;j<4;j++)in.add(Ingredient.fromItem(APContent.CELLS.get(i-1)));in.add(Ingredient.fromItem(APContent.MEGA_AIR_CELL_SHELL));
            event.getRegistry().register(new EmptyCellRecipe(new ItemStack(APContent.CELLS.get(i)),in).setRegistryName(APContent.id("tiered/air_cell_"+APContent.TIERS[i])));
            in=NonNullList.create();in.add(Ingredient.fromItem(APContent.CELLS.get(i)));in.add(Ingredient.fromItem(AEBlocks.ME_CHEST.asItem()));in.add(Ingredient.fromItem(AEBlocks.ENERGY_CELL.asItem()));
            event.getRegistry().register(new EmptyCellRecipe(new ItemStack(APContent.PORTABLE_CELLS.get(i)),in).setRegistryName(APContent.id("tiered/portable_air_cell_"+APContent.TIERS[i])));
        }
        var upgrade=NonNullList.<Ingredient>create();upgrade.add(Ingredient.fromItem(AEItems.ENGINEERING_PROCESSOR.asItem()));upgrade.add(Ingredient.fromItem(me.desht.pneumaticcraft.common.item.Itemss.PRINTED_CIRCUIT_BOARD));upgrade.add(Ingredient.fromItem(AEItems.CAPACITY_CARD.asItem()));
        event.getRegistry().register(new ShapelessRecipes("ae2s-appliedpneumatics",new ItemStack(APContent.PROCESS_UPGRADE),upgrade).setRegistryName(APContent.id("tiered/process_upgrade")));
        var extended=NonNullList.<Ingredient>create();extended.add(Ingredient.fromItem(Item.getItemFromBlock(APContent.STATION)));extended.add(Ingredient.fromItem(APContent.PROCESS_UPGRADE));
        event.getRegistry().register(new ShapelessRecipes("ae2s-appliedpneumatics",new ItemStack(APContent.EXTENDED_STATION),extended).setRegistryName(APContent.id("tiered/extended_station")));
    }
    private static final class EmptyCellRecipe extends ShapelessRecipes {
        EmptyCellRecipe(ItemStack output,NonNullList<Ingredient> input){super("ae2s-appliedpneumatics",output,input);}
        @Override public boolean matches(InventoryCrafting inventory,World world){
            if(!super.matches(inventory,world))return false;
            for(int i=0;i<inventory.getSizeInventory();i++){
                var stack=inventory.getStackInSlot(i);
                if(stack.getItem() instanceof IAirStorageCell cell&&(AirCellInventory.getAir(stack)>0||!cell.getUpgrades(stack).isEmpty()))return false;
            }
            return true;
        }
    }
}
