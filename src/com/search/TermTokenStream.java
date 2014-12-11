/**
 * 
 */
package com.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

/**
 * This class represents a stream of tokens as the name suggests. It wraps the
 * token stream and provides utility methods to manipulate it
 * 
 * @author nikhillo
 * 
 */
public class TermTokenStream implements Iterator<String> {
	public LinkedList<String> stream = new LinkedList<String>();
	public Integer current = -1;
	public Integer prev = -1;

	/**
	 * Default constructor
	 * 
	 * @param bldr
	 *            : THe stringbuilder to seed the stream
	 */
	public TermTokenStream(StringBuilder bldr) {
		// TODO: Implement this method
		if (!(bldr == null || bldr.toString().equals(""))) {
			stream.add(bldr.toString().trim());
		}
		current = 0;
		prev = -1;
	}

	/**
	 * Overloaded constructor
	 * 
	 * @param bldr
	 *            : THe stringbuilder to seed the stream
	 */
	public TermTokenStream(String string) {
		// TODO: Implement this method
		if (!(string == null || string.equals(""))) {
			stream.add(string.trim());
		}
		current = 0;
		prev = -1;
	}

	/**
	 * Method to append tokens to the stream
	 * 
	 * @param tokens
	 *            : The tokens to be appended
	 */
	public void append(String... tokens) {
		// TODO: Implement this method
		if ((tokens != null) && tokens.length > 0 && !tokens.equals("")) {
			for (String currentToken : tokens) {
				if (!(currentToken == null || currentToken == "")) {
					stream.add(currentToken.trim());
				}
			}
		}
	}

	/**
	 * Method to retrieve a map of token to count mapping This map should
	 * contain the unique set of tokens as keys The values should be the number
	 * of occurrences of the token in the given stream
	 * 
	 * @return The map as described above, no restrictions on ordering
	 *         applicable
	 */
	public Map<String, Integer> getTokenMap() {
		Iterator<String> itrAllTokens = stream.iterator();
		HashMap<String, Integer> termCountMap = new HashMap<String, Integer>();
		while (itrAllTokens.hasNext()) {
			String term = itrAllTokens.next();
			if (!(term.equals("") || term == null)) {
				if (termCountMap.containsKey(term)) {
					int termCount = termCountMap.get(term);
					termCount += 1;
					termCountMap.put(term, termCount);
				} else {
					termCountMap.put(term, 1);
				}
			}
		}
		if (termCountMap.size() > 0)
			return termCountMap;
		else
			return null;
	}

	/**
	 * Method to get the underlying token stream as a collection of tokens
	 * 
	 * @return A collection containing the ordered tokens as wrapped by this
	 *         stream Each token must be a separate element within the
	 *         collection. Operations on the returned collection should NOT
	 *         affect the token stream
	 */
	public Collection<String> getAllTokens() {
		// TODO: Implement this method
		if (stream.isEmpty()) {
			return null;
		} else {
			Collection<String> allTokens = new ArrayList<String>(stream);
			return allTokens;
		}
	}

	/**
	 * Method to query for the given token within the stream
	 * 
	 * @param token
	 *            : The token to be queried
	 * @return: THe number of times it occurs within the stream, 0 if not found
	 */
	public int query(String token) {
		// TODO: Implement this method
		if (stream.isEmpty() || !stream.contains(token)) {
			return 0;
		} else {
			int counter = 0;
			for (int index = 0; index < stream.size(); index++) {
				if (stream.get(index).equals(token)) {
					counter++;
				}
			}
			return counter;
		}
	}

	/**
	 * Iterator method: Method to check if the stream has any more tokens
	 * 
	 * @return true if a token exists to iterate over, false otherwise
	 */
	public boolean hasNext() {
		// TODO: Implement this method
		int index = current;
		return (index < 0) ? false : (index != stream.size());
	}

	/**
	 * Iterator method: Method to check if the stream has any more tokens
	 * 
	 * @return true if a token exists to iterate over, false otherwise
	 */
	public boolean hasPrevious() {
		// TODO: Implement this method
		int index = current;
		return (index <= 0) ? false : true;
	}

	/**
	 * Iterator method: Method to get the next token from the stream Callers
	 * must call the set method to modify the token, changing the value of the
	 * token returned by this method must not alter the stream
	 * 
	 * @return The next token from the stream, null if at the end
	 */
	public String next() {
		// TODO: Implement this method
		int index = current;
		if (index >= stream.size() || index < 0) {
			return null;
		} else {
			current = index + 1;
			return stream.get(prev = index);
		}
	}

	/**
	 * Iterator method: Method to get the previous token from the stream Callers
	 * must call the set method to modify the token, changing the value of the
	 * token returned by this method must not alter the stream
	 * 
	 * @return The next token from the stream, null if at the end
	 */
	public String previous() {
		// TODO: Implement this method
		int index = current - 1;
		if (index < 0) {
			return null;
		} else {
			prev = index - 1;
			return stream.get(current = index);
		}
	}

	/**
	 * Iterator method: Method to remove the current token from the stream
	 */
	public void remove() {
		// TODO: Implement this method
		int index = current;
		if (index >= 0) {
			if (hasNext()) {
				stream.remove(index);
			}
		}
	}

	/**
	 * Method to merge the current token with the previous token, assumes
	 * whitespace separator between tokens when merged. The token iterator
	 * should now point to the newly merged token (i.e. the previous one)
	 * 
	 * @return true if the merge succeeded, false otherwise
	 */
	public boolean mergeWithPrevious() {
		// TODO: Implement this method
		int index = current;
		if (index == stream.size() && stream.size() > 0) {
			current = index - 1;
			prev = current - 1;
			return false;
		} else if (index < stream.size() && index > 0) {
			String currentToken = stream.get(index), previousToken = stream
					.get(index - 1);
			stream.remove(index);
			stream.set(index - 1, previousToken + " " + currentToken);
			current = index - 1;
			prev = current - 1;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Method to merge the current token with the next token, assumes whitespace
	 * separator between tokens when merged. The token iterator should now point
	 * to the newly merged token (i.e. the current one)
	 * 
	 * @return true if the merge succeeded, false otherwise
	 */
	public boolean mergeWithNext() {
		// TODO: Implement this method
		int index = current;
		if (index < stream.size() - 1) {
			String first = stream.get(index), second = stream.get(index + 1);
			stream.set(index, first + " " + second);
			stream.remove(index + 1);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Method to replace the current token with the given tokens The stream
	 * should be manipulated accordingly based upon the number of tokens set It
	 * is expected that remove will be called to delete a token instead of
	 * passing null or an empty string here. The iterator should point to the
	 * last set token, i.e, last token in the passed array.
	 * 
	 * @param newValue
	 *            : The array of new values with every new token as a separate
	 *            element within the array
	 */

	public void set(String... newValue) {
		// TODO: Implement this method
		int index = current;
		if (index >= 0) {
			ArrayList<String> setList = new ArrayList<String>();
			for (String token : newValue) {
				if (!(token == null || token == "")) {
					setList.add(token);
				}
			}
			if (setList.size() > 0) {
				if (stream.size() > 0 && stream.size() > index) {
					stream.remove(index);
					for (String token : setList) {
						stream.add(index, token);
						index++;
					}
					current = index - 1;
					prev = current - 1;
				}
			}
		}
	}

	/**
	 * Iterator method: Method to reset the iterator to the start of the stream
	 * next must be called to get a token
	 */
	public void reset() {
		// TODO: Implement this method
		current = 0;
		prev = -1;
	}

	/**
	 * Iterator method: Method to set the iterator to beyond the last token in
	 * the stream previous must be called to get a token
	 */
	public void seekEnd() {
		current = stream.size();
		prev = current - 1;
	}

	/**
	 * Method to merge this stream with another stream
	 * 
	 * @param other
	 *            : The stream to be merged
	 */
	public void merge(TermTokenStream other) {
		// TODO: Implement this method
		try {
			if (this != null && other != null) {
				if (stream != null && other.stream != null) {
					stream.addAll(other.stream);
				} else if (stream == null && other.stream != null) {
					stream.addAll(other.stream);
				}
			}
		} catch (NullPointerException e) {

		}
	}

	public boolean mergeWithNext(int index) {

		if (index < stream.size() - 1) {
			String first = stream.get(index), second = stream.get(index + 1);
			stream.set(index, first + " " + second);
			stream.remove(index + 1);
			return true;
		} else {
			return false;
		}
	}

	public String toString() {
		return stream.toString();

	}

	public String toSingleString() {
		Iterator<String> itrAllTokens = stream.iterator();
		StringBuilder sb = new StringBuilder();

		while (itrAllTokens.hasNext()) {
			String term = itrAllTokens.next();
			sb.append(term + " ");
		}

		return sb.toString();

	}

	public String toNLPString() {
		Iterator<String> itrAllTokens = stream.iterator();
		StringBuilder sb = new StringBuilder();

		while (itrAllTokens.hasNext()) {
			String term = itrAllTokens.next();
			sb.append(term.replaceAll(" ", "") + " ");
		}

		return sb.toString();

	}

	public String index(int index) {
		if (index < stream.size()) {
			return stream.get(index);
		} else {
			return null;
		}
	}

	public int length() {

		return stream.size();
	}

	public void listMerge(LinkedList<String> list) {
		ListIterator<String> listitr = list.listIterator();
		int count = 0;
		while (listitr.hasNext()) {
			String buffer = listitr.next();
			String temp[] = buffer.split(" ");
			if (temp.length > 2) {
				count = temp.length - 1;
				while (count > 0) {
					this.mergeWithNext(Integer.valueOf(temp[0]));
					count--;
				}
			} else if (temp.length == 2) {
				this.mergeWithNext(Integer.valueOf(temp[0]));
			} else {

			}

		}

	}
}
