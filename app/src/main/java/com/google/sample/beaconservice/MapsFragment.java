package com.google.sample.beaconservice;

import android.os.Bundle;
import android.app.Fragment;
//import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;


public class MapsFragment extends Fragment{
    //public MapView mappa;
    GoogleMap mappa;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        //mappa.getMapAsync();
        //setUpMapIfNeeded();

       // mappa = ((MapFragment) getFragmentManager().findFragmentById(R.id.beaconMapView)).getMap();




        return inflater.inflate(R.layout.maps_layout,container,false);

    }


/*
    //GoogleMap  mMap;
    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }


    private void setUpMapIfNeeded() {
        if (mappa != null) {
            return;
        }
        mappa = ((MapFragment) getFragmentManager().findFragmentById(R.id.beaconMapView)).getMap();
        if (mappa == null) {
            return;
        }
        // Initialize map options. For example:
        mappa.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }

*/
}
