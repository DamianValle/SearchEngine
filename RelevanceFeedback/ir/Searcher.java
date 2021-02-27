/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;
import java.lang.*;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    HITSRanker hitsRanker;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex, HITSRanker hitsRanker) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.hitsRanker = hitsRanker;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normalizationType ) {
    	
    	ArrayList<String> queries = query.getQueryTerms();
    	ArrayList<PostingsList> postingsLists = new ArrayList<PostingsList>();
    	ArrayList<PostingsList> postingsWildcarded = new ArrayList<PostingsList>();
    	PostingsList p = new PostingsList();
    	
		if(queryType==queryType.RANKED_QUERY){
			for(String queryterm : queries) {
    		
				if(queryterm.contains("*")) {
					System.err.println("Tenemos un * chavales");
					List<String> list = kgIndex.getWildcardPostings(queryterm);
	
					postingsWildcarded.clear();
					for(String s : list) {
						//System.err.println("Adding postingsList for " + s + " with size: " + Integer.toString(index.getPostings(s).size()));
						p = index.getPostings(s);
						if(p != null) {
							p.weight = query.query_count.getOrDefault(queryterm, 1.0);
						}
						postingsLists.add(p);
					}
				} else {
					p = index.getPostings(queryterm);
					if(p != null) {
						p.weight = query.query_count.getOrDefault(queryterm, 1.0);
					}
					postingsLists.add(p);
				}
				
				
				
				
			}
		} else {
			for(String queryterm : queries) {
    		
				if(queryterm.contains("*")) {
					System.err.println("Tenemos un * chavales");
					List<String> list = kgIndex.getWildcardPostings(queryterm);
	
					postingsWildcarded.clear();
					for(String s : list) {
						//System.err.println("Adding postingsList for " + s + " with size: " + Integer.toString(index.getPostings(s).size()));
						postingsWildcarded.add(index.getPostings(s));
					}
					p = postingsUnion(postingsWildcarded);
				} else {
					p = index.getPostings(queryterm);
				}
				
				if(p != null) {
					p.weight = query.query_count.getOrDefault(queryterm, 1.0);
				}
				
				postingsLists.add(p);
			}
		}

    	
    	
    	if( queryType == QueryType.INTERSECTION_QUERY ) {
    		System.err.println("Selected Intersection Query");
    		
    		if(postingsLists.size() == 1) {
    			return postingsLists.get(0);
    		} else if (postingsLists.size() > 1){
    			//System.err.println("Intersection Mode");
    			return postingsIntersection(postingsLists);
    		} else {
    			System.err.println("Wrong query size.");
    		}
        	
    	} else if ( queryType == QueryType.PHRASE_QUERY ) {
    		
    		if(postingsLists.size() == 1) {
    			//System.err.println("Query of size 1");
    			return postingsLists.get(0);
    		} else if (postingsLists.size() > 1){
    			//System.err.println("Selected Phrase Query");
        		return postingsPhrase(postingsLists);
    		}
    		
    	} else if ( queryType == QueryType.RANKED_QUERY ) {
    		//System.err.println("Selected Ranked Query");
    		
    		return RankedSearch.search(postingsLists, index, rankingType, normalizationType, hitsRanker);
    		
    	}
    	
    	return null;
    }
    
    private PostingsList postingsIntersection(ArrayList<PostingsList> postingsLists) {
    	PostingsList answer;

		//System.err.println("Intersection between sizes: " + Integer.toString(postingsLists.get(0).size()) + " " + Integer.toString(postingsLists.get(1).size()));
    	
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
				//System.err.println(p1.size());
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
    
    
    private PostingsList postingsPhrase(ArrayList<PostingsList> postingsLists) {
    	
    	System.err.println("PostingsPhrase");
    	System.err.println(postingsLists.size());
		//for(int i=0; i< postingsLists.size(); i++){
		//	System.err.println("Size of list: " + Integer.toString(postingsLists.get(i).size()));
		//}
    	
    	PostingsList answer;
    	
    	Iterator<PostingsList> iter = postingsLists.iterator();
    	
    	PostingsList p1 = iter.next();

		if(p1==null){
			return null;
		}

    	PostingsList p2; // = iter.next(); I need to put this inside the do while so that after the first one you can keep iterating.
    	
    	int current_offset = 1; //distance from the initial word in the query.
    	int pe1_idx, pe2_idx;
    	boolean finished;
    	
    	PostingsEntry postingsEntry1, postingsEntry2;
    	
    	do {
    		
    		answer = new PostingsList();
    		
    		p2 = iter.next();

			if(p2==null){
				return null;
			}
    		
    		pe1_idx = 0;
    		pe2_idx = 0;
    		
    		postingsEntry1 = p1.get(pe1_idx++);
    		postingsEntry2 = p2.get(pe2_idx++);
    		
			if( postingsEntry1.docID == postingsEntry2.docID ) {
				//System.err.println("Match in the first one!");
				
				finished = false;
				for( int offset1 : postingsEntry1.offsetList ) {
					for( int offset2 : postingsEntry2.offsetList ) {
						if(offset1 + current_offset == offset2) {
							answer.add(postingsEntry1);
							finished = true;
							break;
						}
					}
					if(finished) {
						break;
					}
				}
				postingsEntry1 = p1.get(pe1_idx++);
				postingsEntry2 = p2.get(pe2_idx++);
			}
    		
			while( pe1_idx < p1.size() && pe2_idx < p2.size() ) {
    			
    			//System.err.println("p1 docID: " + postingsEntry1.docID + " ||| p2 docID: " + postingsEntry2.docID);
    			
    			if( postingsEntry1.docID == postingsEntry2.docID ) {
    				//System.err.println("Found match!");
    				
    				finished = false;
    				for( int offset1 : postingsEntry1.offsetList ) {
    					for( int offset2 : postingsEntry2.offsetList ) {
    						if(offset1 + current_offset == offset2) {
    							answer.add(postingsEntry1);
    							finished = true;
    							break;
    						}
    					}
    					if(finished) {
    						break;
    					}
    				}
    				
    				postingsEntry1 = p1.get(pe1_idx++);
    				postingsEntry2 = p2.get(pe2_idx++);
    			} else if ( postingsEntry1.docID < postingsEntry2.docID ) {
    				postingsEntry1 = p1.get(pe1_idx++);
    			} else {
    				postingsEntry2 = p2.get(pe2_idx++);
    			}
    		}
    		
    		if( answer.size() > 0 && iter.hasNext() ) {
    			//System.out.println("Answer size: " + answer.size());
    			p1 = answer;
    			//System.err.println("Onto the next word in the query.");
        		current_offset++;
    		}
    		
    	} while( iter.hasNext() ); // Used do while because the 2 long query wouldnt execute otherwise.
    	
    	System.err.println("Postings phrase size: " + Integer.toString(answer.size()));
    	
    	return answer;
    }

	
    
    public PostingsList postingsUnion(ArrayList<PostingsList> postingsLists) {
    	
    	System.err.println("Executing union of size: " + Integer.toString(postingsLists.size()));
    	
    	if ( postingsLists.size() == 1 ) {
    		return postingsLists.get(0);
    	}
    	
    	PostingsList answer = postingsLists.get(0);
    	
    	int idx = 1;
    	
    	while(idx < postingsLists.size()) {
    		answer = mergePostingsLists(answer, postingsLists.get(idx++));
    	}
    	
    	
    	
    	return answer;
    	
    }
    
    public PostingsList mergePostingsLists(PostingsList p1, PostingsList p2) {
    	
		
    	PostingsList answer = new PostingsList();

		if(p1==null && p2==null){
			return null;
		}
		if(p1==null){
			return p2;
		}
		if(p2==null){
			return p1;
		}
    	
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
    	}
    	
    	if (p2_idx < p2.size()) {
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
    
}