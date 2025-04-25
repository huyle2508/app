package com.example.project1732.Adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project1732.Domain.Order;
import com.example.project1732.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;


public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private ArrayList<Order> orderList;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public OrderAdapter(ArrayList<Order> orderList, Context context) {
        this.orderList = orderList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_order, parent, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);

        holder.orderIdTxt.setText("Mã ĐH: " + order.getOrderId().substring(0, 8) + "..."); // Hiển thị ngắn gọn
        holder.orderDateTxt.setText("Ngày đặt: " + dateFormat.format(new Date(order.getTimestamp())));
        holder.orderTotalTxt.setText("Tổng tiền: $" + String.format(Locale.US, "%.2f", order.getTotalAmount()));
        holder.orderStatusTxt.setText(order.getStatus());

        // --- BlurView Setup ---
        float radius = 10f;
        if (context instanceof Activity) {
            View decorView = ((Activity) context).getWindow().getDecorView();
            ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
            Drawable windowBackground = decorView.getBackground();
            if (rootView != null && windowBackground != null) {
                holder.blurView.setupWith(rootView, new RenderScriptBlur(context))
                        .setFrameClearDrawable(windowBackground)
                        .setBlurRadius(radius);
                holder.blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                holder.blurView.setClipToOutline(true);
            }
        }
        // --- End BlurView Setup ---

        // Thêm OnClickListener nếu muốn xem chi tiết đơn hàng
        holder.itemView.setOnClickListener(v -> {
            // Intent intent = new Intent(context, OrderDetailActivity.class);
            // intent.putExtra("order", order); // Order cần implements Serializable or Parcelable
            // context.startActivity(intent);
            Toast.makeText(context, "Xem chi tiết đơn hàng: " + order.getOrderId(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdTxt, orderDateTxt, orderTotalTxt, orderStatusTxt;
        BlurView blurView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdTxt = itemView.findViewById(R.id.orderIdTxt);
            orderDateTxt = itemView.findViewById(R.id.orderDateTxt);
            orderTotalTxt = itemView.findViewById(R.id.orderTotalTxt);
            orderStatusTxt = itemView.findViewById(R.id.orderStatusTxt);
            blurView = itemView.findViewById(R.id.blurView);
        }
    }
}