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
		System.err.println("Started merger with dataFileCount: " + Integer.toString(dataFileCount));
		//System.err.println("data" + Integer.toString(dataFileCount) + " and data_merged" + Integer.toString(dataFileCount) + " should exist now.");
		
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
			
			dataFile1.seek(ptr1);
			dataFile2.seek(ptr2);
		
			while(ptr1 < dataFile1.length()-5 && ptr2 < dataFile2.length()-5) {
				
				/**
				System.err.println("while(ptr1 < dataFile1.length()-5 && ptr2 < dataFile2.length()-5) {");
				System.err.println("ptr1: " + Long.toString(ptr1) + "\ndataFile1.length: " + Long.toString(dataFile1.length()));
				System.err.println("ptr2: " + Long.toString(ptr2) + "\ndataFile2.length: " + Long.toString(dataFile2.length()));
				System.err.println();
				*/
				
				
				entry1 = readEntry(ptr1, dataFile1);
				entry2 = readEntry(ptr2, dataFile2);
				
				//System.err.println("Read entry from dataFile1: " + entry1.word);
				//System.err.println("Read entry from dataFile2: " + entry1.word);
				//System.err.println();
				
				//System.err.println(entry1.word + "\t" + entry2.word);
				
				if(entry1.word.equals(entry2.word)) {
					//System.err.println(entry1.word + " and " + entry2.word + " are equal.");
					
					postingsList = mergePostingsLists(entry1.postingsList, entry2.postingsList);
					
					mergedEntry = new Entry(entry1.word, postingsList);
					
					merged_free += writeData(mergedEntry.serializeEntry(), merged_free, dataFileMerged);
					
					/**
					System.err.println(entry1.serializeEntry());
					System.err.println();
					System.err.println(entry2.serializeEntry());
					System.err.println();
					System.err.println(mergedEntry.serializeEntry());
					*/
					
					//Thread.sleep(1000);
					
					
					ptr1 += 7 + Integer.parseInt(readData(ptr1, 7, dataFile1));
					ptr2 += 7 + Integer.parseInt(readData(ptr2, 7, dataFile2));
					
				} else if (entry1.word.compareTo(entry2.word) < 0) {
					
					merged_free += writeData(entry1.serializeEntry(), merged_free, dataFileMerged);
					
					ptr1 += 7 + Integer.parseInt(readData(ptr1, 7, dataFile1));
					
				} else {
					merged_free += writeData(entry2.serializeEntry(), merged_free, dataFileMerged);
					
					ptr2 += 7 + Integer.parseInt(readData(ptr2, 7, dataFile2));
				}
			}
			
			System.err.println("Hemos salio");
			
			/**
			
			if(ptr1 < dataFile1.length()-5) {
				while(ptr1 < dataFile1.length()-5) {
					
					entry1 = readEntry(ptr1, dataFile1);
					
					merged_free += writeData(entry1.serializeEntry(), merged_free, dataFileMerged);
					
					ptr1 += 7 + Integer.parseInt(readData(ptr1, 7, dataFile1));
				}
			} else if(ptr2 < dataFile2.length()-5) {
					while(ptr2 < dataFile2.length()-5) {
					
					entry2 = readEntry(ptr2, dataFile2);
					
					merged_free += writeData(entry2.serializeEntry(), merged_free, dataFileMerged);
					
					ptr2 += 7 + Integer.parseInt(readData(ptr2, 7, dataFile2));
				}
			}
			*/
			
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Ha saltao error del merge eof probablemente.");
		}
		
		//dataFile1.close();
		//dataFile2.close();
		//dataFileMerged.close();
		
		System.err.println("Merge bien hecho (creo)");
		
	}
	
	public PostingsList mergePostingsLists(PostingsList p1, PostingsList p2) {
		
    	PostingsList answer = new PostingsList();
    	
    	int p1_idx = 0;
    	int p2_idx = 0;
    	
    	PostingsEntry postingsEntry1, postingsEntry2;
    	ArrayList<Integer> offsets;
    	
    	while(p1_idx < p1.size() && p2_idx < p2.size()) {
    		
    		postingsEntry1 = p1.get(p1_idx);
    		postingsEntry2 = p2.get(p2_idx);
    		
    		if(postingsEntry1.docID == postingsEntry2.docID) {
    			
    			offsets = mergeOffsets(postingsEntry1.offsetList, postingsEntry2.offsetList);
    			answer.add(new PostingsEntry(postingsEntry1.docID, offsets));
    			
    			p1_idx++;
    			p2_idx++;
    			
    		} else if (postingsEntry1.docID < postingsEntry2.docID) {
    			answer.add(postingsEntry1);
    			p1_idx++;
    		} else {
    			answer.add(postingsEntry2);
    			p2_idx++;
    		}
    	}
    	
    	if(p1_idx < p1.size()) {
    		for(int i=p1_idx; i<p1.size(); i++) {
    			answer.add(p1.get(i));
    		}
    	} else if (p2_idx < p2.size()) {
    		for(int i=p2_idx; i<p2.size(); i++) {
    			answer.add(p2.get(i));
    		}
    	}
    	
    	return answer;
	}
	
	public ArrayList<Integer> mergeOffsets(ArrayList<Integer> offsets1, ArrayList<Integer> offsets2) {
		HashSet<Integer> hashset = new HashSet<>();
		hashset.addAll(offsets1);
		hashset.addAll(offsets2);
		
		return new ArrayList<>(hashset);
	}
	/**
	public PostingsList postingsIntersection(PostingsList p1, PostingsList p2) {
		
    	PostingsList answer = new PostingsList();
    	
    	int p1_idx = 0;
    	int p2_idx = 0;
    	
    	
    	
    	Iterator<PostingsList> iter = postingsLists.iterator();
    	
    	PostingsList p1 = iter.next();
    	PostingsList p2; // = iter.next(); I need to put this inside the do while so that after the first one you can keep iterating.
    	
    	PostingsEntry postingsEntry1, postingsEntry2;
    	
    	int pe1_idx, pe2_idx;
    	
    	HashSet<Integer> hashset; 
    	
    	do {
    		
    		answer = new PostingsList();
    		
    		p2 = iter.next();
    		
    		if(p1==null && p2==null) {
    			System.err.println("Ambos son null");
    			return null;
    		} else if(p1==null) {
    			System.err.println("Uno null");
    			return p2;
    		} else if(p2==null) {
    			System.err.println("Uno null");
    			return p1;
    		}
    		
    		pe1_idx = 0;
    		pe2_idx = 0;
    		
    		postingsEntry1 = p1.get(pe1_idx++);
    		postingsEntry2 = p2.get(pe2_idx++);
    		
			if( postingsEntry1.docID == postingsEntry2.docID ) {
				
				hashset = new HashSet<>();
				hashset.addAll(postingsEntry1.offsetList);
				hashset.addAll(postingsEntry2.offsetList);
				
				answer.add(new PostingsEntry(postingsEntry1.docID, new ArrayList<>(hashset)));
				
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
			}
    		
			while( postingsEntry1 != null && postingsEntry2 != null ) {
    			
    			if( postingsEntry1.docID == postingsEntry2.docID ) {
    				
    				hashset = new HashSet<>();
    				hashset.addAll(postingsEntry1.offsetList);
    				hashset.addAll(postingsEntry2.offsetList);
    				
    				answer.add(new PostingsEntry(postingsEntry1.docID, new ArrayList<>(hashset)));
    				
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
    				
    				answer.add(postingsEntry1);
    				
    				if(pe1_idx >= p1.size()) {
    					postingsEntry1 = null;
    				} else {
    					postingsEntry1 = p1.get(pe1_idx++);
    				}
    			} else {
    				
    				answer.add(postingsEntry2);
    				
    				if(pe2_idx >= p2.size()) {
    					postingsEntry2 = null;
    				} else {
    					postingsEntry2 = p2.get(pe2_idx++);
    				}
    			}
    		}
			
			
			if(postingsEntry1 != null) {
				answer.add(postingsEntry1);
				while( (pe1_idx) < p1.size() ) {
					answer.add( p1.get(pe1_idx++) );
				}
			} else if(postingsEntry2 != null) {
				answer.add(postingsEntry2);
				while( (pe2_idx) < p2.size() ) {
					answer.add( p2.get(pe2_idx++) );
					System.err.println("atascao");
				}
			}
			
			
    		if( answer.size() > 0 && iter.hasNext() ) {
    			p1 = answer;
    		}
    		
    		
    	} while( iter.hasNext() ); // Used do while because the 2 long query wouldnt execute otherwise.
    	
    	return answer;
    }
	*/
	
}