package com.mad.customer;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

public class RestaurantViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

    TextView name;
    TextView addr;
    TextView cuisine;
    TextView opening;
    ImageView img;
    int position;
    RestaurantItem current;
    private String key;



    public RestaurantViewHolder(View itemView){
        super(itemView);
        name = itemView.findViewById(R.id.listview_name);
        addr = itemView.findViewById(R.id.listview_address);
        cuisine = itemView.findViewById(R.id.listview_cuisine);
        img = itemView.findViewById(R.id.restaurant_image);
        opening = itemView.findViewById(R.id.listview_opening);


        itemView.setOnClickListener(this);
    }
    void setData (RestaurantItem current, int position, String key){
        this.name.setText(current.getName());
        this.addr.setText(current.getAddr());
        this.cuisine.setText(current.getCuisine());
        this.opening.setText(current.getOpening());
        Picasso.get()
                .load(current.getImg())
                .resize(150, 150)
                .centerCrop()
                .into(this.img);
        this.position = position;
        this.current = current;
        this.key = key;

    }
    @Override
    public void onClick(View view) {
        //Toast.makeText(view.getContext(), "Item"+getAdapterPosition(), Toast.LENGTH_LONG).show();

        Toast.makeText(view.getContext(), this.key, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(view.getContext(), Ordering.class);
        intent.putExtra("name", current.getName());
        intent.putExtra("addr", current.getAddr());
        intent.putExtra("cell", current.getCell());
        intent.putExtra("description", current.getDescription());
        intent.putExtra("email", current.getEmail());
        intent.putExtra("opening", current.getOpening());
        intent.putExtra("img", current.getImg());
        intent.putExtra("key", this.key);
        view.getContext().startActivity(intent);
    }
}