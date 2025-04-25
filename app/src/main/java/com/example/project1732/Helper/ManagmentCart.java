package com.example.project1732.Helper;

import android.content.Context;
import android.widget.Toast;

import com.example.project1732.Domain.Foods;

import java.util.ArrayList;


public class ManagmentCart {
    private Context context;
    private TinyDB tinyDB;

    public ManagmentCart(Context context) {
        this.context = context;
        this.tinyDB=new TinyDB(context);
    }

    public void insertFood(Foods item) {
        ArrayList<Foods> listpop = getListCart();
        boolean existAlready = false;
        int n = 0;
        for (int i = 0; i < listpop.size(); i++) {
            // Sửa lỗi tiềm ẩn: Kiểm tra null trước khi gọi equals
            if (listpop.get(i) != null && listpop.get(i).getTitle() != null &&
                    listpop.get(i).getTitle().equals(item.getTitle())) {
                existAlready = true;
                n = i;
                break;
            }
        }
        if(existAlready){
            listpop.get(n).setNumberInCart(item.getNumberInCart());
        }else{
            listpop.add(item);
        }
        tinyDB.putListObject("CartList",listpop);
        Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
    }

    public ArrayList<Foods> getListCart() {
        // Trả về danh sách rỗng nếu không có gì trong SharedPreferences thay vì null
        ArrayList<Foods> list = tinyDB.getListObject("CartList");
        return (list != null) ? list : new ArrayList<>();
    }

    public Double getTotalFee(){
        ArrayList<Foods> listItem=getListCart();
        double fee=0;
        for (int i = 0; i < listItem.size(); i++) {
            // Kiểm tra null trước khi tính toán
            if (listItem.get(i) != null) {
                fee = fee + (listItem.get(i).getPrice() * listItem.get(i).getNumberInCart());
            }
        }
        return fee;
    }
    public void updateItemQuantity(ArrayList<Foods> listItem, int position, int newQuantity) {
        if (position >= 0 && position < listItem.size() && listItem.get(position) != null && newQuantity > 0) {
            listItem.get(position).setNumberInCart(newQuantity);
            tinyDB.putListObject("CartList", listItem);
            // Không cần gọi changeNumberItemsListener ở đây, CartAdapter sẽ gọi
        }
    }
    public void minusNumberItem(ArrayList<Foods> listItem,int position,ChangeNumberItemsListener changeNumberItemsListener){
        if (position < 0 || position >= listItem.size() || listItem.get(position) == null) {
            return; // Kiểm tra hợp lệ
        }
        if(listItem.get(position).getNumberInCart()==1){
            listItem.remove(position);
        }else{
            listItem.get(position).setNumberInCart(listItem.get(position).getNumberInCart()-1);
        }
        tinyDB.putListObject("CartList",listItem);
        changeNumberItemsListener.change();
    }
    public void plusNumberItem(ArrayList<Foods> listItem, int position, ChangeNumberItemsListener changeNumberItemsListener){
        if (position < 0 || position >= listItem.size() || listItem.get(position) == null) {
            return; // Kiểm tra hợp lệ
        }
        listItem.get(position).setNumberInCart(listItem.get(position).getNumberInCart()+1);
        tinyDB.putListObject("CartList",listItem);
        changeNumberItemsListener.change();
    }

    // Hàm xóa giỏ hàng
    public void clearCart() {
        // Đảm bảo tinyDB đã được khởi tạo
        if (tinyDB == null) {
            tinyDB = new TinyDB(context);
        }
        // Gọi phương thức remove của TinyDB để xóa dữ liệu giỏ hàng khỏi SharedPreferences
        tinyDB.remove("CartList");
    }
}