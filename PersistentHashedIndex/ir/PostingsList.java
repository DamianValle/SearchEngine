/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.Iterator;

public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();
    
    /** The postings list iterator method. */
    public Iterator<PostingsEntry> iterator(){
        return list.iterator();
    }

    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
    return list.get( i );
    }
    
    /** Adds a new entry or creates it if it does not yet exist. */
    public void add( int docID, int offset ) {
    	
    	if(!list.isEmpty()) {
    	PostingsEntry lastEntry = list.get(list.size()-1);
    	
    	//Potential performance improvement merging both ifs into one and checking that if first one doesnt fulfill the second condition does not evaluate.
    	
    	if( lastEntry.docID == docID ) {
    		// If we find a match then we just add the offset and leave.
    		lastEntry.offsetList.add(offset);
    		return;
    	} else {
    		for( PostingsEntry entry : list ) {
        		if( entry.docID == docID ) {
        			// If we find a match then we just add the offset and leave.
        			entry.offsetList.add(offset);
        			return;
        		}
        	}
    	}
    	}
    	
    	// If there is no match then we create an empty offsetList, add the one offset we have, create a Postings entry and add docID and offsetList.
    	ArrayList<Integer> offsetList = new ArrayList<Integer>();
    	offsetList.add(offset);
    	
    	PostingsEntry entry = new PostingsEntry(docID, offsetList);
    	list.add(entry);
    }
    
    /** Adds a new entry. */
    public void add( PostingsEntry p) {
    	
    	list.add(p);
    }
    
}

