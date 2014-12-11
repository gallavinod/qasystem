package com.search;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.types.Expressions.CaseExpression;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class NLParser {
	static TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	static GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	static TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(
			new CoreLabelTokenFactory(), "");
	static CRFClassifier classifier = null;
	static HashMap<String, String> exrelmap = new HashMap<String, String>();
	static HashMap<String, String> questypemap = new HashMap<String, String>();
	static HashMap<String, String> mappingmap = new HashMap<String, String>();
	static LexicalizedParser lp = null;

	public static void main(String[] args) {
		exrelmap = readHashMapFromDisk("expandedRelations");
		questypemap = readHashMapFromDisk("questionTypeProbability");
		mappingmap = readHashMapFromDisk("Mappings150");
		try {
			classifier = CRFClassifier
					.getClassifier("edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz");
		} catch (ClassCastException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lp = LexicalizedParser
				.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					"D:\\solr\\my_files\\Questions.txt"));
			String line;
			while ((line = br.readLine()) != null) {
				System.out
						.println("--------------------------------------------");
				demoAPI(line);
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void demoAPI(String str) {
		ParsedQuestion parsedq = new ParsedQuestion();

		parsedq = parseQues(str);
		System.out.println(parsedq);
	}

	private static ParsedQuestion parseQues(String str) {
		// String str =
		// "what is the caption of European Council of Religious Leaders ? ";
		TermTokenStream queryStream = new TermTokenStream((String) null);
		String[] strlist = corelabeltostr(tokenizerFactory.getTokenizer(
				new StringReader(str)).tokenize());
		queryStream.append(strlist);
		Iterator<List> it = classifier.classify(queryStream.toSingleString())
				.iterator();
		boolean NameFound = false;
		String buffer = "";
		String finalstr = "";
		while (it.hasNext()) {
			Iterator<CoreLabel> itr = it.next().iterator();
			while (itr.hasNext()) {
				if (!itr.next().get(CoreAnnotations.AnswerAnnotation.class)
						.equals("O")) {
					NameFound = true;
				}
			}
		}

		finalstr = compressNames(queryStream);

		System.out.println(finalstr);

		Tree parse = null;
		parse = lp.apply(tokenizerFactory.getTokenizer(
				new StringReader(queryStream.toNLPString())).tokenize());

		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);

		// detect Answer Type START
		String whclause = null;
		for (int i = 0; i < queryStream.length(); i++) {
			if (gs.getNodeByIndex(i + 1).parent().value().equals("WRB")
					|| (gs.getNodeByIndex(i + 1).parent().value().equals("WP"))
					|| (gs.getNodeByIndex(i + 1).parent().value().equals("WDT"))) {
				whclause = queryStream.index(i);
			}
		}
		// detect Answer Type END

		List<TypedDependency> tdl = getTyped(queryStream.toNLPString());
		System.out.println(tdl);

		// Compress based on Noun Compounds and Adjectives , Determiners
		String compressedstr = compress(queryStream);
		System.out.println(compressedstr);

		// Compress based on Caps in letters.
		String comstr = compressCaps(queryStream);
		System.out.println(comstr);

		parse = lp.apply(tokenizerFactory
				.getTokenizer(new StringReader(comstr)).tokenize());

		GrammaticalStructure gs2 = gsf.newGrammaticalStructure(parse);
		List<TypedDependency> tdl2 = gs2.typedDependenciesCCprocessed();

		Iterator<TypedDependency> itlist = GrammaticalStructure.getRoots(tdl2)
				.iterator();
		// System.out.println(queryStream.toNLPString());

		// Identify ROOT in the Grammatical Structure
		String multi[][] = new String[queryStream.length()][queryStream
				.length()];
		int root = 0;
		while (itlist.hasNext()) {

			int tr = itlist.next().dep().index() - 1;
			root = tr;
			multi[tr][tr] = "root";

		}

		// Fill the Array based on Relations
		ListIterator<TypedDependency> tdllist = tdl2.listIterator();
		while (tdllist.hasNext()) {

			TypedDependency tr = tdllist.next();

			int govin = tr.gov().index() - 1;
			int depin = tr.dep().index() - 1;

			if (govin >= 0) {
				// Have to fix the multiple words in the sequence issue

				multi[govin][depin] = tr.reln().toString();
			}
		}

		// PRINT ARRAY
		// for (int i = 0; i < queryStream.length(); i++) {
		// for (int j = 0; j < queryStream.length(); j++) {
		// System.out.print((multi[i][j] == null ? "-" : multi[i][j])
		// + " ");
		// }
		// System.out.println();
		// }

		// parse.pennPrint();

		HashMap<String, String> depHashMap = new HashMap<String, String>();
		HashMap<String, String> govHashMap = new HashMap<String, String>();

		for (int i = 0; i < queryStream.length(); i++) {
			for (int j = 0; j < queryStream.length(); j++) {
				if (multi[i][j] != null && i != j) {
					if (!depHashMap.containsKey(multi[i][j])
							&& !govHashMap.containsKey(multi[i][j])) {
						depHashMap.put(multi[i][j], queryStream.index(j));
						govHashMap.put(multi[i][j], queryStream.index(i));
					} else if (!depHashMap.containsKey(multi[i][j] + "1")
							&& !govHashMap.containsKey(multi[i][j] + "1")) {
						depHashMap.put(multi[i][j] + "1", queryStream.index(j));
						govHashMap.put(multi[i][j] + "1", queryStream.index(i));
					} else {
						depHashMap.put(multi[i][j] + "2", queryStream.index(j));
						govHashMap.put(multi[i][j] + "2", queryStream.index(i));
					}
				}
			}
		}

		// System.out.println(gs2.getNodeByIndex(root + 1).value());

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < queryStream.length(); i++) {
			sb.append(gs2.getNodeByIndex(i + 1).parent().value());
		}

		// System.out.println(queryStream.length());
		String template = sb.toString();
		System.out.println(template);
		System.out.println(getQuestionType(template, queryStream));

		// Initialize Required fields for Query
		ParsedQuestion parques = new ParsedQuestion();
		parques.setWhclause(whclause);

		String relation = "";
		TreeMap<String, Double> whtype = new TreeMap<String, Double>();
		String searchname = "";
		String relationkeyword = "";
		switch (getQuestionType(template, queryStream)) {

		case "fourtype1":
			whtype = getWHMAP(questypemap.get(whclause.toLowerCase().trim()));

			// get Relation from ROOT of the sentence
			relationkeyword = gs2.getNodeByIndex(root + 1).value();
			relation = getRelation(gs2.getNodeByIndex(root + 1).value(), whtype
					.firstKey().toLowerCase());

			// get Name from the NN in the sentence
			for (int i = 0; i < queryStream.length(); i++) {
				if (isNoun(gs2.getNodeByIndex(i + 1).parent().value())) {
					searchname = queryStream.index(i);
				}
			}

			break;
		case "fivetype1":
			// get WH clause from the sentence
			whtype = getWHMAP(questypemap.get(whclause.toLowerCase().trim()));
			// get Relation from ROOT of the sentence
			relationkeyword = gs2.getNodeByIndex(root + 1).value();
			relation = getRelation(gs2.getNodeByIndex(root + 1).value(), whtype
					.firstKey().toLowerCase());

			// get Name from the NN in the sentence
			for (int i = 0; i < queryStream.length(); i++) {
				if (isNoun(gs2.getNodeByIndex(i + 1).parent().value())) {
					searchname = queryStream.index(i);
				}
			}
			break;

		case "sixtype1":
			// get WH clause from the sentence

			whtype = getWHMAP(questypemap.get(whclause.toLowerCase().trim()));

			// get the nsubj / nsubjpass relation in the sentence
			if (depHashMap.containsKey("nsubj")) {
				relationkeyword = depHashMap.get("nsubj");
				relation = getRelation(depHashMap.get("nsubj"), whtype
						.firstKey().toLowerCase());
			} else if (depHashMap.containsKey("nsubjpass")) {
				relationkeyword = depHashMap.get("nsubjpass");
				relation = getRelation(depHashMap.get("nsubjpass"), whtype
						.firstKey().toLowerCase());
			}

			// get the possessive dependent relation in the sentence
			if (depHashMap.containsKey("poss")) {
				searchname = depHashMap.get("poss");
			}
			break;

		case "sixtype2":
			// WRBJJVBDNNPVB.
			// get WH clause from the sentence

			whtype = getWHMAP(questypemap.get(whclause.toLowerCase().trim()));
			// get the dep relation in the sentence
			if (depHashMap.containsKey("dep")) {
				relationkeyword = depHashMap.get("dep");
				relation = getRelation(depHashMap.get("dep"), whtype.firstKey()
						.toLowerCase());
			}

			// get the nsubj dependent relation in the sentence
			if (depHashMap.containsKey("nsubj")) {
				searchname = depHashMap.get("nsubj");
			} else if (depHashMap.containsKey("nsubjpass")) {
				searchname = depHashMap.get("nsubjpass");
			}
			break;

		case "sixtype3":
			// WRBJJVBDNNPVB.
			// get WH clause from the sentence

			whtype = getWHMAP(questypemap.get(whclause.toLowerCase().trim()));

			// get the dep relation in the sentence
			if (depHashMap.containsKey("dep")) {
				relationkeyword = depHashMap.get("dep") + " "
						+ govHashMap.get("dep");
				relation = getRelation(
						depHashMap.get("dep") + " " + govHashMap.get("dep"),
						whtype.firstKey().toLowerCase());
			}

			// get the nsubj dependent relation in the sentence
			if (depHashMap.containsKey("nsubj")) {
				searchname = depHashMap.get("nsubj");
			} else if (depHashMap.containsKey("nsubjpass")) {
				searchname = depHashMap.get("nsubjpass");
			}
			break;

		case "sixtype4":
			// WRBVBDNNPVBNN.
			// when did CatchMeIfYouCan hit theboxoffice ?
			// not written yet
			// get WH clause from the sentence

			whtype = getWHMAP(questypemap.get(whclause.toLowerCase().trim()));

			// get the dep relation in the sentence
			if (depHashMap.containsKey("dobj")) {
				relationkeyword = govHashMap.get("dobj") + " "
						+ depHashMap.get("dobj");
				relation = getRelation(govHashMap.get("dobj") + " "
						+ depHashMap.get("dobj"), whtype.firstKey()
						.toLowerCase());
			}

			// get the nsubj dependent relation in the sentence
			if (depHashMap.containsKey("nsubj")) {
				searchname = depHashMap.get("nsubj");
			} else if (depHashMap.containsKey("nsubjpass")) {
				searchname = depHashMap.get("nsubjpass");
			}
			break;

		case "sixtype5":
			// WPVBDNNINNNP.
			// who was thedirector of AClockworkOrange ?

			whtype = getWHMAP(questypemap.get(whclause.toLowerCase().trim()));

			// get the nsubj relation in the sentence
			if (depHashMap.containsKey("nsubj")) {
				relationkeyword = depHashMap.get("nsubj");
				relation = getRelation(depHashMap.get("nsubj"), whtype
						.firstKey().toLowerCase());
			} else if (depHashMap.containsKey("nsubjpass")) {
				relationkeyword = depHashMap.get("nsubjpass");
				relation = getRelation(depHashMap.get("nsubjpass"), whtype
						.firstKey().toLowerCase());
			}

			// get the nsubj dependent relation in the sentence
			if (depHashMap.containsKey("prep_" + queryStream.index(3).trim())) {
				searchname = depHashMap.get("prep_"
						+ queryStream.index(3).trim());
			}
			break;

		case "seventype1":
			// WPVBPNNSVBNINNNP.
			// what are thenotableinstruments played by SamCollins ?

			whtype = getWHMAP(questypemap.get(whclause.toLowerCase().trim()));

			// get the nsubj relation in the sentence
			if (depHashMap.containsKey("nsubj")) {
				relationkeyword = depHashMap.get("nsubj") + " "
						+ govHashMap.get("nsubj");
				relation = getRelation(depHashMap.get("nsubj") + " "
						+ govHashMap.get("nsubj"), whtype.firstKey()
						.toLowerCase());
			} else if (depHashMap.containsKey("nsubjpass")) {
				relationkeyword = depHashMap.get("nsubjpass") + " "
						+ govHashMap.get("nsubjpass");
				relation = getRelation(depHashMap.get("nsubjpass") + " "
						+ govHashMap.get("nsubjpass"), whtype.firstKey()
						.toLowerCase());
			}

			// get the nsubj dependent relation in the sentence
			if (depHashMap.containsKey("agent")) {
				searchname = depHashMap.get("agent");
			}
			break;

		case "":
			// WPVBDNNINNNP.
			// who was thedirector of AClockworkOrange ?

			whtype = getWHMAP(questypemap.get(whclause.toLowerCase().trim()));

			int s = identifylastleaf(multi, root, 0);
			searchname = queryStream.index(s);

			// exclude s and 0 and get all the words
			// get the nsubj relation in the sentence
			relationkeyword = getWords(s, queryStream, gs2);

			for (int i = 0; i < queryStream.length(); i++) {
				for (int j = 0; j < queryStream.length(); j++) {
					if (multi[i][j] != null && i != j) {

						System.out.println(multi[i][j] + ":"
								+ type(multi[i][j]));
						System.out.println(queryStream.index(i) + " --> "
								+ queryStream.index(j));
					}
				}
			}

			break;
		default:
			break;
		}
		// System.out.println("WH clause : "+whclause);

		// System.out.println("WH Type : "+whtype);
		// System.out.println("Relation : "+relation);
		// System.out.println("Relation Key word : "+ relationkeyword);
		// System.out.println("Search Name:"+searchname);

		if (whtype == null || searchname == null || relationkeyword == null) {

			for (int i = 0; i < queryStream.length(); i++) {
				for (int j = 0; j < queryStream.length(); j++) {
					if (multi[i][j] != null && i != j) {

						System.out.println(multi[i][j] + ":"
								+ type(multi[i][j]));
						System.out.println(queryStream.index(i) + " --> "
								+ queryStream.index(j));
					}
				}
			}
		} else {
			parques.setRelationKeyWord(relationkeyword);
			parques.setSearchName(searchname);
			parques.setWhtype(whtype);

		}

		System.out.println(parques);

		return parques;
	}

	private static String compressNames(TermTokenStream queryStream) {

		String strclass = classifier.classifyToString(queryStream
				.toSingleString());
		System.out.println(strclass);
		String buffer = " ";
		LinkedList<String> list = new LinkedList<String>();
		String temp[] = strclass.split(" ");
		String temp1[] = null;
		for (int j = 0; j < temp.length; j++) {
			temp1 = temp[j].split("/");

			if (temp1[1].equals("O")) {
				if (!buffer.equals("")) {

					list.add(buffer);
					buffer = "";
				} else {

					// buffer = "";
				}
				//
			} else {
				// buffer += temp1[0];
				buffer += j + " ";
			}

		}

		queryStream.listMerge(list);

		return queryStream.toNLPString();
	}

	private static String getWords(int s, TermTokenStream queryStream,
			GrammaticalStructure gs2) {

		String temp = "";
		for (int i = 0; i < queryStream.length(); i++) {
			if (i != s
					&& !gs2.getNodeByIndex(i + 1).parent().value().equals(".")) {
				temp += queryStream.index(i) + " ";
			}
		}

		return temp;
	}

	private static int identifylastleaf(String[][] multi, int root, int level) {
		int count = 0;

		for (int i = 0; i < multi.length; i++) {
			if (root != i && multi[root][i] != null) {
				// System.out.println(multi[root][i]);
				count++;
			}
		}

		if (count == 1) {

			for (int i = 0; i < multi.length; i++) {
				if (root != i && multi[root][i] != null) {
					// System.out.println(multi[root][i]);
					level = i;
					return i;
				}
			}
		} else {
			for (int i = 0; i < multi.length; i++) {
				if (root != i && multi[root][i] != null) {
					level = identifylastleaf(multi, i, level);
				}
			}
		}

		return level;
		// return 0;

	}

	private static String compressCaps(TermTokenStream queryStream) {
		// Pattern pat = Pattern.compile("^[A-Z]([a-zA-Z0-9]+?)");
		// int count = 0;
		for (int i = 0; i < queryStream.length(); i++) {
			if (Pattern.matches("^[A-Z]([a-zA-Z0-9 ]+?)", queryStream.index(i))
					&& Pattern.matches("^[A-Z]([a-zA-Z0-9 ]+?)",
							queryStream.index(i + 1))) {
				System.out.println(queryStream.index(i) + " "
						+ queryStream.index(i + 1));
				queryStream.mergeWithNext(i);
				i = i - 2;
				// count++;
			}
		}
		return queryStream.toNLPString();
	}

	private static String getRelation(String value, String whtype) {
		String rel = "";
		String exrelstring = "";
		// System.out.println(value);
		String[] strarray = value.split(" ");

		for (int i = 0; i < strarray.length; i++) {
			if (exrelmap.containsKey(strarray[i].toLowerCase().trim())) {
				exrelstring += exrelmap.get(strarray[i].toLowerCase().trim());
			}
		}

		// System.out.println(exrelstring);

		if (!exrelstring.equals("")) {
			String[] temp = exrelstring.split("~~");
			for (int i = 0; i < temp.length; i++) {
				if (!temp[i].trim().equals("")) {

					String mapdetect = mappingmap.get(temp[i].trim());

					if (mapdetect.toLowerCase().trim().equals(whtype.trim())) {
						rel = temp[i].trim();
						// System.out.println(temp[i].trim());
					}
				}

			}
			return rel;
		}
		return null;

	}

	private static TreeMap<String, Double> getWHMAP(String str) {
		// TODO Auto-generated method stub
		String[] temp = str.split("\\|\\|");
		String[] temp2 = null;
		HashMap<String, Double> hash = new HashMap<String, Double>();
		for (int i = 0; i < temp.length; i++) {

			temp2 = temp[i].split("~");
			if (temp2.length == 2) {
				hash.put(temp2[0], Double.valueOf(temp2[1]));
			}

		}
		TreeMap<String, Double> treeMap = new TreeMap<String, Double>();
		treeMap = (TreeMap<String, Double>) sortByValues(hash);

		return treeMap;
	}

	private static boolean isNoun(String value) {

		Matcher nounPatttern = Pattern.compile("NN(|P|S|PS)").matcher(value);
		if (nounPatttern.matches()) {
			return true;
		}
		return false;
	}

	private static String getQuestionType(String template,
			TermTokenStream queryStream) {
		// WPVBZNNPPOSNNP
		// System.out.println(template);
		Matcher infosixPatttern = Pattern.compile(
				"W(P|DT|RB)VB(|D|G|Z|N|P)NN(|P|S|PS)POSNN(|P|S|PS)(.)?")
				.matcher(template);
		// WRBJJVBDNNPVB.
		Matcher infosixtwoPatttern = Pattern.compile(
				"W(P|DT|RB)JJVB(|D|G|Z|N|P)NN(|P|S|PS)(VB(|D|G|Z|N|P))?(.)?")
				.matcher(template);

		// WRBJJVBDNNPSJJ
		Matcher infosixthreePatttern = Pattern.compile(
				"W(P|DT|RB)JJVB(|D|G|Z|N|P)NN(|P|S|PS)JJ(.)?")
				.matcher(template);

		// WRBVBDNNPVBNN.
		// WDTNNVBDNNVBN.
		Matcher infosixfourPatttern = Pattern
				.compile(
						"W(P|DT|RB)(NN(|P|S|PS))?VB(|D|G|Z|N|P)NN(|P|S|PS)VB(|D|G|Z|N|P)(NN(|P|S|PS))?(.)?")
				.matcher(template);

		// WPVBDNNINNNP.
		Matcher infosixfivePatttern = Pattern.compile(
				"W(P|DT|RB)VB(|D|G|Z|N|P)NN(|P|S|PS)INNN(|P|S|PS)(.)?")
				.matcher(template);

		// WPVBPNNSVBNINNNP.
		Matcher infosevenonePatttern = Pattern
				.compile(
						"W(P|DT|RB)VB(|D|G|Z|N|P)NN(|P|S|PS)VB(|D|G|Z|N|P)INNN(|P|S|PS)(.)?")
				.matcher(template);

		Matcher infofivePatttern = Pattern.compile(
				"W(P|DT|RB)VB(|D|G|Z|N|P)NN(|P|S|PS)VB(|D|G|Z|N|P)(.)?")
				.matcher(template);

		Matcher infofourPatttern = Pattern.compile(
				"W(P|DT|RB)VB(|D|G|Z|N|P)NN(|P|S|PS)(.)?").matcher(template);

		switch (queryStream.length()) {
		case 4:
			if (infofourPatttern.matches()) {
				return "fourtype1";
			}
			break;
		case 5:
			if (infofivePatttern.matches()) {
				return "fivetype1";
			} else if (infosixtwoPatttern.matches()) {
				return "sixtype2";
			}
			break;
		case 6:
			if (infosixPatttern.matches()) {
				return "sixtype1";
			} else if (infosixtwoPatttern.matches()) {
				return "sixtype2";
			} else if (infosixthreePatttern.matches()) {
				return "sixtype3";
			} else if (infosixfourPatttern.matches()) {
				return "sixtype4";
			} else if (infosixfivePatttern.matches()) {
				return "sixtype5";
			}
			break;

		case 7:
			if (infosevenonePatttern.matches()) {
				return "seventype1";
			}
			break;

		default:
			break;
		}

		return "";
	}

	private static String type(String str) {

		// root - root
		// dep - dependent

		// aux - auxiliary
		// auxpass - passive auxiliary
		// cop - copula

		// arg - argument
		// agent - agent

		// comp - complement
		// acomp - adjectival complement
		// ccomp - clausal complement with internal subject
		// xcomp - clausal complement with external subject

		// obj - object
		// dobj - direct object
		// iobj - indirect object
		// pobj - object of preposition

		// --- Subject of the Phrase
		// subj - subject
		// nsubj - nominal subject
		// nsubjpass - passive nominal subject
		// csubj - clausal subject
		// csubjpass - passive clausal subject

		// cc - coordination
		// conj - conjunct
		// expl - expletive (expletive \there")
		// mod - modifier

		// amod - adjectival modi er
		// appos - appositional modi er
		// advcl - adverbial clause modi er
		// det - determiner
		// predet - predeterminer
		// preconj - preconjunct
		// infmod - in nitival modi er
		// mwe - multi-word expression modi er
		// mark - marker (word introducing an advcl or ccomp
		// partmod - participial modi er
		// advmod - adverbial modi er
		// neg - negation modi er
		// rcmod - relative clause modi er
		// quantmod - quanti er modi er
		// nn - noun compound modi er
		// npadvmod - noun phrase adverbial modi er
		// tmod - temporal modi er

		// num - numeric modi er
		// number - element of compound number
		// prep - prepositional modi er
		// poss - possession modi er
		// possessive - possessive modi er ('s)
		// prt - phrasal verb particle

		// ----- Not Sure ------
		// parataxis - parataxis
		// punct - punctuation
		// ref - referent
		// sdep - semantic dependent
		// xsubj - controlling subject

		switch (str) {
		case "acomp:":
			return "An adjectival complement of a verb is an adjectival phrase which functions as the complement"
					+ "like an object of the verb";
		case "advcl":
			return "An adverbial clause modier of a VP or S is a clause modifying the verb";
		case "advmod":
			return " An adverbial modier of a word is a (non-clausal) adverb or adverbial phrase (ADVP) that"
					+ "serves to modify the meaning of the word";
		case "agent":
			return "An agent is the complement of a passive verb which is introduced by the preposition \by and"
					+ "does the action";
		case "amod":
			return "An adjectival modifier of an NP is any adjectival phrase that serves to modify the meaning of"
					+ "the NP";
		case "appos":
			return "An appositional modier of an NP is an NP immediately to the right of the rst NP that serves"
					+ "to define or modify that NP. It includes parenthesized examples, as well as defining abbreviations"
					+ "in one of these structures";
		case "aux":
			return "non-main verb of the clause be, do or have in a periphrastic tense.";
		case "auxpass":
			return "non-main verb of the clause which contains the passive information.";
		case "cc":
			return "coordination is the relation between an element of a conjunct and the coordinating conjunction of the conjunct";
		case "ccomp":
			return "A clausal complement of a verb or adjective is a dependent clause with an internal subject which functions like an object of the verb, or adjective";
		case "conj":
			return "A conjunct is the relation between two elements connected by a coordinating conjunction";
		case "cop":
			return "A copula is the relation between the complement of a copular verb and the copular verb";
		case "csubj":
			return "A clausal subject is a clausal syntactic subject of a clause";
		case "csubjpass":
			return "A clausal passive subject is a clausal syntactic subject of a passive clause";
		case "dep":
			return "A clausal passive subject is a clausal syntactic subject of a passive clause";
		case "det":
			return "A determiner is the relation between the head of an NP and its determiner";
		case "discourse":
			return "This is used for interjections and other discourse particles and elements linked to the structure of the sentence, except in an expressive way";
		case "dobj":
			return "The direct object of a VP is the noun phrase which is the (accusative) object of the verb";
		case "expl":
			return "This relation captures an existential there";
		case "goeswith":
			return "This relation links two parts of a word that are separated in text that is not well edited.";
		case "infmod":
			return "An innitival modier of an NP is an innitive that serves to modify the meaning of the NP";
		case "iobj":
			return "The indirect object of a VP is the noun phrase which is the (dative) object of the verb";
		case "mark":
			return "A marker is the word introducing a nite clause subordinate to another clause";
		case "mwe":
			return "The multi-word expression (modier) relation is used for certain multi-word idioms that behave like a single function word";
		case "neg":
			return "The negation modier is the relation between a negation word and the word it modies.";
		case "nn":
			return "A noun compound modier of an NP is any noun that serves to modify the head noun";
		case "npadvmod":
			return "This relation captures various places where something syntactically a noun phrase (NP) is used as an adverbial modier in a sentence";
		case "nsubj":
			return "A nominal subject is a noun phrase which is the syntactic subject of a clause";
		case "nsubjpass":
			return "A passive nominal subject is a noun phrase which is the syntactic subject of a passive clause";
		case "num":
			return "A numeric modier of a noun is any number phrase that serves to modify the meaning of the noun with a quantity.";
		case "number":
			return "element of compound number";
		case "parataxis":
			return "The parataxis relation is a relation between the main verb of a clause and other sentential elements";
		case "partmod":
			return "A participial modier of an NP or VP or sentence is a participial verb form that serves to modify the meaning of a noun phrase or sentence";
		case "pcomp":
			return "This is used when the complement of a preposition is a clause or prepositional phrase";
		case "pobj":
			return "The object of a preposition is the head of a noun phrase following the preposition, or the adverbs here and there";
		case "poss":
			return "Possession modifier";
		case "possessive":
			return "The possessive modier relation appears between the head of an NP and the genitive 's";
		case "preconj":
			return "A preconjunct is the relation between the head of an NP and a word that appears at the beginning bracketing a conjunction";
		case "predet":
			return "A predeterminer is the relation between the head of an NP and a word that precedes and modies the meaning of the NP determiner.";
		case "prep":
			return "A prepositional modier of a verb, adjective, or noun is any prepositional phrase that serves to modify the meaning of the verb";
		case "prepc":
			return "prepositional clausal modier";
		case "prt":
			return "The phrasal verb particle relation identies a phrasal verb, and holds between the verb and its particle.";
		case "punct":
			return "This is used for any piece of punctuation in a clause, if punctuation is being retained in the typed dependencies";
		case "quantmod":
			return "A quantier modier is an element modifying the head of a QP constituent.";
		case "ref":
			return "A referent of the head of an NP is the relative word introducing the relative clause modifying the NP.";
		case "root":
			return "fake ROOT of the sentence ";
		case "tmod":
			return "bare noun phrase constituent that serves to modify the meaning of the constituent by specifying a time";
		case "xcomp":
			return "An open clausal complement (xcomp) of a VP or an ADJP is a clausal complement without its own subject, whose reference is determined by an external subject";
		case "xsubj":
			return "A controlling subject is the relation between the head of a open clausal complement - (xcomp)";

		}

		return null;
	}

	public static List<TypedDependency> getTyped(String str) {
		Tree parse = null;
		parse = lp.apply(tokenizerFactory.getTokenizer(new StringReader(str))
				.tokenize());

		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

		return tdl;

	}

	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(
			final Map<K, V> map) {
		Comparator<K> valueComparator = new Comparator<K>() {
			public int compare(K k1, K k2) {
				int compare = map.get(k2).compareTo(map.get(k1));
				// int compare = map.get(k1).compareTo(map.get(k2));
				if (compare == 0)
					return 1;
				else
					return compare;
			}
		};
		Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
		sortedByValues.putAll(map);
		return sortedByValues;
	}

	private static String compress(TermTokenStream queryStream) {

		List<TypedDependency> tdl = getTyped(queryStream.toNLPString());

		ListIterator<TypedDependency> tdllist = tdl.listIterator();
		int count = 1;
		while (tdllist.hasNext()) {

			TypedDependency typd = tdllist.next();
			// Have to fix the multiple words in the sequence issue
			if ((typd.reln().toString().equals("nn")
					| typd.reln().toString().equals("amod")
					| (typd.reln().toString().equals("det") && typd.dep()
							.index() != 1)
					| typd.reln().toString().equals("num") | typd.reln()
					.toString().equals("number"))
					&& ((typd.gov().index() - typd.dep().index()) == 1 || (typd
							.gov().index() - typd.dep().index()) == -1)) {
				if ((typd.gov().index() - typd.dep().index()) == -1) {
					queryStream.mergeWithNext(typd.gov().index() - count);
					count++;
				} else {
					queryStream.mergeWithNext(typd.dep().index() - count);
					count++;
				}
			}
		}

		if (found(getTyped(queryStream.toNLPString()))) {
			return compress(queryStream);
		}

		return queryStream.toNLPString();
	}

	private static boolean found(List<TypedDependency> typed) {
		ListIterator<TypedDependency> tdllist = typed.listIterator();
		boolean iftrue = false;
		while (tdllist.hasNext()) {

			TypedDependency typd = tdllist.next();
			// Have to fix the multiple words in the sequence issue
			if ((typd.reln().toString().equals("nn")
					| typd.reln().toString().equals("amod")
					| (typd.reln().toString().equals("det") && typd.dep()
							.index() != 1)
					| typd.reln().toString().equals("num") | typd.reln()
					.toString().equals("number"))
					&& ((typd.gov().index() - typd.dep().index()) == 1 || (typd
							.gov().index() - typd.dep().index()) == -1)) {
				iftrue = true;
			}

		}
		return iftrue;
	}

	private static void createstream(List<CoreLabel> rawWords2) {
		// TODO Auto-generated method stub

	}

	private static String[] corelabeltostr(List<CoreLabel> rawWords2) {
		String[] tmpstr = new String[rawWords2.size()];
		ListIterator<CoreLabel> lst = rawWords2.listIterator();
		for (int i = 0; i < rawWords2.size() && lst.hasNext(); i++) {
			tmpstr[i] = lst.next().toString();
		}

		return tmpstr;

	}

	public static List<Tree> getNounPhrases(Tree parse, List<Tree> nounPhrases) {
		if (parse.value().equals("NP")) {
			nounPhrases.add(parse);
		}
		for (Tree child : parse.children()) {
			getNounPhrases(child, nounPhrases);
		}
		return nounPhrases;
	}

	public static List<Tree> getVerbPhrases(Tree parse, List<Tree> verbPhrases) {
		if (parse.value().equals("VP")) {
			verbPhrases.add(parse);
		}
		for (Tree child : parse.children()) {
			getVerbPhrases(child, verbPhrases);
		}
		return verbPhrases;
	}

	public static List<Tree> getWHPhrases(Tree parse, List<Tree> whPhrases) {
		if (parse.value().equals("WRB") || parse.value().equals("WDT")
				|| parse.value().equals("WP")) {
			whPhrases.addAll(parse.getLeaves());
		}
		for (Tree child : parse.children()) {
			getWHPhrases(child, whPhrases);
		}
		return whPhrases;
	}

	public static <K, V> HashMap<K, V> readHashMapFromDisk(String fileName) {
		Map<K, V> ldapContent = new HashMap<K, V>();
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(fileName + ".properties"));
			for (Object key : properties.keySet()) {
				ldapContent.put((K) key, (V) properties.get(key));
			}
			return (HashMap<K, V>) ldapContent;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
