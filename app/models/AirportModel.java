package models;


import common.Constants;
import common.Global;
import models.entities.Response;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AirportModel {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_ELEVATION = "elev";
    private static final String FIELD_DIRECT_FLIGHTS = "directFlights";
    private static final String FIELD_RATING = "rating";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_COUNTRY = "country";

    private static final String PARAM_SORT_ELEVATION = "elevation";
    private static final String PARAM_SORT_DIRECT_FLIGHTS = "direct-flights";
    private static final String PARAM_ORDER_ASCENDING = "asc";
    private static final String PARAM_ORDER_DESCENDING = "desc";


    public static Response getAirports(String from, String size, String type, String q, String sort, String order) {
        Response response = new Response();

        // Defaults and validations
        int fromInt = 0, sizeInt = 10;
        try {
            if (from != null)
                fromInt = Integer.valueOf(from);
            if (size != null)
                sizeInt = Integer.valueOf(size);
        } catch (NumberFormatException nfe) {
            response.setError(Constants.ERROR_INCORRECT_PARAMS);
            return response;
        }
        SortOrder sortOrder = null;
        if (order != null) {
            if (order.equalsIgnoreCase(PARAM_ORDER_ASCENDING))
                sortOrder = SortOrder.ASC;
            else if (order.equalsIgnoreCase(PARAM_ORDER_DESCENDING))
                sortOrder = SortOrder.DESC;
            else {
                response.setError(Constants.ERROR_INCORRECT_PARAMS);
                return response;
            }
        }

        SearchRequestBuilder searchRequestBuilder = Global.getElasticTransportClient().prepareSearch(Global.getEsIndexAirport());

        if (sort != null) {
            switch (sort.toLowerCase()) {
                case PARAM_SORT_ELEVATION: {
                    searchRequestBuilder.addSort(SortBuilders.fieldSort(FIELD_ELEVATION)
                            .order(order == null ? SortOrder.ASC : sortOrder));
                    break;
                }
                case PARAM_SORT_DIRECT_FLIGHTS: {
                    searchRequestBuilder.addSort(SortBuilders.fieldSort(FIELD_DIRECT_FLIGHTS)
                            .order(order == null ? SortOrder.DESC : sortOrder));
                    break;
                }
                case FIELD_RATING: {
                    searchRequestBuilder.addSort(SortBuilders.fieldSort(FIELD_RATING)
                            .order(order == null ? SortOrder.DESC : sortOrder));
                    break;
                }
                default: {
                    response.setError(Constants.ERROR_INCORRECT_PARAMS);
                    return response;
                }
            }
        }

        searchRequestBuilder.setTypes(Global.getEsTypeAirport())
                .setFrom(fromInt)
                .setSize(sizeInt);

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        if (type != null)
            boolQueryBuilder.filter(QueryBuilders.termQuery(FIELD_TYPE, type));
        if (q != null)
            boolQueryBuilder.must(QueryBuilders.multiMatchQuery(q, FIELD_NAME, FIELD_CODE, FIELD_COUNTRY));
        if (boolQueryBuilder.hasClauses())
            searchRequestBuilder.setQuery(boolQueryBuilder);

        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
        List<Map> result = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits().getHits())
            result.add(searchHit.getSource());
        response.setData(result);
        response.setSuccess(true);
        return response;
    }
}