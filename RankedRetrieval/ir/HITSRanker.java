/**
 *   Computes the Hubs and Authorities for an every document in a query-specific
 *   link graph, induced by the base set of pages.
 *
 *   @author Dmytro Kalpakchi
 */

package ir;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;


public class HITSRanker {

    /**
     *   Max number of iterations for HITS
     */
    final static int MAX_NUMBER_OF_STEPS = 1000;

    /**
     *   Convergence criterion: hub and authority scores do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.001;

    /**
     *   The inverted index
     */
    Index index;

    /**
     *   Mapping from the titles to internal document ids used in the links file
     */
    HashMap<String,Integer> titleToId = new HashMap<String,Integer>();

    /**
     *   Sparse vector containing hub scores
     */
    HashMap<Integer,Double> hubs;

    /**
     *   Sparse vector containing authority scores
     */
    HashMap<Integer,Double> authorities;
    
    Map<Integer, Set<Integer>> linksTo;
    Map<Integer, Set<Integer>> linkedBy;

    
    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * 
     * A set of linked documents can be presented as a graph.
     * Each page is a node in graph with a distinct nodeID associated with it.
     * There is an edge between two nodes if there is a link between two pages.
     * 
     * Each line in the links file has the following format:
     *  nodeID;outNodeID1,outNodeID2,...,outNodeIDK
     * This means that there are edges between nodeID and outNodeIDi, where i is between 1 and K.
     * 
     * Each line in the titles file has the following format:
     *  nodeID;pageTitle
     *  
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the same
     *       as docIDs used by search engine's Indexer
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     * @param      index           The inverted index
     */
    public HITSRanker( String linksFilename, String titlesFilename, Index index ) {
        this.index = index;
        readDocs( linksFilename, titlesFilename );
    }


    /* --------------------------------------------- */

    /**
     * A utility function that gets a file name given its path.
     * For example, given the path "davisWiki/hello.f",
     * the function will return "hello.f".
     *
     * @param      path  The file path
     *
     * @return     The file name.
     */
    private String getFileName( String path ) {
        String result = "";
        StringTokenizer tok = new StringTokenizer( path, "\\/" );
        while ( tok.hasMoreTokens() ) {
            result = tok.nextToken();
        }
        return result;
    }


    /**
     * Reads the files describing the graph of the given set of pages.
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     */
    void readDocs( String linksFilename, String titlesFilename ) {
    	
    	Map<Integer, Set<Integer>> linksTo = new HashMap<>();
        Map<Integer, Set<Integer>> linkedBy = new HashMap<>();
    	
    	int filesRead = 0;
    	
    	try {
    		BufferedReader links = new BufferedReader(new FileReader(linksFilename));
            BufferedReader titles = new BufferedReader(new FileReader(titlesFilename));
            
            String line;
            while ((line = titles.readLine()) != null) {
            	String[] split = line.split(";");
                titleToId.put(split[1], Integer.parseInt(split[0]));
            }
            
            int docID;
            while ((line = links.readLine()) != null) {
            	filesRead++;
            	
            	String[] split = line.split(";");
            	docID = Integer.parseInt(split[0]);
            	
            	//System.err.println("\n\n");
            	//System.err.println("docID: " + docID);
            	
            	if(split.length > 1) {
            		String[] targets = split[1].split(",");
                	HashSet<Integer> targetsList = new HashSet<Integer>();
                	
                	for(int i=0; i<targets.length; i++) {
                		targetsList.add(Integer.parseInt(targets[i]));
                	}
                	
            		linksTo.put(docID, targetsList);
            		
            		
            		Set<Integer> linkedBySet = new HashSet<>();
            		for( int target : targetsList ) {
                		linkedBySet = linkedBy.get(target);
                		if(linkedBySet == null) {
                			linkedBySet = new HashSet<Integer>();
                		}
                		linkedBySet.add(docID);
                		
                		//System.err.println("Target is: " + target);
                		//System.err.println(linkedBySet);
                		
                		linkedBy.put(target, linkedBySet);
            		}
            	}
            }
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	System.err.println("Read " + filesRead + " number of docs");
    	
    	
    	this.linkedBy = linkedBy;
    	this.linksTo = linksTo;
    	
    	
    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param      titles  The titles of the documents in the root set
     */
    public void iterate(String[] titles) {
    	
    	
    	int[] titleIds = new int[titles.length];
    	
    	for ( int i=0; i<titles.length; i++ ) {
    		
    		try {
    			titleIds[i] = titleToId.get(titles[i]);
    		} catch (Exception e) {
    			//System.err.println(titles[i]);
    		}
    		
    		
    	}
    	
    	iterate(titleIds);
    }
    
    public void iterate(int[] titlesIds) {
    	
    	this.hubs = new HashMap<>();
        this.authorities = new HashMap<>();
    	
    	int[] root = titlesIds;
    	
    	Set<Integer> base = new HashSet<>();
    	
    	for(int i=0; i<root.length; i++) {
    		HashSet<Integer> base_part = new HashSet<Integer>();
    		base_part.addAll(linkedBy.computeIfAbsent(root[i], k -> new HashSet<>()));
    	    base_part.add(root[i]);
    	    base_part.addAll(linksTo.getOrDefault(root[i], new HashSet<>()));
    	    base.addAll(base_part);
    	}
    	
    	System.err.println(root.length);
    	System.err.println(base.size());
    	System.err.println("\n\n\n");
    	
    	for (int base_node : base) {
    		this.hubs.put(base_node, 1.0);
    		this.authorities.put(base_node, 1.0);
    	}
    	
	    double auth_score = 0.0;
	    double hub_score = 0.0;
    	
    	int iterations = 0;
    	while( iterations++ < MAX_NUMBER_OF_STEPS ) {
        	HashMap<Integer, Double> tempHubs = new HashMap<Integer, Double>();
    	    HashMap<Integer, Double> tempAuthorities = new HashMap<Integer, Double>();
    	    
    	    /**
    	    for( int base_node : base) {
    	    	
    	    	auth_score = 0.0;
    	    	hub_score = 0.0;
    	    	
    	    	if(linksTo.containsKey(base_node)){
    	    		for(int links_to : linksTo.get(base_node)) {
        	    		hub_score += this.hubs.get(links_to);
        	    	}
    	    	}
    	    	
    	    	if(linkedBy.containsKey(base_node)) {
    	    		for(int link_by : linkedBy.get(base_node)) {
        	    		auth_score += this.authorities.get(link_by);
        	    	}
    	    	}
    	    	
    	    	tempHubs.put(base_node, hub_score);
    	    	tempAuthorities.put(base_node, auth_score);
    	    	
    	    }
    	    */
    	    
    	    base.forEach(authority -> tempAuthorities.put(authority,
    	            linkedBy.getOrDefault(authority, new HashSet<>()).stream().mapToDouble(id -> this.hubs.getOrDefault(id, 0.0)).sum()));

    	    base.forEach(hub -> tempHubs.put(hub,
    	            linksTo.getOrDefault(hub, new HashSet<>()).stream().mapToDouble(id->tempAuthorities.getOrDefault(id, 0.0)).sum()));
    	    	
    	    double authSum = Math.sqrt(tempAuthorities.values().stream().map(v -> Math.pow(v, 2)).reduce(0.0, Double::sum));
    	    double hubSum = Math.sqrt(tempHubs.values().stream().map(v -> Math.pow(v, 2)).reduce(0.0, Double::sum));

    	    base.forEach(key -> tempAuthorities.compute(key, (__, value) -> value / authSum));
	        base.forEach(key -> tempHubs.compute(key, (__, value) -> value / hubSum));
	        
	        double hubError = Math.sqrt(tempHubs.entrySet().stream()
	        		.map(hub -> Math.pow(hub.getValue() - this.hubs.get(hub.getKey()), 2))
	        		.reduce(0.0, Double::sum));
	        
	        double authError = Math.sqrt(tempAuthorities.entrySet().stream()
	        		.map(auth -> Math.pow(auth.getValue() - this.authorities.get(auth.getKey()), 2))
	        		.reduce(0.0, Double::sum));
	        
	        this.hubs = tempHubs;
	        this.authorities = tempAuthorities;
	        
	        //System.err.println(hubError);
	        
	        if(hubError < EPSILON && authError < EPSILON) {
	        	//System.err.println("Converged!!!");
	        	break;
	        }
    	}
    }


    /**
     * Rank the documents in the subgraph induced by the documents present
     * in the postings list `post`.
     *
     * @param      post  The list of postings fulfilling a certain information need
     *
     * @return     A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(PostingsList postingsList) {
    	
    	ArrayList<String> docIDs = new ArrayList<String>();
    	
    	for(int i=0; i<postingsList.size(); i++) {
    		String[] split = index.docNames.get(postingsList.get(i).docID).split("/");
			docIDs.add(split[split.length-1]);
		}
    	
    	iterate(docIDs.toArray(new String[0]));
    	
    	for(int i=0; i<postingsList.size(); i++) {
    		String[] split = index.docNames.get(postingsList.get(i).docID).split("/");
    		if(split[split.length-1].equals("Z-World.html") || split[split.length-1].equals("Z-World.txt")) {
    			postingsList.get(i).setScore(0);
    		} else {
    			int id = titleToId.get(split[split.length-1]);
        		postingsList.get(i).setScore(this.hubs.get(id) + this.authorities.get(id));
    		}
    		
		}
    	
        return postingsList;
    }

    /**
     * Sort a hash map by values in the descending order
     *
     * @param      map    A hash map to sorted
     *
     * @return     A hash map sorted by values
     */
    private HashMap<Integer,Double> sortHashMapByValue(HashMap<Integer,Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer,Double> > list = new ArrayList<Map.Entry<Integer,Double> >(map.entrySet());
      
            Collections.sort(list, new Comparator<Map.Entry<Integer,Double>>() {
                public int compare(Map.Entry<Integer,Double> o1, Map.Entry<Integer,Double> o2) { 
                    return (o2.getValue()).compareTo(o1.getValue()); 
                } 
            }); 
              
            HashMap<Integer,Double> res = new LinkedHashMap<Integer,Double>(); 
            for (Map.Entry<Integer,Double> el : list) { 
                res.put(el.getKey(), el.getValue()); 
            }
            return res;
        }
    } 


    /**
     * Write the first 'k' entries of a hash map `map` to the file `fname`.
     *
     * @param      map        A hash map
     * @param      fname      The filename
     * @param      k          A number of entries to write
     */
    void writeToFile(HashMap<Integer,Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
            
            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer,Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k) break;
                }
            }
            writer.close();
        } catch (IOException e) {}
    }


    /**
     * Rank all the documents in the links file. Produces two files:
     *  hubs_top_30.txt with documents containing top 30 hub scores
     *  authorities_top_30.txt with documents containing top 30 authority scores
     */
    void rank() {
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer,Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer,Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, "hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, "authorities_top_30.txt", 30);
    }


    /* --------------------------------------------- */


    public static void main( String[] args ) {
        if ( args.length != 2 ) {
            System.err.println( "Please give the names of the link and title files" );
        }
        else {
            HITSRanker hr = new HITSRanker( args[0], args[1], null );
            hr.rank();
        }
    }
} 