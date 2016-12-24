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

package org.blockartistry.mod.DynSurround.entity;

import javax.annotation.Nonnull;

import org.blockartistry.mod.DynSurround.util.WeightTable;

public final class MessageTable extends WeightTable<MessageTable.MessageTableEntry> {
	
	protected static class MessageTableEntry extends WeightTable.Item {

		protected final String messageId;
		
		public MessageTableEntry(final int weight, @Nonnull final String messageId) {
			super(weight);
			this.messageId = messageId;
		}
		
	}
	
	@Nonnull
	public MessageTable.MessageTableEntry add(final int weight, @Nonnull final String messageId) {
		final MessageTableEntry entry = new MessageTableEntry(weight, messageId);
		this.add(entry);
		return entry;
	}
	
	@Nonnull
	public String getMessage() {
		return this.next().messageId;
	}
}