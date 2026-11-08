package com.wintercogs.appliedpneumatics.verification;

import ae2.api.client.StorageCellModels;
import com.google.gson.GsonBuilder;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import static com.wintercogs.appliedpneumatics.verification.RuntimeVerification.require;

/** Inspect the models and atlas sprites actually selected by the packaged client. */
public final class ModelVerification {
    private static Map<String,Object> inspect(IBakedModel model,String label){
        var mc=Minecraft.getMinecraft();
        require(model!=null&&model!=mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel(),"Missing baked model: "+label);
        var quads=new ArrayList<>(model.getQuads(null,null,0));
        for(var face:EnumFacing.values())quads.addAll(model.getQuads(null,face,0));
        require(!quads.isEmpty(),"Empty baked model: "+label);
        var sprites=new java.util.TreeSet<String>();
        for(var quad:quads){
            var name=quad.getSprite().getIconName();
            require(!name.contains("missingno"),"Missing atlas texture: "+label+" -> "+name);
            sprites.add(name);
        }
        return Map.of("quads",quads.size(),"sprites",sprites);
    }

    public static void verify(String phase)throws Exception{
        var mc=Minecraft.getMinecraft();
        var items=new LinkedHashMap<String,Object>();
        for(var item:APContent.ITEMS){
            var stack=new ItemStack(item);
            items.put(item.getRegistryName().toString(),inspect(mc.getRenderItem().getItemModelWithOverrides(stack,mc.world,mc.player),item.getRegistryName().toString()));
        }
        var blocks=new LinkedHashMap<String,Object>();
        for(var block:APContent.BLOCKS)for(var state:block.getBlockState().getValidStates())
            blocks.put(state.toString(),inspect(mc.getBlockRendererDispatcher().getModelForState(state),state.toString()));
        var cells=new LinkedHashMap<String,Object>();
        for(var item:APContent.CELLS){
            var location=StorageCellModels.model(item);
            var portable=APContent.PORTABLE_CELLS.get(APContent.CELLS.indexOf(item));
            require(location!=null&&location.equals(StorageCellModels.model(portable)),"Missing drive cell registration: "+item.getRegistryName());
            var model=net.minecraftforge.client.model.ModelLoaderRegistry.getModel(location);
            cells.put(location.toString(),inspect(model.bake(model.getDefaultState(),net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM,texture->mc.getTextureMapBlocks().getAtlasSprite(texture.toString())),location.toString()));
        }
        var aliases=new LinkedHashMap<String,Object>();
        for(var item:APContent.ITEMS)for(var namespace:java.util.List.of(AppliedPneumatics.MODID,AppliedPneumatics.PREVIOUS_RESOURCE_NAMESPACE)){
            var location=new net.minecraft.util.ResourceLocation(namespace,"item/"+item.getRegistryName().getPath());
            var model=net.minecraftforge.client.model.ModelLoaderRegistry.getModel(location);
            aliases.put(location.toString(),inspect(model.bake(model.getDefaultState(),net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM,texture->mc.getTextureMapBlocks().getAtlasSprite(texture.toString())),location.toString()));
        }
        var parts=new LinkedHashMap<String,Object>();
        for(var name:java.util.List.of("air","heat")){
            var location=APContent.renderId("part/p2p/p2p_tunnel_"+name);
            var model=net.minecraftforge.client.model.ModelLoaderRegistry.getModel(location);
            parts.put(location.toString(),inspect(model.bake(model.getDefaultState(),net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM,texture->mc.getTextureMapBlocks().getAtlasSprite(texture.toString())),location.toString()));
        }
        var output=mc.gameDir.toPath().resolve("apneumatics-verification/models-"+phase+".json");
        Files.createDirectories(output.getParent());
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("items",items,"blocks",blocks,"drive_cells",cells,"resource_aliases",aliases,"p2p",parts)));
        RuntimeVerification.INSTANCE.checked("Actual baked models and atlas sprites: 35 items, 12 block states, 10 drive cells, 2 P2P models and 70 old/new resource aliases after "+phase);
    }

    public static net.minecraft.client.gui.GuiScreen catalog(){
        return new net.minecraft.client.gui.GuiScreen(){
            @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){
                drawRect(0,0,width,height,0xff27303c);
                drawCenteredString(fontRenderer,"AE2S Applied Pneumatics - all 35 item textures",width/2,12,0xffffff);
                var renderer=Minecraft.getMinecraft().getRenderItem();
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                int columns=7,cellWidth=width/columns,cellHeight=(height-50)/5;
                for(int i=0;i<APContent.ITEMS.size();i++){
                    var stack=new ItemStack(APContent.ITEMS.get(i));
                    int x=(i%columns)*cellWidth,y=40+(i/columns)*cellHeight;
                    net.minecraft.client.renderer.GlStateManager.pushMatrix();
                    net.minecraft.client.renderer.GlStateManager.translate(x+cellWidth/2-16,y,0);
                    net.minecraft.client.renderer.GlStateManager.scale(2,2,1);
                    renderer.renderItemAndEffectIntoGUI(stack,0,0);
                    net.minecraft.client.renderer.GlStateManager.popMatrix();
                    drawCenteredString(fontRenderer,String.valueOf(i+1),x+cellWidth/2,y+34,0xc8d6e5);
                }
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            }
        };
    }
}
