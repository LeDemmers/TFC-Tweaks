/*
 * Copyright (c) 2015 Dries007
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted (subject to the limitations in the
 * disclaimer below) provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 *  * Neither the name of Dries007 nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE
 * GRANTED BY THIS LICENSE.  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.dries007.tfctweaks;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICrashCallable;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.dries007.tfctweaks.asm.FluidContainerRegistryCT;
import net.dries007.tfctweaks.asm.FluidRegistryCT;
import net.dries007.tfctweaks.asm.TFCTweaksLoadingPlugin;
import net.dries007.tfctweaks.cmd.CmdWorldExplorer;
import net.dries007.tfctweaks.util.FluidHacks;
import net.dries007.tfctweaks.util.Helper;
import net.dries007.tfctweaks.util.OreDictionaryArmorDyeRecipe;
import net.dries007.tfctweaks.util.WorldExplorer;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.RecipeSorter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static net.dries007.tfctweaks.util.Constants.*;
import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

/**
 * @author Dries007
 */
@Mod(modid = MODID, useMetadata = false, dependencies = "required-after:" + TFC)
public class TFCTweaks
{
    public static Logger log;

    @Mod.Instance(MODID)
    public static TFCTweaks instance;

    private Configuration cfg;

    private boolean autoPregen_enabled = false;
    private int autoPregen_size = 1000;

    @Mod.EventHandler
    public void construction(FMLConstructionEvent e)
    {
        if (!DISABLE_LOADORDER)
        {
            Helper.doLoadOrderHaxing();
            FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable()
            {
                public String call() throws Exception
                {
                    return "PLEASE TRY TO START THE GAME AT LEAST TWICE. The load order modifications we do only kick in after a reload.";
                }

                public String getLabel()
                {
                    return MODID;
                }
            });
        }

        FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable()
        {
            public String call() throws Exception
            {
                return String.format("FluidContainerRegistryCT: %d / %d; FluidRegistryCT: %d / %d", FluidContainerRegistryCT.done, FluidContainerRegistryCT.DONE, FluidRegistryCT.done, FluidRegistryCT.DONE);
            }

            public String getLabel()
            {
                return MODID + "-ASM";
            }
        });
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        log = event.getModLog();

        FMLCommonHandler.instance().bus().register(EventHandlers.I);
        MinecraftForge.EVENT_BUS.register(EventHandlers.I);

        cfg = new Configuration(event.getSuggestedConfigurationFile());
        doConfig(cfg);

        if (TFCTweaksLoadingPlugin.DISABLE_ASM)
        {
            log.warn("********************************************************************************************");
            log.warn("          You have disabled ASM tweaks. All FluidHacks have been disabled.");
            log.warn("Please don't include this in your pack config. This is to be considered a DEBUG option only!");
            log.warn("********************************************************************************************");
        }
        else
        {
            FluidHacks.doTheMagic();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        //noinspection unchecked
        CraftingManager.getInstance().getRecipeList().add(OreDictionaryArmorDyeRecipe.INSTANCE);
        RecipeSorter.register(MODID + ":armordyes", OreDictionaryArmorDyeRecipe.class, RecipeSorter.Category.SHAPELESS, "before:minecraft:armordyes");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CmdWorldExplorer());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event)
    {
        try
        {
            FileUtils.writeStringToFile(new File(DimensionManager.getCurrentSaveRootDirectory(), "seed.txt"), String.valueOf(DimensionManager.getWorld(0).getSeed()));
            WorldExplorer.jsonFile = new File(DimensionManager.getCurrentSaveRootDirectory(), "WorldExplorer.json");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        if (autoPregen_enabled && !WorldExplorer.jsonFile.exists())
        {
            ChunkCoordinates spawn = MinecraftServer.getServer().worldServers[0].getSpawnPoint();
            WorldExplorer.start(MinecraftServer.getServer(), spawn.posX >> 4, spawn.posZ >> 4, autoPregen_size / 16);
        }
    }

    @SubscribeEvent
    public void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent eventArgs)
    {
        doConfig(cfg);
    }

    private void doConfig(Configuration cfg)
    {
        EventHandlers.maxAge = cfg.getInt("maxAge", CATEGORY_GENERAL, 6000, 0, Integer.MAX_VALUE, "Despawn time of all items, (except if mods override this themselves). 0 is forever, be careful with that one. Setting it lower then 6000 (5 minutes) has no effect.");
        EventHandlers.fuelOnFireMaxAge = cfg.getInt("fuelOnFireMaxAge", CATEGORY_GENERAL, 6000, 0, Integer.MAX_VALUE, "Despawn time of fuel thrown on a firepit in ticks. Setting it lower then 6000 (5 minutes) has no effect. 0 will extend the lifetime infinitely.");
        EventHandlers.stackOnPickup = cfg.getBoolean("stackOnPickup", CATEGORY_GENERAL, false, "Auto-stack food together on pickup.");
        EventHandlers.disableZombieFlesh = cfg.getBoolean("disableZombieFlesh", CATEGORY_GENERAL, false, "Disable rotten flesh drops");
        EventHandlers.disableSpiderEye = cfg.getBoolean("disableSpiderEye", CATEGORY_GENERAL, false, "Disable spider eye drops");

        cfg.getCategory(CATEGORY_GENERAL + ".autopregen").setRequiresWorldRestart(true);
        autoPregen_enabled = cfg.getBoolean("enabled", CATEGORY_GENERAL + ".autopregen", autoPregen_enabled, "Enable the automatic pregeneration of the world once the server starts. Only happens when WorldExplorer.json doesn't exist in the world folder.");
        autoPregen_size = cfg.getInt("size", CATEGORY_GENERAL + ".autopregen", autoPregen_size, 0, Integer.MAX_VALUE, "The size, in blocks, of the autoPregen.");

        if (!TFCTweaksLoadingPlugin.DISABLE_ASM)
        {
            cfg.addCustomCategoryComment(CATEGORY_GENERAL + ".fluidhacks", "Experimental.");
            cfg.getCategory(CATEGORY_GENERAL + ".fluidhacks").setRequiresMcRestart(true);
            FluidHacks.makeAllWaterFTCWater = cfg.getBoolean("makeAllWaterFTCWater", CATEGORY_GENERAL + ".fluidhacks", false, "Override the vanilla water with TFC's fresh water.");
            FluidHacks.makeAllLavaFTCLava = cfg.getBoolean("makeAllLavaFTCLava", CATEGORY_GENERAL + ".fluidhacks", false, "Override the vanilla lava with TFC's fresh lava.");
        }

        if (cfg.hasChanged()) cfg.save();
    }
}
