package com.teambrmodding.neotech.common.tiles.machines.processors

import java.util

import com.teambrmodding.neotech.client.gui.machines.processors.GuiAlloyer
import com.teambrmodding.neotech.collections.EnumInputOutputMode
import com.teambrmodding.neotech.common.container.machines.processors.ContainerAlloyer
import com.teambrmodding.neotech.common.tiles.MachineProcessor
import com.teambrmodding.neotech.managers.{MetalManager, RecipeManager}
import com.teambrmodding.neotech.registries.AlloyerRecipeHandler
import com.teambr.bookshelf.client.gui.{GuiColor, GuiTextFormat}
import com.teambr.bookshelf.common.tiles.traits.FluidHandler
import com.teambrmodding.neotech.common.tiles.traits.IUpgradeItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.text.translation.I18n
import net.minecraft.util.{EnumFacing, EnumParticleTypes}
import net.minecraft.world.World
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.{Fluid, FluidStack, FluidTank}

import scala.util.control.Breaks._

/**
  * This file was created for NeoTech
  *
  * NeoTech is licensed under the
  * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License:
  * http://creativecommons.org/licenses/by-nc-sa/4.0/
  *
  * @author Paul Davis <pauljoda>
  * @since 2/20/2016
  */
class TileAlloyer extends MachineProcessor[(FluidStack, FluidStack), FluidStack] with FluidHandler {

    lazy val BASE_ENERGY_TICK = 300

    /**
      * The initial size of the inventory
      *
      * @return
      */
    override def initialSize: Int = 0

    /**
      * Add all modes you want, in order, here
      */
    def addValidModes() : Unit = {
        validModes += EnumInputOutputMode.INPUT_ALL
        validModes += EnumInputOutputMode.INPUT_PRIMARY
        validModes += EnumInputOutputMode.INPUT_SECONDARY
        validModes += EnumInputOutputMode.OUTPUT_ALL
        validModes += EnumInputOutputMode.ALL_MODES
    }

    /**
      * Return the list of upgrades by their id that are allowed in this machine
      * @return A list of valid upgrades
      */
    override def getAcceptableUpgrades: util.ArrayList[String] = {
        val list = new util.ArrayList[String]()
        list.add(IUpgradeItem.CPU_SINGLE_CORE)
        list.add(IUpgradeItem.CPU_DUAL_CORE)
        list.add(IUpgradeItem.CPU_QUAD_CORE)
        list.add(IUpgradeItem.CPU_OCT_CORE)
        list.add(IUpgradeItem.MEMORY_DDR1)
        list.add(IUpgradeItem.MEMORY_DDR2)
        list.add(IUpgradeItem.MEMORY_DDR3)
        list.add(IUpgradeItem.MEMORY_DDR4)
        list.add(IUpgradeItem.PSU_250W)
        list.add(IUpgradeItem.PSU_500W)
        list.add(IUpgradeItem.PSU_750W)
        list.add(IUpgradeItem.PSU_960W)
        list.add(IUpgradeItem.TRANSFORMER)
        list.add(IUpgradeItem.REDSTONE_CIRCUIT)
        list.add(IUpgradeItem.NETWORK_CARD)
        list
    }

    /**
      * Used to get how much energy to drain per tick, you should check for upgrades at this point
      *
      * @return How much energy to drain per tick
      */
    override def getEnergyCostPerTick: Int =
    BASE_ENERGY_TICK * getMultiplierByCategory(IUpgradeItem.ENUM_UPGRADE_CATEGORY.MEMORY) +
            ((getMultiplierByCategory(IUpgradeItem.ENUM_UPGRADE_CATEGORY.CPU) - 1) * 62)

    /**
      * Used to get how long it takes to cook things, you should check for upgrades at this point
      *
      * @return The time it takes in ticks to cook the current item
      */
    override def getCookTime : Int = {
        1000 - (62 * getMultiplierByCategory(IUpgradeItem.ENUM_UPGRADE_CATEGORY.CPU))
    }

    /**
      * Used to tell if this tile is able to process
      *
      * @return True if you are able to process
      */
    override def canProcess: Boolean = {
        if (energyStorage.getEnergyStored >= getEnergyCostPerTick) {
            return tanks(INPUT_TANK_1).getFluid != null && tanks(INPUT_TANK_2) != null &&
                    RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer)
                            .isValidInput(tanks(INPUT_TANK_1).getFluid, tanks(INPUT_TANK_2).getFluid) &&
                    (if(tanks(OUTPUT_TANK).getFluid == null) true else RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer)
                            .getOutput(tanks(INPUT_TANK_1).getFluid, tanks(INPUT_TANK_2).getFluid).get.amount + tanks(OUTPUT_TANK).getFluidAmount <= tanks(OUTPUT_TANK).getCapacity)
        }
        failCoolDown = 40
        false
    }

    /**
      * Used to actually cook the item
      */
    override def cook(): Unit = cookTime += 1

    /**
      * Called when the tile has completed the cook process
      */
    override def completeCook(): Unit = {
        breakable {
            for (x <- 0 until getModifierForCategory(IUpgradeItem.ENUM_UPGRADE_CATEGORY.MEMORY)) {
                if (canProcess) {
                    //Just to be safe
                    val recipeTest = RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer).getRecipe((tanks(INPUT_TANK_1).getFluid, tanks(INPUT_TANK_2).getFluid))
                    if (recipeTest.isDefined) {
                        val recipe = recipeTest.get
                        val output = recipe.getOutput((tanks(INPUT_TANK_1).getFluid, tanks(INPUT_TANK_2).getFluid))
                        //Drain Inputs
                        val drain1 = tanks(INPUT_TANK_1).drain(recipe.getFluidFromString(recipe.fluidOne).amount, false)
                        val drain2 = tanks(INPUT_TANK_2).drain(recipe.getFluidFromString(recipe.fluidTwo).amount, false)

                        if (drain1 != null && drain2 != null && drain1.amount > 0 && drain2.amount > 0) {
                            tanks(INPUT_TANK_1).drain(recipe.getFluidFromString(recipe.fluidOne).amount, true)
                            tanks(INPUT_TANK_2).drain(recipe.getFluidFromString(recipe.fluidTwo).amount, true)
                            tanks(OUTPUT_TANK).fill(output.get, true)
                        }
                    }
                } else break
            }
        }
        markForUpdate(6)
    }

    /**
      * Get the output of the recipe
      *
      * @param input The input
      * @return The output
      */
    override def getOutput(input: (FluidStack, FluidStack)): FluidStack =
    if(RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer).getOutput(input).isDefined)
        RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer).getOutput(input).get
    else
        null

    /**
      * Get the output of the recipe (used in insert options)
      *
      * @param input The input
      * @return The output
      */
    override def getOutputForStack(input: ItemStack): ItemStack = null

    /*******************************************************************************************************************
      **************************************************  Tile Methods  ************************************************
      ******************************************************************************************************************/

    /**
      * This will try to take things from other inventories and put it into ours
      */
    override def tryInput() : Unit = {
        for(dir <- EnumFacing.values) {
            worldObj.getTileEntity(pos.offset(dir)) match {
                case tile : TileEntity if tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite) =>
                    val otherTank = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite)

                    if(canInputFromSide(dir)) { // Left Tank
                        // If we have something, try to match and fill
                        if(tanks(INPUT_TANK_1).getFluid != null && otherTank.drain(tanks(INPUT_TANK_1).getFluid, false) != null) {
                            val amount = fill(otherTank.drain(1000, false), doFill = false)
                            if(amount > 0) {
                                fill(otherTank.drain(amount, true), doFill = true)
                                markForUpdate()
                            }
                        }
                        // Check our tank, if we can take fluid, do so
                        else if(tanks(INPUT_TANK_1).getFluid == null && otherTank.drain(1000, false) != null) {
                            val otherFluid = otherTank.drain(1000, false)
                            val hasOther = tanks(INPUT_TANK_2).getFluid != null
                            val amount = tanks(INPUT_TANK_1).fill(otherFluid, false)
                            if(amount > 0 &&
                                    (if(hasOther) RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer)
                                            .isValidInput(otherFluid, tanks(INPUT_TANK_2).getFluid) else true)) {
                                tanks(INPUT_TANK_1).fill(otherTank.drain(amount, true), true)
                                markForUpdate()
                            }
                        }
                    }

                    if(canInputFromSide(dir, isPrimary = false)) { // Right Tank
                        // If we have something, try to match and fill
                        if(tanks(INPUT_TANK_2).getFluid != null && otherTank.drain(tanks(INPUT_TANK_2).getFluid, false) != null) {
                            val amount = fill(otherTank.drain(1000, false), doFill = false)
                            if(amount > 0) {
                                fill(otherTank.drain(amount, true), doFill = true)
                                markForUpdate()
                            }
                        }
                        // Check our tank, if we can take fluid, do so
                        else if(tanks(INPUT_TANK_2).getFluid == null && otherTank.drain(1000, false) != null) {
                            val otherFluid = otherTank.drain(1000, false)
                            val hasOther = tanks(INPUT_TANK_1).getFluid != null
                            val amount = tanks(INPUT_TANK_2).fill(otherFluid, false)
                            if(amount > 0 &&
                                    (if(hasOther) RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer)
                                            .isValidInput(otherFluid, tanks(INPUT_TANK_1).getFluid) else true)) {
                                tanks(INPUT_TANK_2).fill(otherTank.drain(amount, true), true)
                                markForUpdate()
                            }
                        }
                    }

                case _ =>
            }
        }
    }

    /**
      * This will try to take things from other inventories and put it into ours
      */
    override def tryOutput() : Unit = {
        for(dir <- EnumFacing.values) {
            if(canOutputFromSide(dir)) {
                worldObj.getTileEntity(pos.offset(dir)) match {
                    case tile : TileEntity if tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite) =>
                        val otherTank = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite)

                        // If we have something, try to match and fill
                        if(tanks(OUTPUT_TANK).getFluid != null && otherTank.fill(tanks(OUTPUT_TANK).getFluid, false) > 0) {
                            val amount = drain(otherTank.fill(tanks(OUTPUT_TANK).getFluid, false), doDrain = false)
                            if(amount != null)
                                drain(otherTank.fill(amount, true), doDrain = true)
                        }
                    case _ =>
                }
            }
        }
    }

    override def writeToNBT(tag : NBTTagCompound) : NBTTagCompound = {
        super[MachineProcessor].writeToNBT(tag)
        super[FluidHandler].writeToNBT(tag)
        tag
    }

    override def readFromNBT(tag : NBTTagCompound) : Unit = {
        super[MachineProcessor].readFromNBT(tag)
        super[FluidHandler].readFromNBT(tag)
    }

    /*******************************************************************************************************************
      ************************************************ Inventory methods ***********************************************
      ******************************************************************************************************************/

    /**
      * Used to get what slots are allowed to be input
      *
      * @return The slots to input from
      */
    override def getInputSlots(mode : EnumInputOutputMode) : Array[Int] = Array()

    /**
      * Used to get what slots are allowed to be output
      *
      * @return The slots to output from
      */
    override def getOutputSlots(mode : EnumInputOutputMode) : Array[Int] = Array()

    /*******************************************************************************************************************
      **************************************************** Fluid methods ***********************************************
      ******************************************************************************************************************/

    lazy val INPUT_TANK_1 = 0
    lazy val INPUT_TANK_2 = 1
    lazy val OUTPUT_TANK  = 2

    /**
      * Used to set up the tanks needed. You can insert any number of tanks
      */
    override def setupTanks(): Unit = {
        tanks += new FluidTank(10 * MetalManager.BLOCK_MB) // IN 1
        tanks += new FluidTank(10 * MetalManager.BLOCK_MB) // IN 2
        tanks += new FluidTank(10 * MetalManager.BLOCK_MB) // OUT
    }

    /**
      * Which tanks can input
      *
      * @return
      */
    override def getInputTanks: Array[Int] = Array(INPUT_TANK_1, INPUT_TANK_2)

    /**
      * Which tanks can output
      *
      * @return
      */
    override def getOutputTanks: Array[Int] = Array(OUTPUT_TANK)

    /**
      * Called when something happens to the tank, you should mark the block for update here if a tile
      */
    override def onTankChanged(tank: FluidTank): Unit =
    worldObj.notifyBlockUpdate(pos, worldObj.getBlockState(pos), worldObj.getBlockState(pos), 6)
    /**
      * Returns true if the given fluid can be inserted into the given direction.
      *
      * More formally, this should return true if fluid is able to enter from the given direction.
      */
    override def canFill(from: EnumFacing, fluid: Fluid): Boolean = {
        if(fluid == null) return false
        if(isDisabled(from)) return false
        if(tanks(INPUT_TANK_1).getFluid == null)
            return RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer).isValidSingle(new FluidStack(fluid, 1000))
        else if(tanks(INPUT_TANK_2).getFluid == null)
            return RecipeManager.getHandler[AlloyerRecipeHandler](RecipeManager.Alloyer).isValidSingle(new FluidStack(fluid, 1000))
        else {
            if(fluid == tanks(INPUT_TANK_1).getFluid.getFluid || fluid == tanks(INPUT_TANK_2).getFluid.getFluid)
                return true
            else
                return false
        }
        false
    }


    /*******************************************************************************************************************
      ***************************************************** Misc methods ***********************************************
      ******************************************************************************************************************/

    /**
      * Return the container for this tile
      *
      * @param ID Id, probably not needed but could be used for multiple guis
      * @param player The player that is opening the gui
      * @param world The world
      * @param x X Pos
      * @param y Y Pos
      * @param z Z Pos
      * @return The container to open
      */
    override def getServerGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef =
    new ContainerAlloyer(player.inventory, this)

    /**
      * Return the gui for this tile
      *
      * @param ID Id, probably not needed but could be used for multiple guis
      * @param player The player that is opening the gui
      * @param world The world
      * @param x X Pos
      * @param y Y Pos
      * @param z Z Pos
      * @return The gui to open
      */
    override def getClientGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef =
    new GuiAlloyer(player, this)

    override def getDescription : String = {
        "" +
                GuiColor.GREEN + GuiTextFormat.BOLD + GuiTextFormat.UNDERLINE + ClientUtils.translate("neotech.text.stats") + ":\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + ClientUtils.translate("neotech.text.energyUsage") + ":\n" +
                GuiColor.WHITE + "  " + ClientUtils.formatNumber(getEnergyCostPerTick) + " RF/tick\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + ClientUtils.translate("neotech.text.processTime") + ":\n" +
                GuiColor.WHITE + "  " + getCookTime + " ticks\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + ClientUtils.translate("neotech.text.operations") + ":\n" +
                GuiColor.WHITE + "  " + getMultiplierByCategory(IUpgradeItem.ENUM_UPGRADE_CATEGORY.MEMORY) + "\n\n" +
                GuiColor.WHITE + I18n.translateToLocal("neotech.alloyer.desc") + "\n\n" +
                GuiColor.GREEN + GuiTextFormat.BOLD + GuiTextFormat.UNDERLINE + I18n.translateToLocal("neotech.text.upgrade") + ":\n" + GuiTextFormat.RESET +
                GuiColor.YELLOW + GuiTextFormat.BOLD + ClientUtils.translate("neotech.text.processors") + ":\n" +
                GuiColor.WHITE + I18n.translateToLocal("neotech.electricFurnace.processorUpgrade.desc") + "\n\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + I18n.translateToLocal("neotech.text.memory") + ":\n" +
                GuiColor.WHITE + I18n.translateToLocal("neotech.electricFurnace.memoryUpgrade.desc") + "\n\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + I18n.translateToLocal("neotech.text.psu") + ":\n" +
                GuiColor.WHITE + I18n.translateToLocal("neotech.electricFurnace.psuUpgrade.desc") + "\n\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + I18n.translateToLocal("neotech.text.control") + ":\n" +
                GuiColor.WHITE + I18n.translateToLocal("neotech.electricFurnace.controlUpgrade.desc") + "\n\n" +
                GuiColor.YELLOW + GuiTextFormat.BOLD + I18n.translateToLocal("neotech.text.network") + ":\n" +
                GuiColor.WHITE +  I18n.translateToLocal("neotech.electricFurnace.networkUpgrade.desc")
    }

    /**
      * Used to output the redstone single from this structure
      *
      * Use a range from 0 - 16.
      *
      * 0 Usually means that there is nothing in the tile, so take that for lowest level. Like the generator has no energy while
      * 16 is usually the flip side of that. Output 16 when it is totally full and not less
      *
      * @return int range 0 - 16
      */
    override def getRedstoneOutput: Int = if(isActive) 16 else 0

    /**
      * Used to get what particles to spawn. This will be called when the tile is active
      */
    override def spawnActiveParticles(x: Double, y: Double, z: Double): Unit = {
        worldObj.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0, 0, 0)
        worldObj.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0, 0, 0)
        worldObj.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0, 0, 0)
    }
}
