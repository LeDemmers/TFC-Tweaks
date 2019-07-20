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

package net.dries007.tfctweaks.asm;

import com.dunk.tfc.api.TFCFluids;
import com.google.common.collect.BiMap;
import cpw.mods.fml.common.FMLLog;
import net.dries007.tfctweaks.util.FluidHacks;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author Dries007
 */
public class FluidRegistryCT implements IClassTransformer
{
    public static final int DONE = 6; // water register + lava register + getFluid + isFluidRegistered(fluid) + isFluidRegistered(string) + getFluidStack
    public static int done = 0;

    @Override
    public byte[] transform(String originalName, String transformedName, byte[] bytes)
    {
        if (originalName.equals("net.minecraftforge.fluids.FluidRegistry")) return magic(bytes);
        return bytes;
    }

    private byte[] magic(byte[] bytes)
    {
        FMLLog.info("Found the FluidRegistry class...");
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        for (MethodNode m : classNode.methods)
        {
            if (m.name.equals("<clinit>") && m.desc.equals("()V"))
            {
                FMLLog.info("Found the <clinit> method...");

                ListIterator<AbstractInsnNode> i = m.instructions.iterator();
                while (i.hasNext())
                {
                    AbstractInsnNode node = i.next();
                    if (!(node instanceof FieldInsnNode) || node.getOpcode() != GETSTATIC) continue;
                    FieldInsnNode fieldInsnNode = ((FieldInsnNode) node);
                    if (!fieldInsnNode.owner.equals("net/minecraftforge/fluids/FluidRegistry")) continue;
                    if (!fieldInsnNode.name.equals("WATER") && !fieldInsnNode.name.equals("LAVA")) continue;
                    if (!fieldInsnNode.desc.equals("Lnet/minecraftforge/fluids/Fluid;")) continue;
                    node = i.next();
                    if (!(node instanceof MethodInsnNode) || node.getOpcode() != INVOKESTATIC) continue;
                    MethodInsnNode methodInsnNode = ((MethodInsnNode) node);
                    if (!methodInsnNode.owner.equals("net/minecraftforge/fluids/FluidRegistry")) continue;
                    if (!methodInsnNode.name.equals("registerFluid")) continue;
                    if (!methodInsnNode.desc.equals("(Lnet/minecraftforge/fluids/Fluid;)Z")) continue;
                    node = i.next();
                    if (!(node instanceof InsnNode) || node.getOpcode() != POP) continue;
                    InsnNode insnNode = ((InsnNode) node);
                    m.instructions.remove(fieldInsnNode);
                    m.instructions.remove(methodInsnNode);
                    m.instructions.remove(insnNode);
                    FMLLog.info("[FluidRegistryCT] Removed the " + fieldInsnNode.name + " registration.");
                    done++;
                }
            }
            else if (m.name.equals("getFluid") && m.desc.equals("(Ljava/lang/String;)Lnet/minecraftforge/fluids/Fluid;"))
            {
                FMLLog.info("Found the getFluid method...");
                InsnList insnList = new InsnList();
                {
                    LabelNode labelFirstIf = new LabelNode();
                    insnList.add(new FieldInsnNode(GETSTATIC, "net/dries007/tfctweaks/util/FluidHacks", "makeAllWaterFTCWater", "Z"));
                    insnList.add(new JumpInsnNode(IFEQ, labelFirstIf));
                    insnList.add(new VarInsnNode(ALOAD, 0));
                    insnList.add(new LdcInsnNode("water"));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
                    insnList.add(new JumpInsnNode(IFEQ, labelFirstIf));
                    insnList.add(new FieldInsnNode(GETSTATIC, "com/bioxx/tfc/api/TFCFluids", "FRESHWATER", "Lnet/minecraftforge/fluids/Fluid;"));
                    insnList.add(new InsnNode(ARETURN));
                    insnList.add(labelFirstIf);
                }
                {
                    LabelNode lableSecondIf = new LabelNode();
                    insnList.add(new FieldInsnNode(GETSTATIC, "net/dries007/tfctweaks/util/FluidHacks", "makeAllLavaFTCLava", "Z"));
                    insnList.add(new JumpInsnNode(IFEQ, lableSecondIf));
                    insnList.add(new VarInsnNode(ALOAD, 0));
                    insnList.add(new LdcInsnNode("lava"));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
                    insnList.add(new JumpInsnNode(IFEQ, lableSecondIf));
                    insnList.add(new FieldInsnNode(GETSTATIC, "com/bioxx/tfc/api/TFCFluids", "LAVA", "Lnet/minecraftforge/fluids/Fluid;"));
                    insnList.add(new InsnNode(ARETURN));
                    insnList.add(lableSecondIf);
                }
                m.instructions.insertBefore(m.instructions.getFirst(), insnList);

                FMLLog.info("[FluidRegistryCT] Edited getFluid(String) : Fluid.");
                done++;
            }
            else if (m.name.equals("isFluidRegistered"))
            {
                if (m.desc.equals("(Lnet/minecraftforge/fluids/Fluid;)Z"))
                {
                    InsnList insnList = new InsnList();
                    LabelNode falseLabel = new LabelNode();
                    LabelNode trueLabel = new LabelNode();
                    insnList.add(new VarInsnNode(ALOAD, 0));
                    insnList.add(new JumpInsnNode(IFNULL, falseLabel));
                    insnList.add(new VarInsnNode(ALOAD, 0));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraftforge/fluids/Fluid", "getName", "()Ljava/lang/String;", false));
                    insnList.add(new MethodInsnNode(INVOKESTATIC, "net/minecraftforge/fluids/FluidRegistry", "isFluidRegistered", "(Ljava/lang/String;)Z", false));
                    insnList.add(new JumpInsnNode(IFEQ, falseLabel));
                    insnList.add(new InsnNode(ICONST_1));
                    insnList.add(new JumpInsnNode(GOTO, trueLabel));
                    insnList.add(falseLabel);
                    insnList.add(new InsnNode(ICONST_0));
                    insnList.add(trueLabel);
                    insnList.add(new InsnNode(IRETURN));
                    // replace entire method
                    m.instructions.clear();
                    m.instructions.add(insnList);

                    FMLLog.info("[FluidRegistryCT] Edited isFluidRegistered(Fluid) : bool.");
                    done++;
                }
                else if (m.desc.equals("(Ljava/lang/String;)Z"))
                {
                    InsnList insnList = new InsnList();
                    LabelNode trueLabel = new LabelNode();
                    insnList.add(new LdcInsnNode("water"));
                    insnList.add(new VarInsnNode(ALOAD, 0));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
                    insnList.add(new JumpInsnNode(IFNE, trueLabel));
                    insnList.add(new LdcInsnNode("lava"));
                    insnList.add(new VarInsnNode(ALOAD, 0));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
                    insnList.add(new JumpInsnNode(IFNE, trueLabel));
                    insnList.add(new FieldInsnNode(GETSTATIC, "net/minecraftforge/fluids/FluidRegistry", "fluids", "Lcom/google/common/collect/BiMap;"));
                    insnList.add(new VarInsnNode(ALOAD, 0));
                    insnList.add(new MethodInsnNode(INVOKEINTERFACE, "com/google/common/collect/BiMap", "containsKey", "(Ljava/lang/Object;)Z", true));
                    LabelNode falseLabel = new LabelNode();
                    insnList.add(new JumpInsnNode(IFEQ, falseLabel));
                    insnList.add(trueLabel);
                    insnList.add(new InsnNode(ICONST_1));
                    LabelNode returnLabel = new LabelNode();
                    insnList.add(new JumpInsnNode(GOTO, returnLabel));
                    insnList.add(falseLabel);
                    insnList.add(new InsnNode(ICONST_0));
                    insnList.add(returnLabel);
                    insnList.add(new InsnNode(IRETURN));

                    // replace entire method
                    m.instructions.clear();
                    m.instructions.add(insnList);

                    FMLLog.info("[FluidRegistryCT] Edited isFluidRegistered(String) : bool.");
                    done++;
                }
            }
            else if (m.name.equals("getFluidStack"))
            {
                LabelNode notNullNode = null;
                { // Grab first jump node label
                    ListIterator<AbstractInsnNode> i = m.instructions.iterator();
                    while (i.hasNext())
                    {
                        AbstractInsnNode node = i.next();
                        if (node.getOpcode() == IFNE)
                        {
                            notNullNode = ((JumpInsnNode) node).label;
                            break;
                        }
                    }
                }

                InsnList insnList = new InsnList();
                insnList.add(new LdcInsnNode("water"));
                insnList.add(new VarInsnNode(ALOAD, 0));
                insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
                insnList.add(new JumpInsnNode(IFNE, notNullNode));
                insnList.add(new LdcInsnNode("lava"));
                insnList.add(new VarInsnNode(ALOAD, 0));
                insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
                insnList.add(new JumpInsnNode(IFNE, notNullNode));

                // add to the beginning of the list, leave the rest of the method in place.
                m.instructions.insert(insnList);

                FMLLog.info("[FluidRegistryCT] Edited getFluidStack(String, int) : FluidStack.");
                done++;
            }
        }

        if (done != DONE)
        {
            FMLLog.severe("\n######################################################################################\n" +
                    "######################################################################################\n" +
                    "######################################################################################\n" +
                    "OUR ASM FLUID HACK FAILED! PLEASE MAKE AN ISSUE REPORT ON GITHUB WITH A COMPLETE MODLIST! https://github.com/dries007/TFC-Tweaks\n" +
                    "Done %d out of %d ASM tweaks on class FluidRegistry\n" +
                    "########################################################################################\n" +
                    "########################################################################################\n" +
                    "########################################################################################\n\n", done, DONE);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /*
     * Dummy methods
     */
    static BiMap<String, Fluid> fluids = null;

    /*
    ORIGINAL:
        GETSTATIC net/minecraftforge/fluids/FluidRegistry.fluids : Lcom/google/common/collect/BiMap;
        ALOAD 0
        INVOKEINTERFACE com/google/common/collect/BiMap.get (Ljava/lang/Object;)Ljava/lang/Object;
        CHECKCAST net/minecraftforge/fluids/Fluid
        ARETURN
     */
    @SuppressWarnings("ALL")
    private static Fluid getFluid(String fluidName)
    {
        if (FluidHacks.makeAllWaterFTCWater && fluidName.equals("water")) return TFCFluids.FRESHWATER;
        else if (FluidHacks.makeAllLavaFTCLava && fluidName.equals("lava")) return TFCFluids.LAVA;

        return fluids.get(fluidName);
    }

    /*
    ORIGINAL:
        ALOAD 0
        IFNULL L0
        GETSTATIC net/minecraftforge/fluids/FluidRegistry.fluids : Lcom/google/common/collect/BiMap;
        ALOAD 0
        INVOKEVIRTUAL net/minecraftforge/fluids/Fluid.getName ()Ljava/lang/String;
        INVOKEINTERFACE com/google/common/collect/BiMap.containsKey (Ljava/lang/Object;)Z
        IFEQ L0
        ICONST_1
        GOTO L1
        L0
        ICONST_0
        L1
        IRETURN
     */
    @SuppressWarnings("ALL")
    public static boolean isFluidRegistered(Fluid fluid)
    {
        return fluid != null && FluidRegistry.isFluidRegistered(fluid.getName());
    }

    /*
    ORIGINAL:
        GETSTATIC net/minecraftforge/fluids/FluidRegistry.fluids : Lcom/google/common/collect/BiMap;
        ALOAD 0
        INVOKEINTERFACE com/google/common/collect/BiMap.containsKey (Ljava/lang/Object;)Z
        IRETURN
     */
    @SuppressWarnings("ALL")
    public static boolean isFluidRegistered(String fluidName)
    {
        return "water".equals(fluidName) || "lava".equals(fluidName) || fluids.containsKey(fluidName);
    }

    /*
    ORIGINAL:
       L0
        LINENUMBER 270 L0
        GETSTATIC net/dries007/tfctweaks/asm/FluidRegistryCT.fluids : Lcom/google/common/collect/BiMap;
        ALOAD 0
        INVOKEINTERFACE com/google/common/collect/BiMap.containsKey (Ljava/lang/Object;)Z
        IFNE L1
       L2
        LINENUMBER 272 L2
        ACONST_NULL
        ARETURN
       L1
        LINENUMBER 274 L1
       FRAME SAME
        NEW net/minecraftforge/fluids/FluidStack
        DUP
        ALOAD 0
        INVOKESTATIC net/dries007/tfctweaks/asm/FluidRegistryCT.getFluid (Ljava/lang/String;)Lnet/minecraftforge/fluids/Fluid;
        ILOAD 1
        INVOKESPECIAL net/minecraftforge/fluids/FluidStack.<init> (Lnet/minecraftforge/fluids/Fluid;I)V
        ARETURN
       L3
     */
    @SuppressWarnings("ALL")
    public static FluidStack getFluidStack(String fluidName, int amount)
    {
        if (!("water".equals(fluidName) || "lava".equals(fluidName) || fluids.containsKey(fluidName)))
        {
            return null;
        }
        return new FluidStack(getFluid(fluidName), amount);
    }
}
