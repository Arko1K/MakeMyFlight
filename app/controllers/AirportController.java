package controllers;


import models.AirportModel;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class AirportController extends Controller {

    private static final String QUERY_FROM = "from";
    private static final String QUERY_SIZE = "size";
    private static final String QUERY_TYPES = "types";
    private static final String QUERY_QUERY = "q";
    private static final String QUERY_SORT = "sort";
    private static final String QUERY_ORDER = "order";


    public F.Promise<Result> getAirports() {
        return F.Promise.promise(() -> AirportModel.getAirports(
                request().getQueryString(QUERY_FROM),
                request().getQueryString(QUERY_SIZE),
                request().getQueryString(QUERY_TYPES),
                request().getQueryString(QUERY_QUERY),
                request().getQueryString(QUERY_SORT),
                request().getQueryString(QUERY_ORDER)))
                .thenApply(result -> ok(Json.toJson(result)));
    }

    public F.Promise<Result> getAirportCount() {
        return F.Promise.promise(() -> AirportModel.getAirportCount()).thenApply(result -> ok(Json.toJson(result)));
    }

    public F.Promise<Result> getAirportTypes() {
        return F.Promise.promise(() -> AirportModel.getAirportTypes()).thenApply(result -> ok(Json.toJson(result)));
    }
}