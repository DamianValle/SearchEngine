/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 * 
 *   Damian Valle, 2021
 */

package ir;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.util.*;
import java.nio.charset.StandardCharsets;


public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer,String> id2term = new HashMap<Integer,String>();

    /** Mapping from term strings to term ids */
    HashMap<String,Integer> term2id = new HashMap<String,Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String,List<KGramPostingsEntry>> index = new HashMap<String,List<KGramPostingsEntry>>();

    HashMap<String, Integer> numKGrams = new HashMap<String, Integer>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 2;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }


    /**
     *  Get intersection of two postings lists
     */
    private List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
    	
    	if (p1 == null || p2 == null) {
    		return null;
    	}
    	
    	int ptr1 = 0;
    	int ptr2 = 0;
    	
    	List<KGramPostingsEntry> list = new ArrayList<KGramPostingsEntry>();
    	
    	KGramPostingsEntry kgram1, kgram2;

    	while ( ptr1 < p1.size() && ptr2 < p2.size()) {
    		kgram1 = p1.get(ptr1);
    		kgram2 = p2.get(ptr2);
    		
    		if( kgram1.tokenID == kgram2.tokenID ) {
    			list.add(kgram1);
    			ptr1++;
    			ptr2++;
    		} else if( kgram1.tokenID < kgram2.tokenID ) {
    			ptr1++;
    		} else {
    			ptr2++;
    		}
    	}
    	
    	return list;
    }
    


    /** Inserts all k-grams from a token into the index. */
    public void insert( String token ) {
    	
    	if(term2id.containsKey(token)) {
    		return;
    	}
    	
    	int id = generateTermID();
    	term2id.put(token, id);
    	id2term.put(id, token);

        HashSet<String> kgrams = new HashSet<String>(kgrams(token));
        int size = kgrams.size();
        
        kgrams.stream().forEach(kgram -> {
        	List<KGramPostingsEntry> kgramList = index.getOrDefault(kgram, new ArrayList<KGramPostingsEntry>());
        	if(kgramList.size() == 0) {
        		KGramPostingsEntry kgramPE = new KGramPostingsEntry(id, size);
            	kgramList.add(kgramPE);
        	} else {
        		if(kgramList.get(kgramList.size() - 1).tokenID != id) {
        			KGramPostingsEntry kgramPE = new KGramPostingsEntry(id, size);
                	kgramList.add(kgramPE);
        		}
        	}
            index.put(kgram, kgramList);
        });
    }
    
    public void printKGrams(String s) {
    	
    	String[] kgrams = s.split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {

            if (postings == null) {
                postings = getPostings(kgram);
            } else {
                postings = intersect(postings, getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(getTermByID(postings.get(i).tokenID));
            }
        }
    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
    	return index.getOrDefault(kgram, null);
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }
    
    public List<String> kgrams(String token) {
        if (token.length() == 0) return List.of();
        
        String oreo = "^" + token + "$";

        List<String> answer = new ArrayList<String>();
        for(int i=0; i<oreo.length()-K+1; i++) {
            answer.add(oreo.substring(i, i+K));
        }

        return answer;
    }

    
    public List<String> getWildcardPostings(String term) {

    	int wcIdx = term.indexOf("*");

    	List<String> kgramsLeft = this.kgrams(term.substring(0, wcIdx));
        List<String> kgramsRight = this.kgrams(term.substring(wcIdx + 1, term.length()));

        if (kgramsLeft.size() > 1) kgramsLeft = kgramsLeft.subList(0, kgramsLeft.size()-1);
        if (kgramsRight.size()> 1)kgramsRight = kgramsRight.subList(1, kgramsRight.size());

        if(kgramsLeft.size() > 0) {
            kgramsLeft.addAll(kgramsRight);
        } else {
            kgramsLeft = kgramsRight;
        }

        List<String> rawPostings = kgramsLeft.stream().map(this::getPostings).reduce(this::intersect).orElse(List.of()).stream()
                .map(entry -> this.getTermByID(entry.tokenID)).collect(Collectors.toList());

        String testRegex = "^" + term.substring(0, wcIdx) + ".*" + term.substring(wcIdx + 1, term.length());
        List<String> postings = rawPostings.stream().filter(t -> t.matches(testRegex)).collect(Collectors.toList());
        
    	
    	return postings;
    }

    private static HashMap<String,String> decodeArgs( String[] args ) {
        HashMap<String,String> decodedArgs = new HashMap<String,String>();
        int i=0, j=0;
        while ( i < args.length ) {
            if ( "-p".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ( "-f".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ( "-k".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if ( "-kg".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println( "Unknown option: " + args[i] );
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String,String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
        Tokenizer tok = new Tokenizer( reader, true, false, true, args.get("patterns_file") );
        while ( tok.hasMoreTokens() ) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }
}
