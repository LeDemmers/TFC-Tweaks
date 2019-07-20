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

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Type;

/**
 * @author Dries007
 */
public class Constants
{
    public static final boolean DISABLE_LOADORDER = System.getProperties().containsKey("net.dries007.tfctweaks.loadorder.disable");
    public static final String MODID = "TFC-Tweaks";
    public static final String TFC = "terrafirmacraftplus";
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().registerTypeHierarchyAdapter(WorldExplorer.class, new WorldExplorerJson()).create();

    private static final class WorldExplorerJson implements JsonSerializer<WorldExplorer>
    {
        @Override
        public JsonElement serialize(WorldExplorer src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject object = new JsonObject();

            object.addProperty("seed", MinecraftServer.getServer().worldServers[0].getSeed());
            object.addProperty("commandSender", src.sender.getCommandSenderName());
            object.addProperty("centerX", src.centerX);
            object.addProperty("centerZ", src.centerZ);
            object.addProperty("rad", src.rad);
            object.addProperty("totalSize", src.totalSize);

            JsonObject names = new JsonObject();
            for (int i = 0; i < src.found.length; i++) names.addProperty(WorldExplorer.NAMES.get(i), src.found[i]);
            object.add("minerals", names);

            return object;
        }
    }
}
