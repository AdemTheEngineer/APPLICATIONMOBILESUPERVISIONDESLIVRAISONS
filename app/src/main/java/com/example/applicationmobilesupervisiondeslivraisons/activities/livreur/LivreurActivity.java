package com.example.applicationmobilesupervisiondeslivraisons.activities.livreur;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.applicationmobilesupervisiondeslivraisons.R;
import com.example.applicationmobilesupervisiondeslivraisons.activities.MainActivity;
import com.example.applicationmobilesupervisiondeslivraisons.activities.controleur.DetailLivraisonActivity;
import com.example.applicationmobilesupervisiondeslivraisons.adapters.LivraisonAdapter;
import com.example.applicationmobilesupervisiondeslivraisons.models.Livraison;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LivreurActivity extends AppCompatActivity {

    private LivraisonAdapter adapter;
    private List<Livraison> mesLivraisons = new ArrayList<>();
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livreur);

        // Get uid from intent; fallback to SupabaseManager
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

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_livreur);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new LivraisonAdapter(mesLivraisons, livraison -> {
                Intent intent = new Intent(this, DetailLivraisonActivity.class);
                intent.putExtra(DetailLivraisonActivity.TAG_ID, livraison.getId());
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        }

        // Bottom nav
        View tabSchedule = findViewById(R.id.tab_schedule);
        if (tabSchedule != null) tabSchedule.setOnClickListener(v -> { /* already here */ });

        View tabAlerts = findViewById(R.id.tab_alerts);
        if (tabAlerts != null) {
            tabAlerts.setOnClickListener(v -> {
                Intent intent = new Intent(LivreurActivity.this, EmergencyAlertsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                if (uid != null) intent.putExtra("username", uid);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        View tabHub = findViewById(R.id.tab_hub);
        if (tabHub != null) {
            tabHub.setOnClickListener(v -> {
                Intent intent = new Intent(LivreurActivity.this, DriverHubActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                if (uid != null) intent.putExtra("username", uid);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // Load deliveries for this driver via SupabaseManager
        if (uid != null) {
            SupabaseManager.getLivraisonsParLivreur(uid, list -> {
                if (list != null) {
                    mesLivraisons = list;
                    if (adapter != null) adapter.updateData(list);
                    updateRemainingCount(list);
                }
            });
        }

        // Logout button
        View btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SupabaseManager.logout();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void updateRemainingCount(List<Livraison> list) {
        TextView tvRemaining = findViewById(R.id.tv_remaining);
        if (tvRemaining == null) return;

        int remaining = 0;
        for (Livraison l : list) {
            String s = l.getEtat() != null ? l.getEtat().toLowerCase(java.util.Locale.ROOT) : "";
            if (!s.contains("livr") && !s.contains("termin") && !s.contains("annul") && !s.contains("échou")) {
                remaining++;
            }
        }
        tvRemaining.setText(getString(R.string.livreur_remaining_format, remaining));
    }
}