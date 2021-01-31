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
    public static final long TABLESIZE = 611953L; //2100001L;	//611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    // ===================================================================
    
    public static long byteArrayToLong(byte[] bytes) {
    	long l = 0;
    	for (int i=0; i<8; i++) {
    		l <<= 8;
    		l ^= (long) bytes[i] & 0xff;
    	}
    	return l;
    }
    
    public static byte[] longToByteArray(long data) {
    	return new byte[] {
    	(byte)((data >> 56) & 0xff),
    	(byte)((data >> 48) & 0xff),
    	(byte)((data >> 40) & 0xff),
    	(byte)((data >> 32) & 0xff),
    	(byte)((data >> 24) & 0xff),
    	(byte)((data >> 16) & 0xff),
    	(byte)((data >> 8 ) & 0xff),
    	(byte)((data >> 0) & 0xff),
    	};
    }

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
    	public String serializeEntry() {
    		
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
    		
    		//serialized += "\n";
    		
    		return serialized;
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
        	//System.err.println("Printing this dataString: " + dataString);
            dataFile.seek( ptr );
            
            String finalString = String.format("%07d", dataString.getBytes().length) + dataString;
            
            byte[] data = finalString.getBytes();
            //System.err.println("Size of 7 digits padded: " + Integer.toString(String.format("%07d", data.length).getBytes().length));
            
            //data = finalString.getBytes();
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
        	
            dictionaryFile.seek( ptr );
            byte[] data = new byte[8];
            dictionaryFile.readFully( data );
            System.err.println("\n\n\n\n Reading " + Long.toString(byteArrayToLong(data)) + " from dictionary at position ptr=" + Long.toString(ptr));
            return byteArrayToLong(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            System.err.println("Aqui queria yo llegar");
            return (Long)null;
        }
    }
    
    boolean isDictionaryNull( long ptr ) {
    	try {
    		dictionaryFile.seek(ptr);
    		byte[] data = new byte[8];
    		dictionaryFile.readFully(data);
    		System.err.println("isDictionaryNull():\tContent of the " + Long.toString(ptr) + " ptr. Cell: " + Long.toString(byteArrayToLong(data)));
    		return byteArrayToLong(data)==0;
    	} catch ( IOException e ) {
            //e.printStackTrace();
    		System.err.println("isDictionaryNull() IOException");
    		return true;
        }
    }
    
    
    void writeDictionary( long ptr, long dataPtr ) {
    	try {
    		//System.err.println("Writing the dataPtr: " + Long.toString(dataPtr) + "\t en el ptr: " + Long.toString(ptr));
            dictionaryFile.seek( ptr );
            byte[] data = longToByteArray(dataPtr); //	8 bytes
            dictionaryFile.write( data );
        } catch ( IOException e ) {
            e.printStackTrace();
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
    	
    	writeData(entry.serializeEntry(), ptr);
    	
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {   
    	
    	String size = readData(ptr, 7);
    	
    	int sizeint = Integer.parseInt(size);
    	
    	String serializedEntry = readData(ptr+7, sizeint);
    	
    	System.err.println(serializedEntry);
    	
    	return new Entry(serializedEntry);
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
            String token;
            PostingsList postingsList;
            Entry entry;
            long hash;
            
            for (HashMap.Entry<String, PostingsList> item : index.entrySet()) {
                token = item.getKey();
                postingsList = item.getValue();
                
                hash = Math.abs(token.hashCode()%TABLESIZE);
                
                if(token.equals("they")) {
                	hash = 482480L;
                }
                
                if(isDictionaryNull(hash)) {
                	System.err.println(token + " escrita a la primera!");
                	writeDictionary(hash, free);
                } else {
                	System.err.println("COLISION CON LA PALABRA " + token);
                	collisions++;
                	hash+=8;
                	while(!isDictionaryNull(hash)) {
                		System.err.println("Y otro salto mas...");
                		hash+=8;
                	}
                	writeDictionary(hash, free);
                }
                
                /**
                System.err.println(hash);
                
                if(isDictionaryNull(hash)) {
                	System.err.println(token + " ha dado null en el dictionary.");
                } else {
                	System.err.println(token + " NOOOO ha dado null en el dictionary.");
                }
                
                
                writeDictionary(hash, free);
                */
                
                entry = new Entry(token, postingsList);
                
                free += Long.valueOf(writeData(entry.serializeEntry(), free));
                
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }
    
    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
    	
    	long hash = Math.abs(token.hashCode()%TABLESIZE);
    	
    	Entry e;
    	long ptr;
    	
    	while(!isDictionaryNull(hash)) { // Look for a token that is NOT in the index AND when its hash does not have a pointer in the dictionaryFile.
    		
    		ptr = readDictionary(hash);
    		
    		e = readEntry(ptr);
    		
    		if(e.word.equals(token)) {
    			return e.postingsList;
    		}
    		
    		hash++;
    	}
    	
    	System.err.println(token + " ha dao null.");
    	
    	return null;
    	
    }


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
