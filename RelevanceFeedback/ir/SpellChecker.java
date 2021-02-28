/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.HashSet;


public class SpellChecker {
    /** The regular inverted index to be used by the spell checker */
    Index index;

    /** K-gram index to be used by the spell checker */
    KGramIndex kgIndex;

    /** The auxiliary class for containing the value of your ranking function for a token */
    class KGramStat implements Comparable {
        double score;
        String token;

        KGramStat(String token, double score) {
            this.token = token;
            this.score = score;
        }

        public String getToken() {
            return token;
        }

        public int compareTo(Object other) {
            if (this.score == ((KGramStat)other).score) return 0;
            return this.score < ((KGramStat)other).score ? -1 : 1;
        }

        public String toString() {
            return token + ";" + score;
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling
     * correction should pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.4;


    /**
      * The threshold for edit distance for a candidate spelling
      * correction to be accepted.
      */
    private static final int MAX_EDIT_DISTANCE = 2;


    public SpellChecker(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    private double jaccardCoeff(String x, String y) {
        HashMap<String, Boolean> termMap = new HashMap<String, Boolean>();

        List<String> list1 = kgIndex.kgrams(x);
        List<String> list2 = kgIndex.kgrams(y);

        list1.stream().forEach(term -> termMap.put(term, true));
        int intSize = (int) list2.stream().filter(term -> termMap.containsKey(term)).count();

        return jaccard(list1.size(), list2.size(), intSize);
    }

    /**
     *  Computes the Jaccard coefficient for two sets A and B, where the size of set A is 
     *  <code>szA</code>, the size of set B is <code>szB</code> and the intersection 
     *  of the two sets contains <code>intersection</code> elements.
     */
    private double jaccard(int szA, int szB, int intersection) {
        return Double.valueOf(intersection) / (Double.valueOf(szA) + Double.valueOf(szB) - Double.valueOf(intersection));
    }

    /**
     * Computing Levenshtein edit distance using dynamic programming.
     * Allowed operations are:
     *      => insert (cost 1)
     *      => delete (cost 1)
     *      => substitute (cost 2)
     */
    private int editDistance(String x, String y) {

        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                }
                else if (j == 0) {
                    dp[i][j] = i;
                }
                else {
                    dp[i][j] = min(dp[i - 1][j - 1] 
                    + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)), 
                    dp[i - 1][j] + 1, 
                    dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    private static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 2;
    }

    private static int min(int... numbers) {
        return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }

    /**
     *  Checks spelling of all terms in <code>query</code> and returns up to
     *  <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {

        int count = 0;

        System.err.println("Cheking...");

        //if(query.getQueryTerms().stream().allMatch(term -> index.getPostings(term) != null)) return null;

        int query_size = query.getQueryTerms().size();
        ArrayList<String[]> query_corrections = new ArrayList<String[]>();

        query.getQueryTerms().stream().forEach(x -> {
            query_corrections.add( checkTerm(x) );
            //Arrays.stream(corrections).forEach(System.err::println);
        }
        );

        return null;
    }

    private String[] checkTerm(String word) {

        if(index.getPostings(word) != null){
            return new String[] {word};
        }

        Set<String> set = new HashSet<String>();

        kgIndex.kgrams(word).stream().forEach( kgram -> {
            try{
                kgIndex.getPostings(kgram).stream().forEach( kgEntry -> {
                    String term = kgIndex.getTermByID(kgEntry.tokenID);

                    if(jaccardCoeff(word, term) >= JACCARD_THRESHOLD){
                        set.add(term);
                    }
                });
            } catch (Exception e){
                System.err.println("null kgindex postings for kgram: " + kgram);
            }
        }
        );

        return set.stream().filter(term -> editDistance(word, term) <= MAX_EDIT_DISTANCE).collect(Collectors.toList()).toArray(new String[0]);
    }

    /**
     *  Merging ranked candidate spelling corrections for all query terms available in
     *  <code>qCorrections</code> into one final merging of query phrases. Returns up
     *  to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(List<List<KGramStat>> qCorrections, int limit) {
        System.err.println("mergeCorrections!!!!");
        return null;
    }
}
