package com.teambrmodding.neotech.common.fluids;

import com.teambr.bookshelf.util.ClientUtils;
import com.teambrmodding.neotech.lib.Reference;
import net.minecraft.block.material.Material;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.IFluidBlock;

/**
 * This file was created for NeoTech
 * <p>
 * NeoTech is licensed under the
 * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License:
 * http://creativecommons.org/licenses/by-nc-sa/4.0/
 *
 * @author Paul Davis - pauljoda
 * @since 2/15/2017
 */
public class FluidBlockGas extends BlockFluidClassic{
    private FluidGas fluid;

    /**
     * Creates the fluid block
     * @param fluid The associated fluid
     */
    public FluidBlockGas(FluidGas fluid) {
        super(fluid, Material.WATER);
        setRegistryName(new ResourceLocation(Reference.MOD_ID, fluid.getName()));
        setUnlocalizedName(getRegistryName().toString());
    }

    /**
     * Gets the color of this fluid block
     * @return The block color
     */
    public int getBlockColor() {
        return fluid.getColor();
    }

    /*******************************************************************************************************************
     * Block                                                                                                           *
     *******************************************************************************************************************/

    /**
     * Gets the localized name of this block. Used for the statistics page.
     */
    @Override
    public String getLocalizedName() {
        return ClientUtils.translate("fluid." + fluid.getName() + ".name");
    }
}
