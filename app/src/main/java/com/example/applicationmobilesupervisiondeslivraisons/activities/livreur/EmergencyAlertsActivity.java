package com.example.applicationmobilesupervisiondeslivraisons.activities.livreur;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.applicationmobilesupervisiondeslivraisons.R;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseHelper;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmergencyAlertsActivity extends AppCompatActivity {

    private String uid;
    private TextInputEditText editCustomAlert;
    private MaterialButton btnSendCustom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_alerts);

        // Get uid from intent; fallback to SupabaseManager
        // Note: This correctly gets the Personnel ID (e.g., "P001") which LivreurActivity needs
        uid = getIntent().getStringExtra("username");
        if (uid == null) uid = SupabaseManager.getCurrentUid();

        // Date sub-header
        TextView tvDate = findViewById(R.id.tv_date);
        if (tvDate != null) {
            tvDate.setText(new SimpleDateFormat("M/d/yyyy", Locale.US).format(new Date()));
        }

        // Driver name
        TextView tvDriver = findViewById(R.id.tv_driver_name);
        if (tvDriver != null) {
            String fullName = com.example.applicationmobilesupervisiondeslivraisons.supabase.SessionManager.getFullName();
            if (fullName == null || fullName.trim().isEmpty()) {
                fullName = "—";
            }
            tvDriver.setText(getString(R.string.driver_format, fullName));
        }

        // Toolbar back arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_logout);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Bottom nav
        View tabSchedule = findViewById(R.id.nav_schedule);
        if (tabSchedule != null) tabSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, LivreurActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            if (uid != null) intent.putExtra("username", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        View tabAlerts = findViewById(R.id.nav_alerts);
        if (tabAlerts != null) tabAlerts.setOnClickListener(v -> { /* already on this screen */ });

        View tabHub = findViewById(R.id.nav_hub);
        if (tabHub != null) {
            tabHub.setOnClickListener(v -> {
                Intent intent = new Intent(this, DriverHubActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                // Passing uid here is harmless since DriverHubActivity uses getCurrentAuthId() internally now
                if (uid != null) intent.putExtra("username", uid);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // Quick alert buttons
        setupQuickAlert(R.id.btn_vehicle_breakdown,       "Vehicle Breakdown");
        setupQuickAlert(R.id.btn_customer_not_responding, "Customer Not Responding");
        setupQuickAlert(R.id.btn_incorrect_address,       "Incorrect Address");
        setupQuickAlert(R.id.btn_heavy_traffic,           "Heavy Traffic Delay");

        // Custom alert input
        editCustomAlert = findViewById(R.id.edit_custom_alert);
        btnSendCustom   = findViewById(R.id.btn_send_custom_alert);

        if (editCustomAlert != null && btnSendCustom != null) {
            btnSendCustom.setEnabled(false);
            btnSendCustom.setAlpha(0.5f);

            editCustomAlert.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void afterTextChanged(android.text.Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int st, int b, int c) {
                    boolean hasText = s.toString().trim().length() > 0;
                    btnSendCustom.setEnabled(hasText);
                    btnSendCustom.setAlpha(hasText ? 1f : 0.5f);
                    int activeColor = androidx.core.content.ContextCompat.getColor(EmergencyAlertsActivity.this, R.color.dinex_coral);
                    int inactiveColor = androidx.core.content.ContextCompat.getColor(EmergencyAlertsActivity.this, R.color.dinex_gray_light);
                    btnSendCustom.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(hasText ? activeColor : inactiveColor));
                }
            });

            btnSendCustom.setOnClickListener(v -> {
                String motif = editCustomAlert.getText() != null
                        ? editCustomAlert.getText().toString().trim() : "";
                if (!motif.isEmpty()) {
                    sendAlert(motif);
                    editCustomAlert.setText("");
                }
            });
        }

        // Call dispatch FAB
        View fabCall = findViewById(R.id.fab_call_dispatch);
        if (fabCall != null) {
            fabCall.setOnClickListener(v -> {
                Intent call = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+1234567890"));
                startActivity(call);
            });
        }
    }

    private void setupQuickAlert(int btnId, String motif) {
        View btn = findViewById(btnId);
        if (btn != null) btn.setOnClickListener(v -> sendAlert(motif));
    }

    private void sendAlert(String motif) {
        if (motif == null || motif.isEmpty()) return;

        SupabaseManager.sendUrgenceMessage("—", "—", motif, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "✓ Alert sent: " + motif, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to send alert. Check connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ NEW: Refresh session so the driver can always send alerts even after hours of inactivity
    @Override
    protected void onResume() {
        super.onResume();
        new Thread(() -> {
            SupabaseHelper.restoreSessionIfNeeded();
        }).start();
    }
}