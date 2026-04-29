package com.example.applicationmobilesupervisiondeslivraisons.activities.livreur;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.applicationmobilesupervisiondeslivraisons.R;
import com.example.applicationmobilesupervisiondeslivraisons.activities.MainActivity;
import com.example.applicationmobilesupervisiondeslivraisons.activities.controleur.DetailLivraisonActivity;
import com.example.applicationmobilesupervisiondeslivraisons.adapters.LivraisonAdapter;
import com.example.applicationmobilesupervisiondeslivraisons.models.Livraison;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LivreurActivity extends AppCompatActivity {

    private LivraisonAdapter adapter;
    private List<Livraison> mesLivraisons = new ArrayList<>();
    private String uid;
    private SwipeRefreshLayout swipeRefreshLayout;

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

        // RecyclerView and SwipeRefresh
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_livreur);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadData);
            // Set some colors for the refresh indicator
            swipeRefreshLayout.setColorSchemeResources(R.color.dinex_coral, R.color.dinex_dark);
        }

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

        // Load data is now handled in onResume()

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

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        if (uid != null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
            SupabaseManager.getLivraisonsParLivreur(uid, list -> {
                if (list != null) {
                    // Sort list: "En attente" first, then by ordrePassage (nearer place)
                    Collections.sort(list, new Comparator<Livraison>() {
                        @Override
                        public int compare(Livraison l1, Livraison l2) {
                            int p1 = getPriority(l1.getEtat());
                            int p2 = getPriority(l2.getEtat());

                            if (p1 != p2) {
                                return Integer.compare(p1, p2);
                            }
                            return Integer.compare(l1.getOrdrePassage(), l2.getOrdrePassage());
                        }

                        private int getPriority(String etat) {
                            if (etat == null) return 2;
                            etat = etat.toLowerCase(Locale.ROOT);
                            if (etat.contains("cours") || etat.contains("transit")) return 1;
                            if (etat.contains("attente") || etat.contains("pending") || etat.isEmpty()) return 2;
                            if (etat.contains("livr") || etat.contains("termin") || etat.contains("deliver")) return 3;
                            if (etat.contains("annul") || etat.contains("echou") || etat.contains("échou") || etat.contains("fail") || etat.contains("refus")) return 4;
                            return 5;
                        }
                    });

                    mesLivraisons = list;
                    if (adapter != null) adapter.updateData(list);
                    updateRemainingCount(list);
                }
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
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