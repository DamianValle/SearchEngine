import java.util.*;
import java.io.*;

public class PageRank {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.000001;
    
    double[] ranks;

       
    /* --------------------------------------------- */


    public PageRank( String filename ) {
	int noOfDocs = readDocs( filename );
	iterate( noOfDocs, 1000 );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. 
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    //System.err.print( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
		int index = line.indexOf( ";" );
		String title = line.substring( 0, index );
		Integer fromdoc = docNumber.get( title );
		//  Have we seen this document before?
		if ( fromdoc == null ) {	
		    // This is a previously unseen doc, so add it to the table.
		    fromdoc = fileIndex++;
		    docNumber.put( title, fromdoc );
		    docName[fromdoc] = title;
		}
		// Check all outlinks.
		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    String otherTitle = tok.nextToken();
		    Integer otherDoc = docNumber.get( otherTitle );
		    if ( otherDoc == null ) {
			// This is a previousy unseen doc, so add it to the table.
			otherDoc = fileIndex++;
			docNumber.put( otherTitle, otherDoc );
			docName[otherDoc] = otherTitle;
		    }
		    // Set the probability to 0 for now, to indicate that there is
		    // a link from fromdoc to otherDoc.
		    if ( link.get(fromdoc) == null ) {
			link.put(fromdoc, new HashMap<Integer,Boolean>());
		    }
		    if ( link.get(fromdoc).get(otherDoc) == null ) {
			link.get(fromdoc).put( otherDoc, true );
			out[fromdoc]++;
		    }
		}
	    }
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		//System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		//System.err.print( "done." );
	    }
	}
	catch ( FileNotFoundException e ) {
	    //System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    //System.err.println( "Error reading file " + filename );
	}
	//System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate( int numberOfDocs, int maxIterations ) {

	double[] prev_a = new double[numberOfDocs];
	Arrays.fill(prev_a, 1.0 / numberOfDocs);
	
	this.ranks = new double[numberOfDocs];
	this.ranks[0] = 1;
	
	boolean converged = false;
	int iterations = 0;
	double sum = 0;
	double error = 0;
	
	while( !converged && iterations++ < maxIterations ) {
		
		if(iterations%1000==0) {
			//System.err.println(Integer.toString(iterations) + "\t iterations...");
			//System.err.println(Double.toString(error) + "\t error.");
		}
		
		double[] a = new double[numberOfDocs];
		Arrays.fill(a, BORED / numberOfDocs);
		
		link.entrySet().forEach(entry -> {
	        entry.getValue().keySet().forEach(outLink -> {
	          a[outLink] += ranks[entry.getKey()] * (1.0 - BORED) / out[entry.getKey()];
	        });
	      });
		
		/**
		// Distribution normalization
		double error = 0;
		for(int i=0; i<a.length; i++) {
			error += a[i];
		}
		for(int i=0; i<a.length; i++) {
			a[i] += a[i]/error;
		}
		*/
		
		
		
		// Epsilon threshold checking
		error = 0;
		for(int i=0; i<a.length; i++) {
			error += Math.abs(a[i] - ranks[i]);
		}
		
		this.ranks = a;
		
		if(error < EPSILON) {
			//System.err.println("Power iteration converged at iteration " + Integer.toString(iterations));
			sum = Arrays.stream(a).sum(); //poner a embede ranks?
	        for (int i = 0; i < ranks.length; i++) {
	          ranks[i] = this.ranks[i]/sum;
	        }
			converged = true;
		}
		
	}
    }


    /* --------------------------------------------- */
    
    public static int[] maxKIndex(double[] array, int top_k) {
        double[] max = new double[top_k];
        int[] maxIndex = new int[top_k];
        Arrays.fill(max, Double.NEGATIVE_INFINITY);
        Arrays.fill(maxIndex, -1);

        top: for(int i = 0; i < array.length; i++) {
            for(int j = 0; j < top_k; j++) {
                if(array[i] > max[j]) {
                    for(int x = top_k - 1; x > j; x--) {
                        maxIndex[x] = maxIndex[x-1]; max[x] = max[x-1];
                    }
                    maxIndex[j] = i; max[j] = array[i];
                    continue top;
                }
            }
        }
        return maxIndex;
    }


    public static void main( String[] args ) {
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    PageRank pr = new PageRank( args[0] );
	    
	    int top = 30;
	    
	    int[] bestK = maxKIndex(pr.ranks, 30);
	    
	    //for(int i : bestK) {
	    //	System.err.println(pr.docName[i] + ":" + Double.toString(pr.ranks[i]));
	    //}
	    
	    for(int i=0; i<pr.ranks.length; i++) {
	    	System.out.println(pr.docName[i] + ":" + Double.toString(pr.ranks[i]));
	    }
	    
	}
    }
}
