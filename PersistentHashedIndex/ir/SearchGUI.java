/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   First version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 *   Modifications: Johan Boye, 2016
 */
package ir;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;


/**
 *   A graphical interface to the information retrieval system.
 */
public class SearchGUI extends JFrame {

    /**  The search engine. */
    Engine engine;

    /**  The query posed by the user. */
    private Query query;

    /**  The results of a search query. */
    private PostingsList results;

    /**  The query type (either intersection, phrase, or ranked). */
    QueryType queryType = QueryType.INTERSECTION_QUERY;

    /**  The ranking type (either tf-idf, pagerank, or combination). */
    RankingType rankingType = RankingType.TF_IDF;

    /**  Max number of results to display. */
    static final int MAX_RESULTS = 10;

    /** Demarkator between file name and file contents in the file contents text area*/
    private static final String MARKER = "----------------------------------------------------";


    /*
     *   Common GUI resources
     */
    public JCheckBox[] box = null;
    public JPanel resultWindow = new JPanel();
    private JScrollPane resultPane = new JScrollPane( resultWindow );
    public JTextField queryWindow = new JTextField( "", 28 );
    public JTextArea docTextView = new JTextArea( "", 15, 28 );
    private JScrollPane docViewPane = new JScrollPane( docTextView );
    private Font queryFont = new Font( "Arial", Font.BOLD, 24 );
    private Font resultFont = new Font( "Arial", Font.BOLD, 16 );
    JMenuBar menuBar = new JMenuBar();
    JMenu fileMenu = new JMenu( "File" );
    JMenu optionsMenu = new JMenu( "Search options" );
    JMenu rankingMenu = new JMenu( "Ranking score" );
    JMenu structureMenu = new JMenu( "Text structure" );
    JMenuItem saveItem = new JMenuItem( "Save index and exit" );
    JMenuItem quitItem = new JMenuItem( "Quit" );
    JRadioButtonMenuItem intersectionItem = new JRadioButtonMenuItem( "Intersection query" );
    JRadioButtonMenuItem phraseItem = new JRadioButtonMenuItem( "Phrase query" );
    JRadioButtonMenuItem rankedItem = new JRadioButtonMenuItem( "Ranked retrieval" );
    JRadioButtonMenuItem tfidfItem = new JRadioButtonMenuItem( "tf-idf" );
    JRadioButtonMenuItem pagerankItem = new JRadioButtonMenuItem( "PageRank" );
    JRadioButtonMenuItem combinationItem = new JRadioButtonMenuItem( "Combination" );
    ButtonGroup queries = new ButtonGroup();
    ButtonGroup ranking = new ButtonGroup();


    /**
     *  Constructor
     */
    public SearchGUI( Engine e ) {
        engine = e;
    }


    /**
     *  Sets up the GUI and initializes
     */
    void init() {
        // Create the GUI
        setSize( 600, 650 );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        resultWindow.setLayout(new BoxLayout(resultWindow, BoxLayout.Y_AXIS));
        resultPane.setLayout(new ScrollPaneLayout());
        resultPane.setBorder( new EmptyBorder(10,10,10,0) );
        resultPane.setPreferredSize( new Dimension(400, 450 ));
        getContentPane().add(p, BorderLayout.CENTER);
        // Top menus
        menuBar.add( fileMenu );
        menuBar.add( optionsMenu );
        menuBar.add( rankingMenu );
        fileMenu.add( quitItem );
        optionsMenu.add( intersectionItem );
        optionsMenu.add( phraseItem );
        optionsMenu.add( rankedItem );
        rankingMenu.add( tfidfItem );
        rankingMenu.add( pagerankItem );
        rankingMenu.add( combinationItem );
        queries.add( intersectionItem );
        queries.add( phraseItem );
        queries.add( rankedItem );
        ranking.add( tfidfItem );
        ranking.add( pagerankItem );
        ranking.add( combinationItem );
        intersectionItem.setSelected( true );
        tfidfItem.setSelected( true );
        p.add( menuBar );
        // Logo
        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
        p1.add( new JLabel( new ImageIcon( engine.pic_file )));
        p.add( p1 );
        // Search box
        JPanel p3 = new JPanel();
        p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
        p3.add( queryWindow );
        queryWindow.setFont( queryFont );
        p.add( p3 );
        p.add( resultPane );

        docTextView.setFont(resultFont);
        docTextView.setText("\n  The contents of the document will appear here.");
        docTextView.setLineWrap(true);
        docTextView.setWrapStyleWord(true);
        p.add(docViewPane);
        setVisible( true );

        /*
         *  Searches for documents matching the string in the search box, and displays
         *  the first few results.
         */
        Action search = new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                // Empty the results window
                displayInfoText( " " );
                // Turn the search string into a Query
                String queryString = queryWindow.getText().toLowerCase().trim();
                query = new Query( queryString );
                // Take relevance feedback from the user into account (assignment 3)
                // Check which documents the user has marked as relevant.
                if ( box != null ) {
                    boolean[] relevant = new boolean[box.length];
                    for ( int i=0; i<box.length; i++ ) {
                        if ( box[i] != null )
                            relevant[i] = box[i].isSelected();
                    }
                    query.relevanceFeedback( results, relevant, engine );
                }
                // Search and print results. Access to the index is synchronized since
                // we don't want to search at the same time we're indexing new files
                // (this might corrupt the index).
                long startTime = System.currentTimeMillis();
                synchronized ( engine.indexLock ) {
                    results = engine.searcher.search( query, queryType, rankingType );
                }
                long elapsedTime = System.currentTimeMillis() - startTime;
                // Display the first few results + a button to see all results.
                //
                // We don't want to show all results directly since the displaying itself
                // might take a long time, if there are many results.
                if ( results != null ) {
                    displayResults( MAX_RESULTS, elapsedTime/1000.0 );
                } else {
                    displayInfoText( "Found 0 matching document(s)" );
        		    
                    if (engine.speller != null) {
                        startTime = System.currentTimeMillis();
                        SpellingOptionsDialog dialog = new SpellingOptionsDialog(50);
                        String[] corrections = engine.speller.check(query, 10);
                        if (corrections != null && corrections.length > 0) {
                            String choice = dialog.show(corrections, corrections[0]);
                            if (choice != null) {
                                queryWindow.setText(choice);
                                queryWindow.grabFocus();
                                elapsedTime = System.currentTimeMillis() - startTime;
                                System.err.println("It took " + elapsedTime / 1000.0 + "s to check spelling");
                                this.actionPerformed(e);
                            }
                        }
                    }
                }
            }
            };

        // A search is carried out when the user presses "return" in the search box.
        queryWindow.registerKeyboardAction( search,
                            "",
                            KeyStroke.getKeyStroke( "ENTER" ),
                            JComponent.WHEN_FOCUSED );

        Action quit = new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                System.exit( 0 );
            }
            };
        quitItem.addActionListener( quit );


        Action setIntersectionQuery = new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                queryType = QueryType.INTERSECTION_QUERY;
            }
            };
        intersectionItem.addActionListener( setIntersectionQuery );

        Action setPhraseQuery = new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                queryType = QueryType.PHRASE_QUERY;
            }
            };
        phraseItem.addActionListener( setPhraseQuery );

        Action setRankedQuery = new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                queryType = QueryType.RANKED_QUERY;
            }
            };
        rankedItem.addActionListener( setRankedQuery );

        Action setTfidfRanking = new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                rankingType = RankingType.TF_IDF;
            }
            };
        tfidfItem.addActionListener( setTfidfRanking );

        Action setPagerankRanking = new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                rankingType = RankingType.PAGERANK;
            }
            };
        pagerankItem.addActionListener( setPagerankRanking );

        Action setCombinationRanking = new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                rankingType = RankingType.COMBINATION;
            }
            };
        combinationItem.addActionListener( setCombinationRanking );

    }


    /* ----------------------------------------------- */

    /**
     *  Clears the results window and writes an info text in it.
     */
    void displayInfoText( String info ) {
        resultWindow.removeAll();
        JLabel label = new JLabel( info );
        label.setFont( resultFont );
        resultWindow.add( label );
        revalidate();
        repaint();
    }

    /**
     *  Displays the results in the results window.
     *  @param maxResultsToDisplay The results list is cut off after this many results
     *      have been displayed.
     *  @param elapsedTime Shows how long time it took to compute the results.
     */
    void displayResults( int maxResultsToDisplay, double elapsedTime ) {
        displayInfoText( String.format( "Found %d matching document(s) in %.3f seconds", results.size(), elapsedTime ));
        box = new JCheckBox[maxResultsToDisplay];
        int i;
        for ( i=0; i<results.size() && i<maxResultsToDisplay; i++ ) {
            String description = i + ". " + displayableFileName( engine.index.docNames.get( results.get(i).docID ));
            if ( queryType == QueryType.RANKED_QUERY ) {
                description += "   " + String.format( "%.5f", results.get(i).score );
            }
            box[i] = new JCheckBox();
            box[i].setSelected( false );

            JPanel result = new JPanel();
            result.setAlignmentX(Component.LEFT_ALIGNMENT);
            result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));

            JLabel label = new JLabel(description);
            label.setFont( resultFont );

            MouseAdapter showDocument = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    String fileName = ((JLabel)e.getSource()).getText().split(" ")[1];
                    String contents = "Displaying contents of " + fileName + "\n" + MARKER + "\n";
                    String line;

                    Queue<String> fqueue = new LinkedList<>();

                    for (int j = 0, sz = engine.dirNames.size(); j < sz; j++) {
                        File curDir = new File(engine.dirNames.get(j));
                        fqueue.offer(curDir.toString());

                        String[] directories = curDir.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File current, String name) {
                                return new File(current, name).isDirectory();
                            }
                        });

                        for (String dir : directories) {
                            fqueue.offer(new File(curDir.toString(), dir).toString());
                        }
                    }

                    boolean foundFile = false;
                    while(!fqueue.isEmpty()) {
                        String dirName = fqueue.poll();
                        File file = new File(dirName, fileName);

                        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                            while ((line = br.readLine()) != null) {
                                contents += line.trim() + "\n";
                            }
                            foundFile = true;
                            break;
                        } catch (FileNotFoundException exc) {
                        } catch (IOException exc) {
                        } catch (NullPointerException exc) {
                        }
                    }

                    if (!foundFile) {
                        contents += "No file found\n";
                    }

                    docTextView.setText(contents);
                    docTextView.setCaretPosition(0);
                }
            };
            label.addMouseListener(showDocument);
            result.add(box[i]);
            result.add(label);

            resultWindow.add( result );
        }
        // If there were many results, give the user an option to see all of them.
        if ( i<results.size() ) {
            JPanel actionButtons = new JPanel();
            actionButtons.setLayout(new BoxLayout(actionButtons, BoxLayout.X_AXIS));
            actionButtons.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton display10MoreBut = new JButton( "Display 10 more results" );
            display10MoreBut.setFont( resultFont );
            actionButtons.add( display10MoreBut );
            Action display10More = new AbstractAction() {
                public void actionPerformed( ActionEvent e ) {
                    displayResults( (int)this.getValue("resCurSize") + 10, elapsedTime );
                }
            };
            display10More.putValue("resCurSize", i);
            display10MoreBut.addActionListener( display10More );

            actionButtons.add(Box.createRigidArea(new Dimension(5,0)));

            JButton displayAllBut = new JButton( "Display all " + results.size() + " results" );
            displayAllBut.setFont( resultFont );
            actionButtons.add( displayAllBut );
            Action displayAll = new AbstractAction() {
                public void actionPerformed( ActionEvent e ) {
                    displayResults( results.size(), elapsedTime );
                }
            };
            displayAllBut.addActionListener( displayAll );

            resultWindow.add(actionButtons);
        }
        revalidate();
        repaint();
    };


    /**
     *  Returns the filename at the end of a path.
     */
    String displayableFileName( String path ) {
        String result = "";
        StringTokenizer tok = new StringTokenizer( path, "\\/" );
        while ( tok.hasMoreTokens() ) {
            result = tok.nextToken();
        }
        return result;
    }
}
