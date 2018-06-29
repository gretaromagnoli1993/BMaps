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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.sample.libproximitybeacon.ProximityBeaconImpl;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends Activity  implements MainActivityFragment.OnListUpdated{
  private static final String TAG = MainActivity.class.getSimpleName();

  public void onListUpdated(ArrayList<Beacon> list){
    int i=list.size()-1;
    MapsFragment mMap =(MapsFragment)getFragmentManager().findFragmentById(R.id.mapFragment);
    for( ;i>=0;i--) {
      Log.i(TAG,"Spawning beacon: "+list.get(i).getLatLng().toString() );
      mMap.spawnBeacon(list.get(i));
    }
    if(i>=2){
      //ToDo:this
      //getLocationWithTrilateration(list.get(1),list.get(2),list.get(3));
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


}
