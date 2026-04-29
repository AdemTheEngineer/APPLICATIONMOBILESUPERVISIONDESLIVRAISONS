package com.example.applicationmobilesupervisiondeslivraisons.activities.controleur;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.applicationmobilesupervisiondeslivraisons.R;
import com.example.applicationmobilesupervisiondeslivraisons.activities.MainActivity;
import com.example.applicationmobilesupervisiondeslivraisons.adapters.LivraisonAdapter;
import com.example.applicationmobilesupervisiondeslivraisons.models.Livraison;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseManager;
import com.example.applicationmobilesupervisiondeslivraisons.views.DonutChartView;

import java.util.ArrayList;
import java.util.List;

public class ControleurActivity extends AppCompatActivity {

    private LivraisonAdapter adapter;
    private List<Livraison>  currentList = new ArrayList<>();
    private TextView         tvPending, tvTransit, tvDelivered, tvFailed;
    private DonutChartView   donutChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controleur);

        // ── Stat TextViews ────────────────────────────────────────────────────
        tvPending   = findViewById(R.id.tv_pending_count);
        tvTransit   = findViewById(R.id.tv_transit_count);
        tvDelivered = findViewById(R.id.tv_delivered_count);
        tvFailed    = findViewById(R.id.tv_failed_count);
        donutChart  = findViewById(R.id.donut_chart);

        TextView tvTitle = findViewById(R.id.tv_dashboard_title);
        if (tvTitle != null) {
            String fullName = com.example.applicationmobilesupervisiondeslivraisons.supabase.SessionManager.getFullName();
            if (fullName != null && !fullName.trim().isEmpty()) {
                tvTitle.setText("Dashboard - " + fullName);
            }
        }

        // ── Logout button ─────────────────────────────────────────────────────
        ImageButton btnExit = findViewById(R.id.btn_exit);
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> {
                // ✅ FIX: logout with callback
                SupabaseManager.logout(success -> {
                    Intent i = new Intent(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                });
            });
        }

        // ── Bottom nav ────────────────────────────────────────────────────────
        findViewById(R.id.tab_dashboard).setOnClickListener(v -> { /* already here */ });

        findViewById(R.id.tab_search).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.tab_hub).setOnClickListener(v -> {
            Intent intent = new Intent(this, HubActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // ── RecyclerView ──────────────────────────────────────────────────────
        RecyclerView recyclerView = findViewById(R.id.recycler_deliveries);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LivraisonAdapter(currentList, livraison -> {
            Intent intent = new Intent(this, DetailLivraisonActivity.class);
            intent.putExtra(DetailLivraisonActivity.TAG_ID, livraison.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // ── Load livraisons ───────────────────────────────────────────────────
        loadLivraisons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Refresh list every time we come back to this screen
        loadLivraisons();
    }

    private void loadLivraisons() {
        SupabaseManager.getAllLivraisons(list -> {
            if (list != null) {
                currentList = list;
                adapter.updateData(list);
                updateStats(list);
            }
        });
    }

    private void updateStats(List<Livraison> list) {
        int pending = 0, transit = 0, delivered = 0, failed = 0;
        for (Livraison l : list) {
            String etat = l.getEtat() != null
                    ? l.getEtat().toLowerCase(java.util.Locale.ROOT) : "";

            // ── Match the actual French status values used in the app:
            //    "En attente"  → pending   (contains "attente")
            //    "En cours"    → transit   (contains "cours")
            //    "Livrée"      → delivered (contains "livr")
            //    "Annulée"     → failed    (contains "annul")
            if      (etat.contains("cours")   || etat.contains("transit"))  transit++;
            else if (etat.contains("livr")    || etat.contains("termin")
                    || etat.contains("deliver"))                            delivered++;
            else if (etat.contains("annul")   || etat.contains("echou")
                    || etat.contains("échou")  || etat.contains("fail")
                    || etat.contains("refus"))                              failed++;
            else if (etat.contains("attente") || etat.contains("pending")
                    || etat.isEmpty())                                      pending++;
            else                                                            pending++;
        }

        // ── Update stat cards ────────────────────────────────────────────────
        if (tvPending   != null) tvPending.setText(String.valueOf(pending));
        if (tvTransit   != null) tvTransit.setText(String.valueOf(transit));
        if (tvDelivered != null) tvDelivered.setText(String.valueOf(delivered));
        if (tvFailed    != null) tvFailed.setText(String.valueOf(failed));

        // ── Update donut chart with real proportions ─────────────────────────
        if (donutChart != null) {
            donutChart.setData(pending, transit, delivered, failed);
        }
    }
}