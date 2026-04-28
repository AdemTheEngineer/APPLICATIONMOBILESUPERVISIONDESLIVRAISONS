package com.example.applicationmobilesupervisiondeslivraisons.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;

import com.example.applicationmobilesupervisiondeslivraisons.R;
import com.example.applicationmobilesupervisiondeslivraisons.models.Livraison;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class LivraisonAdapter extends RecyclerView.Adapter<LivraisonAdapter.LivraisonViewHolder> {

    private List<Livraison> livraisons;
    private final OnLivraisonClickListener listener;

    public interface OnLivraisonClickListener {
        void onLivraisonClick(Livraison livraison);
    }

    public LivraisonAdapter(List<Livraison> livraisons, OnLivraisonClickListener listener) {
        this.livraisons = livraisons;
        this.listener   = listener;
    }

    @NonNull
    @Override
    public LivraisonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_livraison, parent, false);
        return new LivraisonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LivraisonViewHolder holder, int position) {
        holder.bind(livraisons.get(position), position, listener);
    }

    @Override
    public int getItemCount() {
        return livraisons != null ? livraisons.size() : 0;
    }

    public void updateData(List<Livraison> newList) {
        if (this.livraisons == null) {
            this.livraisons = new java.util.ArrayList<>(newList);
            notifyDataSetChanged();
            return;
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return livraisons.size();
            }

            @Override
            public int getNewListSize() {
                return newList != null ? newList.size() : 0;
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Livraison oldItem = livraisons.get(oldItemPosition);
                Livraison newItem = newList.get(newItemPosition);
                if (oldItem.getId() != null && newItem.getId() != null) {
                    return oldItem.getId().equals(newItem.getId());
                }
                return false;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Livraison oldItem = livraisons.get(oldItemPosition);
                Livraison newItem = newList.get(newItemPosition);
                String oldEtat = oldItem.getEtat() != null ? oldItem.getEtat() : "";
                String newEtat = newItem.getEtat() != null ? newItem.getEtat() : "";
                return oldEtat.equals(newEtat);
            }
        });

        this.livraisons = new java.util.ArrayList<>(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    // ─────────────────────────────────────────────────────────────────────────

    static class LivraisonViewHolder extends RecyclerView.ViewHolder {

        private final View             viewStatusBar;
        private final MaterialCardView cardEtat;
        private final MaterialCardView btnArrow;
        private final TextView         textEtat;
        private final TextView         textClient;
        private final TextView         textAddress;   // ✅ ADD
        private final TextView         textLivreur;   // city
        private final TextView         textDate;      // phone
        private final TextView         textMontant;
        private final TextView         textPaymentMode;
        private final TextView         textCommande;

        public LivraisonViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusBar   = itemView.findViewById(R.id.view_status_bar);
            cardEtat        = itemView.findViewById(R.id.card_etat);
            btnArrow        = itemView.findViewById(R.id.btn_arrow);
            textEtat        = itemView.findViewById(R.id.text_etat);
            textClient      = itemView.findViewById(R.id.text_client);
            textAddress     = itemView.findViewById(R.id.text_address); // ✅ ADD
            textLivreur     = itemView.findViewById(R.id.text_livreur);
            textDate        = itemView.findViewById(R.id.text_date);
            textMontant     = itemView.findViewById(R.id.text_montant);
            textPaymentMode = itemView.findViewById(R.id.text_payment_mode);
            textCommande    = itemView.findViewById(R.id.text_commande);
        }

        public void bind(final Livraison livraison, int position,
                         final OnLivraisonClickListener listener) {

            // ── Safe field reads ──────────────────────────────────────────────
            String etat   = livraison.getEtat()            != null ? livraison.getEtat()            : "En attente";
            String client = livraison.getClientNom()       != null ? livraison.getClientNom()       : "Inconnu";
            String address = livraison.getClientAdresse()   != null ? livraison.getClientAdresse()   : "—";
            String ville  = livraison.getClientVille()     != null ? livraison.getClientVille()     : "—";
            String tel    = livraison.getClientTelephone() != null ? livraison.getClientTelephone() : "—";
            String mode   = livraison.getModePaiement()    != null ? livraison.getModePaiement()    : "—";

            // ── Bind text fields ──────────────────────────────────────────────
            textEtat.setText(etat.toUpperCase(java.util.Locale.ROOT));
            textClient.setText(client);
            textAddress.setText(address);
            textLivreur.setText(ville);
            textDate.setText(tel);
            
            if (textCommande != null) {
                String commandeStr = livraison.getIdCommande() != null && !livraison.getIdCommande().isEmpty() 
                                     ? "Commande #" + livraison.getIdCommande() : "Commande #—";
                textCommande.setText(commandeStr);
            }

            if (textMontant != null) {
                textMontant.setText(itemView.getContext().getString(R.string.livraison_montant_format, livraison.getMontant()));
            }
            if (textPaymentMode != null) {
                textPaymentMode.setText(mode.toUpperCase(java.util.Locale.ROOT));
            }

            // ── Status colour logic ───────────────────────────────────────────
            String etatLow = etat.toLowerCase(java.util.Locale.ROOT);
            int barColor, badgeBg, badgeText;

            if (etatLow.contains("cours") || etatLow.contains("transit")) {
                barColor  = 0xFF3B82F6; // lighter blue
                badgeBg   = 0x1A3B82F6;
                badgeText = 0xFF3B82F6;
            } else if (etatLow.contains("attente") || etatLow.contains("pending")) {
                barColor  = 0xFFEAB308; // yellow
                badgeBg   = 0x1AEAB308;
                badgeText = 0xFFEAB308;
            } else if (etatLow.contains("livr") || etatLow.contains("termin")) {
                barColor  = 0xFF10B981; // green
                badgeBg   = 0x1A10B981;
                badgeText = 0xFF10B981;
            } else if (etatLow.contains("échou") || etatLow.contains("annul")
                    || etatLow.contains("fail")) {
                barColor  = 0xFFEF4444; // red
                badgeBg   = 0x1AEF4444;
                badgeText = 0xFFEF4444;
            } else {
                barColor  = 0xFFFF6B6B; // dinex coral default
                badgeBg   = 0x1AFF6B6B;
                badgeText = 0xFFFF6B6B;
            }

            viewStatusBar.setBackgroundColor(barColor);
            cardEtat.setCardBackgroundColor(badgeBg);
            textEtat.setTextColor(badgeText);

            // ── Click listeners ───────────────────────────────────────────────
            // Whole card click
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onLivraisonClick(livraison);
            });

            // ✅ ADD: Arrow button also triggers navigation
            if (btnArrow != null) {
                btnArrow.setOnClickListener(v -> {
                    if (listener != null) listener.onLivraisonClick(livraison);
                });
            }
        }
    }
}