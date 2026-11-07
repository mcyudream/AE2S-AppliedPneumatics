package com.wintercogs.appliedpneumatics.common.menu;

import com.wintercogs.appliedpneumatics.common.me.crafting.AmadronPatternDetails;
import me.desht.pneumaticcraft.common.recipes.AmadronOffer;
import java.util.Locale;

/** Server-issued menu identity; PNC 1.12 does not assign ResourceLocation offer IDs. */
public record AmadronOfferSnapshot(int index,AmadronOffer offer,int stock,boolean isStatic,boolean isPlayer,boolean valid,String supplier){
    public boolean matches(String query){
        if(query.isBlank()||offer.passesQuery(query))return true;
        var lower=query.toLowerCase(Locale.ROOT);
        return AmadronPatternDetails.resource(offer.getInput()).what().getId().toString().toLowerCase(Locale.ROOT).contains(lower)
            ||AmadronPatternDetails.resource(offer.getOutput()).what().getId().toString().toLowerCase(Locale.ROOT).contains(lower);
    }
}
