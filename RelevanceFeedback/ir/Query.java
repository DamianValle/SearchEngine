/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 * 
 *   Damian Valle, 2021
 */  

package ir;

import java.util.*;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {
	
	public HashMap<String, Double> query_count = new HashMap<String, Double>();

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }
    

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();
    
    
    public ArrayList<String> getQueryTerms(){
    	ArrayList<String> terms = new ArrayList<String>();
    	for(QueryTerm query : queryterm) {
    		terms.add(query.term);
    	}
    	return terms;
    }

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
        }    
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }
        return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
    	
    	// crearse hasmap de string a double
    	/**
    	 * crearse hashmap de string a double
    	 * mirar la cantidad de relevant documents
    	 * 
    	 * para cada relevant document leer los tokens
    	 * para cada token meterlo al querycount y de double sumarle beta / n_of_relevant
    	 * 
    	 * pasar al process file el engine para los patterns, pasarle el pathname sacado con el docnames
    	 * 
    	 * 
    	 * 
    	 */
    	
    	int n_relevant = 0;
    	for (boolean b : docIsRelevant) {
    		if(b) {
    			n_relevant++;
    		}
    	}
    	if(n_relevant==0) {
            return;
        }
    	
        int relevant_count = 0;
    	for (int i=0; i<docIsRelevant.length; i++) {
    		if(docIsRelevant[i]) {
    			String pathname = engine.index.docNames.get(results.get(i).docID);
                if(pathname.equals("../../SE/lab1/datasets/davisWiki/Math.f")) {
                    pathname = "../../SE/lab1/datasets/davisWiki/Mathematics.f";
                }
    			processFile(pathname, engine, n_relevant);
                relevant_count++;
                if(relevant_count == n_relevant) break;
    		}
    	}
    	
    	//for ( QueryTerm qt : this.queryterm) {
    	//	query_count.merge(qt.term, alpha, Double::sum);
    	//}

        this.queryterm.stream().forEach(qt -> query_count.merge(qt.term, alpha, Double::sum));
    	
    	this.queryterm.clear();
        
    	for (Map.Entry<String, Double> entry : query_count.entrySet()) {
    		this.queryterm.add(new QueryTerm(entry.getKey(), entry.getValue()));
    	}
    }
    
    public void processFile(String pathname, Engine engine, int n_relevant) {
    	try {
    		File f = new File(pathname);
            Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
            Tokenizer tok = new Tokenizer( reader, true, false, true, "patterns.txt" );
            
            while ( tok.hasMoreTokens() ) {
                String token = tok.nextToken();
                query_count.merge(token, beta * 1./n_relevant, Double::sum);
            }
            reader.close();
        } catch ( IOException e ) {
            System.err.println( "Warning: IOException during indexing." );
        }
    }

}
