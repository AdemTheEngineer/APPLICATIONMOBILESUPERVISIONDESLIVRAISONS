package com.example.applicationmobilesupervisiondeslivraisons.supabase

import android.util.Log
import com.example.applicationmobilesupervisiondeslivraisons.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject


object SupabaseHelper {

    private const val TAG = "SupabaseHelper"
    private val client get() = SupabaseClientProvider.client

    // ─── AUTH ─────────────────────────────────────────────────────────────────

    @JvmStatic
    fun login(email: String, password: String): String? {
        return try {
            Log.d(TAG, "Attempting login for: $email")
            runBlocking {
                client.auth.signInWith(Email) {
                    this.email    = email
                    this.password = password
                }
            }
            val authId = client.auth.currentSessionOrNull()?.user?.id
            Log.d(TAG, "Login success — authId: $authId")
            authId
        } catch (e: Exception) {
            Log.e(TAG, "login error: ${e.message}", e)
            null
        }
    }

    @JvmStatic
    fun logout() {
        try {
            Log.d(TAG, "Logging out...")
            runBlocking { client.auth.signOut() }
            Log.d(TAG, "Logout success")
        } catch (e: Exception) {
            Log.e(TAG, "logout error: ${e.message}", e)
        }
    }

    @JvmStatic
    fun getCurrentAuthId(): String? {
        return client.auth.currentSessionOrNull()?.user?.id
    }

    @JvmStatic
    fun restoreSessionIfNeeded(): Boolean {
        return try {
            val session = client.auth.currentSessionOrNull()
            if (session != null) {
                Log.d(TAG, "Session exists — userId: ${session.user?.id}")
                try {
                    runBlocking { client.auth.refreshCurrentSession() }
                    Log.d(TAG, "Session refreshed successfully")
                } catch (refreshEx: Exception) {
                    Log.w(TAG, "Token refresh attempt: ${refreshEx.message}")
                }
                true
            } else {
                Log.e(TAG, "No active session — user needs to re-login")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "restoreSessionIfNeeded error: ${e.message}", e)
            false
        }
    }

    // ─── PERSONNEL ────────────────────────────────────────────────────────────

    @JvmStatic
    fun getPersonnelByAuthId(authId: String): JSONObject? {
        return try {
            Log.d(TAG, "Fetching personnel for authId: $authId")
            val result = runBlocking {
                client.postgrest["personnel"]
                    .select {
                        filter { eq("auth_id", authId) }
                    }
                    .data
            }
            Log.d(TAG, "Personnel raw result: $result")
            val array = JSONArray(result)
            if (array.length() > 0) {
                val obj = array.getJSONObject(0)
                Log.d(TAG, "Personnel found — role: ${obj.optString("role")}")
                obj
            } else {
                Log.e(TAG, "No personnel record found for authId: $authId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPersonnelByAuthId error: ${e.message}", e)
            null
        }
    }

    // ─── LIVRAISONS ───────────────────────────────────────────────────────────

    @JvmStatic
    fun getAllLivraisons(): String {
        return try {
            Log.d(TAG, "Fetching all livraisons...")
            val result = runBlocking {
                client.postgrest["livraisoncom"]
                    .select(columns = Columns.raw("*, commandes(*, clients(*), ligcdes(*, articles(*)))"))
                    .data
            }
            Log.d(TAG, "getAllLivraisons result length: ${result.length}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "getAllLivraisons error: ${e.message}", e)
            "[]"
        }
    }

    @JvmStatic
    fun getLivraisonsParLivreur(livreurId: String): String {
        return try {
            Log.d(TAG, "Fetching livraisons for livreur: $livreurId")
            val result = runBlocking {
                client.postgrest["livraisoncom"]
                    .select(columns = Columns.raw("*, commandes(*, clients(*), ligcdes(*, articles(*)))")) {
                        filter { eq("livreur", livreurId) }
                    }
                    .data
            }
            Log.d(TAG, "getLivraisonsParLivreur result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "getLivraisonsParLivreur error: ${e.message}", e)
            "[]"
        }
    }

    @JvmStatic
    fun getLivraisonsParDate(date: String): String {
        return try {
            Log.d(TAG, "Fetching livraisons for date: $date")
            val result = runBlocking {
                client.postgrest["livraisoncom"]
                    .select(columns = Columns.raw("*, commandes(*, clients(*), ligcdes(*, articles(*)))")) {
                        filter { eq("dateliv", date) }
                    }
                    .data
            }
            Log.d(TAG, "getLivraisonsParDate result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "getLivraisonsParDate error: ${e.message}", e)
            "[]"
        }
    }

    @JvmStatic
    fun getLivraisonById(livraisonId: String): JSONObject? {
        return try {
            Log.d(TAG, "Fetching livraison by id: $livraisonId")
            val result = runBlocking {
                client.postgrest["livraisoncom"]
                    .select(columns = Columns.raw("*, commandes(*, clients(*), ligcdes(*, articles(*)))")) {
                        filter { eq("id", livraisonId) }
                    }
                    .data
            }
            Log.d(TAG, "getLivraisonById result: $result")
            val array = JSONArray(result)
            if (array.length() > 0) array.getJSONObject(0) else null
        } catch (e: Exception) {
            Log.e(TAG, "getLivraisonById error: ${e.message}", e)
            null
        }
    }

    @JvmStatic
    fun updateLivraisonEtat(
        livraisonId: String,
        nouvelEtat: String,
        remarques: String
    ): Boolean {
        return try {
            Log.d(TAG, "Updating livraison $livraisonId → etat: $nouvelEtat")
            runBlocking {
                client.postgrest["livraisoncom"]
                    .update(
                        {
                            set("etatliv",  nouvelEtat)
                            set("remarque", remarques)
                        }
                    ) {
                        filter { eq("id", livraisonId) }
                    }
            }
            Log.d(TAG, "updateLivraisonEtat success")
            true
        } catch (e: Exception) {
            Log.e(TAG, "updateLivraisonEtat error: ${e.message}", e)
            false
        }
    }

    // ─── MESSAGES ─────────────────────────────────────────────────────────────

    /**
     * Insert a message via raw HTTP POST to the Supabase REST API.
     *
     * KEY FACTS confirmed from logs + DB schema:
     *   - messages.from_user  → FK → personnel.idpers  (UUID, e.g. "60fdbb11-...")
     *   - personnel.idpers    = SessionManager.getPersonnelId()
     *   - auth UUID           = SessionManager.getAuthId()  (different value!)
     *
     * We MUST send personnel.idpers as from_user, NOT the auth UUID.
     * The caller always passes SessionManager.getPersonnelId() as fromUser — use it.
     */
    @JvmStatic
    fun insertMessage(
        texte: String,
        role: String,
        type: String,
        motif: String?,
        fromUser: String?   // Must be personnel.idpers — FK constraint enforces this
    ): Boolean {
        try {
            Log.d(TAG, "=== insertMessage START (raw HTTP) ===")
            Log.d(TAG, "  texte   : $texte")
            Log.d(TAG, "  role    : $role")
            Log.d(TAG, "  type    : $type")
            Log.d(TAG, "  motif   : $motif")
            Log.d(TAG, "  fromUser: $fromUser")

            if (texte.isBlank()) {
                Log.e(TAG, "insertMessage FAILED — texte is blank")
                return false
            }

            // 1. Refresh the auth session to get a fresh JWT
            try {
                runBlocking { client.auth.refreshCurrentSession() }
                Log.d(TAG, "insertMessage — session refreshed OK")
            } catch (refreshEx: Exception) {
                Log.w(TAG, "insertMessage — refresh warning: ${refreshEx.message}")
            }

            // 2. Get the current auth session for the Bearer token only
            val session = client.auth.currentSessionOrNull()
            val accessToken = session?.accessToken
            val currentAuthId = session?.user?.id

            Log.d(TAG, "insertMessage — currentAuthId: $currentAuthId")
            Log.d(TAG, "insertMessage — hasAccessToken: ${accessToken != null}")

            if (accessToken == null) {
                Log.e(TAG, "insertMessage FAILED — no active auth session or token")
                return false
            }

            // 3. Resolve from_user = personnel.idpers (required by FK constraint)
            //    Priority: use passed fromUser → fallback to SessionManager → fallback to DB lookup
            val effectiveFromUser: String = when {
                !fromUser.isNullOrEmpty() -> {
                    Log.d(TAG, "insertMessage — using passed fromUser: $fromUser")
                    fromUser
                }
                !SessionManager.getPersonnelId().isNullOrEmpty() -> {
                    val pid = SessionManager.getPersonnelId()
                    Log.d(TAG, "insertMessage — using SessionManager.personnelId: $pid")
                    pid!!
                }
                currentAuthId != null -> {
                    // Last resort: look up idpers from personnel table
                    val personnelObj = getPersonnelByAuthId(currentAuthId)
                    val idpers = personnelObj?.optString("idpers")
                    Log.d(TAG, "insertMessage — fallback DB lookup idpers: $idpers")
                    if (idpers.isNullOrEmpty()) {
                        Log.e(TAG, "insertMessage FAILED — could not resolve personnel.idpers")
                        return false
                    }
                    idpers
                }
                else -> {
                    Log.e(TAG, "insertMessage FAILED — no auth session and no personnelId")
                    return false
                }
            }

            Log.d(TAG, "insertMessage — effectiveFromUser (personnel.idpers): $effectiveFromUser")

            // 4. Build the JSON body
            val jsonBody = JSONObject().apply {
                put("texte", texte.trim())
                put("role", role)
                put("type", type)
                put("lu", false)
                put("from_user", effectiveFromUser)
                if (!motif.isNullOrEmpty()) {
                    put("motif", motif)
                }
            }
            Log.d(TAG, "insertMessage — JSON body: $jsonBody")

            // 5. Build the Supabase REST API URL
            val supabaseUrl = "https://ahbramrtgcvvmbwkgwyk.supabase.co"
            val apiKey = "sb_publishable_mk2_gF-1rAqQnVm1fhCFaw_eiuQyLIM"
            val url = java.net.URL("$supabaseUrl/rest/v1/messages")

            // 6. Make the raw HTTP POST request
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", apiKey)
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            // 7. Write the body
            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            // 8. Read the response
            val responseCode = conn.responseCode
            val responseBody = try {
                conn.inputStream.bufferedReader().readText()
            } catch (e: Exception) {
                try {
                    conn.errorStream?.bufferedReader()?.readText() ?: "no error body"
                } catch (e2: Exception) {
                    "could not read error body"
                }
            }
            conn.disconnect()

            Log.d(TAG, "insertMessage — HTTP $responseCode")
            Log.d(TAG, "insertMessage — response: $responseBody")

            return if (responseCode in 200..299) {
                Log.d(TAG, "=== insertMessage SUCCESS ===")
                true
            } else {
                Log.e(TAG, "=== insertMessage FAILED — HTTP $responseCode ===")
                Log.e(TAG, "  response: $responseBody")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "=== insertMessage ERROR ===")
            Log.e(TAG, "  message : ${e.message}")
            Log.e(TAG, "  cause   : ${e.cause?.message}")
            Log.e(TAG, "  class   : ${e.javaClass.simpleName}")
            e.printStackTrace()
            return false
        }
    }

    @JvmStatic
    fun getAllMessages(): String {
        return try {
            Log.d(TAG, "Fetching all messages...")
            val result = runBlocking {
                client.postgrest["messages"]
                    .select {
                        order("created_at", Order.ASCENDING)
                    }
                    .data
            }
            Log.d(TAG, "getAllMessages result length: ${result.length}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "getAllMessages error: ${e.message}", e)
            "[]"
        }
    }

    // ─── CLIENT INFO ──────────────────────────────────────────────────────────

    @JvmStatic
    fun getClientByCommande(nocde: String): JSONObject? {
        return try {
            Log.d(TAG, "getClientByCommande — nocde: $nocde")

            val cmdResult = runBlocking {
                client.postgrest["commandes"]
                    .select {
                        filter { eq("nocde", nocde) }
                    }
                    .data
            }
            Log.d(TAG, "commande result: $cmdResult")

            val cmdArray = JSONArray(cmdResult)
            if (cmdArray.length() == 0) {
                Log.e(TAG, "No commande found for nocde: $nocde")
                return null
            }

            val noclt = cmdArray.getJSONObject(0).optString("noclt")
            Log.d(TAG, "found noclt: $noclt")

            val cltResult = runBlocking {
                client.postgrest["clients"]
                    .select {
                        filter { eq("noclt", noclt) }
                    }
                    .data
            }
            Log.d(TAG, "client result: $cltResult")

            val cltArray = JSONArray(cltResult)
            if (cltArray.length() > 0) cltArray.getJSONObject(0) else null

        } catch (e: Exception) {
            Log.e(TAG, "getClientByCommande error: ${e.message}", e)
            null
        }
    }
}