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

package org.blockartistry.mod.DynSurround.client.gui;

import org.blockartistry.mod.DynSurround.DSurround;
import org.blockartistry.mod.DynSurround.ModOptions;
import org.blockartistry.mod.DynSurround.util.Color;
import org.blockartistry.mod.DynSurround.util.Localization;

import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiPageButtonList.GuiResponder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.gui.GuiSlider.FormatHelper;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.config.Configuration;

public class VolumeControlGui extends GuiScreen implements GuiResponder {

	private static final FormatHelper FORMAT = new FormatHelper() {
		@Override
		public String getText(int id, String name, float value) {
			return Localization.format("dlg.format.Display", name, (int) (value * 100));
		}
	};
	
	private static final int ID_MASTER_SOUND = 1;
	private static final int ID_BIOME_SOUND = 2;
	private static final int ID_FOOTSTEP_SOUND = 3;
	private static final int ID_LABEL = 4;

	protected final Configuration config = DSurround.config();
	protected final Minecraft mc = Minecraft.getMinecraft();

	protected float master = mc.gameSettings.getSoundLevel(SoundCategory.MASTER);
	protected float sound = ModOptions.soundLevel;
	protected float footstep = ModOptions.footstepsSoundFactor;

	@Override
	public void initGui() {
		final int drawX = (this.width + 1) / 2 - 75;
		final int drawY = 40;
		addButton(new GuiSlider(this, ID_MASTER_SOUND, drawX, drawY, "dlg.name.MasterSound", 0F, 1F, this.master, FORMAT));
		addButton(new GuiSlider(this, ID_BIOME_SOUND, drawX, drawY + 25, "dlg.name.BiomeSound", 0F, 1F, this.sound, FORMAT));
		addButton(new GuiSlider(this, ID_FOOTSTEP_SOUND, drawX, drawY + 50, "dlg.name.FootstepSound", 0F, 1F, this.footstep, FORMAT));

		final GuiLabel label = new GuiLabel(mc.fontRendererObj, ID_LABEL, drawX, drawY + 75, 150, 10, Color.MC_WHITE.rgb());
		label.setCentered().addLine(Localization.format("dlg.name.Close"));
		this.labelList.add(label);
	}

	@Override
	public void setEntryValue(int id, boolean value) {
	}

	@Override
	public void setEntryValue(int id, float value) {
		switch (id) {
		case ID_MASTER_SOUND:
			this.master = value;
			break;
		case ID_BIOME_SOUND:
			this.sound = value;
			break;
		case ID_FOOTSTEP_SOUND:
			this.footstep = value;
			break;
		}
	}

	@Override
	public void setEntryValue(int id, String value) {

	}

	@Override
	public void onGuiClosed() {
		this.mc.gameSettings.setSoundLevel(SoundCategory.MASTER, this.master);
		this.mc.gameSettings.saveOptions();

		ModOptions.soundLevel = this.sound;
		ModOptions.footstepsSoundFactor = this.footstep;
		this.config.save();
	}

}