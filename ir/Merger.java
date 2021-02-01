/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Damian Valle, 2020
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import java.nio.ByteBuffer;

public class Merger extends Thread {
	
	/** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The data file 1 name */
    public static final String DATA_FNAME1 = "data1";

    /** The data file 2 name */
    public static final String DATA_FNAME2 = "data2";
    
    /** The data file merged name */
    public static final String DATA_FNAME3 = "data_merged";
    
    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile1;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile2;
    
    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFileMerged;
    
    private long merged_free = 0L;
    
    /**
     *   A helper class representing one entry in the dictionary hashtable.
     *   IS THIS ENTRY REFERRING A LONG POINTER OR THE FULL <WORD, POSTINGSLISTS>?
     */ 
    public class Entry {
        
    	public String word;
    	public PostingsList postingsList;
    	
    	public Entry(String word, PostingsList postingsList) {
    		this.word = word;
        	this.postingsList = postingsList;
        }
    	
    	/**
    	 * 	Gets an Entry object from a serialized entry.
    	 * 	The following syntax is used: word#docID1;offset1;offset2#docID2;offset1*offset2*offset3
    	 * 
    	 * 	Feisimo usar esto de constructor a ver si lo arreglo.
    	 */
    	public Entry(String s) {
    		PostingsList postingsList = new PostingsList();
    		String[] split;
    		String[] offsets;
    		
    		split = s.split("#");
    		
    		this.word = split[0];
    		
    		for( int i=1; i<split.length; i++ )	{
    			
    			offsets = split[i].split(";");
    			
    			for( int j=1; j<offsets.length; j++ ) {
    				postingsList.add(Integer.parseInt(offsets[0]), Integer.parseInt(offsets[j]));
    			}
    		}
    		
    		this.postingsList = postingsList;
    	}
    	
    	/**
    	 * 	Gets the String serialization of an entry.
    	 * 	The following syntax is used: word#docID1;offset1;offset2#docID2;offset1;offset2;offset3
    	 */
    	public String serialize() {
    		
    		String serialized = this.word;
    		Iterator<PostingsEntry> iter = this.postingsList.iterator();
    		PostingsEntry entry;
    		ArrayList<Integer> offsetList;
    		
    		while(iter.hasNext()) {
    			entry = iter.next();
    			
    			serialized += "#" + Integer.toString(entry.docID);
    			
    			for( int offset : entry.offsetList ) {
    				serialized += ";" + Integer.toString(offset);
    			}
    			
    		}
    		
    		return serialized;
    	}
    	
    	public String serializeEntry() {
    		
    		var builder = new StringBuilder();
    		
    		builder.append(this.word);
    		
    		Iterator<PostingsEntry> iter = this.postingsList.iterator();
    		PostingsEntry entry;
    		
    		while(iter.hasNext()) {
    			entry = iter.next();
    			
    			builder.append("#" + Integer.toString(entry.docID));
    			
    			for( int offset : entry.offsetList ) {
    				builder.append(";" + Integer.toString(offset));
    			}
    			
    		}
    		
    		
    		return builder.toString();
    	}
    	
    }
	
	public Merger(int dataFileCount) {
		try {
			dataFile1 = new RandomAccessFile( "./index/data" + Integer.toString(dataFileCount), "rw" );
			dataFile2 = new RandomAccessFile( "./index/data_merged" + Integer.toString(dataFileCount), "rw" );
			dataFileMerged = new RandomAccessFile( "./index/data_merged" + Integer.toString(dataFileCount+1), "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
	}
	
	public Entry readEntry(long ptr, RandomAccessFile dataFile) {
		
		int size = Integer.parseInt(readData(ptr, 7, dataFile));
		String serializedEntry = readData(ptr+7, size, dataFile);
		
		return new Entry(serializedEntry);
	}
	
	/**
     *  Reads data from a data file
     */ 
    String readData( long ptr, int size , RandomAccessFile dataFile) {
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
	
	public void run() {
		
		long ptr1 = 0L;
		long ptr2 = 0L;
		
		Entry entry1;
		Entry entry2;
		
		Entry mergedEntry;
		
		ArrayList<PostingsList> postingsLists = new ArrayList<PostingsList>();
		
		PostingsList postingsList;
		
		try {
			
			System.err.println("Length of dataFile1: " + dataFile1.length());
			System.err.println("Length of dataFile2: " + dataFile2.length());
			
			dataFile1.seek(ptr1);
			dataFile2.seek(ptr2);
		
			while(ptr1 < dataFile1.length() - 5 && ptr2 < dataFile2.length()) {
				
				entry1 = readEntry(ptr1, dataFile1);
				entry2 = readEntry(ptr2, dataFile2);
				
				System.err.println(entry1.word + "\t" + entry2.word);
				
				if(entry1.word.equals(entry2.word)) {
					System.err.println(entry1.word + " and " + entry2.word + " are equal.");
					postingsLists.add(entry1.postingsList);
					postingsLists.add(entry2.postingsList);
					
					postingsList = postingsIntersection(postingsLists);
					
					mergedEntry = new Entry(entry1.word, postingsList);
					
					merged_free += writeData(mergedEntry.serializeEntry(), merged_free, dataFileMerged);
					
					ptr1 += 7 + Integer.parseInt(readData(ptr1, 7, dataFile1));
					ptr2 += 7 + Integer.parseInt(readData(ptr2, 7, dataFile2));
					
					
				} else if (entry1.word.compareTo(entry2.word) < 0) {
					System.err.println(entry1.word + " es mas pequeña que " + entry2.word);
					
					merged_free += writeData(entry1.serializeEntry(), merged_free, dataFileMerged);
					
					ptr1 += 7 + Integer.parseInt(readData(ptr1, 7, dataFile1));
				} else {
					merged_free += writeData(entry2.serializeEntry(), merged_free, dataFileMerged);
					
					ptr2 += 7 + Integer.parseInt(readData(ptr2, 7, dataFile2));
				}
				
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Sacabao el merge");
		}
		
		System.err.println("Intentando borrar el data_merge");
		
		
	}
	
	public PostingsList postingsIntersection(ArrayList<PostingsList> postingsLists) {
    	PostingsList answer;
    	
    	Iterator<PostingsList> iter = postingsLists.iterator();
    	
    	PostingsList p1 = iter.next();
    	PostingsList p2; // = iter.next(); I need to put this inside the do while so that after the first one you can keep iterating.
    	
    	/**
    	for( int i = 0; i < p1.size(); i++ ) {
    		System.err.println(p1.get(i).docID);
    	} // Checks if docIDs are sorted in ascending order.
    	*/
    	
    	//Iterator<PostingsEntry> postingsEntryIterator1, postingsEntryIterator2;
    	PostingsEntry postingsEntry1, postingsEntry2;
    	
    	int pe1_idx, pe2_idx;
    	
    	do {
    		
    		answer = new PostingsList();
    		
    		p2 = iter.next();
    		
    		//p1.add(null);
    		//p2.add(null);
    		
    		if(p1==null || p2==null) {
    			return null;
    		}
    		
    		/**
    		System.err.println("p1 posting list: ");
    		for( int i = 0; i < p1.size(); i++ ) {
        		System.err.println(p1.get(i).docID);
        	}
    		
    		System.err.println("p2 posting list: ");
    		for( int i = 0; i < p2.size(); i++ ) {
        		System.err.println(p2.get(i).docID);
        	}
        	*/
    		
    		pe1_idx = 0;
    		pe2_idx = 0;
    		
    		postingsEntry1 = p1.get(pe1_idx++);
    		postingsEntry2 = p2.get(pe2_idx++);
    		
			if( postingsEntry1.docID == postingsEntry2.docID ) {
				
				answer.add(postingsEntry1);
				
				if(pe1_idx >= p1.size()) {
					postingsEntry1 = null;
				} else {
					postingsEntry1 = p1.get(pe1_idx++);
				}
				
				if(pe2_idx >= p2.size()) {
					postingsEntry2 = null;
				} else {
					postingsEntry2 = p2.get(pe2_idx++);
				}
				
				//postingsEntry1 = p1.get(pe1_idx++);
				//postingsEntry2 = p2.get(pe2_idx++);
			}
    		
			while( postingsEntry1 != null && postingsEntry2 != null ) {
				
    			//System.err.println("p1 docID: " + postingsEntry1.docID + " ||| p2 docID: " + postingsEntry2.docID);
    			
    			if( postingsEntry1.docID == postingsEntry2.docID ) {
    				//System.err.println("Found match!");
    				answer.add(postingsEntry1);
    				
    				if(pe1_idx >= p1.size()) {
    					postingsEntry1 = null;
    				} else {
    					postingsEntry1 = p1.get(pe1_idx++);
    				}
    				
    				if(pe2_idx >= p2.size()) {
    					postingsEntry2 = null;
    				} else {
    					postingsEntry2 = p2.get(pe2_idx++);
    				}
    				
    				
    			} else if ( postingsEntry1.docID < postingsEntry2.docID ) {
    				if(pe1_idx >= p1.size()) {
    					postingsEntry1 = null;
    				} else {
    					postingsEntry1 = p1.get(pe1_idx++);
    				}
    			} else {
    				if(pe2_idx >= p2.size()) {
    					postingsEntry2 = null;
    				} else {
    					postingsEntry2 = p2.get(pe2_idx++);
    				}
    			}
    		}
			
			
    		
    		if( answer.size() > 0 && iter.hasNext() ) {
    			//System.out.println("Iteramos nueva palabra.");
    			//System.out.println("answer size: " + answer.size());
    			p1 = answer;
    		}
    		
    		
    	} while( iter.hasNext() ); // Used do while because the 2 long query wouldnt execute otherwise.
    	
    	return answer;
    }
	
	
}