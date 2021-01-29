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
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The dictionary file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 2100001L;	//611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    // ===================================================================

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
    	 * 	Gets the String serialization of an entry.
    	 * 	The following syntax is used: word#docID1*offset1*offset2#docID2*offset1*offset2*offset3}
    	 */
    	public String serializeEntry() {
    		
    		String serialized = this.word;
    		Iterator<PostingsEntry> iter = this.postingsList.iterator();
    		PostingsEntry entry;
    		ArrayList<Integer> offsetList;
    		
    		while(iter.hasNext()) {
    			entry = iter.next();
    			
    			serialized += "#" + Integer.toString(entry.docID);
    			
    			for( int offset : entry.offsetlist ) {
    				serialized += "*" + Integer.toString(offset);
    			}
    			
    		}
    		
    		serialized += "}";
    		
    		return serialized;
    	}
    	
    	/**
    	 * 	Gets an Entry object from a serialized entry.
    	 * 	The following syntax is used: word#docID1*offset1*offset2#docID2*offset1*offset2*offset3}
    	 */
    	public static Entry deserializeEntry(String s) {
    		
    		String word;
    		PostingsList postingsList;
    		String[] split;
    		String[] offsets;
    		
    		split = s.split("#");
    		
    		word = split[0];
    		
    		for( int i=1; i<split.length; i++ ) {
    			offsets = split[i].split("*");
    			
    			for( int j=1; j<offsets.length; j++ ) {
    				postingsList.add(offsets[0], offsets[j]);
    			}
    		}
    		
    		return new Entry(word, postingsList);
    	}
    }


    // ==================================================================
    
    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr );
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
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
     *  Reads data from the dictionary file
     */ 
    long readDictionary( long ptr ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[8];
            dataFile.readFully( data );
            return new Long(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }
    
    


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
        //
        //  YOUR CODE HERE
        //
    	//	mio
    	
    	writeData(serialize(entry), ptr);
    	
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {   
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE 
        //
    	
    	int entry_size = String.toInt(readData(ptr, 4));
    	
    	
    	
    	
        return null;
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for (Map.Entry<Integer,String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list

            // 
            //  YOUR CODE HERE
            //
            
            //	for all 
            
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }


    // ==================================================================


    public long getHash(String s) {
		long hashCode = 0;
		for(int i = 0; i < s.length(); i++) {
			hashCode = (31 * hashCode + s.charAt(i));
		}
		
		return hashCode%TABLESIZE;
	}
    
    
    
    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
    	
    	//	hacer hash de la palabra
    	long hash = getHash(token);
    	
    	
    	//	mirar la entry del dictionary file
    	long ptr = readDictionary(hash);
    	
    	
    	Entry e = readEntry(ptr);
    	
    	
    	//	con el ptr de la entry coger la info del datafile i parsearla a un PostingsList
    	
    	
    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        //  YOUR CODE HERE
        //
    	//to esto copiao del HashedIndex
    	PostingsList list = index.get(token);
    	if(list == null) {
    		list = new PostingsList();
    	}
    	list.add(docID, offset);
    	index.put(token, list);
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
