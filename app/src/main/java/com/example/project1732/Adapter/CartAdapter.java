package com.example.project1732.Adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.project1732.Domain.Foods;
import com.example.project1732.Helper.ChangeNumberItemsListener;
import com.example.project1732.Helper.ManagmentCart;
import com.example.project1732.R;

import java.util.ArrayList;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.Viewholder> {
    ArrayList<Foods> listItemSelected;
    private ManagmentCart managmentCart;
    ChangeNumberItemsListener changeNumberItemsListener;

    public CartAdapter(ArrayList<Foods> listItemSelected, Context context, ChangeNumberItemsListener changeNumberItemsListener) {
        this.listItemSelected = listItemSelected;
        this.managmentCart = new ManagmentCart(context);
        this.changeNumberItemsListener = changeNumberItemsListener;
    }

    @NonNull
    @Override
    public CartAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_cart, parent, false);
        return new Viewholder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull CartAdapter.Viewholder holder, int position) {
        float radius = 10f;
        View decorView = ((Activity) holder.itemView.getContext()).getWindow().getDecorView();
        ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
        Drawable windowBackground = decorView.getBackground();

        holder.blurView.setupWith(rootView, new RenderScriptBlur(holder.itemView.getContext())) // or RenderEffectBlur
                .setFrameClearDrawable(windowBackground) // Optional
                .setBlurRadius(radius);
        holder.blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        holder.blurView.setClipToOutline(true);

        Glide.with(holder.itemView.getContext())
                .load(listItemSelected.get(position).getImagePath())
                .transform(new CenterCrop(), new RoundedCorners(30))
                .into(holder.pic);

        holder.title.setText(listItemSelected.get(position).getTitle());
        double price = listItemSelected.get(position).getPrice();
        holder.feeEachItem.setText(String.format("%.3fđ", price)); // Giá của 1 sản phẩm
        holder.totalEachItem.setText(listItemSelected.get(position).getNumberInCart() + " * "
                + String.format("%.3f đ", price)); // Số lượng × giá của 1 sản phẩm

        holder.numEdt.setText(String.valueOf(listItemSelected.get(position).getNumberInCart()));

        holder.plusItem.setOnClickListener(v ->
                managmentCart.plusNumberItem(listItemSelected, position, () -> {
                    changeNumberItemsListener.change();
                    notifyDataSetChanged();
                }));

        holder.minusItem.setOnClickListener(v ->
                managmentCart.minusNumberItem(listItemSelected, position, () -> {
                    changeNumberItemsListener.change();
                    notifyDataSetChanged();
                }));
        holder.numEdt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_NULL) {
                try {
                    int newQuantity = Integer.parseInt(holder.numEdt.getText().toString());
                    int currentPosition = holder.getAdapterPosition(); // Lấy vị trí hiện tại

                    if (newQuantity > 0 && currentPosition != RecyclerView.NO_POSITION) {
                        // Kiểm tra xem số lượng có thay đổi không để tránh gọi cập nhật không cần thiết
                        if (newQuantity != listItemSelected.get(currentPosition).getNumberInCart()) {
                            // Gọi hàm cập nhật trong ManagmentCart (Cần tạo hàm này)
                            managmentCart.updateItemQuantity(listItemSelected, currentPosition, newQuantity); // Giả sử có hàm này
                            changeNumberItemsListener.change(); // Cập nhật tổng tiền
                            // Không cần notifyDataSetChanged() ở đây vì onBindViewHolder sẽ tự cập nhật khi focus thay đổi
                            // Hoặc có thể gọi notifyItemChanged(currentPosition) nếu cần cập nhật ngay lập tức item đó
                        }
                    } else if (newQuantity <= 0 && currentPosition != RecyclerView.NO_POSITION) {
                        // Nếu nhập số lượng <= 0, có thể đặt lại giá trị cũ hoặc xóa item
                        holder.numEdt.setText(String.valueOf(listItemSelected.get(currentPosition).getNumberInCart()));
                        Toast.makeText(holder.itemView.getContext(), "Số lượng phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    // Xử lý nếu người dùng nhập không phải số
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        holder.numEdt.setText(String.valueOf(listItemSelected.get(currentPosition).getNumberInCart()));
                    }
                    Toast.makeText(holder.itemView.getContext(), "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
                }
                // Ẩn bàn phím (tùy chọn)
                //InputMethodManager imm = (InputMethodManager) holder.itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                //imm.hideSoftInputFromWindow(holder.numEdt.getWindowToken(), 0);
                //holder.numEdt.clearFocus(); // Bỏ focus khỏi EditText
                return true; // Đã xử lý sự kiện
            }
            return false; // Chưa xử lý
        });
    }

    @Override
    public int getItemCount() {
        return listItemSelected.size();
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        TextView title, feeEachItem, plusItem, minusItem;
        ImageView pic;
        TextView totalEachItem;
        EditText numEdt;
        BlurView blurView;

        public Viewholder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.titleTxt);
            pic = itemView.findViewById(R.id.pic);
            feeEachItem = itemView.findViewById(R.id.feeEachItem);
            totalEachItem = itemView.findViewById(R.id.totalEachItem);
            plusItem = itemView.findViewById(R.id.plusBtn);
            minusItem = itemView.findViewById(R.id.minusBtn);
            numEdt = itemView.findViewById(R.id.numEdt);
            blurView = itemView.findViewById(R.id.blurView);
        }
    }
}
