package com.google.sample.beaconservice;

import android.os.Bundle;
import android.app.Fragment;
//import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Map;


public class MapsFragment extends Fragment implements OnMapReadyCallback{
    public MapView mappa;
    public GoogleMap mMap;
    public View v;
    private static final String TAG = MainActivityFragment.class.getSimpleName();



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

       v=inflater.inflate(R.layout.maps_layout,container,false);

        mappa= (MapView) v.findViewById(R.id.beaconMapView);
        mappa.onCreate(savedInstanceState);
        mappa.getMapAsync(this);
        return v;


    }
    public void spawnBeacon(Beacon b){
        if(b.getLatLng()!=null) {
            mMap.addMarker(new MarkerOptions().position(b.getLatLng()).title("beacon " + b.getRssi()));
        }
        else{
            Log.i(TAG,"no LatLng for this beacon! skipping");
        }
        mappa.onResume();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap=map;
        LatLng Ancona = new LatLng(43.587131,13.517399);
        mMap.addMarker(new MarkerOptions().position(Ancona).title("UnivPM"));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Ancona));
        mappa.onResume();
    }
    @Override
    public final void onDestroy()
    {
        mappa.onDestroy();
        super.onDestroy();
    }

    @Override
    public final void onLowMemory()
    {
        mappa.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public final void onPause()
    {
        mappa.onPause();
        super.onPause();
        mappa.onResume();
    }


}

