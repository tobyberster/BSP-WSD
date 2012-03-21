import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;

import mw.utils.Bobcat;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.FSDirectory;

import pitt.search.semanticvectors.Flags;
import pitt.search.semanticvectors.ObjectVector;
import pitt.search.semanticvectors.SearchResult;
import pitt.search.semanticvectors.VectorSearcher;
import pitt.search.semanticvectors.VectorStoreRAM;
import pitt.search.semanticvectors.VectorStoreWriter;
import pitt.search.semanticvectors.ZeroVectorException;
import pitt.search.semanticvectors.vectors.BinaryVector;
import pitt.search.semanticvectors.vectors.VectorFactory;
import pitt.search.semanticvectors.vectors.VectorType;

public class BuildDisambiguationIndex {
    public static Logger logger = Logger
	    .getLogger("pitt.search.semanticvectors");

    /**
     * Set the vector dimensions here.
     * Feel free to add additional ones, but these here were used for the experiments.
     */
    //private static int _intVectorDimension = 8192;
    private static int _intVectorDimension = 16384;
    //private static int _intVectorDimension = 65536;
    //private static int _intVectorDimension = 262144;
    
    /**
     * Set the context right here. Either sentence or abstract.
     */
    private static String _strTermField = "sentence";
    //private static String _strTermField = "abstract";
    
    /**
     * Create all the different vector stores.
     * In the current version elemental vectors are not actually used anywhere,
     * and could be discarded but are included for completeness.
     */
    private static VectorStoreRAM _objElementalVectors;
    private static VectorStoreRAM _objSenseVectors;
    private static VectorStoreRAM _objSemanticVectors;
    
    private static ArrayList<String> _objStopWordList;
    
    /**
     * Control the 10-fold cross-validation across all experiments
     * A random approach would be more appropriate here.
     */
    private static String[][] _arrCrossValidation = { 
	{ "R", "R", "R", "R", "R", "R", "R", "R", "R", "T"},
	{ "T", "R", "R", "R", "R", "R", "R", "R", "R", "R"},
	{ "R", "T", "R", "R", "R", "R", "R", "R", "R", "R"},
	{ "R", "R", "T", "R", "R", "R", "R", "R", "R", "R"},
	{ "R", "R", "R", "T", "R", "R", "R", "R", "R", "R"},
	{ "R", "R", "R", "R", "T", "R", "R", "R", "R", "R"},
	{ "R", "R", "R", "R", "R", "T", "R", "R", "R", "R"},
	{ "R", "R", "R", "R", "R", "R", "T", "R", "R", "R"},
	{ "R", "R", "R", "R", "R", "R", "R", "T", "R", "R"},
	{ "R", "R", "R", "R", "R", "R", "R", "R", "T", "R"}
    };
    
    
    private static LuceneUtils lUtils;
    private static IndexReader wsdReader;
    
    private static ArrayList<String> _objExcludedSenses = new ArrayList<String>();

    /**
     * Prints the following usage message: <code>
     * <br> BuildDisambiguationIndex class utilizes package pitt.search.semanticvectors
     * <br> Usage: java BuildDisambiguationIndex PATH_TO_DISAMBIGUATION_INDEX
     * <br> BuildDisambiguationIndex creates no files on standard behavior. 
     * </code>
     */
    public static void usage() {
	String usageMessage = "\nBuildDisambiguationIndex class utilizes package pitt.search.semanticvectors"
		+ "\nUsage: java BuildDisambiguationIndex "
		+ "PATH_TO_DISAMBIGUATION_INDEX"
		+ "\nBuildDisambiguationIndex creates no files on standard behavior.";
	System.out.println(usageMessage);
    }

    public static void main(String[] args) throws IOException,
    IllegalArgumentException {
	Flags.docidfield = "filename";

	try {
	    args = Flags.parseCommandLineFlags(args);
	} catch (IllegalArgumentException e) {
	    usage();
	    throw e;
	}

	if (!Flags.docidfield.equals("filename")) {
	    logger.log(Level.WARNING,
		    "Docid field is normally 'filename' for disambiguation index."
			    + " Are you sure you wanted to change this?");
	}

	// Only three arguments should remain, the path to Disambiguation index
	if (args.length != 1) {
	    usage();
	    throw (new IllegalArgumentException(
		    "After parsing command line flags, there were "
			    + args.length
			    + " arguments, instead of the expected 1."));
	}
	
	//Setting the flags
	Flags.dimension = _intVectorDimension;
	Flags.vectortype = VectorType.BINARY.toString().toLowerCase();
	
	//Leroy inclusion
	/*
	_objExcludedSenses.add("adjustment");
	_objExcludedSenses.add("blood_pressure");
	_objExcludedSenses.add("evaluation");
	_objExcludedSenses.add("degree");
	_objExcludedSenses.add("growth");
	_objExcludedSenses.add("immunosuppression");
	_objExcludedSenses.add("man");
	_objExcludedSenses.add("mosaic");
	_objExcludedSenses.add("radiation");
	_objExcludedSenses.add("nutrition");
	_objExcludedSenses.add("repair");
	_objExcludedSenses.add("scale");
	_objExcludedSenses.add("sensitivity");
	_objExcludedSenses.add("weight");
	_objExcludedSenses.add("white");*/
	
	//Liu inclusion
	/*
	_objExcludedSenses.add("cold");
	_objExcludedSenses.add("degree");
	_objExcludedSenses.add("depression");
	_objExcludedSenses.add("discharge");
	_objExcludedSenses.add("extraction");
	_objExcludedSenses.add("fat");
	_objExcludedSenses.add("growth");
	_objExcludedSenses.add("implantation");
	_objExcludedSenses.add("japanese");
	_objExcludedSenses.add("lead");
	_objExcludedSenses.add("man");
	_objExcludedSenses.add("mole");
	_objExcludedSenses.add("mosaic");
	_objExcludedSenses.add("nutrition");
	_objExcludedSenses.add("pathology");
	_objExcludedSenses.add("reduction");
	_objExcludedSenses.add("repair");
	_objExcludedSenses.add("scale");
	_objExcludedSenses.add("sex");
	_objExcludedSenses.add("ultrasound");
	_objExcludedSenses.add("weight");
	_objExcludedSenses.add("white");*/
	
	//Joshi exclusion
	/*_objExcludedSenses.add("association");
	_objExcludedSenses.add("cold");
	_objExcludedSenses.add("condition");
	_objExcludedSenses.add("energy");
	_objExcludedSenses.add("extraction");
	_objExcludedSenses.add("failure");
	_objExcludedSenses.add("fluid");
	_objExcludedSenses.add("frequency");
	_objExcludedSenses.add("ganglion");
	_objExcludedSenses.add("glucose");
	_objExcludedSenses.add("inhibition");
	_objExcludedSenses.add("pathology");
	_objExcludedSenses.add("pressure");
	_objExcludedSenses.add("reduction");
	_objExcludedSenses.add("resistance");
	_objExcludedSenses.add("secretion");
	_objExcludedSenses.add("single");
	_objExcludedSenses.add("surgery");
	_objExcludedSenses.add("transient");
	_objExcludedSenses.add("transport");*/
	
	
	

	String wsdIndex = args[args.length - 1];

	for(int wheretolook=0; wheretolook<_arrCrossValidation.length; wheretolook++)
	{
	_objElementalVectors = new VectorStoreRAM(VectorType.BINARY,
		_intVectorDimension);
	//_objElementalVectors.initFromFile("words.txt");
	_objSemanticVectors = new VectorStoreRAM(VectorType.BINARY,
		_intVectorDimension);
	//_objSemanticVectors.initFromFile("semantic.txt");
	_objSenseVectors = new VectorStoreRAM(VectorType.BINARY,
		_intVectorDimension);
	//_objSenseVectors.initFromFile("senses.txt");

	//LuceneUtils.compressIndex(wsdIndex);
	lUtils = new LuceneUtils(wsdIndex);
	wsdReader = IndexReader.open(FSDirectory.open(new File(wsdIndex)));
	int intNumDocs = wsdReader.numDocs();
	Random objRandom = new Random();
	VectorFactory objVectorFactory = new VectorFactory();
	
	HashMap<String, ArrayList<Integer>> objPossibleDocuments = new HashMap<String, ArrayList<Integer>>();
	HashMap<String, ArrayList<Integer>> objTrainingSet = new HashMap<String, ArrayList<Integer>>();
	HashMap<String, ArrayList<Integer>> objTestSet = new HashMap<String, ArrayList<Integer>>();
	
	//Create E(sense) : All Senses
	for (int i = 0; i < intNumDocs; i++) {
	    TermFreqVector objSenses = wsdReader.getTermFreqVector(i, "sense");
	    if(objSenses.size() == 0){
		System.out.println("Skipping a document...");
		continue;
	    }
	    String[] arrSenses = objSenses.getTerms();
	   
	    
	    TermFreqVector objTerm = wsdReader.getTermFreqVector(i, "term");
	    String strWSDTerm = objTerm.getTerms()[0];
	    
	    if(arrSenses[0].equalsIgnoreCase("none"))
		continue;
	    
	    
	    for (String strSense : arrSenses) {
		
		if(!objPossibleDocuments.containsKey(strWSDTerm))
			objPossibleDocuments.put(strWSDTerm, new ArrayList<Integer>());
		    
		objPossibleDocuments.get(strWSDTerm).add(i);
		
		objRandom.setSeed(Bobcat.getHashAsLong(strSense));
		if(_objSenseVectors.getVector(strSense)==null)
		    _objSenseVectors.putVector(strSense, objVectorFactory
			.generateRandomVector(VectorType.BINARY,
				_intVectorDimension,
				(int) Math.floor(_intVectorDimension / 2),
				objRandom));
	    }
	}
	
	//Create E(word) : All words in all sentences
	for(int i = 0; i <intNumDocs; i++)
	{
	    //TermFreqVector objSentences = wsdReader.getTermFreqVector(i, "sentence");
	    TermFreqVector objSentences = wsdReader.getTermFreqVector(i, _strTermField);
	    TermFreqVector objTitle = wsdReader.getTermFreqVector(i, "title");
	    String[] arrSentences = objSentences.getTerms();
	    String[] arrTitle = objTitle.getTerms();
	    
	    arrSentences = (String[]) ArrayUtils.addAll(arrSentences, arrTitle);
	    
	    for (String strWord : arrSentences) {
		strWord = prepareWord(strWord);
		
		if(strWord == null)
		    continue;
		
		objRandom.setSeed(Bobcat.getHashAsLong(strWord));
		if(_objElementalVectors.getVector(strWord) == null)
		{   
		    _objElementalVectors.putVector(strWord, objVectorFactory
			.generateRandomVector(VectorType.BINARY,
				_intVectorDimension,
				(int) Math.floor(_intVectorDimension / 2),
				objRandom));
		
		    _objSemanticVectors.putVector(strWord, VectorFactory.createZeroVector(VectorType.BINARY,_intVectorDimension));
		} else if(_objElementalVectors.getVector(strWord) != null && _objSemanticVectors.getVector(strWord) == null)
		    _objSemanticVectors.putVector(strWord, VectorFactory.createZeroVector(VectorType.BINARY,_intVectorDimension));
	    }
	}
	
	//k-fold
	Iterator it = objPossibleDocuments.entrySet().iterator();
	Random randomGenerator = new Random();
	while (it.hasNext()) {
	    Map.Entry pairs = (Map.Entry)it.next();
	    String strWsdWord = pairs.getKey().toString();
	    ArrayList<Integer> objWsdArray = objPossibleDocuments.get(strWsdWord);
	    
	    if(!objTrainingSet.containsKey(strWsdWord) || objTrainingSet.get(strWsdWord) == null)
		objTrainingSet.put(strWsdWord, new ArrayList<Integer>());
	    
	    int incrementSize = Math.round(objWsdArray.size()/10);
	    int lastIncrementSize = objWsdArray.size() - (incrementSize * 9);

	    for(int j=0;j<_arrCrossValidation[wheretolook].length;j++)
	    {
		if(_arrCrossValidation[wheretolook][j] == "R"){
		    int limit = incrementSize;
		    if(j == _arrCrossValidation[wheretolook].length - 1)
			limit = lastIncrementSize;

		    for(int h=0;h<limit;h++){
			objTrainingSet.get(strWsdWord).add(objWsdArray.get((j*incrementSize)+h));
		    }
		}
	    }
	    
	    //Create Test Set numbers
	    ArrayList<Integer> objTempTestSet = (ArrayList<Integer>)objWsdArray.clone();
	    objTempTestSet.removeAll(objTrainingSet.get(strWsdWord));
	    objTestSet.put(strWsdWord, objTempTestSet);
	    
	}
	
	int trainingSize = objTrainingSet.size();
	int testSize = objTestSet.size();
	//Training
	//Create S(word:sentence) += E(WSDword) XOR E(sense)
	//k-fold
	Iterator iterTraining = objTrainingSet.entrySet().iterator();
	while (iterTraining.hasNext()) {
	    Map.Entry pairs = (Map.Entry)iterTraining.next();
	    String strWsdWord = pairs.getKey().toString();
	    ArrayList<Integer> objWsdArray = objTrainingSet.get(strWsdWord);
	    
	    Iterator itr = objWsdArray.iterator();
	    while (itr.hasNext()) {
		int intDocument = Integer.parseInt(itr.next().toString());
		TermFreqVector objSenses = wsdReader.getTermFreqVector(intDocument, "sense");
		if(objSenses.size() == 0){
		    System.out.println("Skipping a document...");
		    continue;
		}
		String[] arrSenses = objSenses.getTerms();
		
		if(arrSenses[0].equalsIgnoreCase("none"))
		    continue;
		
		TermFreqVector objTerm = wsdReader.getTermFreqVector(intDocument, "term");
		
		String strWSDTerm = objTerm.getTerms()[0];
		
		if(_objExcludedSenses.contains(strWSDTerm))
		  continue;
		    
		TermFreqVector objSentences = wsdReader.getTermFreqVector(intDocument, _strTermField);
		//TermFreqVector objSentences = wsdReader.getTermFreqVector(intDocument, "abstract");
		String[] arrSentences = objSentences.getTerms();
		
		TermFreqVector objTitle = wsdReader.getTermFreqVector(intDocument, "title");
		String[] arrTitle = objTitle.getTerms();
		
		arrSentences = (String[]) ArrayUtils.addAll(arrSentences, arrTitle);
		
		ArrayList<String> wordsInSentence = new ArrayList<String>();
		for(String strWord : arrSentences){
		    strWord = prepareWord(strWord);
		    //strWSDTerm = prepareWord(strWSDTerm);
			
		    if (strWord == null)
			continue;
		    
		    if(strWSDTerm == null){
			System.out.println("Something is wrong here");
			continue;
		    }
		    
		    if(strWord.equalsIgnoreCase(strWSDTerm)) //do blood_pressure as well
			continue;
		    
		    if(wordsInSentence.contains(strWord))
			continue;
			
		    //System.out.println(strWSDTerm);
		    BinaryVector objSemanticVector = (BinaryVector)_objSemanticVectors.getVector(strWord);
		    BinaryVector objTempVector = (BinaryVector) _objElementalVectors.getVector(strWSDTerm);
		    if (objTempVector == null) {
			String[] words = strWSDTerm.split("_");
			for (String word : words) {
			    BinaryVector objLoopVector = (BinaryVector) _objElementalVectors
				    .getVector(word);
			    if (objTempVector == null) {
				if (objLoopVector == null)
				    continue;
				objTempVector = objLoopVector.copy();
			    } else
				objTempVector.bind(objLoopVector);
			}
		    } else
			objTempVector = objTempVector.copy();

		    if (objTempVector == null)
			continue;

		    BinaryVector objTempSenseVector = (BinaryVector) _objSenseVectors.getVector(arrSenses[0]);
		    objTempVector.bind(objTempSenseVector);

		    float weight = lUtils.getEntropy(new Term(_strTermField, strWSDTerm));

		    objSemanticVector.superpose(objTempVector, weight, null);
		    wordsInSentence.add(strWord);
		}
	    }
	}
	
	VectorStoreWriter objVectorWriter = new VectorStoreWriter();
	
	Enumeration<ObjectVector> termEnumeration = _objSemanticVectors.getAllVectors();
	while (termEnumeration.hasMoreElements()) {
	    termEnumeration.nextElement().getVector().normalize();
	}
	
	//Testing & Search
	Iterator iterTesting = objTestSet.entrySet().iterator();
	while (iterTesting.hasNext()) {
	    Map.Entry pairs = (Map.Entry)iterTesting.next();
	    String strWsdWord = pairs.getKey().toString();
	    ArrayList<Integer> objWsdArray = objTestSet.get(strWsdWord);
	    
	    Iterator itr = objWsdArray.iterator();
	    while (itr.hasNext()) {
		int intDocument = Integer.parseInt(itr.next().toString());
		TermFreqVector objSenses = wsdReader.getTermFreqVector(intDocument, "sense");
		TermFreqVector objpmid = wsdReader.getTermFreqVector(intDocument, "PMID");
		String pmid = objpmid.getTerms()[0];
		if(objSenses.size() == 0){
		    System.out.println("Skipping a document...");
		    continue;
		}
		String[] arrSenses = objSenses.getTerms();
		
		if(arrSenses[0].equalsIgnoreCase("none"))
		    continue;
		
		TermFreqVector objTerm = wsdReader.getTermFreqVector(intDocument, "term");
		String strWSDTerm = objTerm.getTerms()[0];
		
		if(_objExcludedSenses.contains(strWSDTerm))
		    continue;
		
		BinaryVector objTermVector = (BinaryVector) _objElementalVectors.getVector(strWSDTerm);
		    if (objTermVector == null) {
			String[] words = strWSDTerm.split("_");
			for (String word : words) {
			    BinaryVector objLoopVector = (BinaryVector) _objElementalVectors
				    .getVector(word);
			    if (objTermVector == null) {
				if (objLoopVector == null)
				    continue;
				objTermVector = objLoopVector.copy();
			    } else
				objTermVector.bind(objLoopVector);
			}
		    } else
			objTermVector = objTermVector.copy();

		    if (objTermVector == null)
			continue;
		
		TermFreqVector objSentences = wsdReader.getTermFreqVector(intDocument, _strTermField);
		//TermFreqVector objSentences = wsdReader.getTermFreqVector(intDocument, "sentence");    
		String[] arrSentences = objSentences.getTerms();
		int[] freqSentences = objSentences.getTermFrequencies();
		
		TermFreqVector objTitle = wsdReader.getTermFreqVector(intDocument, "title");
		
		
		String[] arrTitle = objTitle.getTerms();
		int[] freqTitle = objTitle.getTermFrequencies();
		
		arrSentences = (String[]) ArrayUtils.addAll(arrSentences, arrTitle);
		freqSentences = (int[]) ArrayUtils.addAll(freqSentences, freqTitle);
		
		ArrayList<String> wordsInTestSentence = new ArrayList<String>();
		
		BinaryVector objSentenceVector = new BinaryVector(_intVectorDimension).createZeroVector(_intVectorDimension);
		//for(String strWord : arrSentences){
		for(int k=0; k<arrSentences.length;k++){
		    String strWord = arrSentences[k];
		    int intFreq = freqSentences[k];
		    
		    strWord = prepareWord(strWord);
		    //strWSDTerm = prepareWord(strWSDTerm);
			
		    if (strWord == null)
			continue;
		    
		    if(strWSDTerm == null){
			System.out.println("Something is wrong here");
			continue;
		    }
		    
		    if(strWord.equalsIgnoreCase(strWSDTerm)) //do blood_pressure as well
			continue;
		    
		    if(wordsInTestSentence.contains(strWord))
			continue;
		    
		    BinaryVector objSemanticVector = (BinaryVector)_objSemanticVectors.getVector(strWord);
		    
		    //float weight = (float)Math.log(intFreq+1)*lUtils.getEntropy(new Term(_strTermField, strWSDTerm));
		    //float weight = (float)Math.log(intFreq+1)*lUtils.getEntropy(new Term(_strTermField, strWord));
		    objSentenceVector.superpose(objSemanticVector, 1, null);
		    
		    wordsInTestSentence.add(strWord);
		}
		try{
		objSentenceVector.normalize();
		} catch (Exception ex){  System.out.println("------------- " + pmid + " -------------"); continue; }
		
		objSentenceVector.bind(objTermVector);
		
		
		VectorSearcher vecSearcher;
		    LinkedList<SearchResult> results;
		    try {
			vecSearcher = new VectorSearcher.VectorSearcherCosine(
				_objElementalVectors, _objSenseVectors, null, objSentenceVector);
			results = vecSearcher.getNearestNeighbors(1);
		    } catch (ZeroVectorException zve) {
			// logger.info(zve.getMessage());
			results = new LinkedList<SearchResult>();
		    }

		    System.out.print(pmid + ";");
		    boolean bolFoundIt = false;
		    double dblVectorScore = 0;
		    String neighbors ="";
		    if(arrSenses[0].toString().equalsIgnoreCase(results.get(0).getObjectVector().getObject().toString())){
			    bolFoundIt = true;
			    dblVectorScore = results.get(0).getScore();
		    }
		    for (SearchResult objResult : results) {
			String strVectorName = objResult.getObjectVector().getObject()
				.toString();
			neighbors += "\"" +strVectorName + "\";" + objResult.getScore() + ";";
		    }
		    if(bolFoundIt)
			System.out.print("1;");
		    else
			System.out.print("0;");
		    
		    
		    System.out.print(strWsdWord+";");
		    System.out.println(neighbors);
		
	    }
	    
	}
	//VectorStoreWriter objVectorWriter = new VectorStoreWriter();
	//objVectorWriter.writeVectors("words.txt", _objElementalVectors);
	//objVectorWriter.writeVectors("senses.txt", _objSenseVectors);
	//objVectorWriter.writeVectors("semantic.txt", _objSemanticVectors);
    }
    }

    private static String prepareWord(String strWord) {
	if (lUtils.stoplistContains(strWord))
	    return null;
	
	String[] df = {_strTermField};
	if(!lUtils.termFilter(new Term(_strTermField,strWord), df, 1, 1500, 0)) // occurs at least twice
	    return null;
	
	return strWord;
    }

}
