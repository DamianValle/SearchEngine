package ir;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RankedSearch {


  public static PostingsList search(Query query, Index index, RankingType rankingType, NormalizationType normType) {

    double RANKING_RATIO = 0.25;

    switch (rankingType) {
      case TF_IDF: return tf_idfRanking(query, index, normType);
      case PAGERANK: return pageRankRanking(query, index);
      case COMBINATION: {
        var tf_idf =  tf_idfRanking(query, index, normType);
        var pagerank = pageRankRanking(query, index);
        normalize(tf_idf);
        normalize(pagerank);
        var list = new RankedPostingsList();

        tf_idf.entries.stream().map(entry -> {
          var idx = pagerank.entries.indexOf(entry);
          var prEntry = pagerank.entries.get(idx);
          prEntry.score *= RANKING_RATIO;
          prEntry.score += entry.score * (1.0-RANKING_RATIO);
          return prEntry;
        }).sorted().forEach(list::insert);
        return list;
      }
      default:
        break;
    }

    return null;
