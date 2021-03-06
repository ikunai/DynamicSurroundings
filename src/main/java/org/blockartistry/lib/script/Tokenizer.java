/*
 * Copyright 2012 Udo Klimaschewski
 * Copyright 2016 OreCruncher
 * 
 * http://UdoJava.com/
 * http://about.me/udo.klimaschewski
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

// Sourced from: https://github.com/uklimaschewski/EvalEx

package org.blockartistry.lib.script;

import java.util.Iterator;
import java.util.Set;

/**
 * Expression tokenizer that allows to iterate over a {@link String}
 * expression token by token. Blank characters will be skipped.
 */
public final class Tokenizer implements Iterator<String> {

	/**
	 * What character to use for decimal separators.
	 */
	public static final char decimalSeparator = '.';

	/**
	 * What character to use for minus sign (negative values).
	 */
	public static final char minusSign = '-';

	/**
	 * What character to use for enclosing a string literal
	 */
	public static final char quote = '\'';
	
	/**
	 * What character to use for the not
	 */
	public static final char bang = '!';

	/**
	 * Actual position in expression string.
	 */
	private int pos = 0;

	/**
	 * The original input expression.
	 */
	private String input;
	/**
	 * The previous token or <code>null</code> if none.
	 */
	private String previousToken;
	
	/**
	 * List of operators for the engine
	 */
	private Set<String> operators;

	/**
	 * Creates a new tokenizer for an expression.
	 * 
	 * @param input
	 *            The expression string.
	 */
	public Tokenizer(final String input, final Set<String> operators) {
		this.input = input.trim();
		this.operators = operators;
	}

	@Override
	public boolean hasNext() {
		return (pos < input.length());
	}

	/**
	 * Peek at the next character, without advancing the iterator.
	 * 
	 * @return The next character or character 0, if at end of string.
	 */
	private char peekNextChar() {
		if (pos < (input.length() - 1)) {
			return input.charAt(pos + 1);
		} else {
			return 0;
		}
	}

	@Override
	public String next() {
		StringBuilder token = new StringBuilder();
		if (pos >= input.length()) {
			return previousToken = null;
		}
		char ch = input.charAt(pos);
		while (Character.isWhitespace(ch) && pos < input.length()) {
			ch = input.charAt(++pos);
		}
		if (Character.isDigit(ch)) {
			while ((Character.isDigit(ch) || ch == decimalSeparator || ch == 'e' || ch == 'E'
					|| (ch == minusSign && token.length() > 0
							&& ('e' == token.charAt(token.length() - 1) || 'E' == token.charAt(token.length() - 1)))
					|| (ch == '+' && token.length() > 0 && ('e' == token.charAt(token.length() - 1)
							|| 'E' == token.charAt(token.length() - 1))))
					&& (pos < input.length())) {
				token.append(input.charAt(pos++));
				ch = pos == input.length() ? 0 : input.charAt(pos);
			}
		} else if (ch == minusSign && Character.isDigit(peekNextChar()) && ("(".equals(previousToken)
				|| ",".equals(previousToken) || previousToken == null || operators.contains(previousToken))) {
			token.append(minusSign);
			pos++;
			token.append(next());
		} else if (Character.isLetter(ch) || (ch == '_')) {
			while ((Character.isLetter(ch) || Character.isDigit(ch) || (ch == '_') || (ch == '.'))
					&& (pos < input.length())) {
				token.append(input.charAt(pos++));
				ch = pos == input.length() ? 0 : input.charAt(pos);
			}
		} else if (ch == '(' || ch == ')' || ch == ',') {
			token.append(ch);
			pos++;
		} else if (ch == quote) {
			token.append(ch);
			pos++;
			ch = pos == input.length() ? 0 : input.charAt(pos);
			while (ch != quote && (pos < input.length())) {
				token.append(input.charAt(pos++));
				ch = pos == input.length() ? 0 : input.charAt(pos);
			}
			if (ch == 0)
				throw new ExpressionException("String not terminated '" + token + "'");
			token.append(ch);
			pos++;
		} else {
			while (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_' && !Character.isWhitespace(ch)
					&& ch != '(' && ch != ')' && ch != ',' && (pos < input.length())) {
				token.append(input.charAt(pos));
				pos++;
				ch = pos == input.length() ? 0 : input.charAt(pos);
				if (ch == minusSign || ch == bang) {
					break;
				}
			}
			if (!operators.contains(token.toString())) {
				throw new ExpressionException(
						"Unknown operator '" + token + "' at position " + (pos - token.length() + 1));
			}
		}
		return previousToken = token.toString();
	}

	@Override
	public void remove() {
		throw new ExpressionException("remove() not supported");
	}

	/**
	 * Get the actual character position in the string.
	 * 
	 * @return The actual character position.
	 */
	public int getPos() {
		return pos;
	}

}

