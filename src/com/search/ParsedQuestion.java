package com.search;

import java.util.TreeMap;



public class ParsedQuestion {
	private String whclause = null;
	private TreeMap<String, Double> whtype = null;
	private String RelationKeyWord = null;
	private String SearchName = null;
	private TermTokenStream queryStream = null;

	public ParsedQuestion(){
		whclause = "";
		whtype = new TreeMap<String, Double>();
		RelationKeyWord = "";
		SearchName = "";
		queryStream = new TermTokenStream((String) null);

	}
	/**
	 * @return the whclause
	 */
	public String getWhclause() {
		return whclause;
	}

	/**
	 * @param whclause
	 *            the whclause to set
	 */
	public void setWhclause(String whclause) {
		this.whclause = whclause;
	}

	/**
	 * @return the whtype
	 */
	public TreeMap<String, Double> getWhtype() {
		return whtype;
	}

	/**
	 * @param whtype
	 *            the whtype to set
	 */
	public void setWhtype(TreeMap<String, Double> whtype) {
		this.whtype = whtype;
	}

	/**
	 * @return the relationKeyWord
	 */
	public String getRelationKeyWord() {
		return RelationKeyWord;
	}

	/**
	 * @param relationKeyWord
	 *            the relationKeyWord to set
	 */
	public void setRelationKeyWord(String relationKeyWord) {
		RelationKeyWord = relationKeyWord;
	}

	/**
	 * @return the searchName
	 */
	public String getSearchName() {
		return SearchName;
	}

	/**
	 * @param searchName
	 *            the searchName to set
	 */
	public void setSearchName(String searchName) {
		SearchName = searchName;
	}

	public String toString() {

		String ret = "WH clause : " + whclause + System.lineSeparator();
		ret += "WH Type : " + whtype + System.lineSeparator();
		ret += "Relation Key word : " + RelationKeyWord
				+ System.lineSeparator();
		ret += "Search Name :" + SearchName + System.lineSeparator();

		return ret;

	}
	/**
	 * @return the queryStream
	 */
	public TermTokenStream getQueryStream() {
		return queryStream;
	}
	/**
	 * @param queryStream the queryStream to set
	 */
	public void setQueryStream(TermTokenStream queryStream) {
		this.queryStream = queryStream;
	}
}
