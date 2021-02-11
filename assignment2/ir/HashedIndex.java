/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;
import java.io.*;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
    	
    	PostingsList list = index.get(token);
    	if(list == null) {
    		list = new PostingsList();
    	}
    	list.add(docID, offset);
    	index.put(token, list);
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        return index.get(token);
    }
    
    public PostingsList getPostingsOnTheFly( String token ) {
        return index.get(token);
    }
    
    
    public void loadPageRank() {
    	try {
    		File f = new File("./pagerank/rankDoc.txt");
    		FileReader freader = new FileReader(f);
    		
    		System.err.println("Opened file");
    	
    		BufferedReader br = new BufferedReader(freader);
    		String line;
    		while ((line = br.readLine()) != null) {
    			String[] entry = line.split(":");
    			docPageRank.put(Integer.parseInt(entry[0]), Double.parseDouble(entry[1]));
    		}
    		freader.close();
    	} catch (Exception e) {
    		System.err.println("ojico maño");
    	}
    }
    



    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
