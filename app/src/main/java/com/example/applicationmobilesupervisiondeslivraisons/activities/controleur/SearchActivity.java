package com.example.applicationmobilesupervisiondeslivraisons.activities.controleur;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.applicationmobilesupervisiondeslivraisons.R;
import com.example.applicationmobilesupervisiondeslivraisons.adapters.LivraisonAdapter;
import com.example.applicationmobilesupervisiondeslivraisons.models.Livraison;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private List<Livraison> allLivraisons = new ArrayList<>();
    private List<Livraison> filteredList  = new ArrayList<>();

    private LivraisonAdapter  adapter;
    private TextInputEditText editSearch;
    private TextInputEditText editDate;
    private TextView          tvResultsCount;
    private RecyclerView      recyclerSearch;
    private View              layoutEmpty;
    private String            activeFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        editSearch     = findViewById(R.id.edit_search);
        editDate       = findViewById(R.id.edit_date);
        tvResultsCount = findViewById(R.id.tv_results_count);
        recyclerSearch = findViewById(R.id.recycler_search);
        layoutEmpty    = findViewById(R.id.layout_empty);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View tabDashboard = findViewById(R.id.tab_dashboard);
        if (tabDashboard != null) tabDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(this, ControleurActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        View tabHub = findViewById(R.id.tab_hub);
        if (tabHub != null) tabHub.setOnClickListener(v -> {
            Intent intent = new Intent(this, HubActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        recyclerSearch.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LivraisonAdapter(filteredList, livraison -> {
            Intent intent = new Intent(this, DetailLivraisonActivity.class);
            intent.putExtra(DetailLivraisonActivity.TAG_ID, livraison.getId());
            startActivity(intent);
        });
        recyclerSearch.setAdapter(adapter);

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
        };
        editSearch.addTextChangedListener(watcher);
        editDate.addTextChangedListener(watcher);

        editDate.setFocusable(false);
        editDate.setClickable(true);
        editDate.setOnClickListener(v -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        // month is 0-based
                        String selectedDate = String.format(java.util.Locale.ROOT, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        editDate.setText(selectedDate);
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
            );
            dialog.setButton(android.content.DialogInterface.BUTTON_NEUTRAL, "Effacer", (d, w) -> editDate.setText(""));
            dialog.show();
        });

        View btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) btnFilter.setOnClickListener(v -> showFilterDialog());

        // Load all livraisons from Supabase
        SupabaseManager.getAllLivraisons(list -> {
            if (list != null) {
                allLivraisons = list;
                applyFilters();
            }
        });
    }

    private void applyFilters() {
        String query     = editSearch.getText() != null ? editSearch.getText().toString().trim().toLowerCase(java.util.Locale.ROOT) : "";
        String dateQuery = editDate.getText()   != null ? editDate.getText().toString().trim() : "";

        List<Livraison> result = new ArrayList<>();

        for (Livraison l : allLivraisons) {
            if (activeFilter != null) {
                String etat = l.getEtat() != null ? l.getEtat().toLowerCase(java.util.Locale.ROOT) : "";
                if (!etat.contains(activeFilter)) continue;
            }
            if (!dateQuery.isEmpty()) {
                String dateLiv = l.getDateLivraison() != null ? l.getDateLivraison().toLowerCase(java.util.Locale.ROOT) : "";
                if (!dateLiv.contains(dateQuery.toLowerCase(java.util.Locale.ROOT))) continue;
            }
            if (!query.isEmpty()) {
                String nom    = l.getClientNom()   != null ? l.getClientNom().toLowerCase(java.util.Locale.ROOT)   : "";
                String id     = l.getIdCommande()  != null ? l.getIdCommande().toLowerCase(java.util.Locale.ROOT)  : "";
                String city   = l.getClientVille() != null ? l.getClientVille().toLowerCase(java.util.Locale.ROOT) : "";
                String driver = l.getLivreurNom()  != null ? l.getLivreurNom().toLowerCase(java.util.Locale.ROOT)  : "";
                if (!nom.contains(query) && !id.contains(query) && !city.contains(query) && !driver.contains(query)) continue;
            }
            result.add(l);
        }

        filteredList = result;
        adapter.updateData(new ArrayList<>(filteredList));
        updateResultsLabel(query, dateQuery);
        toggleEmptyState();
    }

    private void updateResultsLabel(String query, String dateQuery) {
        if (tvResultsCount == null) return;
        int count = filteredList.size();
        if (query.isEmpty() && dateQuery.isEmpty() && activeFilter == null) {
            tvResultsCount.setText(getString(R.string.search_all_deliveries_format, count));
        } else {
            tvResultsCount.setText(getString(R.string.search_results_found, count, (count != 1 ? "s" : "")));
        }
    }

    private void toggleEmptyState() {
        boolean isEmpty = filteredList.isEmpty();
        recyclerSearch.setVisibility(isEmpty ? View.GONE  : View.VISIBLE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showFilterDialog() {
        String[] options = {"All", "Pending", "In Transit", "Delivered", "Failed/Cancelled"};
        String[] keys    = {null, "attente", "cours", "livr", "annul"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Filter by status")
                .setItems(options, (dialog, which) -> { activeFilter = keys[which]; applyFilters(); })
                .show();
    }
}