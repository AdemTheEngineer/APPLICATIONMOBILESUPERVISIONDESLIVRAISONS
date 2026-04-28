package com.example.applicationmobilesupervisiondeslivraisons.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applicationmobilesupervisiondeslivraisons.R;
import com.example.applicationmobilesupervisiondeslivraisons.activities.controleur.ControleurActivity;
import com.example.applicationmobilesupervisiondeslivraisons.activities.livreur.LivreurActivity;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SessionManager;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ FIX: Check session via SessionManager instead of SupabaseManager.getCurrentUid()
        // This works even if the Supabase client hasn't loaded yet
        if (SessionManager.isLoggedIn()) {
            Log.d(TAG, "Session found — redirecting role: " + SessionManager.getRole());
            navigateTo(SessionManager.getRole(), SessionManager.getPersonnelId());
            return;
        }

        TextInputEditText editLogin    = findViewById(R.id.edit_login);
        TextInputEditText editPassword = findViewById(R.id.edit_password);
        MaterialButton    btnLogin     = findViewById(R.id.btn_login);

        // ── Login button ──────────────────────────────────────────────────────
        btnLogin.setOnClickListener(v -> {
            String email    = editLogin.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this,
                        "Veuillez remplir tous les champs",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Connexion...");

            // ✅ FIX: SupabaseManager.login now fully implemented
            SupabaseManager.login(email, password, personnel -> {
                // ✅ Already on main thread — no runOnUiThread needed
                if (personnel == null) {
                    Log.e(TAG, "Login failed — personnel is null");
                    Toast.makeText(this,
                            "Email ou mot de passe incorrect",
                            Toast.LENGTH_LONG).show();
                    resetButton(btnLogin);
                    return;
                }

                Log.d(TAG, "Login OK — id: " + personnel.getId()
                        + " role: " + personnel.getRole());

                if (personnel.getRole() == null) {
                    Toast.makeText(this,
                            "Rôle non défini pour cet utilisateur",
                            Toast.LENGTH_LONG).show();
                    // ✅ logout with callback — non-blocking
                    SupabaseManager.logout(success ->
                            Log.d(TAG, "Logged out after null role"));
                    resetButton(btnLogin);
                    return;
                }

                // ✅ Navigate — SessionManager already populated inside login()
                navigateTo(personnel.getRole(), personnel.getId());
            });
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // ✅ FIX: navigateTo now accepts personnelId directly
    private void navigateTo(String role, String personnelId) {
        if (role == null) {
            Log.e(TAG, "navigateTo called with null role");
            return;
        }

        Intent intent;
        if ("controleur".equals(role)) {
            intent = new Intent(this, ControleurActivity.class);
        } else {
            // livreur
            intent = new Intent(this, LivreurActivity.class);
            // ✅ Pass personnelId (idpers) not auth_id
            if (personnelId != null) {
                intent.putExtra("username", personnelId);
            }
        }

        startActivity(intent);
        finish();
    }

    private void resetButton(MaterialButton btn) {
        btn.setEnabled(true);
        btn.setText("Se connecter");
    }
}