package com.finedine.rms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.finedine.rms.R;

import com.finedine.rms.User;
import java.util.List;



public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {

    private List<User> staffList;
    private OnStaffClickListener listener;

    public interface OnStaffClickListener {
        void onStaffClick(User staff);
        void onEditClick(User staff);
        void onDeleteClick(User staff);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvRole, tvEmail;
        public ImageButton btnEdit, btnDelete;
        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStaffName);
            tvRole = itemView.findViewById(R.id.tvStaffRole);
            tvEmail = itemView.findViewById(R.id.tvStaffEmail);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    public StaffAdapter(List<User> staffList) {
        this.staffList = staffList;
    }

    public StaffAdapter(List<User> staffList, OnStaffClickListener listener) {
        this.staffList = staffList;
        this.listener = listener;
    }


    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder( ViewHolder holder, int position) {
        User staff = staffList.get(position);

        holder.tvName.setText(staff.name);
        holder.tvRole.setText(staff.role);
        holder.tvEmail.setText(staff.email);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                listener.onEditClick(staffList.get(position));
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                listener.onDeleteClick(staffList.get(position));
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                listener.onStaffClick(staffList.get(position));
            }
        });

    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    public void updateStaffList(List<User> newStaffList) {
        staffList = newStaffList;
        notifyDataSetChanged();
    }
}