package models;


import common.Constants;
import common.Global;
import models.entities.AirportType;
import models.entities.Response;
import models.entities.SearchResult;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
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
    private static final String FIELD_NAME_TERM = "name.term";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_COUNTRY = "country";
    private static final String FIELD_COUNTRY_TERM = "country.term";

    private static final String PARAM_SORT_RELEVANCE = "relevance";
    private static final String PARAM_SORT_ELEVATION = "elevation";
    private static final String PARAM_SORT_DIRECT_FLIGHTS = "direct-flights";
    private static final String PARAM_ORDER_ASCENDING = "asc";
    private static final String PARAM_ORDER_DESCENDING = "desc";
    private static final String PARAM_TYPE_ALL = "all";


    public static Response getAirports(String from, String size, String types, String q, String sort, String order) {
        Response response = new Response();
        try {
            // Defaults and validations
            int fromInt = 0, sizeInt = 10;
            try {
                if (from != null && !from.isEmpty())
                    fromInt = Integer.valueOf(from);
                if (size != null && !size.isEmpty())
                    sizeInt = Integer.valueOf(size);
            } catch (NumberFormatException nfe) {
                response.setError(Constants.ERROR_INCORRECT_PARAMS);
                return response;
            }
            SortOrder sortOrder = null;
            if (order != null && !order.isEmpty()) {
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

            if (sort != null && !sort.isEmpty()) {
                switch (sort.toLowerCase()) {
                    case PARAM_SORT_RELEVANCE: {
                        if (sortOrder != null)
                            searchRequestBuilder.addSort(SortBuilders.scoreSort().order(sortOrder));
                        break;
                    }
                    case PARAM_SORT_ELEVATION: {
                        searchRequestBuilder.addSort(SortBuilders.fieldSort(FIELD_ELEVATION)
                                .order(sortOrder == null ? SortOrder.ASC : sortOrder));
                        break;
                    }
                    case PARAM_SORT_DIRECT_FLIGHTS: {
                        searchRequestBuilder.addSort(SortBuilders.fieldSort(FIELD_DIRECT_FLIGHTS)
                                .order(sortOrder == null ? SortOrder.DESC : sortOrder));
                        break;
                    }
                    case FIELD_RATING: {
                        searchRequestBuilder.addSort(SortBuilders.fieldSort(FIELD_RATING)
                                .order(sortOrder == null ? SortOrder.DESC : sortOrder));
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
            if (types != null && !types.isEmpty()) {
                String[] typesArr = types.split(",");
                boolean all = false;
                for (String type : typesArr) {
                    if (type.equalsIgnoreCase(PARAM_TYPE_ALL)) {
                        all = true;
                        break;
                    }
                }
                if (!all)
                    boolQueryBuilder.filter(QueryBuilders.termsQuery(FIELD_TYPE, typesArr));
            }
            if (q != null && !q.isEmpty()) {
                // boolQueryBuilder.must(QueryBuilders.multiMatchQuery(q, FIELD_NAME, FIELD_CODE, FIELD_COUNTRY));

                boolQueryBuilder
                        .should(QueryBuilders.matchQuery(FIELD_NAME_TERM, q).boost(2))
                        .should(QueryBuilders.matchQuery(FIELD_NAME, q))
                        .should(QueryBuilders.matchQuery(FIELD_COUNTRY_TERM, q).boost(2))
                        .should(QueryBuilders.matchQuery(FIELD_COUNTRY, q))
                        .should(QueryBuilders.matchQuery(FIELD_CODE, q).boost(2));
            }
            if (boolQueryBuilder.hasClauses())
                searchRequestBuilder.setQuery(boolQueryBuilder);

            SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
            List<Map> results = new ArrayList<>();
            for (SearchHit searchHit : searchResponse.getHits().getHits())
                results.add(searchHit.getSource());
            response.setData(new SearchResult(results, searchResponse.getHits().getTotalHits()));
            response.setSuccess(true);
        } catch (Exception ex) {
            response.setError(ex.getMessage());
        }
        return response;
    }

    public static Response getAirportCount() {
        Response response = new Response();
        try {
            response.setData(Global.getElasticTransportClient().prepareSearch(Global.getEsIndexAirport()).setTypes(Global.getEsTypeAirport())
                    .setSize(0).execute().actionGet().getHits().getTotalHits());
            response.setSuccess(true);
        } catch (Exception ex) {
            response.setError(ex.getMessage());
        }
        return response;
    }

    public static Response getAirportTypes() {
        Response response = new Response();
        try {
            SearchResponse searchResponse = Global.getElasticTransportClient().prepareSearch(Global.getEsIndexAirport())
                    .setTypes(Global.getEsTypeAirport()).addAggregation(AggregationBuilders.terms(FIELD_TYPE).field(FIELD_TYPE))
                    .setSize(0).execute().actionGet();

            List<AirportType> airportTypes = new ArrayList<>();
            for (Terms.Bucket bucket : ((StringTerms) searchResponse.getAggregations().get(FIELD_TYPE)).getBuckets()) {
                String key = bucket.getKeyAsString();
                if (key.equalsIgnoreCase("airports"))
                    airportTypes.add(new AirportType("Civil Airports", key));
                else
                    airportTypes.add(new AirportType(key.substring(0, 1).toUpperCase() + key.substring(1), key));
            }
            response.setData(airportTypes);
            response.setSuccess(true);
        } catch (Exception ex) {
            response.setError(ex.getMessage());
        }
        return response;
    }
}