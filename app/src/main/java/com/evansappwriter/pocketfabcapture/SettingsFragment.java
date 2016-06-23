package com.evansappwriter.pocketfabcapture;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by markevans on 6/23/16.
 */
public class SettingsFragment extends PreferenceFragment {
    private final static int REQUIRED_PERMISSION_REQUEST_CODE = 2121;
    private final static String SERVICE_ENABLED_KEY = "serviceEnabledKey";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        enableHeadServiceCheckbox(false);

        if(!isRequiredPermissionGranted()){
            enableHeadServiceCheckbox(false);
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getActivity().getPackageName()));
            startActivityForResult(intent, REQUIRED_PERMISSION_REQUEST_CODE);
        } else {
            enableHeadServiceCheckbox(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUIRED_PERMISSION_REQUEST_CODE) {
            if (!isRequiredPermissionGranted()) {
                Toast.makeText(getActivity(), "Required permission is not granted. Please restart the app and grant required permission.", Toast.LENGTH_LONG).show();
            } else {
                enableHeadServiceCheckbox(true);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        stopHeadService();
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean enabled = SP.getBoolean(SERVICE_ENABLED_KEY, false);
        if(enabled) {
            startHeadService();
        } else {
            stopHeadService();
        }
    }

    private void enableHeadServiceCheckbox(boolean enabled) {
        getPreferenceScreen().findPreference(SERVICE_ENABLED_KEY).setEnabled(enabled);
    }

    private void startHeadService() {
        Intent i = new Intent();
        i.setClassName("com.pocket.doorway", "com.pocket.doorway.GameActivity");
        try {
            startActivity(i);
            getActivity().startService(new Intent(getActivity(), BeamService.class));
        } catch (Exception e) {
            Log.e("SettingsFragment" ,e.toString());
            getActivity().finish();
        }
    }

    private void stopHeadService() {
        getActivity().stopService(new Intent(getActivity(), BeamService.class));
    }

    private boolean isRequiredPermissionGranted() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(getActivity());
        }
        return true;
    }
}
