package com.wintercogs.appliedpneumatics.verification;

import com.wintercogs.appliedpneumatics.common.init.APContent;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import java.util.List;
import static com.wintercogs.appliedpneumatics.verification.RuntimeVerification.require;

public final class PonderVerification {
    public static void verify(String sceneId)throws Exception{
        var entries=PonderIndex.getSceneAccess().getRegisteredEntries().stream().filter(e->"ae2s-appliedpneumatics".equals(e.getValue().getNamespace())).toList();
        require(entries.size()==33,"Expected 33 Ponder subject associations: "+entries.size());
        var air=entries.stream().map(java.util.Map.Entry::getValue).filter(e->APContent.id("air_network").equals(e.getDeclaredSceneId())).toList();
        require(air.size()==26&&air.stream().allMatch(e->e.getBoard()==air.getFirst().getBoard()),"Air network subjects must share one storyboard identity");
        var diagnostics=PonderIndex.getDiagnosticSnapshot(net.createmod.ponder.api.diagnostic.PonderDiagnosticView.EFFECTIVE);
        require(diagnostics.getScenes().stream().filter(s->s.getSceneId()!=null&&"ae2s-appliedpneumatics".equals(s.getSceneId().getNamespace()))
            .flatMap(s->s.getIssues().stream()).noneMatch(issue->"registration.duplicate_scene_id".equals(issue.getCode())),"Ponder must report zero duplicate Applied Pneumatics scenes");
        var loader=new PonderStructureLoader();
        for(var id:List.of("air_network","temperature","amadron")){
            var structure=loader.load(APContent.id(id));require(structure.getDiagnostics().isEmpty(),"Unresolved Ponder structure "+id+": "+structure.getDiagnostics());
            var entry=entries.stream().map(java.util.Map.Entry::getValue).filter(e->e.getSchematicLocation().equals(APContent.id(id))).findFirst().orElseThrow();
            var compiled=PonderIndex.getSceneAccess().compile(List.of(entry));var scene=compiled.getFirst();scene.seek(scene.getTotalTicks());require(scene.isFinished(),"Ponder did not finish "+id);
        }
        for(var key:List.of("air_power","air_import","air_pressure","air_safety","air_portable","air_p2p","heat_target","heat_cost","heat_p2p","trade_station","trade_link","trade_order","trade_pattern","trade_cpu")){
            String text=PonderIndex.getLangAccess().getShared(APContent.id(key));require(!text.startsWith("Format error:")&&!text.equals("ae2s-appliedpneumatics.ponder.shared."+key),"Ponder narration missing "+key);
        }
        var entry=entries.stream().map(java.util.Map.Entry::getValue).filter(e->e.getSchematicLocation().equals(APContent.id(sceneId))).findFirst().orElseThrow();
        var scenes=PonderIndex.getSceneAccess().compile(List.of(entry));Minecraft.getMinecraft().displayGuiScreen(new PonderUI(scenes));((PonderUI)Minecraft.getMinecraft().currentScreen).seekToTime(160);
        RuntimeVerification.INSTANCE.checked("Ponder 33 subjects and all three complete timelines; displayed "+sceneId+" in "+Minecraft.getMinecraft().gameSettings.language);
    }
}
