// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sample.beaconservice;

import android.app.Activity;
import android.app.Fragment;
//import android.app.FragmentManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;
//import android.support.v4.app.FragmentManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.sample.libproximitybeacon.ProximityBeaconImpl;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.apache.commons.math3.analysis.function.Cos;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity  implements MainActivityFragment.OnListUpdated{
  private static final String TAG = MainActivity.class.getSimpleName();
  public  LatLng actualPosition=null;
  public void onListUpdated(ArrayList<Beacon> list){
    MapsFragment mMap =(MapsFragment)getFragmentManager().findFragmentById(R.id.mapFragment);
    for( Beacon b:list ) {
      mMap.spawnBeacon(b);
    }
      double[][] position =new double[list.size()][3];
      double distance[]=new double[list.size()];
      for(Beacon beacon: list){
        position[list.indexOf(beacon)][0]=convertToCartesian(beacon.getLatLng())[0];//x
        position[list.indexOf(beacon)][1]=convertToCartesian(beacon.getLatLng())[1];//y
        position[list.indexOf(beacon)][2]=convertToCartesian(beacon.getLatLng())[2];//z
        distance[list.indexOf(beacon)]=calculateDistance(60,beacon.getRssi());
      }
      if((actualPosition==new LatLng(0,0)|true)) { //ToDo: il true bypassa il check sull'ultima posizione (forza il calcolo)
        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(position, distance), new LevenbergMarquardtOptimizer());
        try {
          Optimum optimum = solver.solve();
          double[] centroid = optimum.getPoint().toArray();
          if (convertToLatLng(centroid) != actualPosition) {
            Log.i(TAG, "centroid:  (" + centroid[0] + "," + centroid[1] + "," + centroid[2] + ")");
            actualPosition = convertToLatLng(centroid);
            mMap.spawnMe(actualPosition);
            Log.i(TAG, "Spawning position: " + actualPosition.toString());
          }
        }catch(Exception e){
          Log.e(TAG,"too many iterations");
        }
      }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    MapsFragment mappa =new MapsFragment();
    MainActivityFragment lista =new MainActivityFragment();

    FragmentManager manager= getFragmentManager();
    FragmentTransaction fragmentTransaction=manager.beginTransaction();

    fragmentTransaction.add(R.id.mapFragment, mappa,"Map_frag");
    fragmentTransaction.add(R.id.listFragment, lista,"List_frag");

    fragmentTransaction.commit();


  }
  public Double calculateDistance(int txPower, int rssi) {


    if (rssi == 0) {
      return -1.0; // if we cannot determine accuracy, return -1.
    }

    double ratio = rssi * 1.0 / txPower;
    if (ratio < 1.0) {
      return Math.pow(ratio, 10);
    } else {
      double distance = (0.89976) * Math.pow(ratio, 7.7095) + 0.111; //  restituisce la potenza della base che si desidera moltiplicare per se stessa a seconda del valore dell'esponenete

      return distance;

    }

  }
  public double[] convertToCartesian(LatLng latLng){
    int R= 6371; //raggio medio terra
    double[] coordinate=new double[]{0,0,0};
    coordinate[0]=R*(Math.cos(Math.toRadians(latLng.latitude)))*Math.cos(Math.toRadians(latLng.longitude));
    coordinate[1]=R*(Math.cos(Math.toRadians(latLng.latitude)))*Math.sin(Math.toRadians(latLng.longitude));
    coordinate[2]=R*Math.sin(Math.toRadians(latLng.latitude)  );
    return coordinate;
  }
  public LatLng convertToLatLng(double[] coordinate){
    int R= 6371; //raggio medio terra
    double latitude=Math.asin(coordinate[2]/R)*(180/Math.PI);
    double longitude;
    if(coordinate [0]>0){
      longitude=Math.atan2(coordinate[1],coordinate[0])*(180/Math.PI);
    }
    else if(coordinate[1]>0){
      longitude=Math.atan2(coordinate[1],coordinate[0])*(180/Math.PI)+180;
    }
    else {
      longitude = Math.atan2(coordinate[1], coordinate[0]) * (180 / Math.PI) - 180;
    }
    return new LatLng(latitude,longitude);

  }


}
