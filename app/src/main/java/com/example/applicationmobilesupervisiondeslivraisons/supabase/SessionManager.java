package com.example.applicationmobilesupervisiondeslivraisons.supabase;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME   = "AppSession";
    private static final String KEY_ID      = "personnelId";
    private static final String KEY_ROLE    = "role";
    private static final String KEY_NOM     = "nom";
    private static final String KEY_PRENOM  = "prenom";
    private static final String KEY_AUTH_ID = "authId";

    private static SharedPreferences prefs;

    public static void init(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveSession(String personnelId, String role,
                                   String nom, String prenom, String authId) {
        if (prefs == null) return;
        prefs.edit()
                .putString(KEY_ID,      personnelId)
                .putString(KEY_ROLE,    role)
                .putString(KEY_NOM,     nom)
                .putString(KEY_PRENOM,  prenom)
                .putString(KEY_AUTH_ID, authId)
                .apply();
    }

    public static String getPersonnelId() {
        return prefs != null ? prefs.getString(KEY_ID,      null) : null;
    }
    public static String getRole() {
        return prefs != null ? prefs.getString(KEY_ROLE,    null) : null;
    }
    public static String getNom() {
        return prefs != null ? prefs.getString(KEY_NOM,     null) : null;
    }
    public static String getPrenom() {
        return prefs != null ? prefs.getString(KEY_PRENOM,  null) : null;
    }
    public static String getAuthId() {
        return prefs != null ? prefs.getString(KEY_AUTH_ID, null) : null;
    }
    public static String getFullName() {
        String p = getPrenom();
        String n = getNom();
        return ((p != null ? p : "") + " " + (n != null ? n : "")).trim();
    }
    public static boolean isLoggedIn() {
        return getPersonnelId() != null;
    }
    public static boolean isControleur() {
        return "controleur".equals(getRole());
    }
    public static boolean isLivreur() {
        return "livreur".equals(getRole());
    }
    public static void clearSession() {
        if (prefs != null) prefs.edit().clear().apply();
    }
}