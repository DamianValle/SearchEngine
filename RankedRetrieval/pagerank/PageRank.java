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


    public PageRank( String filename, String algorithm, int N) {
    	int noOfDocs = readDocs( filename );
    	
    	if(algorithm.equals("power iteration")) {
    		iterate( noOfDocs, N );
    	} else if(algorithm.equals("MC1")){
    		MC1( noOfDocs, N);
    	} else if(algorithm.equals("MC2")) {
    		MC2( noOfDocs, N);
    	} else if(algorithm.equals("MC4")) {
    		MC4( noOfDocs, N);
    	} else if(algorithm.equals("MC5")) {
    		MC5( noOfDocs, N);
    	}
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
		System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		//System.err.print( "done." );
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	//System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }
    
    /* --------------------------------------------- */
    
    void MC1( int noOfDocs, int N ) {
    	
    	this.ranks = new double[noOfDocs];
		Random rand = new Random();
		
		for(int i = 0; i < N; i++) {
			int state = rand.nextInt(noOfDocs);
			
			while(rand.nextFloat() > BORED) {
				HashMap<Integer,Boolean> nextState = link.get(state);
				if(nextState != null) {
					List<Integer> keys = new ArrayList<Integer>(nextState.keySet());
					state = keys.get(rand.nextInt(keys.size()));
				} else {
					state = rand.nextInt(noOfDocs);
				}
			}
			this.ranks[state] += 1.0 / N;
		}
		
		
    }
    
    void MC2( int noOfDocs, int N ) {
    	this.ranks = new double[noOfDocs];
    	int m = N / noOfDocs;
    	int state = -1;
		Random rand = new Random();
		
		for(int i = 0; i < noOfDocs; i++) {
			for(int j=0; j<m; j++) {
				state = i;
				
				while(rand.nextFloat() > BORED) {
					HashMap<Integer,Boolean> nextState = link.get(state);
					if(nextState != null) {
						List<Integer> keys = new ArrayList<Integer>(nextState.keySet());
						state = keys.get(rand.nextInt(keys.size()));
					} else {
						state = rand.nextInt(noOfDocs);
					}
				}
				this.ranks[state] += 1.0 / (m * noOfDocs);
			}
		}
    }
    
    void MC4( int noOfDocs, int N ) {
    	this.ranks = new double[noOfDocs];
    	int m = N / noOfDocs;
    	m = 500;
    	int state = -1;
    	int visits = 0;
		Random rand = new Random();
		
		for(int i = 0; i < noOfDocs; i++) {
			for(int j=0; j<m; j++) {
				state = i;
				
				while(rand.nextFloat() > BORED) {
					visits++;
					this.ranks[state] += 1.0;
					
					HashMap<Integer,Boolean> nextState = link.get(state);
					if(nextState != null) {
						List<Integer> keys = new ArrayList<Integer>(nextState.keySet());
						state = keys.get(rand.nextInt(keys.size()));
					} else {
						break;
					}
				}
				
			}
		}
		for(int i=0; i<this.ranks.length; i++) {
			this.ranks[i] /= visits;
		}
    }
    
    void MC5( int noOfDocs, int N ) {
    	this.ranks = new double[noOfDocs];
    	int m = N / noOfDocs;
    	int visits = 0;
		Random rand = new Random();
		
		for(int i = 0; i < N; i++) {
			int state = rand.nextInt(noOfDocs);
			while(rand.nextFloat() > BORED) {
				visits++;
				this.ranks[state] += 1.0;
				
				HashMap<Integer,Boolean> nextState = link.get(state);
				if(nextState != null) {
					List<Integer> keys = new ArrayList<Integer>(nextState.keySet());
					state = keys.get(rand.nextInt(keys.size()));
				} else {
					break;
				}
			}
		}
		for(int i=0; i<this.ranks.length; i++) {
			this.ranks[i] /= visits;
		}
    }
    
    static double compareTop30(PageRank pr, String[] top30exact, double[] top30ranks) {
    	double mse = 0;
    	int top30count = 0;
    	for(int i=0; i<top30ranks.length; i++) {
    		
    		mse += Math.pow(top30ranks[i] - pr.ranks[pr.docNumber.get(top30exact[i])], 2);
    		
    	}
    	return mse;
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
			System.err.println(Integer.toString(iterations) + "\t iterations...");
			System.err.println(Double.toString(error) + "\t error.");
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
			System.err.println("Power iteration converged at iteration " + Integer.toString(iterations));
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
    	
    	if(args.length<2 || (args.length==2 && args[1].equals("--help"))){
    		System.out.println("usage: java -Xmx1g PageRank [link file] [options]\n"
    				+ "\toptions:\n"
    				+ "\t\t--help\n"
    				+ "\t\t--print-all\n"
    				+ "\t\t--top30\n"
    				+ "\t\t--MC1 --MC2 --MC4 --MC5\n"
    				+ "\t\t--sw-wiki");
    				
    	} else if(args.length==2 && args[1].equals("--print-all")) {
    		PageRank pr = new PageRank( args[0] , "power iteration", 1000);
    	    
    	    for(int i=0; i<pr.ranks.length; i++) {
    	    	System.out.println(pr.docName[i] + ":" + Double.toString(pr.ranks[i]));
    	    }
    	}
    	if(args.length==2 && args[1].equals("--top30")) {
    		PageRank pr = new PageRank( args[0] , "power iteration", 1000);
    	    
    	    int[] bestK = maxKIndex(pr.ranks, 30);
    	    
    	    for(int i : bestK) {
    	    	System.out.println(pr.docName[i] + ":" + Double.toString(pr.ranks[i]));
    	    }
    	}
    		
    	if(args.length==2 && args[1].equals("--MC1")) {
    		PageRank pr = new PageRank( args[0], "MC1", 1000000);
    		int[] bestK = maxKIndex(pr.ranks, 30);
    		for(int i : bestK) {
    	    	System.out.println(pr.docName[i] + ":" + Double.toString(pr.ranks[i]));
    	    }
    	}
    	
    	if(args.length==2 && args[1].equals("--MC2")) {
    		PageRank pr = new PageRank( args[0], "MC2", 1000000);
    		int[] bestK = maxKIndex(pr.ranks, 30);
    		for(int i : bestK) {
    	    	System.out.println(pr.docName[i] + ":" + Double.toString(pr.ranks[i]));
    	    }
    	}
    	
    	if(args.length==2 && args[1].equals("--MC4")) {
    		PageRank pr = new PageRank( args[0], "MC4", 1000000);
    		int[] bestK = maxKIndex(pr.ranks, 30);
    		for(int i : bestK) {
    	    	System.out.println(pr.docName[i] + ":" + Double.toString(pr.ranks[i]));
    	    }
    	}
    	
    	if(args.length==2 && args[1].equals("--MC5")) {
    		PageRank pr = new PageRank( args[0], "MC5", 1000000);
    		int[] bestK = maxKIndex(pr.ranks, 30);
    		for(int i : bestK) {
    	    	System.out.println(pr.docName[i] + ":" + Double.toString(pr.ranks[i]));
    	    }
    	}
    	
    	if(args.length==2 && args[1].equals("--test")) {
    		int top30count=0;
    		String[] top30exact = new String[30];
    		double[] top30ranks = new double[30];
    		
    		BufferedReader reader;
    		try {
    			reader = new BufferedReader(new FileReader(
    					"./davis_top_30.txt"));
    			String line = reader.readLine();
    			while (line != null) {
    				String[] split = line.split(":");
    				top30exact[top30count] = split[0];
    				top30ranks[top30count++] = Double.parseDouble(split[1]);
    				line = reader.readLine();
    			}
    			reader.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		
    		System.out.println("--------------TESTING--------------");
    		double mse;
    		
    		
    		System.out.println("----------Power iteration----------");
    		
    		PageRank pr = new PageRank( args[0], "power iteration", 1000);
    		int[] bestK = maxKIndex(pr.ranks, 30);
    		mse = compareTop30(pr, top30exact, top30ranks);
    		System.out.println("MSE: " + Double.toString(mse));
    		
    		System.out.println();
    		
    		System.out.println("---------------MC1----------------");
    		
    		int[] its = new int[] {5000, 10000, 100000, 500000, 1000000, 5000000, 10000000};
    		
    		for(int iterations : its) {
    			System.out.println("N:" + Integer.toString(iterations));
        		
        		pr = new PageRank( args[0], "MC1", iterations);
        		bestK = maxKIndex(pr.ranks, 30);
        		mse = compareTop30(pr, top30exact, top30ranks);
        		if( mse >= 0) {
        			System.out.println("\tMSE: " + Double.toString(mse));
        		} else {
        			System.out.println("\tAlgorithm hasn't converged.");
        		}
        		System.out.println();
    		}
    		
    		System.out.println("---------------MC2----------------");
    		
    		for(int iterations : its) {
    			System.out.println("N:" + Integer.toString(iterations));
        		
        		pr = new PageRank( args[0], "MC2", iterations);
        		bestK = maxKIndex(pr.ranks, 30);
        		mse = compareTop30(pr, top30exact, top30ranks);
        		if( mse >= 0) {
        			System.out.println("\tMSE: " + Double.toString(mse));
        		} else {
        			System.out.println("\tAlgorithm hasn't converged.");
        		}
        		System.out.println();
    		}
    		
    		System.out.println("---------------MC4----------------");
    		
    		for(int iterations : its) {
    			System.out.println("N:" + Integer.toString(iterations));
        		
        		pr = new PageRank( args[0], "MC4", iterations);
        		bestK = maxKIndex(pr.ranks, 30);
        		mse = compareTop30(pr, top30exact, top30ranks);
        		if( mse >= 0) {
        			System.out.println("\tMSE: " + Double.toString(mse));
        		} else {
        			System.out.println("\tAlgorithm hasn't converged.");
        		}
        		System.out.println();
    		}
    		
    		System.out.println("---------------MC5----------------");
    		
    		for(int iterations : its) {
    			System.out.println("N:" + Integer.toString(iterations));
        		
        		pr = new PageRank( args[0], "MC5", iterations);
        		bestK = maxKIndex(pr.ranks, 30);
        		mse = compareTop30(pr, top30exact, top30ranks);
        		if( mse >= 0) {
        			System.out.println("\tMSE: " + Double.toString(mse));
        		} else {
        			System.out.println("\tAlgorithm hasn't converged.");
        		}
        		System.out.println();
    		}
    		
    	}
    		
    	if(args.length==2 && args[1].equals("--sw-wiki-stable")) {
    		int N = 100000;
    		int[] prevK = new int[30];
    		int[] bestK;
    		boolean coverged = false;
    		PageRank pr;
    		while(true) {
    			System.err.println("Calculating for N=" + N);
    			
    			pr = new PageRank( args[0], "MC1", N);
    			
        		bestK = maxKIndex(pr.ranks, 30);
        		
        		if(Arrays.equals(bestK, prevK)){
        			System.err.println("Top 30 has converged.");
        			break;
        		}
        		
        		for(int i : bestK) {
        	    	System.out.println(pr.docName[i] + ":" + Double.toString(pr.ranks[i]));
        	    }
    			
        		prevK = bestK;
    			N *= 10;
    		}
    		
    	}
    	
    	if(args.length==2 && args[1].equals("--sw-wiki")) {
    		int[] bestK;
    		PageRank pr;
    		
    		System.err.println("Running simulation...");
			
			pr = new PageRank( args[0], "MC1", 960000*2);
			
    		bestK = maxKIndex(pr.ranks, 30);
    		
    		for(int i : bestK) {
    	    	System.out.println(pr.docName[i] + ":" + Double.toString(pr.ranks[i]));
    	    }
    			
    		
    	}
    		
	    
	    
    }
    
    
    
    
    
    
    
    
    
    
    
}
