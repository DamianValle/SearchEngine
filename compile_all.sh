#!/bin/sh
if ! [ -d classes ];
then
   mkdir classes
fi
javac -cp . -d classes ir/Engine.java ir/HashedIndex.java ir/HITSRanker.java ir/Index.java ir/Indexer.java ir/KGramIndex.java ir/KGramPostingsEntry.java ir/PersistentHashedIndex.java ir/PostingsEntry.java ir/PostingsList.java ir/Query.java ir/QueryType.java ir/RankingType.java ir/Searcher.java ir/SearchGUI.java ir/SpellChecker.java ir/SpellingOptionsDialog.java ir/Tokenizer.java ir/TokenTest.java 
