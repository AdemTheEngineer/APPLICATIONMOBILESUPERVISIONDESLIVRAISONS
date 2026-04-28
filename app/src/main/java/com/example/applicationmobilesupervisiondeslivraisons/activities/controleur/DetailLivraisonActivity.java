package com.example.applicationmobilesupervisiondeslivraisons.activities.controleur;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.applicationmobilesupervisiondeslivraisons.R;
import com.example.applicationmobilesupervisiondeslivraisons.models.Livraison;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseManager;

import java.util.Arrays;
import java.util.List;

public class DetailLivraisonActivity extends AppCompatActivity {

    private Livraison livraison;
    private String    livraisonDocId;
    public static final String TAG_ID = "livraison_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_livraison);

        livraisonDocId = getIntent().getStringExtra(TAG_ID);

        if (livraisonDocId == null) {
            Toast.makeText(this, "Livraison introuvable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Toolbar setup
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        loadLivraison();
    }

    // ── Load livraison by ID ──────────────────────────────────────────────────

    private void loadLivraison() {
        // ✅ FIX: Uses the optimized single-item fetch method instead of getAllLivraisons loop
        SupabaseManager.getLivraisonDetail(livraisonDocId, result -> {
            if (result == null) {
                Toast.makeText(this, "Erreur chargement ou livraison introuvable", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            this.livraison = result;
            setupUI();
        });
    }

    // ── Setup UI ──────────────────────────────────────────────────────────────

    private void setupUI() {
        // Safe null checks for all fields
        String nomClient  = livraison.getClientNom()       != null ? livraison.getClientNom()       : "—";
        String tel        = livraison.getClientTelephone() != null ? livraison.getClientTelephone() : "—";
        String adresse    = livraison.getClientAdresse()   != null ? livraison.getClientAdresse()   : "—";
        String ville      = livraison.getClientVille()     != null ? livraison.getClientVille()     : "—";
        String idCmd      = livraison.getIdCommande()      != null ? livraison.getIdCommande()      : "—";
        String modePay    = livraison.getModePaiement()    != null ? livraison.getModePaiement()    : "—";

        ((TextView) findViewById(R.id.detail_nom_client))
                .setText(nomClient);
        ((TextView) findViewById(R.id.detail_tel_client))
                .setText(tel);
        ((TextView) findViewById(R.id.detail_adresse))
                .setText(adresse + ", " + ville);
        ((TextView) findViewById(R.id.detail_id_cmd))
                .setText("Commande #" + idCmd
                        + " (Ordre: " + livraison.getOrdrePassage() + ")");

        // ✅ UI bindings for Articles and Montant
        ((TextView) findViewById(R.id.detail_articles))
                .setText(livraison.getNombreArticles() + " article(s)");
        ((TextView) findViewById(R.id.detail_montant))
                .setText(String.format(java.util.Locale.ROOT,
                        "%.2f DT", livraison.getMontant()));
        ((TextView) findViewById(R.id.detail_paiement))
                .setText("Payment: " + modePay.toUpperCase(java.util.Locale.ROOT));

        // Google Maps button
        findViewById(R.id.btn_google_maps).setOnClickListener(v -> {
            String query = Uri.encode(adresse + ", " + ville);
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=" + query));
            startActivity(intent);
        });

        // Call Client button
        findViewById(R.id.btn_call_client).setOnClickListener(v -> {
            if (tel != null && !tel.equals("—") && !tel.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Numéro de téléphone non disponible", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_detail_modifier).setOnClickListener(v -> showUpdateDialog());
        findViewById(R.id.btn_detail_urgence).setOnClickListener(v -> showEmergencyDialog());
    }

    // ── Update status dialog ──────────────────────────────────────────────────

    private void showUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_update_livraison, null);
        Spinner  spinner      = view.findViewById(R.id.spinner_etat);
        EditText editRemarque = view.findViewById(R.id.edit_remarque);

        List<String> etats = Arrays.asList(
                "En attente", "En cours", "Livrée", "Annulée");
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, etats));

        // Pre-select current state
        int currentIndex = etats.indexOf(livraison.getEtat());
        if (currentIndex >= 0) spinner.setSelection(currentIndex);

        builder.setView(view)
                .setTitle("Mettre à jour l'état")
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String newEtat  = spinner.getSelectedItem().toString();
                    String remarque = editRemarque.getText().toString().trim();

                    if (newEtat.equals("Annulée") && remarque.isEmpty()) {
                        Toast.makeText(this,
                                "Remarque obligatoire pour commande annulée",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SupabaseManager.updateLivraisonEtat(
                            livraisonDocId,
                            newEtat,
                            remarque,
                            success -> {
                                if (Boolean.TRUE.equals(success)) {
                                    livraison.setEtat(newEtat);
                                    livraison.setRemarques(remarque);
                                    Toast.makeText(this,
                                            "État mis à jour",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this,
                                            "Erreur lors de la mise à jour",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ── Emergency dialog ──────────────────────────────────────────────────────

    private void showEmergencyDialog() {
        String[] motifs = {
                "Client ne répond pas",
                "Client refuse la commande",
                "Adresse introuvable",
                "Autre"
        };

        new AlertDialog.Builder(this)
                .setTitle("Signaler une urgence")
                .setItems(motifs, (dialog, which) -> {
                    String msg = "URGENCE: " + motifs[which]
                            + "\nClient: " + livraison.getClientNom()
                            + " (" + livraison.getClientTelephone() + ")"
                            + "\nCommande: " + livraison.getIdCommande();

                    SupabaseManager.sendUrgenceMessage(
                            livraison.getIdCommande(),
                            livraison.getClientTelephone(),
                            msg,
                            success -> {
                                if (Boolean.TRUE.equals(success)) {
                                    Toast.makeText(this,
                                            "Message d'urgence envoyé",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this,
                                            "Erreur envoi",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .show();
    }
}