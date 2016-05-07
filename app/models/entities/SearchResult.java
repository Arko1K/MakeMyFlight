package models.entities;


import java.util.List;
import java.util.Map;

public class SearchResult {

    private List<Map> results;
    private long matchCount;


    public SearchResult(List<Map> results, long matchCount) {
        this.results = results;
        this.matchCount = matchCount;
    }


    public List<Map> getResults() {
        return results;
    }

    public void setResults(List<Map> results) {
        this.results = results;
    }

    public long getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(long matchCount) {
        this.matchCount = matchCount;
    }
}