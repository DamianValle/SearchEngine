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
       
    	/**
    	Iterator<QueryTerm> iter = query.iterator();
    	
    	while(iter.hasNext()) {
    		System.err.println(iter.next().getTerm());
    	}
    	*/
    	
    	ArrayList<String> queries = query.getQueryTerms();
    	
    	ArrayList<PostingsList> postingsLists = new ArrayList<PostingsList>();
    	
    	for(String queryterm : queries) {
    		System.err.println("Added " + queryterm + " to the query postings list.");
    		postingsLists.add(index.getPostings(queryterm));
    	}
    	
    	/**
    	//here iterate over query tokens
    	String token = "a";
    	postingsLists.add(index.getPostings(token));
    	token = "cell";
    	postingsLists.add(index.getPostings(token));
    	token = "phone";
    	postingsLists.add(index.getPostings(token));
    	*/
    	
    	
    	if( queryType == QueryType.INTERSECTION_QUERY ) {
    		System.err.println("Selected Intersection Query");
    		
    		if(postingsLists.size() == 1) {
    			System.err.println("query of size 1");
    			//return index.getPostings(token);
    			return index.getPostings(queries.get(0));
    		} else if (postingsLists.size() > 1){
    			System.err.println("Intersection Mode");
    			System.err.println("Query of size: " + postingsLists.size());
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
    
    
    private PostingsList postingsIntersection(ArrayList<PostingsList> postingsLists) {
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
    		
    		System.err.println("p1 posting list: ");
    		for( int i = 0; i < p1.size(); i++ ) {
        		System.err.println(p1.get(i).docID);
        	}
    		
    		System.err.println("p2 posting list: ");
    		for( int i = 0; i < p2.size(); i++ ) {
        		System.err.println(p2.get(i).docID);
        	}
    		
    		pe1_idx = 0;
    		pe2_idx = 0;
    		
    		//postingsEntryIterator1 = p1.iterator();
    		//postingsEntryIterator2 = p2.iterator();
    		//postingsEntry1 = postingsEntryIterator1.next();
			//postingsEntry2 = postingsEntryIterator2.next();
    		
    		postingsEntry1 = p1.get(pe1_idx++);
    		postingsEntry2 = p2.get(pe2_idx++);
    		
    		System.err.println("PostingsEntry1: " + postingsEntry1 + "PostingsEntry2: " + postingsEntry2);
			
			if( postingsEntry1.docID == postingsEntry2.docID ) {
				System.err.println("Match in the first one!");
				answer.add(postingsEntry1);
				//postingsEntry1 = postingsEntryIterator1.next();
				//postingsEntry2 = postingsEntryIterator2.next();
				postingsEntry1 = p1.get(pe1_idx++);
				postingsEntry2 = p2.get(pe2_idx++);
			}
    		
    		//while(postingsEntryIterator1.hasNext() && postingsEntryIterator2.hasNext()) {
			while( pe1_idx < p1.size() && pe2_idx < p2.size() ) {
    			
    			System.err.println("p1 docID: " + postingsEntry1.docID + " ||| p2 docID: " + postingsEntry2.docID);
    			
    			if( postingsEntry1.docID == postingsEntry2.docID ) {
    				System.err.println("Found match!");
    				answer.add(postingsEntry1); // Need to add new add() method.
    				
    				postingsEntry1 = p1.get(pe1_idx++);
    				postingsEntry2 = p2.get(pe2_idx++);
    			} else if ( postingsEntry1.docID < postingsEntry2.docID ) {
    				postingsEntry1 = p1.get(pe1_idx++);
    			} else {
    				postingsEntry2 = p2.get(pe2_idx++);
    			}
    		}
    		
    		if( answer.size() > 0 && iter.hasNext() ) {
    			System.out.println("Iteramos nueva palabra.");
    			System.out.println("answer size: " + answer.size());
    			p1 = answer;
    		}
    		
    		
    	} while( iter.hasNext() ); // Used do while because the 2 long query wouldnt execute otherwise.
    	
    	return answer;
    }
    
    
    private PostingsList postingsPhrase(ArrayList<PostingsList> postingsLists) {
    	
    	PostingsList answer;
    	
    	Iterator<PostingsList> iter = postingsLists.iterator();
    	
    	PostingsList p1 = iter.next();
    	PostingsList p2; // = iter.next(); I need to put this inside the do while so that after the first one you can keep iterating.
    	
    	int current_offset = 0; //distance from the initial word in the query.
    	int pe1_idx, pe2_idx;
    	
    	PostingsEntry postingsEntry1, postingsEntry2;
    	
    	do {
    		
    		answer = new PostingsList();
    		
    		p2 = iter.next();
    		
    		pe1_idx = 0;
    		pe2_idx = 0;
    		
    		postingsEntry1 = p1.get(pe1_idx++);
    		postingsEntry2 = p2.get(pe2_idx++);
    		
    		System.err.println("PostingsEntry1: " + postingsEntry1 + "PostingsEntry2: " + postingsEntry2);
			
			if( postingsEntry1.docID == postingsEntry2.docID ) {
				System.err.println("Match in the first one!");
				answer.add(postingsEntry1);
				//postingsEntry1 = postingsEntryIterator1.next();
				//postingsEntry2 = postingsEntryIterator2.next();
				postingsEntry1 = p1.get(pe1_idx++);
				postingsEntry2 = p2.get(pe2_idx++);
			}
    		
    		//while(postingsEntryIterator1.hasNext() && postingsEntryIterator2.hasNext()) {
			while( pe1_idx < p1.size() && pe2_idx < p2.size() ) {
    			
    			System.err.println("p1 docID: " + postingsEntry1.docID + " ||| p2 docID: " + postingsEntry2.docID);
    			
    			if( postingsEntry1.docID == postingsEntry2.docID ) {
    				System.err.println("Found match!");
    				answer.add(postingsEntry1); // Need to add new add() method.
    				
    				postingsEntry1 = p1.get(pe1_idx++);
    				postingsEntry2 = p2.get(pe2_idx++);
    			} else if ( postingsEntry1.docID < postingsEntry2.docID ) {
    				postingsEntry1 = p1.get(pe1_idx++);
    			} else {
    				postingsEntry2 = p2.get(pe2_idx++);
    			}
    		}
    		
    		if( answer.size() > 0 && iter.hasNext() ) {
    			System.out.println("Iteramos nueva palabra.");
    			System.out.println("answer size: " + answer.size());
    			p1 = answer;
    		}
    		
    		System.err.println("Onto the next word in the query.");
    		current_offset++;
    		
    	} while( iter.hasNext() ); // Used do while because the 2 long query wouldnt execute otherwise.
    	
    	return answer;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}