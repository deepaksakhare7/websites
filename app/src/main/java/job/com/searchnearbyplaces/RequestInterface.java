package job.com.searchnearbyplaces;


import java.util.LinkedHashMap;

import job.com.searchnearbyplaces.model.NearByPlaces;
import job.com.searchnearbyplaces.model.route.RouteData;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

public interface RequestInterface {

    @POST("maps/api/place/textsearch/json?")
    Call<NearByPlaces> getNearByPlaces(@QueryMap LinkedHashMap<String, String> data);

    @POST("maps/api/directions/json?")
    Call<RouteData> getRoute(@QueryMap LinkedHashMap<String, String> data);
}
