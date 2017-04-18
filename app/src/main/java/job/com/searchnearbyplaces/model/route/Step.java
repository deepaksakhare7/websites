package job.com.searchnearbyplaces.model.route;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Step {

    @SerializedName("distance")
    @Expose
    private Distance1 distance;
    @SerializedName("duration")
    @Expose
    private Duration1 duration;
    @SerializedName("end_location")
    @Expose
    private EndLocation1 endLocation;
    @SerializedName("html_instructions")
    @Expose
    private String htmlInstructions;
    @SerializedName("polyline")
    @Expose
    private Polyline polyline;
    @SerializedName("start_location")
    @Expose
    private StartLocation1 startLocation;
    @SerializedName("travel_mode")
    @Expose
    private String travelMode;
    @SerializedName("maneuver")
    @Expose
    private String maneuver;

    public Distance1 getDistance() {
        return distance;
    }

    public void setDistance(Distance1 distance) {
        this.distance = distance;
    }

    public Duration1 getDuration() {
        return duration;
    }

    public void setDuration(Duration1 duration) {
        this.duration = duration;
    }

    public EndLocation1 getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(EndLocation1 endLocation) {
        this.endLocation = endLocation;
    }

    public String getHtmlInstructions() {
        return htmlInstructions;
    }

    public void setHtmlInstructions(String htmlInstructions) {
        this.htmlInstructions = htmlInstructions;
    }

    public Polyline getPolyline() {
        return polyline;
    }

    public void setPolyline(Polyline polyline) {
        this.polyline = polyline;
    }

    public StartLocation1 getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(StartLocation1 startLocation) {
        this.startLocation = startLocation;
    }

    public String getTravelMode() {
        return travelMode;
    }

    public void setTravelMode(String travelMode) {
        this.travelMode = travelMode;
    }

    public String getManeuver() {
        return maneuver;
    }

    public void setManeuver(String maneuver) {
        this.maneuver = maneuver;
    }
}
