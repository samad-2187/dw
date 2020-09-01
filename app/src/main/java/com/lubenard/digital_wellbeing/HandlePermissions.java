package com.lubenard.digital_wellbeing;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

/**
 * This Fragment show to the user how to grant access to the usage permission.
 */
public class HandlePermissions extends Fragment {

    public static final String TAG = "HandlePermission";

    /**
     * Check if Usage Permission is granted
     * @param context As this is a static method, we need a given context
     * @return Return true if permission is granted, else return false
     */
    public static boolean checkIfUsagePermissionGranted (Context context) {
        int mode;
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        else
            mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());

        if (mode == AppOpsManager.MODE_ALLOWED)
            return true;
        else
            return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        return inflater.inflate(R.layout.handle_permissions_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button permissionButton = view.findViewById(R.id.permission_button);

        // I do not like the idea of having to check twice for authorisation (one at launch + one here)
        // TODO: Find a way to change to autodetect auth, so no need to click a second time on the button ?
        permissionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (checkIfUsagePermissionGranted(getContext())) {
                    MainFragment mainFragment = new MainFragment();
                    getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, mainFragment, "findThisFragment")
                        .addToBackStack(null)
                        .commit();
                } else {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivity(intent);
                }
            }
        });
    }
}
