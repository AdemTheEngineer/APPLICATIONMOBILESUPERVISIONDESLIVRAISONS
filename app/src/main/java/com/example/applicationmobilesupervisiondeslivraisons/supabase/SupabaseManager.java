package com.example.applicationmobilesupervisiondeslivraisons.supabase;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.applicationmobilesupervisiondeslivraisons.models.Livraison;
import com.example.applicationmobilesupervisiondeslivraisons.models.Personnel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SupabaseManager {

    private static final String TAG = "SupabaseManager";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface FirestoreCallback<T> {
        void onCallback(T data);
    }

    // ─── AUTH ─────────────────────────────────────────────────────────────────

    public static void login(String email, String password,
                             FirestoreCallback<Personnel> callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting login for: " + email);

                // Step 1: Supabase Auth
                String authId = SupabaseHelper.login(email, password);
                Log.d(TAG, "Auth result authId: " + authId);

                if (authId == null) {
                    Log.e(TAG, "Auth failed — authId is null");
                    mainHandler.post(() -> callback.onCallback(null));
                    return;
                }

                // Step 2: Fetch personnel record
                JSONObject obj = SupabaseHelper.getPersonnelByAuthId(authId);
                Log.d(TAG, "Personnel JSON: "
                        + (obj != null ? obj.toString() : "null"));

                if (obj == null) {
                    Log.e(TAG, "No personnel record for authId: " + authId);
                    mainHandler.post(() -> callback.onCallback(null));
                    return;
                }

                // Step 3: Build Personnel object
                Personnel personnel = new Personnel();
                personnel.setId(obj.optString("idpers"));
                personnel.setNompers(obj.optString("nompers"));
                personnel.setPrenompers(obj.optString("prenompers"));
                personnel.setRole(obj.optString("role"));
                personnel.setLogin(obj.optString("login"));
                personnel.setCodeposte(obj.optString("codeposte"));
                personnel.setAdrpers(obj.optString("adrpers"));
                personnel.setVillepers(obj.optString("villepers"));
                personnel.setTelpers(obj.optString("telpers"));

                Log.d(TAG, "Personnel built — id: " + personnel.getId()
                        + " role: " + personnel.getRole());

                // Step 4: Validate role
                String role = personnel.getRole();
                if (role == null || role.isEmpty()
                        || (!role.equals("controleur")
                        && !role.equals("livreur"))) {
                    Log.e(TAG, "Invalid role: '" + role + "'");
                    mainHandler.post(() -> callback.onCallback(null));
                    return;
                }

                // Step 5: Save session
                SessionManager.saveSession(
                        personnel.getId(),
                        personnel.getRole(),
                        personnel.getNompers(),
                        personnel.getPrenompers(),
                        authId);

                Log.d(TAG, "Session saved — personnelId: "
                        + SessionManager.getPersonnelId()
                        + " role: " + SessionManager.getRole());

                mainHandler.post(() -> callback.onCallback(personnel));

            } catch (Exception e) {
                Log.e(TAG, "Login error: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onCallback(null));
            }
        }).start();
    }

    public static void logout(FirestoreCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Logging out...");
                SupabaseHelper.logout();
                SessionManager.clearSession();
                Log.d(TAG, "Logout complete");
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onCallback(true);
                });
            } catch (Exception e) {
                Log.e(TAG, "Logout error: " + e.getMessage(), e);
                SessionManager.clearSession();
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onCallback(false);
                });
            }
        }).start();
    }

    public static void logout() {
        logout(null);
    }

    public static String getCurrentUid() {
        return SessionManager.getPersonnelId();
    }

    public static String getCurrentAuthId() {
        return SessionManager.getAuthId();
    }

    // ─── LIVRAISONS ───────────────────────────────────────────────────────────

    public static void getAllLivraisons(
            FirestoreCallback<List<Livraison>> callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "getAllLivraisons called");
                String result = SupabaseHelper.getAllLivraisons();
                List<Livraison> list = parseLivraisons(result);
                Log.d(TAG, "getAllLivraisons parsed: "
                        + list.size() + " items");
                mainHandler.post(() -> callback.onCallback(list));
            } catch (Exception e) {
                Log.e(TAG, "getAllLivraisons error: "
                        + e.getMessage(), e);
                mainHandler.post(() -> callback.onCallback(new ArrayList<>()));
            }
        }).start();
    }

    public static void getLivraisonsParLivreur(String livreurId,
                                               FirestoreCallback<List<Livraison>> callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "getLivraisonsParLivreur — livreurId: "
                        + livreurId);
                if (livreurId == null || livreurId.isEmpty()) {
                    Log.e(TAG, "livreurId is null or empty");
                    mainHandler.post(() -> callback.onCallback(new ArrayList<>()));
                    return;
                }
                String result = SupabaseHelper
                        .getLivraisonsParLivreur(livreurId);
                List<Livraison> list = parseLivraisons(result);
                Log.d(TAG, "getLivraisonsParLivreur parsed: "
                        + list.size() + " items");
                mainHandler.post(() -> callback.onCallback(list));
            } catch (Exception e) {
                Log.e(TAG, "getLivraisonsParLivreur error: "
                        + e.getMessage(), e);
                mainHandler.post(() -> callback.onCallback(new ArrayList<>()));
            }
        }).start();
    }

    public static void getLivraisonsParDate(String date,
                                            FirestoreCallback<List<Livraison>> callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "getLivraisonsParDate — date: " + date);
                String result = SupabaseHelper.getLivraisonsParDate(date);
                List<Livraison> list = parseLivraisons(result);
                mainHandler.post(() -> callback.onCallback(list));
            } catch (Exception e) {
                Log.e(TAG, "getLivraisonsParDate error: "
                        + e.getMessage(), e);
                mainHandler.post(() -> callback.onCallback(new ArrayList<>()));
            }
        }).start();
    }

    public static void updateLivraisonEtat(String livraisonId,
                                           String nouvelEtat, String remarques,
                                           FirestoreCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "updateLivraisonEtat — id: " + livraisonId
                        + " etat: " + nouvelEtat);
                boolean success = SupabaseHelper.updateLivraisonEtat(
                        livraisonId,
                        nouvelEtat,
                        remarques != null ? remarques : "");
                Log.d(TAG, "updateLivraisonEtat result: " + success);
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onCallback(success);
                });
            } catch (Exception e) {
                Log.e(TAG, "updateLivraisonEtat error: "
                        + e.getMessage(), e);
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onCallback(false);
                });
            }
        }).start();
    }

    public static void getLivraisonDetail(String livraisonId, FirestoreCallback<Livraison> callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "getLivraisonDetail — id: " + livraisonId);
                JSONObject obj = SupabaseHelper.getLivraisonById(livraisonId);

                if (obj != null) {
                    // Wrap the single object in an array to reuse our robust parsing logic
                    JSONArray wrapperArray = new JSONArray();
                    wrapperArray.put(obj);

                    List<Livraison> parsedList = parseLivraisons(wrapperArray.toString());
                    if (!parsedList.isEmpty()) {
                        mainHandler.post(() -> callback.onCallback(parsedList.get(0)));
                        return;
                    }
                }
                mainHandler.post(() -> callback.onCallback(null));
            } catch (Exception e) {
                Log.e(TAG, "getLivraisonDetail error: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onCallback(null));
            }
        }).start();
    }

    // ─── MESSAGES ─────────────────────────────────────────────────────────────

    public static void sendBroadcastMessage(String texte,
                                            FirestoreCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                String personnelId = SessionManager.getPersonnelId();
                Log.d(TAG, "sendBroadcastMessage — texte: " + texte
                        + " personnelId: " + personnelId);

                boolean success = SupabaseHelper.insertMessage(
                        texte, "controleur", "broadcast",
                        null, personnelId);
                Log.d(TAG, "sendBroadcastMessage result: " + success);
                mainHandler.post(() -> callback.onCallback(success));

            } catch (Exception e) {
                Log.e(TAG, "sendBroadcastMessage error: "
                        + e.getMessage(), e);
                mainHandler.post(() -> callback.onCallback(false));
            }
        }).start();
    }

    public static void sendDriverMessage(String texte,
                                         FirestoreCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                String personnelId = SessionManager.getPersonnelId();
                Log.d(TAG, "sendDriverMessage — texte: " + texte
                        + " personnelId: " + personnelId);

                boolean success = SupabaseHelper.insertMessage(
                        texte, "livreur", "driver_message",
                        null, personnelId);
                Log.d(TAG, "sendDriverMessage result: " + success);
                mainHandler.post(() -> callback.onCallback(success));

            } catch (Exception e) {
                Log.e(TAG, "sendDriverMessage error: "
                        + e.getMessage(), e);
                mainHandler.post(() -> callback.onCallback(false));
            }
        }).start();
    }

    public static void sendUrgenceMessage(String nocde, String telClient,
                                          String motif, FirestoreCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                String personnelId = SessionManager.getPersonnelId();
                Log.d(TAG, "sendUrgenceMessage — motif: " + motif
                        + " personnelId: " + personnelId);

                boolean success = SupabaseHelper.insertMessage(
                        motif, "livreur", "urgence",
                        motif, personnelId);
                Log.d(TAG, "sendUrgenceMessage result: " + success);
                mainHandler.post(() -> callback.onCallback(success));

            } catch (Exception e) {
                Log.e(TAG, "sendUrgenceMessage error: "
                        + e.getMessage(), e);
                mainHandler.post(() -> callback.onCallback(false));
            }
        }).start();
    }

    public static void listenAllMessages(
            FirestoreCallback<List<Map<String, Object>>> callback) {
        fetchMessages(false, null, callback);
    }

    public static void listenDriverMessages(String authId,
                                            FirestoreCallback<List<Map<String, Object>>> callback) {
        fetchMessages(true, authId, callback);
    }

    public static void getMessages(
            FirestoreCallback<List<Map<String, Object>>> callback) {
        listenAllMessages(callback);
    }

    private static void fetchMessages(boolean isDriver,
                                      String personnelId, // NOTE: For drivers, this MUST be the Auth UUID now
                                      FirestoreCallback<List<Map<String, Object>>> callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "fetchMessages — isDriver: " + isDriver
                        + " personnelId: " + personnelId);

                String result = SupabaseHelper.getAllMessages();
                List<Map<String, Object>> all = parseMessages(result);
                Log.d(TAG, "fetchMessages total: " + all.size());

                if (isDriver && personnelId != null) {
                    List<Map<String, Object>> filtered = new ArrayList<>();
                    for (Map<String, Object> msg : all) {
                        String msgType = (String) msg.get("type");
                        String fromUser = (String) msg.get("from_user");
                        String msgRole = (String) msg.get("role"); // Extract the role

                        // ✅ THE FIX: Let drivers see messages from the controleur
                        if ("broadcast".equals(msgType)
                                || "controleur".equals(msgRole)
                                || personnelId.equals(fromUser)) {
                            filtered.add(msg);
                        }
                    }
                    Log.d(TAG, "fetchMessages filtered for driver: "
                            + filtered.size());
                    mainHandler.post(() -> callback.onCallback(filtered));
                } else if (isDriver) {
                    // personnelId is null — return empty rather than leaking all messages
                    Log.w(TAG, "fetchMessages: isDriver=true but personnelId is null, returning empty");
                    mainHandler.post(() -> callback.onCallback(new ArrayList<>()));
                } else {
                    mainHandler.post(() -> callback.onCallback(all));
                }
            } catch (Exception e) {
                Log.e(TAG, "fetchMessages error: "
                        + e.getMessage(), e);
                mainHandler.post(() -> callback.onCallback(new ArrayList<>()));
            }
        }).start();
    }

    // ─── PARSERS ──────────────────────────────────────────────────────────────

    private static List<Livraison> parseLivraisons(String json) {
        List<Livraison> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Livraison l = new Livraison();

                // 🚨 PRINT RAW JSON TO LOGCAT: Search for "RAW JSON ROW" in Logcat!
                Log.w(TAG, "RAW JSON ROW " + i + ": " + obj.toString());

                // ── Core fields
                l.setId(obj.optString("id"));
                l.setIdCommande(obj.optString("nocde"));
                l.setEtat(obj.optString("etatliv"));
                l.setDateLivraison(obj.optString("dateliv"));
                l.setLivreurNom(obj.optString("livreur"));
                l.setModePaiement(obj.optString("modepay", obj.optString("modepaiement", "—")));
                l.setRemarques(obj.optString("remarque"));
                l.setOrdrePassage(obj.optInt("ordrepassage", 0));

                // ── Relationships
                JSONObject cmdObj = obj.optJSONObject("commandes");
                if (cmdObj == null) {
                    JSONArray cmdArray = obj.optJSONArray("commandes");
                    if (cmdArray != null && cmdArray.length() > 0) {
                        cmdObj = cmdArray.getJSONObject(0);
                    }
                }

                if (cmdObj != null) {
                    // ── NEW: Calculate Totals from 'ligcdes' and 'articles' ──
                    double totalMontant = 0.0;
                    int totalArticles = 0;

                    JSONArray ligcdesArray = cmdObj.optJSONArray("ligcdes");
                    if (ligcdesArray != null) {
                        for (int j = 0; j < ligcdesArray.length(); j++) {
                            JSONObject ligne = ligcdesArray.getJSONObject(j);

                            // 1. Get the quantity (guessing the column is qte, quantite, or qtecde)
                            int qte = getAnyInt(ligne, "qte", "quantite", "qtecde", "quantitecde");
                            totalArticles += qte;

                            // 2. Get the article to find the price
                            JSONObject articleObj = ligne.optJSONObject("articles");

                            // Handle potential array return for articles
                            if (articleObj == null) {
                                JSONArray artArray = ligne.optJSONArray("articles");
                                if (artArray != null && artArray.length() > 0) {
                                    articleObj = artArray.getJSONObject(0);
                                }
                            }

                            // 3. Multiply price by quantity
                            if (articleObj != null) {
                                // Using "prixv" based exactly on your screenshot!
                                double prixUnitaire = getAnyDouble(articleObj, "prixv", "prix");
                                totalMontant += (prixUnitaire * qte);
                            } else {
                                // Fallback: if the price was saved directly in the ligcdes table
                                totalMontant += getAnyDouble(ligne, "totalligne", "montant", "prix");
                            }
                        }
                    }

                    // Apply the calculated totals!
                    l.setMontant(totalMontant);
                    l.setNombreArticles(totalArticles);

                    // ── Clients (Keep this exactly as you had it) ──
                    JSONObject cltObj = cmdObj.optJSONObject("clients");
                    if (cltObj == null) {
                        JSONArray cltArray = cmdObj.optJSONArray("clients");
                        if (cltArray != null && cltArray.length() > 0) cltObj = cltArray.getJSONObject(0);
                    }

                    if (cltObj != null) {
                        l.setClientNom(cltObj.optString("nomclt", cltObj.optString("clientnom", "—")));
                        l.setClientTelephone(cltObj.optString("telclt", cltObj.optString("clienttel", "—")));
                        l.setClientVille(cltObj.optString("villeclt", cltObj.optString("clientville", "—")));
                        l.setClientAdresse(cltObj.optString("adrclt", cltObj.optString("clientadresse", "—")));
                    } else {
                        l.setClientNom("—"); l.setClientTelephone("—"); l.setClientVille("—"); l.setClientAdresse("—");
                    }
                } else {
                    // Total fallback if join failed completely
                    l.setMontant(getAnyDouble(obj, "montant", "montantcde", "total", "prix"));
                    l.setNombreArticles(getAnyInt(obj, "nbarticles", "nbart", "quantite", "qte"));
                    l.setClientNom(obj.optString("nomclt", obj.optString("clientnom", "—")));
                    l.setClientTelephone(obj.optString("telclt", obj.optString("clienttel", "—")));
                    l.setClientVille(obj.optString("villeclt", obj.optString("clientville", "—")));
                    l.setClientAdresse(obj.optString("adrclt", obj.optString("clientadresse", "—")));
                }

                list.add(l);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseLivraisons error: " + e.getMessage(), e);
        }
        return list;
    }

    private static List<Map<String, Object>> parseMessages(String json) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Map<String, Object> msg = new HashMap<>();

                msg.put("id", obj.optString("id"));
                msg.put("texte", obj.optString("texte"));
                msg.put("from_user", obj.optString("from_user"));
                msg.put("from", obj.optString("from_user"));
                msg.put("role", obj.optString("role"));
                msg.put("type", obj.optString("type"));
                msg.put("motif", obj.optString("motif"));
                msg.put("lu", obj.optBoolean("lu", false));

                String createdAt = obj.optString("created_at", "");
                msg.put("created_at", createdAt);
                msg.put("timestamp", createdAt);

                if (!createdAt.isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                        Date date = sdf.parse(
                                createdAt.length() > 19
                                        ? createdAt.substring(0, 19)
                                        : createdAt);
                        msg.put("date", date);
                    } catch (Exception parseEx) {
                        Log.w(TAG, "Could not parse date: " + createdAt);
                    }
                }

                list.add(msg);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseMessages error: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Bulletproof Extractors ───────────────────────────────────────────────

    private static double getAnyDouble(JSONObject json, String... keys) {
        if (json == null) return 0.0;
        for (String k : keys) {
            if (json.has(k) && !json.isNull(k)) {
                try {
                    return json.getDouble(k);
                } catch (Exception e) {
                    try {
                        String s = json.getString(k).replace(",", ".");
                        s = s.replaceAll("[^\\d.]", "");
                        if (!s.isEmpty()) return Double.parseDouble(s);
                    } catch (Exception ex) { /* Ignore and try next key */ }
                }
            }
        }
        return 0.0;
    }

    private static int getAnyInt(JSONObject json, String... keys) {
        if (json == null) return 0;
        for (String k : keys) {
            if (json.has(k) && !json.isNull(k)) {
                try {
                    return json.getInt(k);
                } catch (Exception e) {
                    try {
                        String s = json.getString(k).replaceAll("[^\\d]", "");
                        if (!s.isEmpty()) return Integer.parseInt(s);
                    } catch (Exception ex) { /* Ignore and try next key */ }
                }
            }
        }
        return 0;
    }
}