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
//import android.support.v4.app.FragmentManager;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import java.io.FileDescriptor;
import java.io.PrintWriter;


public class MainActivity extends Activity {


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    /*getFragmentManager().beginTransaction()
        .add(R.id.listFragment, new MainActivityFragment())
        .commit();*/
    MapsFragment mappa =new MapsFragment();
    MainActivityFragment lista =new MainActivityFragment();

    FragmentManager manager= getFragmentManager();
    FragmentTransaction fragmentTransaction=manager.beginTransaction();

    fragmentTransaction.add(R.id.mapFragment, mappa,"Map_frag");
    fragmentTransaction.add(R.id.listFragment, lista,"List_frag");

    fragmentTransaction.commit();
  }

}
