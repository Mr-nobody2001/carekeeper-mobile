package com.example.carekeeper.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.carekeeper.ui.AccidentPopupActivity;

public class AccidentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.carekeeper.ACCIDENT_DETECTED".equals(intent.getAction())) {
            Log.i("AccidentReceiver", "🚨 Acidente detectado! Abrindo popup...");

            Intent popupIntent = new Intent(context, AccidentPopupActivity.class);
            popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            context.startActivity(popupIntent);
        }
    }
}
