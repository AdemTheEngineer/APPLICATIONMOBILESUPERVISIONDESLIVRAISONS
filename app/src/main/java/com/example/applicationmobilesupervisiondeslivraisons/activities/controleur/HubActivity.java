package com.example.applicationmobilesupervisiondeslivraisons.activities.controleur;

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
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseHelper;
import com.example.applicationmobilesupervisiondeslivraisons.supabase.SupabaseManager;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HubActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private TextInputEditText editMessage;
    private MessageAdapter messageAdapter;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);

        myUid = SupabaseManager.getCurrentUid(); // personnel ID — matches from_user in messages
        editMessage = findViewById(R.id.edit_message);
        recyclerMessages = findViewById(R.id.recycler_messages);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View tabDashboard = findViewById(R.id.tab_dashboard);
        if (tabDashboard != null) {
            tabDashboard.setOnClickListener(v -> {
                Intent intent = new Intent(this, ControleurActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }
        View tabSearch = findViewById(R.id.tab_search);
        if (tabSearch != null) {
            tabSearch.setOnClickListener(v -> {
                Intent intent = new Intent(this, SearchActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(lm);
        messageAdapter = new MessageAdapter(new ArrayList<>(), myUid);
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

        loadMessages();
    }

    private void sendMessage() {
        if (editMessage == null) return;
        String text = editMessage.getText() != null
                ? editMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        // ✅ Debug session state before sending
        android.util.Log.d("HubActivity", "=== sendMessage called ===");
        android.util.Log.d("HubActivity", "text        : " + text);
        android.util.Log.d("HubActivity", "myUid       : " + myUid);
        android.util.Log.d("HubActivity", "SessionId   : "
                + com.example.applicationmobilesupervisiondeslivraisons
                .supabase.SessionManager.getPersonnelId());
        android.util.Log.d("HubActivity", "SessionRole : "
                + com.example.applicationmobilesupervisiondeslivraisons
                .supabase.SessionManager.getRole());

        SupabaseManager.sendBroadcastMessage(text, success -> {
            android.util.Log.d("HubActivity", "sendBroadcastMessage result: " + success);
            if (success) {
                editMessage.setText("");
                // ✅ Refresh messages after send
                refreshMessages();
            } else {
                Toast.makeText(this,
                        "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Add refresh method
    private void refreshMessages() {
        SupabaseManager.listenAllMessages(list -> {
            if (list != null) {
                int oldSize = messageAdapter.getItemCount();
                messageAdapter.updateData(list);
                if (list.size() > oldSize && !list.isEmpty()) {
                    recyclerMessages.scrollToPosition(list.size() - 1);
                }
            }
        });
    }

    private void loadMessages() {
        SupabaseManager.listenAllMessages(list -> {
            if (list == null) return;
            messageAdapter.updateData(list);
            if (!list.isEmpty()) recyclerMessages.scrollToPosition(list.size() - 1);
        });
    }

    static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

        private final List<Map<String, Object>> data;
        private final String myUid;

        MessageAdapter(List<Map<String, Object>> data, String myUid) {
            this.data = new ArrayList<>(data);
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
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            holder.bind(data.get(position), myUid);
        }

        @Override public int getItemCount() { return data.size(); }

        static class MessageViewHolder extends RecyclerView.ViewHolder {
            private final LinearLayout rootLayout;
            private final TextView textSenderTime;
            private final TextView textBody;

            MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                rootLayout = itemView.findViewById(R.id.layout_message_root);
                textSenderTime = itemView.findViewById(R.id.text_sender_time);
                textBody = itemView.findViewById(R.id.text_message_body);
            }

            void bind(Map<String, Object> msg, String myUid) {
                String texte = msg.get("texte") != null ? (String) msg.get("texte") : "";
                String role  = msg.get("role")  != null ? (String) msg.get("role")  : "";
                String from  = msg.get("from")  != null ? (String) msg.get("from")  : "";
                String type  = msg.get("type")  != null ? (String) msg.get("type")  : "";

                if ("urgence".equals(type)) {
                    String motif = msg.get("motif") != null ? (String) msg.get("motif") : texte;
                    texte = "🚨 " + motif;
                }

                // Timestamp is an ISO-8601 string from Supabase
                String timeStr = "—";
                Object ts = msg.get("timestamp");
                if (ts instanceof String) {
                    try {
                        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse((String) ts);
                        if (date != null) timeStr = new SimpleDateFormat("hh:mm a", Locale.US).format(date).toUpperCase(Locale.US);
                    } catch (Exception ignored) {}
                }

                boolean isMyMessage = myUid != null && myUid.equals(from);
                String senderLabel = isMyMessage ? "YOU" : "controleur".equals(role) ? "ADMIN" : "urgence".equals(type) ? "DRIVER 🚨" : "DRIVER";

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

        // ✅ NEW: Restore the Supabase session token so polling doesn't fail with 401 Unauthorized
        new Thread(() -> {
            SupabaseHelper.restoreSessionIfNeeded();
        }).start();

        pollHandler.post(pollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }
}