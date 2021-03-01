/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;


public class KGramPostingsEntry {
    int tokenID;
    int count;

    public KGramPostingsEntry(int tokenID) {
        this.tokenID = tokenID;
    }

    public KGramPostingsEntry(int tokenID, int count) {
        this.tokenID = tokenID;
        this.count = count;
    }

    public KGramPostingsEntry(KGramPostingsEntry other) {
        this.tokenID = other.tokenID;
    }

    public String toString() {
        return tokenID + "";
    }
}
