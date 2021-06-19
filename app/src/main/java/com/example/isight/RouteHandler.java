package com.example.isight;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.here.sdk.core.GeoCircle;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.routing.Maneuver;
import com.here.sdk.routing.PedestrianOptions;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.Section;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.TextQuery;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteHandler {

    private static final int LOCATION_PERM_CODE = 656;
    private Context context;
    private Activity activity;
    private boolean isPermGranted = false;

    private RoutingEngine routingEngine;
    private SearchEngine searchEngine;
    private FusedLocationProviderClient fusedLocationClient;
    private VoiceHandler voiceHandler;
    public Thread routeThread;


    RouteHandler(Context context, Activity activity, VoiceHandler voiceHandler) {
        this.context = context;
        this.activity = activity;
        this.voiceHandler = voiceHandler;
    }

    void initRouteEngine() {

        try {
            checkPermissions();
            routingEngine = new RoutingEngine();
            searchEngine = new SearchEngine();
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of RoutingEngine failed: " + e.error.name());
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isPermGranted = true;
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERM_CODE);
            }
        }

    }

     void initiateRouting(final String destinationQuery) {
        if (isPermGranted) {
            routeThread = new Thread(() -> {
                while (true) {
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(activity, location -> {
                                if (location != null)
                                    getDestination(location, destinationQuery);
                                else
                                    voiceHandler.forceText("Please turn on location");
                            });
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            routeThread.start();
        }
        else {
            checkPermissions();
        }
    }

    public void stopRouting() {
        routeThread.interrupt();
    }

    private void getDestination(Location location, String destinationQuery) {
        GeoCoordinates selfCoords = new GeoCoordinates(location.getLatitude(), location.getLongitude());
        GeoCircle selfArea = new GeoCircle(selfCoords, 100000);
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, 1);
        TextQuery routeQuery = new TextQuery(destinationQuery, selfArea);
        searchEngine.search(routeQuery, searchOptions, (searchError, places) -> {
            if (searchError != null) {
                Log.d("SEARCH ERROR", searchError.toString());
                voiceHandler.forceText("Could not find your location.");
                return;
            }

            getTransits(selfCoords, places.get(0).getGeoCoordinates());
        });
    }

    private void getTransits(GeoCoordinates startPoint, GeoCoordinates destinationPoint) {
        String query = buildQuery(startPoint, destinationPoint);

        JsonObjectRequest transitRequest = new JsonObjectRequest(Request.Method.GET, query, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.has("notices")) {
                        pedestrianRoute(startPoint, destinationPoint);
                    } else {
                        transitRoute(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, error -> pedestrianRoute(startPoint, destinationPoint));

        Volley.newRequestQueue(context).add(transitRequest);
    }

    private void transitRoute(JSONObject response) throws JSONException{
        JSONObject nextLoc = response.getJSONArray("routes").getJSONObject(0).getJSONArray("sections").getJSONObject(0);
        JSONObject depPlace = nextLoc.getJSONObject("departure").getJSONObject("place");
        JSONObject arrivalPlace = nextLoc.getJSONObject("arrival").getJSONObject("place");
        if (nextLoc.getJSONObject("transport").getString("mode").equals("pedestrian")) {
            voiceHandler.queueText("Next intermediate destination, " + arrivalPlace.getString("name") + arrivalPlace.getString("type") + ", routing path.");
            GeoCoordinates startCoords = new GeoCoordinates(depPlace.getJSONObject("location").getDouble("lat"), depPlace.getJSONObject("location").getDouble("lng"));
            GeoCoordinates endCoords = new GeoCoordinates(arrivalPlace.getJSONObject("location").getDouble("lat"), arrivalPlace.getJSONObject("location").getDouble("lng"));
            pedestrianRoute(startCoords, endCoords);
        }
        else {
            String guideText = "Travel from " + depPlace.getString("name") + ", to " + arrivalPlace.getString("name") + ", via " + nextLoc.getJSONObject("transport").getString("mode");
            voiceHandler.queueText(guideText);
        }
    }

    private void pedestrianRoute(GeoCoordinates startPoint, GeoCoordinates destinationPoint) {

        Waypoint startWaypoint = new Waypoint(startPoint);
        Waypoint destinationWaypoint = new Waypoint(destinationPoint);
        List<Waypoint> waypoints = new ArrayList<>(Arrays.asList(startWaypoint, destinationWaypoint));

        routingEngine.calculateRoute(
                waypoints,
                new PedestrianOptions(),
                (routingError, list) -> {
                    if (routingError != null) {
                        Log.d("ROUTING ERROR", routingError.toString());
                        voiceHandler.forceText("Path could not be set");
                        return;
                    }

                    Route route = list.get(0);
                    List<Section> sections = route.getSections();
                    for (Section section : sections) {
                        List<Maneuver> maneuvers = section.getManeuvers();
                        for (int x = 0; x < maneuvers.size(); x++) {
                            Log.d("MANEUVERS:", maneuvers.get(x).getText() +  "@" + maneuvers.get(x).getAction().name());
                            if (x == 2)
                                break;

                            String directions = maneuvers.get(x).getText();
                            directions = directions.replace("m", "steps");
                            directions = directions.replace("km", "kilometres");
                            voiceHandler.queueText(directions);
                        }
                    }
                }
        );
    }

    private String buildQuery(GeoCoordinates startPoint, GeoCoordinates destinationPoint) {
        String query =
                "https://intermodal.router.hereapi.com/v8/routes?apiKey=YQVNDX1TyM4Gn7RGhV7st-4uCF6zYEywlZyA2gpRpkQ&alternatives=0&destination=" + destinationPoint.latitude + "," + destinationPoint.longitude + "&origin=" + startPoint.latitude + "," + startPoint.longitude + "&taxi[enable]=&vehicle[enable]=&rented[enable]=";
        return query;
    }
}
