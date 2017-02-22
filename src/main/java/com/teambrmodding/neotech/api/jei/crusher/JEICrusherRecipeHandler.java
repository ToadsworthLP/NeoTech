package com.teambrmodding.neotech.api.jei.crusher;

import com.teambrmodding.neotech.api.jei.NeotechJEIPlugin;
import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;

/**
 * This file was created for NeoTech
 *
 * NeoTech is licensed under the
 * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License:
 * http://creativecommons.org/licenses/by-nc-sa/4.0/
 *
 * @author Paul Davis - pauljoda
 * @since 2/5/2017
 */
public class JEICrusherRecipeHandler implements IRecipeHandler<JEICrusherRecipeWrapper> {

    @Override
    public Class<JEICrusherRecipeWrapper> getRecipeClass() {
        return JEICrusherRecipeWrapper.class;
    }

    @Override
    public String getRecipeCategoryUid() {
        return NeotechJEIPlugin.CRUSHER_UUID;
    }

    @Override
    public String getRecipeCategoryUid(JEICrusherRecipeWrapper recipe) {
        return NeotechJEIPlugin.CRUSHER_UUID;
    }

    @Override
    public IRecipeWrapper getRecipeWrapper(JEICrusherRecipeWrapper recipe) {
        return recipe;
    }

    @Override
    public boolean isRecipeValid(JEICrusherRecipeWrapper recipe) {
        return recipe.isValid();
    }
}
