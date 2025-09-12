package com.example.indieeat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<UserModel> userList = new ArrayList<>();
    private Context context;
    private OnUserDeletedListener listener;

    public interface OnUserDeletedListener {
        void onUserDeleted(); // Callback to refresh UI after deletion
    }

    public ReportAdapter(Context context, OnUserDeletedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setUserList(List<UserModel> list) {
        this.userList = list;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, emailText, phoneText, roleText;
        Button deleteBtn;

        public ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.userNameText);
            emailText = itemView.findViewById(R.id.userEmailText);
            phoneText = itemView.findViewById(R.id.userPhoneText);
            roleText = itemView.findViewById(R.id.userRoleText);
            deleteBtn = itemView.findViewById(R.id.deleteUserBtn);
        }
    }

    @NonNull
    @Override
    public ReportAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportAdapter.ViewHolder holder, int position) {
        UserModel user = userList.get(position);

        holder.nameText.setText("Name: " + user.getUsername());
        holder.emailText.setText("Email: " + user.getEmail());
        holder.phoneText.setText("Phone: " + user.getPhone());
        holder.roleText.setText("Role: " + user.getRole());

        // Hide delete button for admins (if listener is null)
        if (listener == null) {
            holder.deleteBtn.setVisibility(View.GONE);
        } else {
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setOnClickListener(v -> {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");

                userRef.orderByChild("email").equalTo(user.getEmail())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot child : snapshot.getChildren()) {
                                    child.getRef().removeValue();
                                }
                                Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onUserDeleted();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(context, "Delete failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }
}
