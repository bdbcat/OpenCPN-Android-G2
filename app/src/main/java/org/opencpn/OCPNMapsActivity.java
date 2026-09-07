package org.opencpn;

import android.util.Log;
import android.os.Bundle;
import java.lang.String;
import java.lang.Math;

import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Intent;

import org.json.JSONObject;
import org.opencpn.opencpn.R;


public class OCPNMapsActivity extends FragmentActivity implements OnMapReadyCallback {
    public int width = 400;
    public int height = 600;

    public double initialZoom = 1.0;
    public double gLat = 0.;
    public double gLon = 0.;
    public double clat = 0;
    public double clon = 0;
    public double scale_ppm = 1;
    JSONObject jsonVP = null;

    public GoogleMap m_map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            width = extras.getInt("WIDTH");
            height = extras.getInt("HEIGHT");

            String svp = extras.getString("Initial_VP");

            try {
                jsonVP = new JSONObject(svp);
            } catch (Exception e) {
                Log.i("DEBUGGER_TAG", "OCPNMapsActivity..Error, Invalid JSON");
                try {
                    jsonVP = new JSONObject("{}");
                } catch (Exception e2) {
                    Log.i("DEBUGGER_TAG", "OCPNMapsActivity..Error2, Invalid JSON");
                }
            }
/*
            vpCorners = extras.getString("VP_CORNERS");
//            Log.i("DEBUGGER_TAG", vpCorners);

            StringTokenizer tkz = new StringTokenizer(vpCorners, ";");
            String tk = tkz.nextToken();
            latne = Double.parseDouble(tk);

            tk = tkz.nextToken();
            lonne = Double.parseDouble(tk);

            tk = tkz.nextToken();
            latsw = Double.parseDouble(tk);

            tk = tkz.nextToken();
            lonsw = Double.parseDouble(tk);

            width = extras.getInt("WIDTH");
            height = extras.getInt("HEIGHT");

            // Ownship
            String vps = "";
            vps = extras.getString("VPS");
 //           Log.i("DEBUGGER_TAG", "VPS" + vps);


            StringTokenizer tkzo = new StringTokenizer(vps, ";");

            try {
                String tko = tkzo.nextToken();
                clat = Double.parseDouble(tko);     // unused

                tko = tkzo.nextToken();
                clon = Double.parseDouble(tko);     // unused

                tko = tkzo.nextToken();
                scale_ppm = Double.parseDouble(tko);    // unused

                tko = tkzo.nextToken();
                gLat = Double.parseDouble(tko);

                tko = tkzo.nextToken();
                gLon = Double.parseDouble(tko);
            } catch (Exception e) {
                Log.i("DEBUGGER_TAG", "OCPNMapsActivity..Error, Invalid VPS");
            }
        }
*/
            setContentView(R.layout.activity_maps);
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {

        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        try {
            // Add a marker for Ownship.
            gLat = jsonVP.getDouble("glat");
            gLon = jsonVP.getDouble("glon");
            LatLng ownship = new LatLng(gLat, gLon);
            map.addMarker(new MarkerOptions().position(ownship).title("OwnShip"));

            LatLng llne = new LatLng( jsonVP.getDouble("maxlat"), jsonVP.getDouble("maxlon"));;
            LatLng llsw = new LatLng( jsonVP.getDouble("minlat"), jsonVP.getDouble("minlon"));;

            LatLngBounds bounds = new LatLngBounds(llsw, llne);
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds( bounds, width, height, 0);
            map.moveCamera(cu);

        // capture the initial zoom
        //Maps on Google Maps have an integer “zoom level” which defines the resolution of the current view.
        //Zoom levels between 0 (the lowest zoom level, in which the entire world can be seen on one map)
        //to 21+ (down to individual buildings) are possible within the default roadmap maps view.


        //Google Maps sets zoom level 0 to encompass the entire earth.
        //Each succeeding zoom level doubles the precision in both horizontal and vertical dimensions.
        //More information on how this is done is available in the Google Maps API documentation.


            CameraPosition cp = map.getCameraPosition();
            initialZoom = Math.pow(2, cp.zoom);
 //       Log.i("DEBUGGER_TAG", "Initial: " + cp.toString());

            m_map = map;
        } catch (Exception e) {

        }
    }

    @Override
    public void finish() {
//        Log.i("DEBUGGER_TAG", "OCPNMapsActivity finish");

        CameraPosition cp = m_map.getCameraPosition();

        double finalZoom = Math.pow(2, cp.zoom);

        double zoom_delta = finalZoom/initialZoom;

 //       Log.i("DEBUGGER_TAG",  "Final: " + cp.toString());

        String finalPosition = "";
        finalPosition = finalPosition.concat(finalPosition.valueOf(cp.target.latitude));
        finalPosition = finalPosition.concat(";");
        finalPosition = finalPosition.concat(finalPosition.valueOf(cp.target.longitude));
        finalPosition = finalPosition.concat(";");
        finalPosition = finalPosition.concat(finalPosition.valueOf(cp.zoom));
        finalPosition = finalPosition.concat(";");
        finalPosition = finalPosition.concat(finalPosition.valueOf(zoom_delta));
        finalPosition = finalPosition.concat(";");
//        Log.i("DEBUGGER_TAG", finalPosition);



        Bundle b = new Bundle();
        b.putString("finalPosition", finalPosition);
        Intent i = new Intent();
        i.putExtras(b);
        setResult(RESULT_OK, i);

        super.finish();
    }

}

