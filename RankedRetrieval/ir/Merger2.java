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
	
	public String readEntry(long ptr, RandomAccessFile dataFile) {
		
		int size = Integer.parseInt(readData(ptr, 7, dataFile));
		String serializedEntry = readData(ptr+7, size, dataFile);
		
		return serializedEntry;
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
		
		String entry1;
		String entry2;
		
		
		
		try {
			
			dataFile1.seek(ptr1);
			dataFile2.seek(ptr2);
			
			entry1 = readEntry(ptr1, dataFile1);
			entry2 = readEntry(ptr2, dataFile2);
		
			while(ptr1 < dataFile1.length() - 5 && ptr2 < dataFile2.length()) {
				
				if(entry1.split("#")[0].equals(entry2.split("#")[0])) {
					
					merged_free += writeData(mergeEntries(entry1, entry2), merged_free, dataFileMerged);
					
					ptr1 += 7 + Integer.parseInt(readData(ptr1, 7, dataFile1));
					ptr2 += 7 + Integer.parseInt(readData(ptr2, 7, dataFile1));
					
					entry1 = readEntry(ptr1, dataFile1);
					entry2 = readEntry(ptr2, dataFile2);
					
					
				} else if (entry1.split("#")[0].compareTo(entry2.split("#")[0]) < 0) {
					//System.err.println(entry1.word + " es mas pequeña que " + entry2.word);
					
					merged_free += writeData(entry1, merged_free, dataFileMerged);
					
					ptr1 += 7 + Integer.parseInt(readData(ptr1, 7, dataFile1));
					
					entry1 = readEntry(ptr1, dataFile1);
					
				} else {
					merged_free += writeData(entry2, merged_free, dataFileMerged);
					
					ptr2 += 7 + Integer.parseInt(readData(ptr2, 7, dataFile2));
					
					entry2 = readEntry(ptr2, dataFile2);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Ha saltao error del merge eof probablemente.");
		}
		
		//dataFile1.close();
		//dataFile2.close();
		//dataFileMerged.close();
		
		System.err.println("Merge bien hecho (creo)");
		
	}
	
	public String mergeEntries(String entry1, String entry2) {
		var builder = new StringBuilder();
		
		String[] split1 = entry1.split("#");
		String[] split2 = entry2.split("#");
		
		//	word#docID1;offset1;offset2#docID2;offset1*offset2*offset3
		//	word#docID3;offset1;offset2#docID2;offset1*offset2*offset3
		
		
		builder.append(split1[0]);
		
		int docid1, docid2;
		int idx1 = 1;
		int idx2 = 1;
		
		HashSet<Integer> hashset;
		
		docid1 = Integer.parseInt(split1[idx1].split(";")[0]);
		docid2 = Integer.parseInt(split2[idx2].split(";")[0]);
		
		while( idx1 < split1.length && idx2 < split2.length ) {
			docid1 = Integer.parseInt(split1[idx1][0]);
			docid2 = Integer.parseInt(split2[idx2][0]);
			
			if(docid1 == docid2) {
				hashset = new HashSet<>();
				for(int i=1; i<split1[idx1][1].split(";").length; i++) {
					hashset.add(Integer.parseInt(split1[idx1].split(";")[i]));
				}
				for(int i=1; i<split2[idx1][1].split(";").length; i++) {
					hashset.add(Integer.parseInt(split2[idx2].split(";")[i]));
				}
				
				builder.append(Integer.toString(docid1));
				
				for (int offset : hashset) {
					builder.append(";" + Integer.toString(offset));
				}
				
			} else if(docid1 < docid2) {
				builder.append(split1[idx1]);
				idx1++;
			} else {
				builder.append(split2[idx2]);
				idx2++;
			}
		}
		
		
		return builder.toString();
		
		
		
		
		
		
	}
	
	
}