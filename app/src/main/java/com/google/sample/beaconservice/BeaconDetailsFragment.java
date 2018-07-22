package com.google.sample.beaconservice;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;


public class BeaconDetailsFragment extends Fragment {
  private Beacon beacon ;
  private TextView attachmentsLabel;
  private TableLayout attachmentsTable;

  private static final String TAG = BeaconDetailsFragment.class.getSimpleName();

  private static final TableLayout.LayoutParams FIXED_WIDTH_COLS_LAYOUT =
          new TableLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f);

  private static final TableLayout.LayoutParams BUTTON_COL_LAYOUT =
          new TableLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle bundle= getArguments();
    if (bundle!=null)beacon =  bundle.getParcelable("beacon");

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.beacon_details_fragment, container, false);
    attachmentsLabel = (TextView)rootView.findViewById(R.id.attachmentsLabel);
    attachmentsTable = (TableLayout)rootView.findViewById(R.id.attachmentsTableLayout);
    TextView id = (TextView) rootView.findViewById(R.id.advertisedId_Id);
    TextView type = (TextView) rootView.findViewById(R.id.advertisedId_Type);
    TextView latLng = (TextView) rootView.findViewById(R.id.latLng);
  try {
    latLng.setText(beacon.getLatLng().toString());
    id.setText(beacon.getHexId());
    type.setText(beacon.type);
  }
  catch (NullPointerException e){
    Log.e(TAG, "this beacon has null arguments...");
  }

    attachmentsTable.removeAllViews();
    attachmentsTable.addView(makeAttachmentTableHeader());
    //attachmentsTable.addView(makeAttachmentInsertRow());
    for (int i = 0; i < beacon.attachments.size(); i++) {
      Beacon.Duet attachment = beacon.attachments.get(i);
      attachmentsTable.addView(makeAttachmentRow(attachment));
    }

    return rootView;
  }

  private LinearLayout makeAttachmentTableHeader() {
    LinearLayout headerRow = new LinearLayout(getActivity());
    headerRow.addView(makeTextView("Type"));
    headerRow.addView(makeTextView("Data"));

    // Attachment rows will have four elements, so insert a fake one here with the same
    // layout weight as the delete button.
    TextView dummyView = new TextView(getActivity());
    dummyView.setLayoutParams(BUTTON_COL_LAYOUT);
    headerRow.addView(dummyView);

    return headerRow;
  }

  private LinearLayout makeAttachmentRow(Beacon.Duet attachment) {
    LinearLayout row = new LinearLayout(getActivity());
    int id = View.generateViewId();
    row.setId(id);
    row.addView(makeTextView(attachment.name));
    row.addView(makeTextView(attachment.data));
    return row;
  }

  private TextView makeTextView(String text) {
    TextView textView = new TextView(getActivity());
    textView.setText(text);
    textView.setLayoutParams(FIXED_WIDTH_COLS_LAYOUT);
    return textView;
  }

}
