package com.finedine.rms;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.finedine.rms.R;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;



public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder> {
    private static List<Reservation> reservations;

    public ReservationAdapter(List<Reservation> reservations) {
        ReservationAdapter.reservations = reservations;
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ReservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        Reservation reservation = reservations.get(position);
        holder.bind(reservation);
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    public void updateData(List<Reservation> newReservations) {
        reservations.clear();
        reservations.addAll(newReservations);
        notifyDataSetChanged();
    }

    class ReservationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCustomerName;
        private final TextView tvDateTime;
        private final TextView tvPartySize;
        private final TextView tvStatus;

        public ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPartySize = itemView.findViewById(R.id.tvPartySize);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        public void bind(Reservation reservation) {
            // In a real app, we'd fetch customer name from user table
            tvCustomerName.setText("Reservation #" + reservation.reservation_id);

            String dateTime = formatDate(reservation.reservation_date) + " at " +
                    formatTime(reservation.reservation_time);
            tvDateTime.setText(dateTime);

            tvPartySize.setText(reservation.number_of_guests + " people");
            tvStatus.setText(capitalize(reservation.status));

            // Set status color
            switch (reservation.status) {
                case "confirmed":
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green));
                    break;
                case "pending":
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.yellow));
                    break;
                case "cancelled":
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.red));
                    break;
            }
        }
        public void updateReservations(List<Reservation> newReservations) {
            reservations = newReservations;
            notifyDataSetChanged();
        }

        private String formatDate(String dateStr) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy");
                Date date = inputFormat.parse(dateStr);
                return outputFormat.format(date);
            } catch (Exception e) {
                return dateStr;
            }
        }

        private String formatTime(String timeStr) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm:ss");
                SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a");
                Date date = inputFormat.parse(timeStr);
                return outputFormat.format(date);
            } catch (Exception e) {
                return timeStr;
            }
        }

        private String capitalize(String str) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                if (str == null || str.isEmpty()) {
                    return str;
                }
            }
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
    }
}
