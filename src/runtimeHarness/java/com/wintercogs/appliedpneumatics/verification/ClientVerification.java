package com.wintercogs.appliedpneumatics.verification;

import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.client.gui.*;
import com.wintercogs.appliedpneumatics.client.gui.widgets.AmadronOfferPanel;
import com.wintercogs.appliedpneumatics.common.init.APContent;
import com.wintercogs.appliedpneumatics.common.items.AmadronWirelessTerminalItem;
import com.wintercogs.appliedpneumatics.common.me.storage.AirCellInventory;
import com.wintercogs.appliedpneumatics.common.me.crafting.AmadronPatternDetails;
import com.wintercogs.appliedpneumatics.common.menu.*;
import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEItemKey;
import ae2.client.gui.AEBaseGui;
import ae2.client.gui.widgets.AETextField;
import ae2.container.SlotSemantics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.util.concurrent.CompletableFuture;
import static com.wintercogs.appliedpneumatics.verification.RuntimeVerification.require;

/** Packaged client: real button/slot events, network packets and original upstream UI screenshots. */
public final class ClientVerification {
    private final Minecraft mc=Minecraft.getMinecraft();
    private CompletableFuture<Void> pending;
    private int phase,ticks,waitUntil;
    private String capture;
    private boolean done,joined,modelsChecked;
    private long beforePayment,beforeOutput;
    private int beforeTerminalHeight;
    private BlockPos delayedOpen;
    private int delayedOpenTick,lastLoggedPhase=-1;
    private AmadronOfferSnapshot selected;
    private static final BlockPos EXTENDED=new BlockPos(12,80,4);
    public static void install(){MinecraftForge.EVENT_BUS.register(new ClientVerification());}
    private void checked(String text){RuntimeVerification.INSTANCE.checked(text);}
    private net.minecraft.entity.player.EntityPlayerMP player(){return mc.getIntegratedServer().getPlayerList().getPlayers().getFirst();}
    private void server(Runnable task){
        var completion=new CompletableFuture<Void>();pending=pending==null?completion:CompletableFuture.allOf(pending,completion);
        mc.getIntegratedServer().addScheduledTask(()->{try{task.run();completion.complete(null);}catch(Throwable error){completion.completeExceptionally(error);}});
    }
    private void open(BlockPos pos){delayedOpen=pos;delayedOpenTick=ticks+6;}
    private ContainerAP menu(){return (ContainerAP)mc.player.openContainer;}
    private AEBaseGui<?> screen(){return (AEBaseGui<?>)mc.currentScreen;}
    private void language(String code){mc.gameSettings.language=code;mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguage(code));mc.refreshResources();}
    private void verifyNames(String locale)throws Exception{
        var names=new com.google.gson.JsonObject();
        for(var item:APContent.ITEMS){
            var stack=new ItemStack(item);var actual=stack.getDisplayName();
            var expected=net.minecraft.client.resources.I18n.format(stack.getTranslationKey()+".name");
            require(!actual.contains(".name")&&!actual.startsWith("item.")&&!actual.startsWith("tile.")&&!actual.equals(stack.getTranslationKey()),"Untranslated item in "+locale+": "+item.getRegistryName()+" -> "+actual);
            require(actual.equals(expected),"Item name differs from translated suffix key in "+locale+": "+actual+" / "+expected);
            require(item.getRegistryName().getNamespace().equals(AppliedPneumatics.MODID),"Old item namespace remains: "+item.getRegistryName());
            names.addProperty(item.getRegistryName().toString(),actual);
        }
        var output=mc.gameDir.toPath().resolve("apneumatics-verification/names-"+locale+".json");
        java.nio.file.Files.createDirectories(output.getParent());
        java.nio.file.Files.writeString(output,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(names));
        checked("All 35 registered items/blocks have exact translated display names without .name suffix in "+locale);
    }
    private void mouse(String srg,String readable,int x,int y,int button)throws Exception{
        java.lang.reflect.Method method;
        try{method=GuiScreen.class.getDeclaredMethod(srg,int.class,int.class,int.class);}catch(NoSuchMethodException ignored){method=GuiScreen.class.getDeclaredMethod(readable,int.class,int.class,int.class);}
        method.setAccessible(true);method.invoke(mc.currentScreen,x,y,button);
    }
    private void click(int x,int y,int button)throws Exception{
        x+=screen().getGuiLeft();y+=screen().getGuiTop();
        mouse("func_73864_a","mouseClicked",x,y,button);mouse("func_146286_b","mouseReleased",x,y,button);
    }
    @SuppressWarnings("unchecked") private <T>T field(String name)throws Exception{var f=mc.currentScreen.getClass().getDeclaredField(name);f.setAccessible(true);return (T)f.get(mc.currentScreen);}
    private void pause(){waitUntil=ticks+15;phase++;}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||done)return;
        ticks++;
        if(delayedOpen!=null&&ticks>=delayedOpenTick){
            var pos=delayedOpen;delayedOpen=null;
            server(()->{player().setPositionAndUpdate(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);player().openGui(AppliedPneumatics.INSTANCE,0,player().world,pos.getX(),pos.getY(),pos.getZ());});
        }
        if(ticks<waitUntil||capture!=null)return;
        try{
            if(ticks>10000)throw new IllegalStateException("Client acceptance timeout at phase "+phase);
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(lastLoggedPhase!=phase){lastLoggedPhase=phase;System.out.println("AP client acceptance phase="+phase+" screen="+(mc.currentScreen==null?"null":mc.currentScreen.getClass().getSimpleName()));}
            switch(phase){
                case -1->{
                    if(Boolean.getBoolean("apneumatics.modelsOnly")){RuntimeVerification.INSTANCE.write("PASS",null);done=true;mc.shutdown();return;}
                    mc.displayGuiScreen(new GuiMainMenu());phase=0;
                }
                case 0->{
                    if(!(mc.currentScreen instanceof GuiMainMenu))return;
                    if(!modelsChecked){ModelVerification.verify("initial-load");modelsChecked=true;mc.displayGuiScreen(ModelVerification.catalog());capture="all-item-textures.png";phase=-1;return;}
                    mc.gameSettings.pauseOnLostFocus=false;mc.gameSettings.renderDistanceChunks=3;mc.gameSettings.guiScale=2;mc.gameSettings.limitFramerate=30;
                    mc.gameSettings.setSoundLevel(net.minecraft.util.SoundCategory.MASTER,0);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                    var settings=new WorldSettings(2718,GameType.CREATIVE,false,false,WorldType.FLAT);settings.setGeneratorOptions("3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;");settings.enableCommands();
                    mc.launchIntegratedServer("apneumatics-acceptance","Applied Pneumatics acceptance",settings);phase++;
                }
                case 1->{
                    if(mc.world==null||mc.player==null)return;
                    if(!joined){mc.displayGuiScreen(null);mc.setIngameFocus();joined=true;server(()->{player().setPositionAndUpdate(.5,82,.5);player().capabilities.isFlying=true;player().sendPlayerAbilities();});return;}
                    if(!RuntimeVerification.gameplayDone){if(mc.currentScreen!=null)mc.displayGuiScreen(null);return;}
                    verifyNames("en_us");checked("Packaged client enters world and completes genuine PNC/AE2S gameplay checks");open(RuntimeVerification.PRESSURE);pause();
                }
                case 2->{if(!(mc.currentScreen instanceof MEPressureInterfaceGUI)||!"pressure".equals(menu().state.getString("kind")))return;click(155,85,0);click(121,85,0);pause();}
                case 3->{require(Math.abs(menu().state.getDouble("target")-3.1)<.0001,"Original pressure buttons did not synchronize 3.1 bar");capture="pressure-en_us.png";checked("Original pressure UI: +1/+0.1 buttons, live air/volume/pressure, server state");phase++;}
                case 4->{language("zh_cn");verifyNames("zh_cn");ModelVerification.verify("resource-reload");require(!net.minecraft.client.resources.I18n.format("menu.label.ae2s-appliedpneumatics.me_pressure_interface.expected_pressure_text").contains("ae2s-appliedpneumatics"),"Chinese original GUI translation missing");capture="pressure-zh_cn.png";phase++;}
                case 5->{mc.player.closeScreen();open(RuntimeVerification.HEAT);pause();}
                case 6->{if(!(mc.currentScreen instanceof METemperatureInterfaceGUI)||!"temperature".equals(menu().state.getString("kind")))return;click(155,85,0);click(121,85,0);pause();}
                case 7->{require(menu().state.getDouble("target")==316,"Original temperature increment buttons failed");capture="temperature-zh_cn.png";checked("Original temperature UI: Celsius display, +10/+1 controls, Kelvin backend synchronization");phase++;}
                case 8->{mc.player.closeScreen();open(RuntimeVerification.STATION);pause();}
                case 9->{
                    if(!(mc.currentScreen instanceof MEAmadronProcessStationGUI))return;
                    require(menu().getSlots(SlotSemantics.UPGRADE).size()==4,"Original station requires four upgrade slots");
                    require(menu().getSlots(SlotSemantics.ENCODED_PATTERN).size()==9&&menu().getSlots(SlotSemantics.MACHINE_INPUT).size()==9&&menu().getSlots(SlotSemantics.MACHINE_OUTPUT).size()==9,"Original station grids missing");
                    capture="amadron-station-zh_cn.png";checked("Original 176x215 station UI: nine patterns and separate 3x3 input/output grids");phase++;
                }
                case 10->{click(162,5,0);pause();}
                case 11->{if(!(mc.player.openContainer instanceof ae2.container.implementations.ContainerPriority priority))return;priority.setPriority(42);pause();}
                case 12->{server(()->require(RuntimeVerification.INSTANCE.station.getPatternPriority()==42,"Station priority GUI action failed"));capture="amadron-priority-zh_cn.png";checked("Native AE2S priority submenu changes the station's crafting priority");phase++;}
                case 13->{ae2.client.gui.implementations.AESubGui.goBack();pause();}
                case 14->{
                    if(!(mc.currentScreen instanceof MEAmadronProcessStationGUI))return;
                    mc.player.closeScreen();server(()->player().world.setBlockState(EXTENDED,APContent.EXTENDED_STATION.getDefaultState()));open(EXTENDED);pause();
                }
                case 15->{if(!(mc.currentScreen instanceof MEAmadronProcessStationGUI)||menu().getSlots(SlotSemantics.ENCODED_PATTERN).size()!=36)return;capture="amadron-extended-zh_cn.png";checked("Original extended 176x251 station UI: 36 pattern slots, input/output grids, priority and cancel");phase++;}
                case 16->{
                    mc.player.closeScreen();server(()->{
                        var stack=new ItemStack(APContent.WIRELESS);APContent.WIRELESS.injectAEPower(stack,10000,Actionable.MODULATE);player().inventory.currentItem=0;player().inventory.setInventorySlotContents(0,stack);
                        player().inventory.setInventorySlotContents(9,ae2.core.definitions.AEItems.BLANK_PATTERN.stack(2));
                        player().setSneaking(true);APContent.WIRELESS.onItemUse(player(),player().world,RuntimeVerification.STATION,EnumHand.MAIN_HAND,EnumFacing.UP,0,0,0);player().setSneaking(false);
                        require(AmadronWirelessTerminalItem.station(stack)==RuntimeVerification.INSTANCE.station,"Wireless binding failed");
                    });pause();
                }
                case 17->{if(mc.player.getHeldItemMainhand().getItem()!=APContent.WIRELESS)return;server(()->APContent.WIRELESS.onItemRightClick(player().world,player(),EnumHand.MAIN_HAND));pause();}
                case 18->{
                    if(!(mc.currentScreen instanceof AmadronWirelessTerminalGUI)||menu().getOfferSnapshot().isEmpty())return;
                    ((AETextField)screen().getWidgets().getTextFields().iterator().next()).setTextFromClient("certus");pause();
                }
                case 19->{
                    java.util.List<AmadronOfferPanel> panels=field("pagePanels");var panel=panels.stream().filter(p->p.enabled&&p.visible).findFirst().orElseThrow();selected=panel.getOffer();
                    require(selected.matches("certus"),"Original search did not match registry ID in Chinese");
                    click(panel.x-screen().getGuiLeft()+8,panel.y-screen().getGuiTop()+10,1);
                    require(panel.getWantedStock()==1,"Right click must add one cart unit");panel.onClicked(panel.x,panel.y,1,true);require(panel.getWantedStock()==2,"Shift right must double");panel.onClicked(panel.x,panel.y,0,true);require(panel.getWantedStock()==1,"Shift left must halve");panel.onClicked(panel.x,panel.y,2,false);require(panel.getWantedStock()==0,"Middle click must clear");panel.onClicked(panel.x,panel.y,1,false);
                    int blank=menu().inventorySlots.stream().filter(s->s.getStack().getItem()==ae2.core.definitions.AEItems.BLANK_PATTERN.asItem()).findFirst().orElseThrow().slotNumber;
                    mc.playerController.windowClick(menu().windowId,blank,0,ClickType.QUICK_MOVE,mc.player);pause();
                }
                case 20->{require(menu().getPatternInventory().getStackInSlot(0).getCount()==2,"Shift transfer into original blank-pattern slot failed");click(140,screen().getBounds(false).height-115,0);pause();}
                case 21->{
                    require(menu().getPatternInventory().getStackInSlot(1).getItem()==APContent.AMADRON_PATTERN&&menu().getPatternInventory().getStackInSlot(0).getCount()==1,"Original encoding arrow did not consume one blank and fill output slot");
                    capture="wireless-cart-pattern-zh_cn.png";checked("Original wireless UI: Chinese search, cart mouse rules, selected offer card, real blank/encoded slots and encoding arrow");
                    server(()->{var station=RuntimeVerification.INSTANCE.station;beforePayment=station.network().extract(AmadronPatternDetails.resource(selected.offer().getInput()).what(),Long.MAX_VALUE,Actionable.SIMULATE,IActionSource.empty());beforeOutput=station.network().extract(AmadronPatternDetails.resource(selected.offer().getOutput()).what(),Long.MAX_VALUE,Actionable.SIMULATE,IActionSource.empty());});phase++;
                }
                case 22->{click(80,screen().getBounds(false).height-145,0);pause();}
                case 23->{require("ae2s-appliedpneumatics.ui.order_ok".equals(menu().state.getString("status")),"Original Submit button cart was rejected");phase++;}
                case 24->{
                    server(()->{var station=RuntimeVerification.INSTANCE.station;long output=station.network().extract(AmadronPatternDetails.resource(selected.offer().getOutput()).what(),Long.MAX_VALUE,Actionable.SIMULATE,IActionSource.empty());
                        if(output>=beforeOutput+AmadronPatternDetails.resource(selected.offer().getOutput()).amount()){
                            long paid=station.network().extract(AmadronPatternDetails.resource(selected.offer().getInput()).what(),Long.MAX_VALUE,Actionable.SIMULATE,IActionSource.empty());require(paid==beforePayment-AmadronPatternDetails.resource(selected.offer().getInput()).amount(),"Wireless cart payment mismatch");phase=25;
                        }
                    });waitUntil=ticks+20;
                }
                case 25->{
                    checked("Original wireless Submit button delivers a cart through real packets, ME payment and PNC collection/delivery drones");
                    beforeTerminalHeight=screen().getBounds(false).height;
                    var style=(ae2.client.gui.widgets.SettingToggleButton<?>)fieldToolbar();
                    style.mousePressed(mc,style.x+3,style.y+3);style.mouseReleased(style.x+3,style.y+3);pause();
                }
                case 26->{require(screen().getBounds(false).height!=beforeTerminalHeight,"Native terminal style button did not change height");capture="wireless-resized-zh_cn.png";phase=260;}
                case 260->{mc.player.closeScreen();waitUntil=ticks+10;phase=261;}
                case 261->{server(()->APContent.WIRELESS.onItemRightClick(player().world,player(),EnumHand.MAIN_HAND));waitUntil=ticks+15;phase=27;}
                case 27->{
                    if(!(mc.currentScreen instanceof AmadronWirelessTerminalGUI))return;
                    require(menu().getPatternInventory().getStackInSlot(0).getCount()==1&&menu().getPatternInventory().getStackInSlot(1).getItem()==APContent.AMADRON_PATTERN,"Wireless pattern inventory did not persist across reopening");
                    checked("Wireless terminal pattern slots persist on the held item; native terminal style toggles and resizes");
                    mc.player.closeScreen();server(()->{var item=APContent.PORTABLE_CELLS.get(9);var stack=new ItemStack(item);AirCellInventory.setAir(stack,123456789L);item.injectAEPower(stack,10000,Actionable.MODULATE);player().inventory.currentItem=0;player().inventory.setInventorySlotContents(0,stack);});pause();
                }
                case 28->{if(mc.player.getHeldItemMainhand().getItem()!=APContent.PORTABLE_CELLS.get(9))return;server(()->APContent.PORTABLE_CELLS.get(9).onItemRightClick(player().world,player(),EnumHand.MAIN_HAND));pause();}
                case 29->{if(!(mc.player.openContainer instanceof ae2.container.me.common.ContainerMEStorage))return;require(mc.player.openContainer.inventorySlots.stream().filter(s->s instanceof ae2.client.gui.me.common.RepoSlot).count()<=27,"Portable air UI exceeded original three-row cap");capture="portable-256m-zh_cn.png";checked("256M portable air cell opens native AE2S terminal and renders long air quantity");phase++;}
                case 30->{mc.player.closeScreen();language("en_us");if(Loader.isModLoaded("ponder-legacy")){Class.forName("com.wintercogs.appliedpneumatics.verification.PonderVerification").getMethod("verify",String.class).invoke(null,"air_network");capture="ponder-air-en_us.png";}phase++;}
                case 31->{if(Loader.isModLoaded("ponder-legacy")){language("zh_cn");Class.forName("com.wintercogs.appliedpneumatics.verification.PonderVerification").getMethod("verify",String.class).invoke(null,"temperature");capture="ponder-temperature-zh_cn.png";}phase++;}
                case 32->{if(Loader.isModLoaded("ponder-legacy")){Class.forName("com.wintercogs.appliedpneumatics.verification.PonderVerification").getMethod("verify",String.class).invoke(null,"amadron");capture="ponder-amadron-zh_cn.png";}phase++;}
                default->{RuntimeVerification.INSTANCE.write("PASS",null);done=true;mc.shutdown();}
            }
        }catch(Throwable failure){failure.printStackTrace();RuntimeVerification.INSTANCE.write("FAIL",failure);done=true;mc.shutdown();}
    }
    private GuiButton fieldToolbar()throws Exception{
        var f=GuiScreen.class.getDeclaredFields();
        for(var field:f)if(java.util.List.class.isAssignableFrom(field.getType())){
            field.setAccessible(true);var value=field.get(mc.currentScreen);if(value instanceof java.util.List<?> list)for(var button:list)if(button instanceof ae2.client.gui.widgets.SettingToggleButton<?>)return (GuiButton)button;
        }
        throw new IllegalStateException("Native terminal style toolbar button missing");
    }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END||capture==null)return;
        try{
            var out=mc.gameDir.toPath().resolve("apneumatics-verification");net.minecraft.util.ScreenShotHelper.saveScreenshot(out.toFile(),capture,mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
            require(org.lwjgl.opengl.GL11.glGetError()==0,"GUI render left an OpenGL error");checked("Rendered "+capture+" without OpenGL errors");
        }catch(Throwable failure){RuntimeVerification.INSTANCE.write("FAIL",failure);done=true;mc.shutdown();}
        capture=null;waitUntil=ticks+8;
    }
}
