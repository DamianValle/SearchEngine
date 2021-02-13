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
	
	public static final int LIMIT_TOKENS = 100000; //300000;
	
	public static int tokens_inserted = 0;
	
	public static final long TABLESIZE = 3499999L;
	
	public int first_insert = 0;
	
	public int dataFileCount = 0;
	
	public RandomAccessFile dataFile;
	
	public Merger merge_thread;
	
	public File toDelete, toDeleteMerge;
	
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
    	
    	tokens_inserted = index.size();
    	
    	if(tokens_inserted == LIMIT_TOKENS) {
    		
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
        		
        		System.err.println("This is the insert numero: " + Integer.toString(first_insert++));
        		try {
        			dataFile = new RandomAccessFile( "./index/data" + Integer.toString(dataFileCount), "rw" );
        			System.err.println("Created data" + Integer.toString(dataFileCount));
            		writeIndex(dataFile);
            		//dataFile.close();
            		
            		
            		
            		toDelete = new File("./index/data" + Integer.toString(dataFileCount-2));
            		toDeleteMerge = new File("./index/data_merged" + Integer.toString(dataFileCount-2));
            		
            		if (toDelete.delete()) { 
            		      System.out.println("Deleted the file: " + toDelete.getName());
            		} else {
            		      System.out.println("Failed to delete the file.");
            		}
            		
            		if (toDeleteMerge.delete()) {
            			System.out.println("Deleted the merge file: " + toDeleteMerge.getName());
            		} else {
            			System.out.println("Failed to delete the merge file.");
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
    		try {
    			writeDocInfo();
    		} catch (Exception e) {
    			System.err.println("Failed writedocinfo.");
    			e.printStackTrace();
    		}
    		
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
            
            String finalString = String.format("%09d", dataString.getBytes().length) + dataString;
            
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
    	System.err.println("Creating dictionary file...");
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
    			sizeint = Integer.parseInt(readFinalData(ptr, 9, finalDataFile));
    	
    			serializedEntry = readFinalData(ptr+9, sizeint, finalDataFile);
    			
    			entry = new Entry(serializedEntry);
    			
    			hash = Math.abs(entry.word.hashCode()%TABLESIZE);
                hash*=8;
                
                while(used_hashes.contains(hash)) {
                	hash+=8;
                }
                
                used_hashes.add(hash);
                
                writeDictionary(hash, ptr);
    			
    			ptr += 9 + sizeint;
    		}
    	} catch(Exception e) {
    		System.err.println("Dictionary done!");
    	}
    	
    }
    
    public void renameLastMerge(int dataFileCount) {
    	
    	File toDelete = new File("./index/data");
		
		if (toDelete.delete()) { 
		      System.out.println("Deleted the file: " + toDelete.getName());
		} else {
		      System.out.println("Failed to delete the file.");
		}
    	
    	File data_merged = new File("index/data_merged" + Integer.toString(dataFileCount));
    	File data = new File("index/data");
    	
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
    		//dataFile.close();
    		
    		try {
        		merge_thread.join();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
    		
    		
    		merge_thread = new Merger(dataFileCount);
    		merge_thread.start();
    		
    		try {
        		merge_thread.join();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
    		
    		
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	
    	
    	dataFileCount++;
    	
    	
    	
    	System.err.println("Last merge done");
    }
    
    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        //System.err.println( index.keySet().size() + " unique words" );
        //System.err.print( "Writing index to disk..." );
        //writeIndex();
        System.err.println( "done!" );
        //System.err.println( "aqui cambiar el nombre del fichero a data y hacer el dictionaryfile!" );
        
        
        System.err.println(dataFileCount);
        
        lastMerge();
        
        renameLastMerge(dataFileCount);
        
        createDictionary();
        
        System.err.println("ALL DONE!!!");
    }
	
    
    
    
    
    
    
    
    
    
    
	
}
