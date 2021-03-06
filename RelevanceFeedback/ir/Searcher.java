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
					List<String> list = kgIndex.getWildcardPostings(queryterm);
	
					postingsWildcarded.clear();
					for(String s : list) {
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
					List<String> list = kgIndex.getWildcardPostings(queryterm);

					postingsWildcarded.clear();
					for(String s : list) {
						postingsWildcarded.add(index.getPostings(s));
					}

					p = postingsUnion(postingsWildcarded, queryType);
				} else {
					p = index.getPostings(queryterm);
				}
				
				postingsLists.add(p);
			}
		}
    	


		
    	if( queryType == QueryType.INTERSECTION_QUERY ) {
    		
    		if(postingsLists.size() == 1) {
    			return postingsLists.get(0);
    		} else if (postingsLists.size() > 1){
    			return postingsIntersection(postingsLists);
    		} else {
    			System.err.println("Wrong query size.");
    		}
        	
    	} else if ( queryType == QueryType.PHRASE_QUERY ) {
    		
    		if(postingsLists.size() == 1) {
    			return postingsLists.get(0);
    		} else if (postingsLists.size() > 1){
				return phrase(postingsLists);
    		}
    		
    	} else if ( queryType == QueryType.RANKED_QUERY ) {
    		return RankedSearch.search(postingsLists, index, rankingType, normalizationType, hitsRanker);
    	}
    	
    	return null;
    }
    
    private PostingsList postingsIntersection(ArrayList<PostingsList> postingsLists) {
    	PostingsList answer;

    	Iterator<PostingsList> iter = postingsLists.iterator();
    	
    	PostingsList p1 = iter.next();
    	PostingsList p2;
    	
    	PostingsEntry postingsEntry1, postingsEntry2;
    	
    	int pe1_idx, pe2_idx;
    	
    	do {
    		
    		answer = new PostingsList();
    		
    		p2 = iter.next();
    		
    		
    		if(p1==null || p2==null) {
    			return null;
    		}
    		
    		
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
				
			}
    		
			while( postingsEntry1 != null && postingsEntry2 != null ) {
    			
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
    			p1 = answer;
    		}
    		
    		
    	} while( iter.hasNext() );
    	
    	return answer;
    }

	private PostingsList phrase(ArrayList<PostingsList> postingsLists) {

		if(postingsLists.stream().anyMatch(postingsList -> postingsList == null)) return null;

		PostingsList answer = new PostingsList();

		int pl_idx = 0;
		int curr_offset = 1;
		int pe1_idx = 0;
		int pe2_idx = 0;
		boolean finished = false;

		HashMap<Integer, ArrayList<Integer>> validInitialOffsets = new HashMap<Integer, ArrayList<Integer>>();
		ArrayList<Integer> validOffsetPE = new ArrayList<Integer>();

		PostingsList p1 = postingsLists.get(0);

		for ( PostingsEntry pe : p1.list ) {
			validInitialOffsets.put(pe.docID, pe.offsetList);
		}

		PostingsList p2;

		PostingsEntry pe1;
		PostingsEntry pe2;


		while(++pl_idx < postingsLists.size()) {

			answer = new PostingsList();

			p2 = postingsLists.get(pl_idx);

			pe1_idx = 0;
			pe2_idx = 0;

			while(pe1_idx < p1.size() && pe2_idx < p2.size()) {

				pe1 = p1.get(pe1_idx);
				pe2 = p2.get(pe2_idx);

				if(pe1.docID == pe2.docID) {

					finished = false;
					validOffsetPE.clear();
					for( int offset1 : validInitialOffsets.get(pe1.docID) ) {
						for( int offset2 : pe2.offsetList ) {
							if(offset2 - offset1 == curr_offset) {
								if( ! finished ) answer.add(pe1);
								validOffsetPE.add(offset1);
								finished = true;
							}
						}
					}

					validInitialOffsets.put(pe1.docID, new ArrayList<>(validOffsetPE));

					pe1_idx++;
					pe2_idx++;

				} else if (pe1.docID < pe2.docID) {
					pe1_idx++;
				} else {
					pe2_idx++;
				}

				
			}
			curr_offset++;
			p1 = answer;
		}

		return answer;
	}
    
    private PostingsList postingsPhrase(ArrayList<PostingsList> postingsLists) {
    	//System.err.println("\n\n");
    	//System.err.println("PostingsPhrase");
    	
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
								//System.err.println(index.docNames.get(postingsEntry1.docID));
								//System.err.println(current_offset);

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
    	
    	//System.err.println("Postings phrase size: " + Integer.toString(answer.size()));
    	
    	return answer;
    }

	
    
    public PostingsList postingsUnion(ArrayList<PostingsList> postingsLists, QueryType queryType) {
    	
    	//System.err.println("Executing union of size: " + Integer.toString(postingsLists.size()));
    	
    	if ( postingsLists.size() == 1 ) {
    		return postingsLists.get(0);
    	}
    	
    	PostingsList answer = postingsLists.get(0);
    	
    	int idx = 1;
    	
    	while(idx < postingsLists.size()) {
    		answer = mergePostingsLists(answer, postingsLists.get(idx++), queryType);
    	}
    	
    	return answer;
    	
    }
    
    public PostingsList mergePostingsLists(PostingsList p1, PostingsList p2, QueryType queryType) {
    	
		
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
    			
				if(queryType==queryType.PHRASE_QUERY){
					offsets = mergeOffsets(postingsEntry1.offsetList, postingsEntry2.offsetList);
    				answer.add(new PostingsEntry(postingsEntry1.docID, offsets));
				} else {
					answer.add(new PostingsEntry(postingsEntry1.docID));
				}
    			
    			
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