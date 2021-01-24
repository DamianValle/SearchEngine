/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType ) { 
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
    	
    	//Iterator iter = query.queryterm.iterator();
    	
    	//while(iter.hasNext()) {
    	//	System.err.println(iter.next());
    	//}
    	
    	ArrayList<PostingsList> postingsLists = new ArrayList<PostingsList>();
    	
    	//here iterate over query tokens
    	String token = "zombie";
    	postingsLists.add(index.getPostings(token));
    	token = "attack";
    	postingsLists.add(index.getPostings(token));
    	
    	
    	if( queryType == QueryType.INTERSECTION_QUERY ) {
    		System.err.println("Selected Intersection Query");
    		
    		if(postingsLists.size() == 1) {
    			System.err.println("query of size 1");
    			return index.getPostings(token);
    		} else if (postingsLists.size() > 1){
    			System.err.println("query of size more than 1");
    			return postingsIntersection(postingsLists);
    		} else {
    			System.err.println("Wrong query size.");
    		}
        	
    	} else if ( queryType == QueryType.PHRASE_QUERY ) {
    		System.err.println("Selected Phrase Query");
    		
    	} else if ( queryType == QueryType.RANKED_QUERY ) {
    		System.err.println("Selected Ranked Query");
    		
    	}
    	
    	return null;
    }
    
    
    private PostingsList postingsIntersection(ArrayList<PostingsList> postingsLists) { //can we assume docIds are in order?
    	PostingsList answer = new PostingsList();
    	
    	Iterator<PostingsList> iter = postingsLists.iterator();
    	
    	System.err.println("Size of the postings arraylist: " + postingsLists.size());
    	
    	PostingsList p1 = iter.next();
    	PostingsList p2; // = iter.next(); I need to put this inside the do while so that after the first one you can keep iterating.
    	
    	for( int i = 0; i < p1.size(); i++ ) {
    		System.err.println(p1.get(i).docID);
    	} // Checks if docIDs are sorted in ascending order.
    	
    	Iterator<PostingsEntry> postingsEntryIterator1, postingsEntryIterator2;
    	PostingsEntry postingsEntry1, postingsEntry2;
    	
    	do {
    		
    		p2 = iter.next();
    		
    		postingsEntryIterator1 = p1.iterator();
    		postingsEntryIterator2 = p2.iterator();
    		
    		while(postingsEntryIterator1.hasNext() && postingsEntryIterator2.hasNext()) {
    			postingsEntry1 = postingsEntryIterator1.next();
    			postingsEntry2 = postingsEntryIterator2.next();
    			
    			if( postingsEntry1.docID == postingsEntry2.docID ) {
    				System.err.println("Found match!");
    				answer.add(postingsEntry1); // Need to add new add() method.
    				
    				postingsEntry1 = postingsEntryIterator1.next();
    				postingsEntry2 = postingsEntryIterator2.next();
    			} else if ( postingsEntry1.docID < postingsEntry2.docID ) {
    				postingsEntry1 = postingsEntryIterator1.next();
    			} else {
    				postingsEntry2 = postingsEntryIterator2.next();
    			}
    		}
    		
    		if( answer.size() > 0 && iter.hasNext() ) {
    			p1 = answer;
    		}
    		
    		
    	} while( iter.hasNext() ); // Used do while because the 2 long query wouldnt execute otherwise.
    	
    	return answer;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}