package ir;

import java.util.Arrays;
import java.util.HashMap;
import java.util.*;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RankedSearch {

  public static PostingsList search(ArrayList<PostingsList> postingsLists, Index index, RankingType rankingType, NormalizationType normalizationType) {
	  
	  double WEIGHT = 0.5;
	  
	  switch (rankingType) {
      case TF_IDF: return tfIdfRanking(postingsLists, index, normalizationType);
      case PAGERANK: return pageRankRanking(postingsLists, index);
      case COMBINATION: {
    	  System.err.println("Combination");
    	  PostingsList pr = pageRankRanking(postingsLists, index);
    	  PostingsList tfidf = tfIdfRanking(postingsLists, index, normalizationType);
    	  
    	  normalizeScores(pr);
    	  normalizeScores(tfidf);
    	  
    	  for(int i=0; i<pr.size(); i++) {
    		  pr.get(i).setScore(pr.get(i).score * WEIGHT + tfidf.get(i).score * (1-WEIGHT));
    	  }
    	  
    	  pr.sortPostings();
    	  
    	  return pr;
      }
      default:
        break;
    }

    return null;
    
  }
  
  private static void normalizeScores(PostingsList postingsList) {
	  
	  double sum = 0;
	  for(int i=0; i<postingsList.size(); i++) {
		  sum += postingsList.get(i).score;
	  }
	  for(int i=0; i<postingsList.size(); i++) {
		  postingsList.get(i).setScore(postingsList.get(i).score / sum);
	  }
	  
  }
  
  private static PostingsList pageRankRanking(ArrayList<PostingsList> postingsLists, Index index) {
	  
	  int docID;
	  PostingsEntry postingsEntry;
	  PostingsList answer = new PostingsList();
	  
	  for(PostingsList postingsList : postingsLists) {
		  for(int i=0; i<postingsList.size(); i++) {
			  //System.err.println(postingsList.get(i).docID);
			  try {
				  postingsList.get(i).setScore(index.docPageRank.get(postingsList.get(i).docID));
			  } catch (Exception e) {
				  
			  }
			  
		  }
		  answer = postingsUnion(answer, postingsList);
	  }
	  
	  answer.sortPostings();
	  
	  return answer;
  }
  
  private static PostingsList tfIdfRanking(ArrayList<PostingsList> postingsLists, Index index, NormalizationType normalizationType) {
  	
  	int N = index.docLengths.size();
  	
  	PostingsList answer = new PostingsList();
  	PostingsEntry postingsEntry;
  	int docID;
  	int tf;
  	double idf;
  	
  	for(PostingsList postingsList : postingsLists) {
      	//postingsList = index.getPostings(queryterm);
      	
      	idf = Math.log((double)N / postingsList.size());
      	
      	System.err.println(idf);
      	
      	System.err.println("El termino  aparece en " + Integer.toString(postingsList.size()) + 
      			" de un total de documentos N=" + Double.toString(N));
      	
      	for(int i=0; i<postingsList.size(); i++) {
      		docID = postingsList.get(i).docID;
      		postingsEntry = postingsList.get(i);
      		tf = postingsEntry.offsetList.size();
      		//postingsEntry.setScore((tf * idf) / index.docLengths.get(docID));
      		postingsEntry.setScore((tf * idf) / docLength(index, docID, normalizationType));
      	}
      	
      	answer = postingsUnion(answer, postingsList);

  	}
  	
  	answer.sortPostings();
  	
  	return answer;
  }
  
  private static double docLength(Index index, int docID, NormalizationType nt) {
	  switch(nt) {
	  case NUMBER_OF_WORDS:
		  return index.docLengths.get(docID);
	  case EUCLIDEAN:
		  return index.docLengthsEuclidean.get(docID);
	  default:
		  System.err.println("docLength error");
		  return 0;
	  }
  }
  
  private static PostingsList postingsUnion(PostingsList p1, PostingsList p2) {
  	
  	PostingsList answer = new PostingsList();
  	
  	int p1_idx = 0;
  	int p2_idx = 0;
  	
  	PostingsEntry pe1;
  	PostingsEntry pe2;
  	
  	while(p1_idx < p1.size() && p2_idx < p2.size()) {
  		
      	pe1 = p1.get(p1_idx);
      	pe2 = p2.get(p2_idx);
  		
  		if(pe1.docID == pe2.docID) {
  			answer.add(new PostingsEntry(pe1.docID, pe1.score + pe2.score));
  			
  			p1_idx++;
  			p2_idx++;
  		} else if(pe1.docID < pe2.docID) {
  			answer.add(pe1);
  			
  			p1_idx++;
  		} else {
  			answer.add(pe2);
  			
  			p2_idx++;
  		}
  	}
  	
  	while(p1_idx < p1.size()) {
  		answer.add(p1.get(p1_idx++));
  	}
  	
  	while(p2_idx < p2.size()) {
  		answer.add(p2.get(p2_idx++));
  	}
  	
  	return answer;
  	
  }
}
