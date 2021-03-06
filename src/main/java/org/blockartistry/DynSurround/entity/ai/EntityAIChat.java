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

package org.blockartistry.DynSurround.entity.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.blockartistry.DynSurround.DSurround;
import org.blockartistry.DynSurround.ModOptions;
import org.blockartistry.DynSurround.network.Locus;
import org.blockartistry.DynSurround.network.Network;
import org.blockartistry.DynSurround.network.PacketSpeechBubble;
import org.blockartistry.lib.Translations;
import org.blockartistry.lib.WeightTable;
import org.blockartistry.lib.random.XorShiftRandom;

import com.google.common.base.Predicate;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.util.ResourceLocation;

public class EntityAIChat extends EntityAIBase {

	public static final int PRIORITY = 1000;

	protected static final long RESCHEDULE_THRESHOLD = 100;

	private static class EntityChatData {
		public static final int DEFAULT_INTERVAL = 400;
		public static final int DEFAULT_RANDOM = 1200;

		public int baseInterval = DEFAULT_INTERVAL;
		public int baseRandom = DEFAULT_RANDOM;

		public final WeightTable<String> table = new WeightTable<String>();
	}

	private static final Map<String, EntityChatData> messages = new HashMap<String, EntityChatData>();

	private static class WeightTableBuilder implements Predicate<Entry<String, String>> {

		private final Pattern TYPE_PATTERN = Pattern.compile("chat\\.([a-zA-Z.]*)\\.[0-9]*$");
		private final Pattern WEIGHT_PATTERN = Pattern.compile("^([0-9]*),(.*)");

		public WeightTableBuilder() {
		}

		@Override
		public boolean apply(@Nonnull final Entry<String, String> input) {
			final Matcher matcher1 = TYPE_PATTERN.matcher(input.getKey());
			if (matcher1.matches()) {
				final String key = matcher1.group(1).toLowerCase();
				final Matcher matcher2 = WEIGHT_PATTERN.matcher(input.getValue());
				if (matcher2.matches()) {
					EntityChatData data = messages.get(key);
					if (data == null)
						messages.put(key, data = new EntityChatData());
					final int weight = Integer.parseInt(matcher2.group(1));
					final String item = input.getKey();
					data.table.add(new WeightTable.IItem<String>() {
						@Override
						public int getWeight() {
							return weight;
						}

						@Override
						public String getItem() {
							return item;
						}
					});
				} else {
					DSurround.log().warn("Invalid value in language file: %s", input.getValue());
				}
			} else {
				DSurround.log().warn("Invalid key in language file: %s", input.getKey());
			}

			return true;
		}

	}

	@Nonnull
	protected static String getEntityClassName(@Nonnull final Class<? extends EntityLiving> entityClass) {
		final ResourceLocation key = EntityList.getKey(entityClass);
		if (key != null)
			return key.getResourcePath();
		DSurround.log().debug("Can't find entity name for class [%s]", entityClass.getName());
		return "EntityHasNoClass";
	}

	private static void setTimers(@Nonnull final Class<? extends EntityLiving> entity, final int base,
			final int random) {
		setTimers(getEntityClassName(entity), base, random);
	}

	private static void setTimers(@Nonnull final String entity, final int base, final int random) {
		final EntityChatData data = messages.get(entity);
		if (data != null) {
			data.baseInterval = base;
			data.baseRandom = random;
		}
	}

	static {
		final Translations xlate = new Translations();
		xlate.load("/assets/dsurround/data/chat/", Translations.DEFAULT_LANGUAGE);
		xlate.forAll(new WeightTableBuilder());

		setTimers(EntitySquid.class, 600, EntityChatData.DEFAULT_RANDOM);
		setTimers("villager.flee", 250, 200);
	}

	public static boolean hasMessages(@Nonnull final EntityLiving entity) {
		return messages.get(getEntityClassName(entity.getClass())) != null;
	}

	private int getBase() {
		return this.data.baseInterval;
	}

	private int getRandom() {
		return this.data.baseRandom;
	}

	protected final EntityChatData data;
	protected final EntityLiving theEntity;
	protected long nextChat;

	public EntityAIChat(@Nonnull final EntityLiving entity) {
		this(entity, null);
	}

	public EntityAIChat(@Nonnull final EntityLiving entity, @Nullable final String entityName) {
		final String theName = StringUtils.isEmpty(entityName) ? getEntityClassName(entity.getClass()) : entityName;
		this.data = messages.get(theName);
		this.theEntity = entity;
		this.nextChat = getWorldTicks() + getNextChatTime();
		this.setMutexBits(1 << 27);
	}

	protected long getWorldTicks() {
		return this.theEntity.getEntityWorld().getTotalWorldTime();
	}

	protected String getChatMessage() {
		return this.data.table.next();
	}

	protected int getNextChatTime() {
		return getBase() + XorShiftRandom.current().nextInt(getRandom());
	}

	@Override
	public void startExecuting() {
		final Locus point = new Locus(this.theEntity, ModOptions.speechBubbleRange);
		final PacketSpeechBubble packet = new PacketSpeechBubble(this.theEntity, getChatMessage(), true);
		Network.sendToAllAround(point, packet);
		this.nextChat = getWorldTicks() + getNextChatTime();
	}

	@Override
	public boolean shouldExecute() {
		final long delta = this.nextChat - getWorldTicks();
		if (delta <= -RESCHEDULE_THRESHOLD) {
			this.nextChat = getWorldTicks() + getNextChatTime();
			return false;
		}
		return delta <= 0;
	}

}
