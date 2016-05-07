package controllers;


import models.AirportModel;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AirportController extends Controller {

    public CompletionStage<Result> getAirports() {
        return CompletableFuture.supplyAsync(() -> AirportModel.getAirports())
                .thenApply(result -> ok(Json.toJson(result)));
    }
}