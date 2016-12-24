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

package org.blockartistry.mod.DynSurround.client.handlers;

import java.util.ArrayList;
import java.util.List;

import org.blockartistry.mod.DynSurround.ModLog;
import org.blockartistry.mod.DynSurround.ModOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class EffectManager {

	private static EffectManager INSTANCE = null;
	
	private final List<EffectHandlerBase> effectHandlers = new ArrayList<EffectHandlerBase>();

	private EffectManager() {
	}
	
	private void init() {
		this.effectHandlers.add(new EnvironStateHandler());
		this.effectHandlers.add(new AreaSurveyHandler());
		this.effectHandlers.add(new FogEffectHandler());
		this.effectHandlers.add(new BlockEffectHandler());

		if (ModOptions.blockedSounds.length > 0 || ModOptions.culledSounds.length > 0)
			this.effectHandlers.add(new SoundCullHandler());

		if (ModOptions.enableFootstepSounds)
			this.effectHandlers.add(new FootstepsHandler());

		if (ModOptions.auroraEnable)
			this.effectHandlers.add(new AuroraEffectHandler());

		if (ModOptions.enableBiomeSounds)
			this.effectHandlers.add(new AreaSoundEffectHandler());

		if (ModOptions.suppressPotionParticles)
			this.effectHandlers.add(new PotionParticleScrubHandler());

		if (ModOptions.enableDamagePopoffs)
			this.effectHandlers.add(new PopoffEffectHandler());

		if (ModOptions.enableSpeechBubbles)
			this.effectHandlers.add(new SpeechBubbleHandler());
		
		if(ModOptions.enableEntityEmojis)
			this.effectHandlers.add(new EntityEmojiHandler());

		ModLog.info("Registered client handlers:");
		for (final EffectHandlerBase h : this.effectHandlers) {
			ModLog.info("* %s", h.getHandlerName());
			h.connect0();
			MinecraftForge.EVENT_BUS.register(h);
		}
	}
	
	private void fini() {
		for (final EffectHandlerBase h : this.effectHandlers) {
			MinecraftForge.EVENT_BUS.unregister(h);
			h.disconnect0();
		}
		this.effectHandlers.clear();
	}

	public static void register() {
		INSTANCE = new EffectManager();
		INSTANCE.init();
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}
	
	public static void unregister() {
		MinecraftForge.EVENT_BUS.unregister(INSTANCE);
		INSTANCE.fini();
		INSTANCE = null;
	}

	@SubscribeEvent
	public void clientTick(final TickEvent.ClientTickEvent event) {
		if (Minecraft.getMinecraft().isGamePaused())
			return;

		final World world = FMLClientHandler.instance().getClient().theWorld;
		if (world == null)
			return;

		if (event.phase == Phase.START) {
			final EntityPlayer player = FMLClientHandler.instance().getClient().thePlayer;
			for (final EffectHandlerBase handler : effectHandlers)
				handler.process(world, player);
		}
	}

}