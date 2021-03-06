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

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
//import android.support.v4.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.*;
import android.support.annotation.IntegerRes;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.drive.events.ChangeListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.sample.libproximitybeacon.ProximityBeacon;
import com.google.sample.libproximitybeacon.ProximityBeaconImpl;
import com.squareup.okhttp.Callback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;


import static java.lang.Thread.getAllStackTraces;
import static java.lang.Thread.sleep;
/**
 * The MainActivityFragment is responsible for launching the account picker, ensuring the user has
 * given their permission for the app to use their account data, and starting the initial scan to
 * discover nearby Eddystone devices.
 */
public class MainActivityFragment extends Fragment{
  private OnListUpdated mCallback;

  public interface OnListUpdated{
    public void onListUpdated(ArrayList<Beacon> list);
  }

  //create an instance of this interface
  private File file;

  private static final String TAG = MainActivityFragment.class.getSimpleName();
  private static final long SCAN_TIME_MILLIS = 5000;

  // Receives the runnable that stops scanning after SCAN_TIME_MILLIS.
  private static final Handler handler = new Handler(Looper.getMainLooper());

  // An aggressive scan for nearby devices that reports immediately.
  private static final ScanSettings SCAN_SETTINGS =
    new ScanSettings.Builder().
      setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
      .setReportDelay(0)
      .build();

  // The Eddystone-UID frame type byte.
  // See https://github.com/google/eddystone for more information.
  private static final byte EDDYSTONE_UID_FRAME_TYPE = 0x00;

  // The Eddystone Service UUID, 0xFEAA.
  private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
    ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");

  // A filter that scans only for devices with the Eddystone Service UUID.
  private static final ScanFilter EDDYSTONE_SCAN_FILTER = new ScanFilter.Builder()
    .setServiceUuid(EDDYSTONE_SERVICE_UUID)
    .build();

  private static final List<ScanFilter> SCAN_FILTERS = buildScanFilters();

  private static List<ScanFilter> buildScanFilters() {
    List<ScanFilter> scanFilters = new ArrayList<>();
    scanFilters.add(EDDYSTONE_SCAN_FILTER);
    return scanFilters;
  }

  private static final Comparator<Beacon> RSSI_COMPARATOR = new Comparator<Beacon>() {
    @Override
    public int compare(Beacon lhs, Beacon rhs) {
      return ((Integer) rhs.rssi).compareTo(lhs.rssi);
    }
  };

  private SharedPreferences sharedPreferences;
  public ArrayList<Beacon> arrayList;
  public ArrayList<Beacon> discovered;
  private BeaconArrayAdapter arrayAdapter;
  private ScanCallback scanCallback;
  private BluetoothLeScanner scanner;
  private Button scanButton;
  public Runnable runThis;
  private TextView apiKeyView;

  ProximityBeacon client= new ProximityBeaconImpl(getActivity(), "");

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sharedPreferences = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
    arrayList = new ArrayList<>();
    discovered = new ArrayList<>();
    // debugging purpose
    file=new File(Environment.getExternalStorageDirectory()+"/discoveredLog.txt");
    arrayAdapter = new BeaconArrayAdapter(getActivity(), R.layout.beacon_list_item, arrayList);
    scanCallback = new ScanCallback() {
      @Override
      public void onScanResult(int callbackType, ScanResult result) {
        ScanRecord scanRecord = result.getScanRecord();
        if (scanRecord == null) {
          Log.w(TAG, "Null ScanRecord for device " + result.getDevice().getAddress());
          return;
        }

        byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID);
        if (serviceData == null) {
          return;
        }

        // We're only interested in the UID frame time since we need the beacon ID to register.
        if (serviceData[0] != EDDYSTONE_UID_FRAME_TYPE) {
          return;
        }

        // Extract the beacon ID from the service data. Offset 0 is the frame type, 1 is the
        // Tx power, and the next 16 are the ID.
        // See https://github.com/google/eddystone/eddystone-uid for more information.
        byte[] id = Arrays.copyOfRange(serviceData, 2, 18);
        if (arrayListContainsId(arrayList, id)) {
          return;
        }

        // Draw it immediately and kick off a async request to fetch the registration status,
        // redrawing when the server returns.
        //Log.i(TAG, "id " + Utils.toHexString(id) + ", rssi " + result.getRssi());

        Beacon beacon = new Beacon("EDDYSTONE", id, Beacon.STATUS_UNSPECIFIED, result.getRssi());
        insertIntoListAndFetchStatus(beacon);

      }

      @Override
      public void onScanFailed(int errorCode) {
        Log.e(TAG, "onScanFailed errorCode " + errorCode);
      }
    };

    createScanner();
  }

  private boolean arrayListContainsId(ArrayList<Beacon> list, byte[] id) {
    for (Beacon beacon : list) {
      if (Arrays.equals(beacon.id, id)) {
        return true;
      }
    }
    return false;
  }

  private void insertIntoListAndFetchStatus(final Beacon beacon) {
    //arrayAdapter.add(beacon);
    //arrayAdapter.sort(RSSI_COMPARATOR);
    Callback getBeaconCallback = new Callback() {
      @Override
      public void onFailure(com.squareup.okhttp.Request request, IOException e) {
        Log.e(TAG, String.format("Failed request: %s, IOException %s", request, e));
      }

      @Override
      public void onResponse(com.squareup.okhttp.Response response) throws IOException {
        Beacon fetchedBeacon;
        switch (response.code()) {
          case 200:
            try {
              String body = response.body().string();
              response.body().close();
              fetchedBeacon = new Beacon(new JSONObject(body));
              fetchedBeacon.setRssi(beacon.getRssi());//fix for rssi not retained
            } catch (JSONException e) {
              Log.e(TAG, "JSONException", e);
              return;
            }
            if(fetchedBeacon!=null&fetchedBeacon.latitude!=null&fetchedBeacon.longitude!=null){
              if(!arrayListContainsId(discovered,fetchedBeacon.id)){
                discovered.add(fetchedBeacon);
                arrayList.add(fetchedBeacon);
                Log.i(TAG,"nuovo beacon: "+fetchedBeacon.getHexId());
              }
              else{
                Integer position=null;
                for(Beacon b: discovered){
                  if(Arrays.equals(b.id,fetchedBeacon.id)){
                    position=discovered.indexOf(b);
                  }
                }
                if(position!=null){
                  discovered.get(position).setRssi(beacon.getRssi());
                  Log.i(TAG,"vecchio beacon: "+fetchedBeacon.getHexId()+", nuovo rssi: "+beacon.getRssi());
                }
                if(!arrayListContainsId(arrayList,fetchedBeacon.id)){
                  arrayList.add(fetchedBeacon);
                }
              }
            }
            else{
              Log.w(TAG,"Problems retrieving lat/lng attachment. Is it assigned online?");
            }

            break;
          case 403:
            fetchedBeacon = new Beacon(beacon.type, beacon.id, Beacon.NOT_AUTHORIZED, beacon.rssi);
            break;
          case 404:
            fetchedBeacon = new Beacon(beacon.type, beacon.id, Beacon.UNREGISTERED, beacon.rssi);
            break;
          default:
            Log.e(TAG, "Unhandled beacon service response: " + response);
            response.body().close();
            return;
        }
       updateArrayAdapter();
        /*   //debugging purpose
        if(discovered!=null) {
          if (discovered.size() >= 3) {
            for(Beacon b: discovered){
              MainActivity.writeLogFile(b.getHexId()+","+b.rssi+"\n",file,MainActivityFragment.super.getActivity());
            }
            MainActivity.writeLogFile("\n\n\n\n",file,MainActivityFragment.super.getActivity());
            //end debugging
            //mCallback.onListUpdated(discovered);
          }
        }
         */
      }
    };
    //client.getBeacon(getBeaconCallback, beacon.getBeaconName());//todo: edited
    try {
      client.getForObserved(getBeaconCallback,getObservedBody(beacon.getId(),getString(R.string.allAttachment)),apiKeyView.getText().toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void updateArrayAdapter() {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        arrayAdapter.notifyDataSetChanged();
      }
    });
  }

  private void createScanner() {
    BluetoothManager btManager =
      (BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter btAdapter = btManager.getAdapter();
    if (btAdapter == null || !btAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, Constants.REQUEST_CODE_ENABLE_BLE);
    }
    if (btAdapter == null || !btAdapter.isEnabled()) {
      Log.e(TAG, "Can't enable Bluetooth");
      Toast.makeText(getActivity(), "Can't enable Bluetooth", Toast.LENGTH_SHORT).show();
      return;
    }
    scanner = btAdapter.getBluetoothLeScanner();
  }

  @Override
  public void onResume() {
    super.onResume();
    // There could be multiple instances when we need to handle a UserRecoverableAuthException
    // from GMS. Run this check every time another activity has finished running.
    String accountName = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0)
          .getString("accountName", "");
    /*if (!accountName.equals("")) {
      new AuthorizedServiceTask(getActivity(), accountName).execute();
    }*/
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data)  {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == Constants.REQUEST_CODE_PICK_ACCOUNT) {
      // Receiving a result from the AccountPicker
      if (resultCode == Activity.RESULT_OK) {
        String name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        //accountNameView.setText(name);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("accountName", name);
        editor.apply();
      }
      else if (resultCode == Activity.RESULT_CANCELED) {
        // The account picker dialog closed without selecting an account.
        // Notify users that they must pick an account to proceed.
        Toast.makeText(getActivity(), "Please pick an account", Toast.LENGTH_SHORT).show();
      }
    }
    else if (requestCode == Constants.REQUEST_CODE_ENABLE_BLE) {
      if (resultCode == Activity.RESULT_OK) {
        createScanner();
      }
      else if (resultCode == Activity.RESULT_CANCELED) {
        Toast.makeText(getActivity(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_main, container, false);
    apiKeyView=(TextView)rootView.findViewById(R.id.apikey);
    apiKeyView.setText(sharedPreferences.getString("apikey",getString(R.string.api_key)));
    final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
    progressBar.setProgress(0);
    progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);

    try {
      mCallback = (OnListUpdated) getActivity();
    }catch(ClassCastException e){
      throw new ClassCastException(getActivity().toString());
    }
    scanButton = (Button) rootView.findViewById(R.id.scanButton);

    runThis = new Runnable() {
      @Override
      public void run() {

          Utils.setEnabledViews(false, scanButton);
          sharedPreferences.edit().putString("apikey",apiKeyView.getText().toString()).apply();
          arrayAdapter.clear();
          scanner.startScan(SCAN_FILTERS, SCAN_SETTINGS, scanCallback);
          Log.i(TAG, "starting scan");
          //client = new ProximityBeaconImpl(getActivity(), accountNameView.getText().toString());//ToDo:ripristinato per null pointer exception (andrebbe eliminato
          CountDownTimer countDownTimer = new CountDownTimer(SCAN_TIME_MILLIS, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
              double i = (1 - millisUntilFinished / (double) SCAN_TIME_MILLIS) * 100;
              progressBar.setProgress((int) i);
            }

            @Override
            public void onFinish() {
              progressBar.setProgress(100);
            }
          };
          countDownTimer.start();
          Runnable stopScanning = new Runnable() {
            @Override
            public void run() {
              scanner.stopScan(scanCallback);
              Log.i(TAG, "stopped scan");
              Utils.setEnabledViews(true, scanButton);
              if(discovered.size()>=3) {//it really makes sense only to do this if we know we have at least 3 beacons
                mCallback.onListUpdated(discovered);//moved here to avoid wasting resources.
              }
              //handler.postDelayed(runThis,1000); //loop
            }
          };
          handler.postDelayed(stopScanning, SCAN_TIME_MILLIS);
      }
    };
    scanButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
          Handler hndl = new Handler();
          hndl.post(runThis);

      }});

    //accountNameView = (TextView)rootView.findViewById(R.id.accountName);
    /*accountNameView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        pickUserAccount();
      }
    });*/

    // Set the account name from the shared prefs if we ever set it before.
    String accountName = sharedPreferences.getString("accountName", "");
    /*if (!accountName.isEmpty()) {
      accountNameView.setText(accountName);
    } else {
      pickUserAccount();
    }*/

    ListView listView = (ListView)rootView.findViewById(R.id.listView);
    listView.setAdapter(arrayAdapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Beacon beacon = arrayAdapter.getItem(position);
        /*if (beacon.status.equals(Beacon.NOT_AUTHORIZED)) {
          new AlertDialog.Builder(getActivity()).setTitle("Not Authorized")
              .setMessage("You don't have permission to view the details of this beacon")
              .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  dialog.dismiss();
                }
              }).show();
          return;
        }
        if (beacon.status.equals(Beacon.STATUS_UNSPECIFIED)) {
          return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("accountName", accountNameView.getText().toString());
        bundle.putParcelable("beacon", arrayAdapter.getItem(position));
        //added
        bundle.putString("rssi",beacon.getRssi().toString());
        */
        Beacon b= arrayList.get(position);
        /*try {
          Log.i(TAG, b.toJson().toString());
        } catch (JSONException e) {
          e.printStackTrace();e.printStackTrace();
        }*/
        Bundle bundle=new Bundle();
        bundle.putParcelable("beacon",b);
        BeaconDetailsFragment fragment = new BeaconDetailsFragment();
        fragment.setArguments(bundle);
        getFragmentManager()
            .beginTransaction()
            .replace(R.id.listFragment, fragment)
            .addToBackStack(TAG)
            .commit();
      }
    });
    return rootView;
  }

  private void pickUserAccount() {
    String[] accountTypes = new String[]{"com.google"};
    Intent intent = AccountPicker.newChooseAccountIntent(
      null, null, accountTypes, false, null, null, null, null);
    startActivityForResult(intent, Constants.REQUEST_CODE_PICK_ACCOUNT);
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


  private JSONObject getObservedBody(byte[] advertisement, String namespace)
          throws JSONException {
    int packetLength = 16;
    int offset = advertisement.length - packetLength;
    String id = Base64.encodeToString(advertisement,
            offset, packetLength, Base64.NO_WRAP);
    //Log.i(TAG, "actualID:  " + id);
    //Log.i(TAG, "from adv:  " + Utils.toHexString(advertisement));
    return new JSONObject()
            .put("observations", new JSONArray()
                    .put(new JSONObject()
                            .put("advertisedId", new JSONObject()
                                    .put("type", "EDDYSTONE")
                                    .put("id", id)
                            )
                    )
            )
            .put("namespacedTypes", new JSONArray()
                    .put(namespace)
            );
  }
}
