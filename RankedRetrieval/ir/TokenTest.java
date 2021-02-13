/*  
 *  This file is part of the computer assignment for the
 *  Information Retrieval course at KTH.
 * 
 *  Johan Boye, 2016
 */  

package ir;

import java.io.*;
import java.nio.charset.*;


/** 
 *  This class traverses a directory and tokenizes every file in it. 
 */
public class TokenTest {

    boolean case_folding = false;
    boolean remove_diacritics = false;
    boolean remove_punctuation = false;
    String patternsfile = null;
    String filename = null;

    /**
     *  Tokenizes and indexes the file @code{f}. If @code{f} is a directory,
     *  all its files and subdirectories are recursively processed.
     */
    public void processFiles( File f ) {
        // do not try to index fs that cannot be read
        if ( f.canRead() ) {
            if ( f.isDirectory() ) {
                String[] fs = f.list();
                // an IO error could occur
                if ( fs != null ) {
                    for ( int i=0; i<fs.length; i++ ) {
                        processFiles( new File( f, fs[i] ));
                    }
                }
            } else {
                try {
                    Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
                    Tokenizer tok = new Tokenizer( reader, case_folding, remove_diacritics, remove_punctuation, patternsfile );
                    int offset = 0;
                    PrintStream out = new PrintStream( System.out, true, "UTF-8" );
                    while ( tok.hasMoreTokens() ) { 
                        out.println( tok.nextToken() );
                    }
                    reader.close();
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
    }


    /** Prints usage information. */
    static void printHelpMessage() {
        System.err.println( "The following parameters are available: " );
        System.err.println( "  -cf : case folding (optional)" );
        System.err.println( "  -rp : removes punctuation (optional)" );
        System.err.println( "  -rd : removes diacritics  (optional)" );
        System.err.println( "  -p <filename> : name of the file containing regular expressions for non-standard words (optional)" );
        System.err.println( "  -f <filename> : name of file or directory to be tokenized (mandatory)" );
    }


    /** Main */
    public static void main( String[] args ) {
        TokenTest t = new TokenTest();
        // Parse command line arguments 
        int i=0; 
        while ( i<args.length ) {
            if ( args[i].equals( "-cf" )) {
                t.case_folding = true;
                i++;
            } else if ( args[i].equals( "-rd" )) {
                t.remove_diacritics = true;
                i++;
            } else if ( args[i].equals( "-rp" )) {
                t.remove_punctuation = true;
                i++;
            } else if ( args[i].equals( "-f" )) {
                i++;
                if ( i<args.length ) {
                    t.filename = args[i];
                    i++;
                } else {
                    printHelpMessage();
                    return;
                }
            } else if ( args[i].equals( "-p" )) {
                i++;
                if ( i<args.length ) {
                    t.patternsfile = args[i];
                    i++;
                } else {
                    printHelpMessage();
                    return;
                }
            } else {
                System.err.println( "Unrecognized parameter: " + args[i] );
                printHelpMessage();
                return;
            }
        }
        if ( t.filename != null ) {
            t.processFiles( new File( t.filename ));
        } else {
            printHelpMessage();
        }
    }
}
