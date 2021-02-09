/*  
 *  This file is part of the computer assignment for the
 *  Information Retrieval course at KTH.
 * 
 *  Johan Boye, 2016
 */  

package ir;

import java.io.Reader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;
import java.lang.System;



/** 
 *  This class performs tokenization of UTF-8 encoded text files. 
 */
public class Tokenizer {

    /**
     *  This flag should be set to 'true' if all letters should be
     *  turned into lowercase.
     */
    public boolean case_folding = true; 

    /**
     *  This flag should be set to 'true' if all diacritics (accents etc.)
     *  should be removed.
     */
    public boolean remove_diacritics = true; 

    /**
     *  This flag should be set to 'true' if all punctuation (full stops etc.)
     *  should be removed.
     */
    public boolean remove_punctuation = true; 

    /** 
     *  The size of the buffer should be considerably larger than
     *  the anticipated length of the longest token.
     */
    public static final int BUFFER_LENGTH = 100001;

    /** The reader from where tokens are read. */
    Reader reader;
    
    /** 
     *  Characters are read @code{BUFFER_LENGTH} characters at a
     *  time into @code{buf}.
     */
    char[] buf = new char[BUFFER_LENGTH];

    /** The current position in the buffer. */
    int ptr = 0;

    /** Starting position of current token, or -1 if we're between tokens. */
    int token_start = -1;

    /** The next tokens to emit. */
    ArrayList<String> token_queue = new ArrayList<String>();

    /** @code{true} if we've started reading tokens. */
    boolean started_reading = false;

    /** The patterns matching non-standard words (e-mail addresses, etc.) */
    ArrayList<Pattern> patterns = null;

    /** Special characters (with diacritics) can be translated into these characters. */
    public static final char[] SPECIAL_CHAR_MAPPING = {
        'A', 'A', 'A', 'A', 'A', 'A', 'E', 'C', 'E', 'E', 'E', 'E', 'I', 'I', 'I', 'I', 'D', 'N', 'O', 'O', 'O', 'O', 'O', '*', 'O', 'U', 'U', 'U', 'U', 'Y', 'T', 'S', 'a', 'a', 'a', 'a', 'a', 'a', 'e', 'c', 'e', 'e', 'e', 'e', 'i', 'i', 'i', 'i', 'd', 'n', 'o', 'o', 'o', 'o', 'o', '/', 'o', 'u', 'u', 'u', 'u', 'y', 't', 'y', 'A', 'a', 'A', 'a', 'A', 'a', 'C', 'c', 'C', 'c', 'C', 'c', 'C', 'c', 'D', 'd', 'E', 'e', 'E', 'e', 'E', 'e', 'E', 'e', 'E', 'e', 'G', 'g', 'G', 'g', 'G', 'g', 'G', 'g', 'H', 'h', 'H', 'h', 'I', 'i', 'I', 'i', 'I', 'i', 'I', 'i', 'I', 'i', 'J', 'j', 'J', 'j', 'K', 'k', 'k', 'L', 'l', 'L', 'l', 'L', 'l', 'L', 'l', 'L', 'l', 'N', 'n', 'N', 'n', 'N', 'n', 'n', 'N', 'n', 'O', 'o', 'O', 'o', 'O', 'o', 'O', 'o', 'R', 'r', 'R', 'r', 'R', 'r', 'S', 's', 'S', 's', 'S', 's', 'S', 's', 'T', 't', 'T', 't', 'T', 't', 'U', 'u', 'U', 'u', 'U', 'u', 'U', 'u', 'U', 'u', 'U', 'u', 'W', 'w', 'Y', 'y', 'Y', 'Z', 'z', 'Z', 'z', 'Z', 'z' }; 


        /* ------------------------------ */


    /**
     *  Constructor
     *  @param reader The reader from which to read the text to be tokenized. 
     *  @param case_folding Should be set to <code>true</code> if every character
     *         should be translated into its lowercase counterpart.
     *  @param remove_diacritics Should be set to <code>true</code> if diacritics 
     *         should be removed (e.g. é will be e).
     *  @param remove_punctuation Should be set to <code>true</code> if punctuation 
     *         should be removed (useful in some applications).
     *  @param pattern_file The name of the file containing regular expressions
     *         for non-standard words (like dates, mail addresses, etc.).
     */
    public Tokenizer( Reader reader, boolean case_folding, boolean remove_diacritics, boolean remove_punctuation, String pattern_file ) {
        this.reader = reader;
        this.case_folding = case_folding;
        this.remove_diacritics = remove_diacritics;
        this.remove_punctuation = remove_punctuation;
        if ( pattern_file != null ) {
            readPatterns( pattern_file );
        }
    }


    /** 
     *  Returns true if the character is a punctuation character.
     */
    public boolean punctuation( char c ) {
        if ( c >= 32 && c <= 47 )
            return true;
        if ( c >= 58 && c <= 64 )
            return true;
        if ( c >= 91 && c <= 96 )
            return true;
        if ( c >= 123 && c <= 126 ) 
            return true;
        return false;
    }


    /**
     *  Read the patterns that match non-standard words  
     */
    private void readPatterns( String filename ) {
        patterns = new ArrayList<Pattern>();
        String line = null;
        try {
            BufferedReader in = new BufferedReader( new FileReader( filename ));
            while (( line = in.readLine()) != null ) {
                line = line.trim();
                if ( !line.startsWith( "//" ) && line.length() > 0 ) {
                    patterns.add( Pattern.compile( line ));
                }
            }
        }
        catch ( IOException e ) {
            System.err.println( "Warning: IOException reading the regular expressions from file" );
        }
        catch ( PatternSyntaxException e ) {
            System.err.println( "ERROR: Malformed regular expression: " + line );
        }
    }


    /** 
     *  Normalizes letters by converting to lower-case and possibly
     *  removing diacritics. This method is also used for checking
     *  whether a character can occur in a token or not.
     *
     *  @return code{true} if the (normalized counterpart of the) character
     *   can occur within a token, and @code{false} otherwise.
     */
    public boolean normalize( char[] buf, int ptr ) {
        char c = buf[ptr];
        if ( Character.isLetter( c )) {
            if ( remove_diacritics ) {
                // Remove diacritics by mapping to the closest character 
                // without diacritics.
                if ( c >= '\u00c0' && c <= '\u017e' ) {
                    buf[ptr] = SPECIAL_CHAR_MAPPING[(int)(c-'\u00c0')];
                }
            }
            if ( case_folding ) {
                buf[ptr] = Character.toLowerCase( buf[ptr] );
            }
            return true;
        }
        if ( c >= '!' && c <= '~' ) {
            return true;
        }
        // This is not a character that can occur in a token.
        return false;
    }



    /**
     *  @return the @code{true} if there are more tokens to be
     *  read, and @code{false} otherwise.
     */
    public boolean hasMoreTokens() throws IOException {
        if ( !started_reading ) {
            readTokens();
            started_reading = true;
        }
        if ( token_queue.size() == 0 ) 
            return readTokens();
        else 
            return true;
    }
    

    /**
     *  @return a String containing the next token, or @code{null} if there
     *  are no more tokens.
     */
    public String nextToken() throws IOException { 
        if ( token_queue.size() == 0 ) {
            if ( readTokens() )
                return token_queue.remove( 0 );
            else
                return null;
        }
        else {
            return token_queue.remove( 0 );
        }
    }


    /**
     *  Reads the next token. 
     */ 
    private boolean readTokens() throws IOException {
        if ( !started_reading ) {
            refillBuffer( 0 );
            started_reading = true;
        }
        boolean token_added_to_queue = false;
        while ( buf[ptr] != 0 ) {
            if ( token_start < 0 ) {
                if ( normalize( buf, ptr )) {
                    // A token starts here
                    token_start = ptr;
                }
                ptr++;
            }
            else {
                if ( normalize( buf, ptr )) {
                    // We're in the middle of a token
                    ptr++;
                }
                else {
                    // Check for non-standard words
                    token_added_to_queue = addTokensToQueue();
                    token_start = -1;
                    ptr++;
                }
            }
            if ( ptr == BUFFER_LENGTH ) {
                // The buffer has been read, so refill it
                if ( token_start >= 0 ) {
                    // We're in the middle of a token. Copy the parts
                    // of the token we have read already into the 
                    // beginning of the buffer.
                    System.arraycopy( buf, token_start, buf, 0, BUFFER_LENGTH-token_start );
                    refillBuffer( BUFFER_LENGTH-token_start );
                    ptr = BUFFER_LENGTH-token_start;
                    token_start = 0;
                }
                else {
                    refillBuffer( 0 );
                    ptr = 0;
                }
            }
            if ( token_added_to_queue ) {
                return true;
            }
        }
        // We have reached end of input. 
        return false; 
    }


    /**
     *  Adds token to the queue
     */
    private boolean addTokensToQueue() {
        if ( token_start < 0 ) {
            return false;
        }
        String s = new String( buf, token_start, ptr-token_start );
        if ( patterns != null ) {
            // Now let's see if the string s matches one of the patterns 
            // for non-standard words
            for ( Pattern p : patterns ) {
                Matcher m = p.matcher( s );
                if ( m.find() ) {
                    // The string contains a non-standard word. First check the prefix 
                    // before the matching substring, then add the non-standard word  
                    // to the token queue, then check the remainder of the string.
                    addStandardTokensToQueue( s.substring(0, m.start() ));
                    token_queue.add( m.group() );
                    token_start += m.end();
                    addTokensToQueue();
                    return true;
                }
            }
        }
        // This string contains only standard words
        return addStandardTokensToQueue( s );
    }


    /**
     *  Adds standard tokens (i.e. tokens not matching any regular
     *  expression) to the queue.
     */
    private boolean addStandardTokensToQueue( String s ) {
        // This string s does not match any specific pattern.
        // Then split it, considering all punctuation symbols
        // to be separators.
        boolean tokens_found = false;
        StringBuffer smallbuf = new StringBuffer();
        for ( int i=0; i<s.length(); i++ ) {
            if ( punctuation( s.charAt( i ))) {
                // The string before the punctuation sign is a token
                // unless it is empty
                String t = smallbuf.toString();
                if ( t.length()>0 ) {
                    token_queue.add( t );
                    smallbuf = new StringBuffer();
                    tokens_found = true;
                }
                if ( !remove_punctuation ) {
                    token_queue.add( "" + s.charAt( i ));
                    tokens_found = true;
                }
            }
            else {
                smallbuf.append( s.charAt( i ));
            }
        }
        // The string after the last punctuation sign is a token
        // unless it is empty
        String t = smallbuf.toString();
        if ( t.length()>0 ) {
            token_queue.add( t );
            tokens_found = true;
        }   
        return tokens_found;
    }
    

    /**
     *  Refills the buffer and adds end_of_file "\0" at the appropriate place.
     */
    private void refillBuffer( int start ) throws IOException {
        int chars_read = reader.read( buf, start, BUFFER_LENGTH-start );
        if ( chars_read >= 0 && chars_read < BUFFER_LENGTH-start ) {
            buf[chars_read] = 0;
        }
    }

}

