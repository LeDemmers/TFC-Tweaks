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

package net.dries007.tfctweaks.util;

import com.dunk.tfc.api.TFCBlocks;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.dries007.tfctweaks.TFCTweaks;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandSaveAll;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.ForgeChunkManager;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;

import static com.dunk.tfc.api.Constant.Global.STONE_FLUXINDEX;
import static net.minecraft.util.EnumChatFormatting.*;

/**
 * @author Dries007
 */
public class WorldExplorer
{
    /**
     * Can't use an AT, because may be overwritten.
     */
    private static final Field CHUNKS_TO_UNLOAD = ReflectionHelper.findField(ChunkProviderServer.class, "field_73248_b", "chunksToUnload");
    private static final Method SAFE_SAVE_CHUNK = ReflectionHelper.findMethod(ChunkProviderServer.class, null, new String[]{"func_73242_b", "safeSaveChunk"}, Chunk.class);
    private static final Method SAFE_SAVE_EXTRA_CHUNK_DATA = ReflectionHelper.findMethod(ChunkProviderServer.class, null, new String[]{"func_73243_a", "safeSaveExtraChunkData"}, Chunk.class);
    private static final CommandSaveAll COMMAND_SAVE_ALL = new CommandSaveAll();
    private static final IChatComponent YES = new ChatComponentText(" Yes").setChatStyle(new ChatStyle().setColor(GREEN));
    private static final IChatComponent NO = new ChatComponentText(" No").setChatStyle(new ChatStyle().setColor(RED));
    public static final List<String> NAMES = ImmutableList.of("Copper", "Gold", "Platinum", "Iron", "Silver", "Tin", "Lead", "Bismuth", "Nickel", "Zinc", "Flux", "Coal", "Kaolinite", "Gypsum", "Graphite", "Kimberlite", "Sulfur", "Jet", "Pitchblende", "Cinnabar", "Cryolite", "Saltpeter", "Sylvite");
    private static final int COPPER = NAMES.indexOf("Copper");
    private static final int GOLD = NAMES.indexOf("Gold");
    private static final int PLATINUM = NAMES.indexOf("Platinum");
    private static final int IRON = NAMES.indexOf("Iron");
    private static final int SILVER = NAMES.indexOf("Silver");
    private static final int TIN = NAMES.indexOf("Tin");
    private static final int LEAD = NAMES.indexOf("Lead");
    private static final int BISMUTH = NAMES.indexOf("Bismuth");
    private static final int NICKEL = NAMES.indexOf("Nickel");
    private static final int ZINC = NAMES.indexOf("Zinc");
    private static final int FLUX = NAMES.indexOf("Flux");
    private static final int COAL = NAMES.indexOf("Coal");
    private static final int KAOLINITE = NAMES.indexOf("Kaolinite");
    private static final int GYPSUM = NAMES.indexOf("Gypsum");
    private static final int GRAPHITE = NAMES.indexOf("Graphite");
    private static final int KIMBERLITE = NAMES.indexOf("Kimberlite");
    private static final int SULFUR = NAMES.indexOf("Sulfur");
    private static final int JET = NAMES.indexOf("Jet");
    private static final int PITCHBLENDE = NAMES.indexOf("Pitchblende");
    private static final int CINNABAR = NAMES.indexOf("Cinnabar");
    private static final int CRYOLITE = NAMES.indexOf("Cryolite");
    private static final int SALTPETER = NAMES.indexOf("Saltpeter");
    private static final int SYLVITE = NAMES.indexOf("Sylvite");

    public static File jsonFile;
    private static WorldExplorer instance;

    public final boolean[] found = new boolean[NAMES.size()];
    public final ICommandSender sender;
    public final int centerX;
    public final int centerZ;
    public final int rad;
    public final int totalSize;
    private int x;
    private int z;
    private int doneChunks = 0;
    private boolean registered;

    private WorldExplorer(ICommandSender sender, int centerX, int centerZ, int rad)
    {
        this.sender = sender;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.rad = rad;
        this.totalSize = rad * rad * 4;
        this.x = centerX - rad;
        this.z = centerZ - rad;
        sender.addChatMessage(new ChatComponentText("Will generate a radius of " + rad + " around " + centerX + ";" + centerZ + " for a total of " + totalSize + " chunks."));
    }

    /**
     * @param centerX IN CHUNK COORDS!
     * @param centerZ IN CHUNK COORDS!
     * @param rad     IN CHUNKS!
     */
    public static void start(ICommandSender sender, int centerX, int centerZ, int rad)
    {
        instance = new WorldExplorer(sender, centerX, centerZ, rad);
        if (!instance.registered)
        {
            FMLCommonHandler.instance().bus().register(instance);
            instance.registered = true;
        }
    }

    public static void stop()
    {
        if (instance.registered)
        {
            instance.registered = false;
            FMLCommonHandler.instance().bus().unregister(instance);
        }
    }

    @SubscribeEvent
    public void tick(ServerTickEvent event)
    {
        registered = true;
        if (event.phase == TickEvent.Phase.START) return;

        final WorldServer world = MinecraftServer.getServer().worldServers[0];
        WorldChunkManager chunkManager = world.getWorldChunkManager();
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        ImmutableSetMultimap<ChunkCoordIntPair, ForgeChunkManager.Ticket> forcedChunks = ForgeChunkManager.getPersistentChunksFor(world);
        final int maxY = world.getHeight();

        try
        {
            for (; x < (centerX + rad); x++)
            {
                for (; z < (centerZ + rad); z++)
                {
                    doneChunks++;

                    {   // gen chunk
                        Chunk chunk = provider.currentChunkLoader.loadChunk(world, x, z);
                        if (chunk == null) chunk = provider.originalLoadChunk(x, z);
                        while (!chunk.isTerrainPopulated)
                        {
                            try
                            {
                                provider.populate(provider, x, z);
                                break; // done
                            }
                            catch (ConcurrentModificationException e)
                            {
                                e.printStackTrace();
                                // try again
                            }
                            catch (RuntimeException e)
                            {
                                e.printStackTrace();
                                break; // Skip
                            }
                        }

                        // Ore check
                        for (int y = 0; y < maxY; y += 8)
                        {
                            Block block = chunk.getBlock(8, y, 8);
                            TFCTweaks.log.trace(block);
                            if (block == TFCBlocks.ore || block == TFCBlocks.ore2 || block == TFCBlocks.ore3)
                            {
                                int meta = chunk.getBlockMetadata(8, y, 8);
                                int dropped = block.damageDropped(meta);
                                int id = getID(meta, dropped);
                                if (id != -1) found[id] = true;
                                //else TFCTweaks.log.info("Found ore with -1: {}, meta: {}, dropped: {}", block, meta, dropped);
                            }
                            else if (block == TFCBlocks.stoneSed || block == TFCBlocks.stoneMM)
                            {
                                int meta = chunk.getBlockMetadata(8, y, 8);
                                for (int flux : STONE_FLUXINDEX)
                                {
                                    if (flux == meta)
                                    {
                                        found[FLUX] = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (doneChunks % 100 == 0)
                    {
                        sender.addChatMessage(new ChatComponentText(String.format("%d out of %d done (%02.2f%%)", doneChunks, totalSize, ((double) doneChunks / totalSize) * 100.0)));

                        //noinspection unchecked
                        for (Chunk chunk : (List<Chunk>) ImmutableList.copyOf(provider.loadedChunks))
                        {
                            if (forcedChunks.containsKey(new ChunkCoordIntPair(chunk.xPosition, chunk.zPosition))) continue;
                            long key = ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition);
                            ((Set) CHUNKS_TO_UNLOAD.get(provider)).remove(key);
                            provider.loadedChunkHashMap.remove(key);
                            provider.loadedChunks.remove(chunk);
                            chunk.onChunkUnload();

                            SAFE_SAVE_CHUNK.invoke(provider, chunk);
                            SAFE_SAVE_EXTRA_CHUNK_DATA.invoke(provider, chunk);
                        }
                    }

                    if (doneChunks % 1000 == 0)
                    {
                        COMMAND_SAVE_ALL.processCommand(MinecraftServer.getServer(), new String[]{"flush"});
                        chunkManager.cleanupCache();
                        return;
                    }

                }
                // reset Z back to start value
                z = centerZ - rad;
            }
            provider.unloadQueuedChunks();
            COMMAND_SAVE_ALL.processCommand(MinecraftServer.getServer(), new String[]{"flush"});
            stop();

            FileUtils.writeStringToFile(jsonFile, Constants.GSON.toJson(this));

            sender.addChatMessage(new ChatComponentText("Found the following ores: ").setChatStyle(new ChatStyle().setColor(AQUA)));
            for (int i = 0; i < found.length; i++) sender.addChatMessage(new ChatComponentText(NAMES.get(i)).appendSibling(found[i] ? YES : NO));
        }
        catch (IOException | IllegalAccessException | InvocationTargetException e)
        {
            sender.addChatMessage(new ChatComponentText(e.getMessage()));
            e.printStackTrace();
            stop();
        }
    }

    private int getID(int meta, int dropped)
    {
        switch (dropped)
        {
            default:
                return -1;
            // From BlockOre
            // "Native Copper", "Native Gold", "Native Platinum", "Hematite","Native Silver", "Cassiterite", "Galena", "Bismuthinite", "Garnierite", "Malachite", "Magnetite", "Limonite", "Sphalerite", "Tetrahedrite", "Bituminous Coal", "Lignite"
            case 0:
                return (meta == 14 || meta == 15) ? COAL : COPPER; // the damagedropped returns 0 for coal
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return dropped;
            case 9:
            case 13:
                return COPPER;
            case 10:
            case 11:
                return IRON;
            case 12:
                return ZINC;
            // 14 & 15 Coal
            // From BlockOre2
            case 16:
                return KAOLINITE;
            case 17:
                return GYPSUM;
            // 18 Satinspar
            case 19:
                return GRAPHITE;
            case 20:
                return KIMBERLITE;
            // 21 Petrified Wood
            case 22:
                return SULFUR;
            case 23:
                return JET;
            // 24 Microcline
            case 25:
                return PITCHBLENDE;
            case 26:
                return CINNABAR;
            case 27:
                return CRYOLITE;
            case 28:
                return SALTPETER;
            // 29 Serpentine
            case 30:
                return SYLVITE;
        }
    }
}
