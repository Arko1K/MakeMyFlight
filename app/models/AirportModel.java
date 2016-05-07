package models;


import common.Global;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AirportModel {

    public static List getAirports() {
        SearchResponse searchResponse = Global.getElasticTransportClient().prepareSearch(Global.getEsIndexAirport())
                .setTypes(Global.getEsTypeAirport())
                .setFrom(0)
                .setSize(1000)
                .execute().actionGet();
        List<Map> result = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits().getHits())
            result.add(searchHit.getSource());
        return result;
    }
}
