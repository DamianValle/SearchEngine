/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import java.nio.ByteBuffer;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class ScalablePersistentHashedIndex extends PersistentHashedIndex {
	
	private TreeMap<String,PostingsList> index = new TreeMap<String,PostingsList>();
	
	public static final int LIMIT_TOKENS = 11;
	
	public static int tokens_inserted = 0;
	
	public static final long TABLESIZE = 611953L; //3499999L;
	
	/**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
    	
    	PostingsList list = index.get(token);
    	if(list == null) {
    		list = new PostingsList();
    	}
    	list.add(docID, offset);
    	index.put(token, list);
    	
    	if(++tokens_inserted == LIMIT_TOKENS) {
    		
    		//tokens_inserted = 0;
    		System.err.println("Alcanzado limite de tokens.");
    		
    		writeIndex();
    		index.clear();
    		free = 0L;
    		
    		System.err.println("EEEEEEEscrito.");
    		
    		Merger merge_thread = new Merger();
    		merge_thread.start();
    	}
    }
    
    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        String token;
        PostingsList postingsList;
        Entry entry;
            
        for (HashMap.Entry<String, PostingsList> item : index.entrySet()) {
                
            entry = new Entry(item.getKey(), item.getValue());
                
            free += Long.valueOf(writeData(entry.serializeEntry(), free));
        }
    }
  
    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
	
    
    
    
    
    
    
    
    
    
    
	
}
