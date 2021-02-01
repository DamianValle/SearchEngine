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
	
	public static final int LIMIT_TOKENS = 100;
	
	public static int tokens_inserted = 0;
	
	public static final long TABLESIZE = 611953L; //3499999L;
	
	public int first_insert = 0;
	
	public int dataFileCount = 0;
	
	/**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
    	
    	Merger merge_thread = new Merger(dataFileCount);
    	
    	PostingsList list = index.get(token);
    	if(list == null) {
    		list = new PostingsList();
    	}
    	list.add(docID, offset);
    	index.put(token, list);
    	
    	if(++tokens_inserted == LIMIT_TOKENS) {
    		
    		System.err.println("Alcanzado limite de tokens.");
    		
    		if(first_insert == 0) {
        		System.err.println("This is my first insert");
        		first_insert++;
        		try {
        			RandomAccessFile dataFile = new RandomAccessFile( "./index/data_merged" + Integer.toString(dataFileCount), "rw" );
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
        		writeIndex(dataFile);
        		dataFileCount--;
        		
        	} else if (first_insert == 1){
        		first_insert++;
        		System.err.println("This is my second insert");
        		
        		try {
        			RandomAccessFile dataFile = new RandomAccessFile( "./index/data" + Integer.toString(dataFileCount), "rw" );
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
        		writeIndex(dataFile); //ojo
        		
        		merge_thread = new Merger(dataFileCount);
        		merge_thread.start();
        		
        	} else {
        		try {
        			RandomAccessFile dataFile = new RandomAccessFile( "./index/data" + Integer.toString(dataFileCount), "rw" );
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
        		writeIndex(dataFile);
        		
        		try {
        			merge_thread.join();
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
        		merge_thread = new Merger(dataFileCount);
        		merge_thread.start();
        	}
    		
    		dataFileCount++;
    		tokens_inserted = 0;
    		free = 0L;
    		index.clear();
    		
    	}
    }
    
    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr, RandomAccessFile dataFile ) {
        try {
            dataFile.seek( ptr );
            
            String finalString = String.format("%07d", dataString.getBytes().length) + dataString;
            
            byte[] data = finalString.getBytes();
            
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     *  Write the index to files.
     */
    public void writeIndex(RandomAccessFile dataFile) {
        String token;
        PostingsList postingsList;
        Entry entry;
            
        for (HashMap.Entry<String, PostingsList> item : index.entrySet()) {
                
            entry = new Entry(item.getKey(), item.getValue());
                
            free += Long.valueOf(writeData(entry.serializeEntry(), free, dataFile));
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
