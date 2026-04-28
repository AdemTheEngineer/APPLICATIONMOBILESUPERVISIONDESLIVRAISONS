package com.example.applicationmobilesupervisiondeslivraisons.activities.livreur;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.applicationmobilesupervisiondeslivraisons.R;
import com.example.applicationmobilesupervisiondeslivraisons.activities.MainActivity;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseHelper;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseManager;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DriverHubActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private TextInputEditText editMessage;
    private DriverMessageAdapter messageAdapter;
    private final List<Map<String, Object>> messages = new ArrayList<>();
    private String myUid;

    private final android.os.Handler pollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            refreshMessages();
            pollHandler.postDelayed(this, 3000);
        }
    };
    private String driverName = "Driver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_hub);

        // Use personnel ID for messaging identity — matches from_user in messages
        myUid = SupabaseManager.getCurrentUid();

        editMessage      = findViewById(R.id.edit_message);
        recyclerMessages = findViewById(R.id.recycler_messages);

        // Load driver name via SessionManager
        TextView tvHeader = findViewById(R.id.tv_hub_driver_name);
        if (tvHeader != null) {
            String driverName = com.example.applicationmobilesupervisiondeslivraisons.supabase.SessionManager.getFullName();
            if (driverName == null || driverName.trim().isEmpty()) {
                driverName = "Driver";
            }
            tvHeader.setText(getString(R.string.driver_hub_header, driverName));
        }

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View tabSchedule = findViewById(R.id.tab_schedule);
        if (tabSchedule != null) {
            tabSchedule.setOnClickListener(v -> {
                Intent i = new Intent(this, LivreurActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                if (myUid != null) i.putExtra("username", myUid);
                startActivity(i);
                overridePendingTransition(0, 0);
            });
        }

        View tabAlerts = findViewById(R.id.tab_alerts);
        if (tabAlerts != null) {
            tabAlerts.setOnClickListener(v -> {
                Intent i = new Intent(this, EmergencyAlertsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                if (myUid != null) i.putExtra("username", myUid);
                startActivity(i);
                overridePendingTransition(0, 0);
            });
        }

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(lm);
        messageAdapter = new DriverMessageAdapter(new ArrayList<>(), myUid);
        recyclerMessages.setAdapter(messageAdapter);

        View btnSend = findViewById(R.id.btn_send);
        if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());

        editMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        if (myUid != null) {
            SupabaseManager.listenDriverMessages(myUid, list -> {
                if (list == null) return;
                messageAdapter.updateData(list);
                if (!list.isEmpty()) recyclerMessages.scrollToPosition(list.size() - 1);
            });
        }
    }

    private void sendMessage() {
        if (editMessage == null) return;
        String text = editMessage.getText() != null
                ? editMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        // ✅ Debug session state before sending
        android.util.Log.d("DriverHub", "=== sendMessage called ===");
        android.util.Log.d("DriverHub", "text        : " + text);
        android.util.Log.d("DriverHub", "myUid       : " + myUid);
        android.util.Log.d("DriverHub", "SessionId   : "
                + com.example.applicationmobilesupervisiondeslivraisons
                .supabase.SessionManager.getPersonnelId());
        android.util.Log.d("DriverHub", "SessionRole : "
                + com.example.applicationmobilesupervisiondeslivraisons
                .supabase.SessionManager.getRole());

        SupabaseManager.sendDriverMessage(text, success -> {
            android.util.Log.d("DriverHub", "sendDriverMessage result: " + success);
            if (success) {
                editMessage.setText("");
                // Refresh messages after send
                refreshMessages();
            } else {
                Toast.makeText(this,
                        "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Add this method to refresh messages after sending
    private void refreshMessages() {
        if (myUid == null) {
            // Session not ready yet — try to recover it
            myUid = SupabaseManager.getCurrentUid();
            if (myUid == null) return; // still null, skip
        }
        SupabaseManager.listenDriverMessages(myUid, list -> {
            if (list != null) {
                int oldSize = messageAdapter.getItemCount();
                messageAdapter.updateData(list);
                if (list.size() > oldSize && !list.isEmpty()) {
                    recyclerMessages.scrollToPosition(list.size() - 1);
                }
            }
        });
    }

    // ── Inner Adapter ─────────────────────────────────────────────────────────

    static class DriverMessageAdapter extends RecyclerView.Adapter<DriverMessageAdapter.MsgViewHolder> {

        private final List<Map<String, Object>> data;
        private final String myUid;

        DriverMessageAdapter(List<Map<String, Object>> data, String myUid) {
            this.data  = new ArrayList<>(data);
            this.myUid = myUid;
        }

        public void updateData(List<Map<String, Object>> newList) {
            androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
                @Override
                public int getOldListSize() { return data.size(); }
                @Override
                public int getNewListSize() { return newList != null ? newList.size() : 0; }
                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    Map<String, Object> oldItem = data.get(oldItemPosition);
                    Map<String, Object> newItem = newList.get(newItemPosition);
                    Object oldId = oldItem.get("id");
                    Object newId = newItem.get("id");
                    return oldId != null && oldId.equals(newId);
                }
                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Map<String, Object> oldItem = data.get(oldItemPosition);
                    Map<String, Object> newItem = newList.get(newItemPosition);
                    Object oldText = oldItem.get("texte");
                    Object newText = newItem.get("texte");
                    return (oldText == null && newText == null) || (oldText != null && oldText.equals(newText));
                }
            });
            this.data.clear();
            if (newList != null) this.data.addAll(newList);
            diffResult.dispatchUpdatesTo(this);
        }

        @NonNull @Override
        public MsgViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MsgViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull MsgViewHolder h, int position) { h.bind(data.get(position), myUid); }
        @Override public int getItemCount() { return data.size(); }

        static class MsgViewHolder extends RecyclerView.ViewHolder {
            private final LinearLayout rootLayout;
            private final TextView textSenderTime;
            private final TextView textBody;

            MsgViewHolder(@NonNull View itemView) {
                super(itemView);
                rootLayout     = itemView.findViewById(R.id.layout_message_root);
                textSenderTime = itemView.findViewById(R.id.text_sender_time);
                textBody       = itemView.findViewById(R.id.text_message_body);
            }

            void bind(Map<String, Object> msg, String myUid) {
                String texte = msg.get("texte") != null ? (String) msg.get("texte") : "";
                String role  = msg.get("role")  != null ? (String) msg.get("role")  : "";
                String from  = msg.get("from")  != null ? (String) msg.get("from")  : "";

                // Timestamp is an ISO-8601 string from Supabase
                String timeStr = "";
                Object ts = msg.get("timestamp");
                if (ts instanceof String) {
                    try {
                        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse((String) ts);
                        if (date != null) timeStr = new SimpleDateFormat("hh:mm a", Locale.US).format(date).toUpperCase(Locale.US);
                    } catch (Exception ignored) {}
                }

                boolean isMyMessage = myUid != null && myUid.equals(from);
                String senderLabel = isMyMessage ? "YOU" : "controleur".equals(role) ? "ADMIN" : "DRIVER";
                textSenderTime.setText(itemView.getContext().getString(R.string.hub_sender_time_format, senderLabel, timeStr));
                textBody.setText(texte);

                if (isMyMessage) {
                    rootLayout.setGravity(Gravity.END);
                    textSenderTime.setGravity(Gravity.END);
                    textBody.setBackground(androidx.appcompat.content.res.AppCompatResources.getDrawable(itemView.getContext(), R.drawable.bg_bubble_admin));
                    textBody.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.dinex_text_primary));
                } else {
                    rootLayout.setGravity(Gravity.START);
                    textSenderTime.setGravity(Gravity.START);
                    textBody.setBackground(androidx.appcompat.content.res.AppCompatResources.getDrawable(itemView.getContext(), R.drawable.bg_bubble_driver));
                    textBody.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.dinex_text_primary));
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(() -> {
            SupabaseHelper.restoreSessionIfNeeded();
            // Re-read uid after session restore in case it was null at onCreate
            String refreshedUid = SupabaseManager.getCurrentUid();
            if (refreshedUid != null) {
                myUid = refreshedUid;
            }
        }).start();
        pollHandler.post(pollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }
}