/*
 * This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.blockartistry.DynSurround.asm;

import static org.objectweb.asm.Opcodes.*;

import java.util.ListIterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.blockartistry.DynSurround.ModOptions;

public class Transformer implements IClassTransformer {

	private static final Logger logger = LogManager.getLogger("dsurround Transform");

	private static boolean isOneOf(final String src, final String[] candidates) {
		for (final String s : candidates)
			if (src.equals(s))
				return true;
		return false;
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (isOneOf(transformedName, new String[] { "net.minecraft.client.renderer.EntityRenderer", "bqc" })) {
			if (ModOptions.enableWeatherASM) {
				logger.debug("Transforming " + transformedName);
				basicClass = transformEntityRenderer(basicClass);
			}
		} else if (isOneOf(transformedName, new String[] { "net.minecraft.world.WorldServer", "lw" })) {
			if (ModOptions.enableResetOnSleepASM) {
				logger.debug("Transforming " + transformedName);
				basicClass = transformWorldServer(basicClass);
			}
		} else if (isOneOf(transformedName, new String[] { "net.minecraft.client.audio.SoundHandler", "ccp" })) {
			logger.debug("Transforming " + transformedName);
			basicClass = transformSoundHandler(basicClass);
		} else if (isOneOf(transformedName, new String[] { "net.minecraft.client.audio.SoundManager", "ccn" })) {
			if (ModOptions.enableSoundCache) {
				logger.debug("Transforming " + transformedName);
				basicClass = transformSoundManager(basicClass);
			}
		}
		
		if (ModOptions.enableRandomReplace) {
			basicClass = replaceRandom(transformedName, basicClass);
		}

		return basicClass;
	}

	private byte[] transformEntityRenderer(final byte[] classBytes) {
		final String names[] = { "func_78484_h", "addRainParticles" };
		final String targetName = "addRainParticles";

		final ClassReader cr = new ClassReader(classBytes);
		final ClassNode cn = new ClassNode(ASM5);
		cr.accept(cn, 0);

		for (final MethodNode m : cn.methods) {
			if (isOneOf(m.name, names)) {
				logger.debug("Hooking " + m.name);
				final InsnList list = new InsnList();
				list.add(new VarInsnNode(ALOAD, 0));
				final String sig = "(Lnet/minecraft/client/renderer/EntityRenderer;)V";
				list.add(new MethodInsnNode(INVOKESTATIC, "org/blockartistry/DynSurround/client/weather/RenderWeather",
						targetName, sig, false));
				list.add(new InsnNode(RETURN));
				m.instructions.insertBefore(m.instructions.getFirst(), list);
			}
		}

		final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}

	private byte[] transformWorldServer(final byte[] classBytes) {
		final String names[] = { "func_73051_P", "resetRainAndThunder" };
		final String targetName = "resetRainAndThunder";

		final ClassReader cr = new ClassReader(classBytes);
		final ClassNode cn = new ClassNode(ASM5);
		cr.accept(cn, 0);

		for (final MethodNode m : cn.methods) {
			if (isOneOf(m.name, names)) {
				logger.debug("Hooking " + m.name);
				InsnList list = new InsnList();
				list.add(new VarInsnNode(ALOAD, 0));
				final String sig = "(Lnet/minecraft/world/WorldServer;)V";
				list.add(new MethodInsnNode(INVOKESTATIC, "org/blockartistry/DynSurround/server/PlayerSleepHandler",
						targetName, sig, false));
				list.add(new InsnNode(RETURN));
				m.instructions.insertBefore(m.instructions.getFirst(), list);
				break;
			}
		}

		final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}

	private byte[] transformSoundHandler(final byte[] classBytes) {
		final String managerToReplace = "net/minecraft/client/audio/SoundManager";
		final String newManager = "org/blockartistry/DynSurround/client/sound/SoundManagerReplacement";

		final ClassReader cr = new ClassReader(classBytes);
		final ClassNode cn = new ClassNode(ASM5);
		cr.accept(cn, 0);

		for (final MethodNode m : cn.methods) {
			final ListIterator<AbstractInsnNode> itr = m.instructions.iterator();
			boolean foundNew = false;
			while (itr.hasNext()) {
				final AbstractInsnNode node = itr.next();
				if (node.getOpcode() == NEW) {
					final TypeInsnNode theNew = (TypeInsnNode) node;
					if (managerToReplace.equals(theNew.desc)) {
						m.instructions.set(node, new TypeInsnNode(NEW, newManager));
						foundNew = true;
					}
				} else if (node.getOpcode() == INVOKESPECIAL) {
					final MethodInsnNode theInvoke = (MethodInsnNode) node;
					if (managerToReplace.equals(theInvoke.owner)) {
						if (foundNew) {
							m.instructions.set(node, new MethodInsnNode(INVOKESPECIAL, newManager, theInvoke.name,
									theInvoke.desc, false));
							foundNew = false;
						}
					}
				}
			}
		}

		final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}

	private byte[] transformSoundManager(final byte[] classBytes) {
		final String names[] = { "func_148612_a", "getURLForSoundResource" };
		final String targetName = "getURLForSoundResource";

		final ClassReader cr = new ClassReader(classBytes);
		final ClassNode cn = new ClassNode(ASM5);
		cr.accept(cn, 0);

		for (final MethodNode m : cn.methods) {
			if (isOneOf(m.name, names)) {
				logger.debug("Hooking " + m.name);
				InsnList list = new InsnList();
				list.add(new VarInsnNode(ALOAD, 0));
				final String sig = "(Lnet/minecraft/util/ResourceLocation;)Ljava/net/URL;";
				list.add(new MethodInsnNode(INVOKESTATIC, "org/blockartistry/lib/sound/SoundCache", targetName, sig,
						false));
				list.add(new InsnNode(ARETURN));
				m.instructions.insertBefore(m.instructions.getFirst(), list);
				break;
			}
		}

		final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}

	private byte[] replaceRandom(final String name, final byte[] classBytes) {

		final String randomToReplace = "java/util/Random";
		final String newRandom = "org/blockartistry/lib/random/XorShiftRandom";

		boolean madeUpdate = false;

		final ClassReader cr = new ClassReader(classBytes);
		final ClassNode cn = new ClassNode(ASM5);
		cr.accept(cn, 0);

		for (final MethodNode m : cn.methods) {
			final ListIterator<AbstractInsnNode> itr = m.instructions.iterator();
			boolean foundNew = false;
			while (itr.hasNext()) {
				final AbstractInsnNode node = itr.next();
				if (node.getOpcode() == NEW) {
					final TypeInsnNode theNew = (TypeInsnNode) node;
					if (randomToReplace.equals(theNew.desc)) {
						m.instructions.set(node, new TypeInsnNode(NEW, newRandom));
						madeUpdate = true;
						foundNew = true;
					}
				} else if (node.getOpcode() == INVOKESPECIAL) {
					final MethodInsnNode theInvoke = (MethodInsnNode) node;
					if (randomToReplace.equals(theInvoke.owner)) {
						if (foundNew) {
							m.instructions.set(node, new MethodInsnNode(INVOKESPECIAL, newRandom, theInvoke.name,
									theInvoke.desc, false));
							foundNew = false;
						}
					}
				}
			}
		}

		if (madeUpdate) {
			logger.debug("Replaced Random in " + name);
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			cn.accept(cw);
			return cw.toByteArray();
		}
		return classBytes;
	}
}
