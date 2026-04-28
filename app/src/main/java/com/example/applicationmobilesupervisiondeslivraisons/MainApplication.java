package com.example.applicationmobilesupervisiondeslivraisons;

import android.app.Application;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SessionManager;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // ✅ Initialize SessionManager so it can be used from any Activity
        SessionManager.init(this);
    }
}