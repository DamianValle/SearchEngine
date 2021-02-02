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
	
	public static final int LIMIT_TOKENS = 1000000;
	
	public static int tokens_inserted = 0;
	
	public static final long TABLESIZE = 611953L; //3499999L;
	
	public int first_insert = 0;
	
	public int dataFileCount = 0;
	
	public RandomAccessFile dataFile;
	
	public Merger merge_thread;
	
	public File toDelete;
	
	RandomAccessFile finalDataFile;
	
	
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
    		
    		System.err.println("Alcanzado limite de tokens.");
    		
    		if(first_insert == 0) {
        		System.err.println("This is my first insert");
        		first_insert++;
        		try {
        			dataFile = new RandomAccessFile( "./index/data_merged" + Integer.toString(dataFileCount), "rw" );
            		writeIndex(dataFile);
            		dataFile.close();
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
        		
        		dataFileCount--;
        		
        		
        	} else if (first_insert == 1){
        		first_insert++;
        		System.err.println("This is my second insert");
        		
        		try {
        			dataFile = new RandomAccessFile( "./index/data" + Integer.toString(dataFileCount), "rw" );
            		writeIndex(dataFile);
            		dataFile.close();
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
        		
        		merge_thread = new Merger(dataFileCount);
        		merge_thread.start();
        		
        	} else {
        		
        		System.err.println("This is the insert numer: " + Integer.toString(first_insert++));
        		try {
        			dataFile = new RandomAccessFile( "./index/data" + Integer.toString(dataFileCount), "rw" );
        			System.err.println("Created data" + Integer.toString(dataFileCount));
            		writeIndex(dataFile);
            		//dataFile.close();
            		
            		
            		
            		toDelete = new File("./index/data" + Integer.toString(dataFileCount-2)); //probably -1 pero que pasa con el merge_thread?
            		
            		if (toDelete.delete()) { 
            		      System.out.println("Deleted the file: " + toDelete.getName());
            		} else {
            		      System.out.println("Failed to delete the file.");
            		}
            		
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
        		
        		
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
    
    String readFinalData( long ptr, int size, RandomAccessFile dataFile ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }
  
    public void createDictionary() {
    	try {
    		finalDataFile = new RandomAccessFile( "./index/data", "rw" );
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	long ptr = 0L;
    	int sizeint = 0;
    	Entry entry;
    	String serializedEntry;
    	long hash;
    	HashSet<Long> used_hashes = new HashSet<Long>();
    	
    	try {
    		//while(ptr < dataFile.length()-5) {
    		while(true) {
    			
    			finalDataFile.seek(ptr);
    			sizeint = Integer.parseInt(readFinalData(ptr, 7, finalDataFile));
    	
    			serializedEntry = readFinalData(ptr+7, sizeint, finalDataFile);
    			
    			entry = new Entry(serializedEntry);
    			
    			hash = Math.abs(entry.word.hashCode()%TABLESIZE);
                hash*=8;
                
                while(used_hashes.contains(hash)) {
                	hash+=8;
                }
                
                used_hashes.add(hash);
                
                writeDictionary(hash, ptr);
    			
    			ptr += 7 + sizeint;
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		System.err.println("Error in CreateDictionary()");
    	}
    	
    }
    
    public void renameLastMerge(int dataFileCount) {
    	
    	File toDelete = new File("./index/data");
		
		if (toDelete.delete()) { 
		      System.out.println("Deleted the file: " + toDelete.getName());
		} else {
		      System.out.println("Failed to delete the file.");
		}
    	
    	File data_merged = new File("./index/data_merged" + Integer.toString(dataFileCount));
    	File data = new File("./index/data");
    	
    	if(data_merged.renameTo(data)) {
    		System.err.println("Rename success");
    	} else {
    		System.err.println("Rename error");
    	}
    }
    
    public void lastMerge() {
    	
    	try {
    		dataFile = new RandomAccessFile( "./index/data" + Integer.toString(dataFileCount), "rw" );
    		System.err.println("Created data" + Integer.toString(dataFileCount));
    		writeIndex(dataFile);
    		dataFile.close();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	try {
    		merge_thread.join();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        //System.err.println( index.keySet().size() + " unique words" );
        //System.err.print( "Writing index to disk..." );
        //writeIndex();
        System.err.println( "done!" );
        System.err.println( "aqui cambiar el nombre del fichero a data y hacer el dictionaryfile!" );
        
        
        System.err.println(dataFileCount);
        
        //lastMerge();
        
        System.err.println("A que acabe el ultmio merge");
        try {
    		merge_thread.join();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
        System.err.println("Acabao el ultmio merge");
        
        renameLastMerge(dataFileCount);
        
        createDictionary();
    }
	
    
    
    
    
    
    
    
    
    
    
	
}
