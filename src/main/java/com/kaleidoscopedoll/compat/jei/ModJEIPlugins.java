package com.kaleidoscopedoll.compat.jei;

import com.kaleidoscopedoll.KaleidoscopeDoll;
import com.kaleidoscopedoll.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.Identifier;

public class ModJEIPlugins implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(KaleidoscopeDoll.MOD_ID, "jei");

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.DOLL_ENTITY_ITEM, new EntityDollSubtype());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RecipeTypes.CRAFTING, EntityDollRecipeMaker.createRecipes());
        registration.addRecipes(RecipeTypes.CRAFTING, DollFunctionRecipeMaker.createRecipes());
    }

    @Override
    public Identifier getPluginUid() {
        return UID;
    }
}
