package com.mad.customer;

import android.content.Intent;
import android.graphics.Color;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

class ViewHolderDailyOffer extends RecyclerView.ViewHolder {
    private ImageView dishPhoto;
    private TextView dishName, dishDesc, dishPrice, dishQuantity;
    private DishItem current;
    private int position;
    private View view;


    ViewHolderDailyOffer(View itemView) {
        super(itemView);
        this.view = itemView;
        dishName = itemView.findViewById(R.id.dish_name);
        dishDesc = itemView.findViewById(R.id.dish_desc);
        dishPrice = itemView.findViewById(R.id.dish_price);
        //dishQuantity = itemView.findViewById(R.id.dish_quant);
        dishPhoto = itemView.findViewById(R.id.dish_image);
    }

    void setData(DishItem current, int position) {
        InputStream inputStream = null;

        this.dishName.setText(current.getName());
        this.dishDesc.setText(current.getDesc());
        this.dishPrice.setText(current.getPrice() + " €");
        //this.dishQuantity.setText(String.valueOf(current.getQuantity()));
        if (current.getPhotoUri() != null) {
            try {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                inputStream = new URL(current.getPhotoUri()).openStream();
                if (inputStream != null)
                    Glide.with(itemView.getContext()).load(current.getPhotoUri()).into(dishPhoto);
                else
                    Glide.with(itemView.getContext()).load(R.drawable.hamburger).into(dishPhoto);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.position = position;
        this.current = current;
    }

    public View getView() {
        return view;
    }
}

public class Ordering extends AppCompatActivity {
    private String key;
    private static ArrayList<String> removed = new ArrayList<>();
    ArrayList<String> keys = new ArrayList<String>();
    ArrayList<String> names = new ArrayList<String>();
    ArrayList<String> nums = new ArrayList<String>();
    ArrayList<String> prices = new ArrayList<String>();
    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<DishItem, ViewHolderDailyOffer> mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private static FirebaseRecyclerOptions<DishItem> options =
            new FirebaseRecyclerOptions.Builder<DishItem>()
                    .setQuery(FirebaseDatabase.getInstance().getReference().child("dishes"),
                            DishItem.class).build();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ordering);
        recyclerView = findViewById(R.id.dish_recyclerview);
        //recyclerView.setHasFixedSize(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new FirebaseRecyclerAdapter<DishItem, ViewHolderDailyOffer>(options) {

            @Override
            protected void onBindViewHolder(@NonNull ViewHolderDailyOffer holder, int position, @NonNull DishItem model) {
                holder.setData(model, position);

                if(removed.contains(Integer.toString(position))) {
                    ((EditText)holder.getView().findViewById(R.id.num)).setText("");
                }

                holder.getView().findViewById(R.id.confirm_dish).setOnClickListener(e->{
                    EditText et = holder.getView().findViewById(R.id.num);
                    String a = et.getText().toString();
                    if (a.length()>0){
                        int num = Integer.parseInt(et.getText().toString());
                        if (num>model.getQuantity()){
                            Toast.makeText(holder.getView().getContext(), "Quantità massima disponibile: "+model.getQuantity(), Toast.LENGTH_LONG).show();
                            //((EditText)holder.getView().findViewById(R.id.num)).setText("");
                        }
                        else{
                            String key = getRef(position).getKey();
                            if(!keys.contains(key)) {
                                names.add(model.getName());
                                nums.add(a);
                                prices.add(Float.toString(model.getPrice()));
                                keys.add(key);
                                Toast.makeText(holder.getView().getContext(), "Aggiunto correttamente", Toast.LENGTH_LONG).show();
                                if (keys.size() == 1) {
                                    findViewById(R.id.button2).setBackgroundColor(Color.GREEN); //TODO cambiare tonalità verde
                                }
                                //((EditText)holder.getView().findViewById(R.id.num)).setText("");
                            }
                            else{
                                int pos = keys.indexOf(key);
                                int new_num = Integer.parseInt(nums.get(pos));
                                if (new_num>model.getQuantity()){
                                    Toast.makeText(holder.getView().getContext(), "Quantità massima disponibile: "+model.getQuantity() + "\nGià selezionati: "+Integer.parseInt(nums.get(pos)), Toast.LENGTH_LONG).show();
                                    //((EditText)holder.getView().findViewById(R.id.num)).setText("");
                                }
                                else{
                                    nums.set(pos, Integer.toString(new_num));
                                    Toast.makeText(holder.getView().getContext(), "Aggiornato correttamente", Toast.LENGTH_LONG).show();
                                    //((EditText)holder.getView().findViewById(R.id.num)).setText("");
                                }
                            }
                        }
                    }
                    else{
                        Toast.makeText(holder.getView().getContext(), "Inserire quantità", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @NonNull
            @Override
            public ViewHolderDailyOffer onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
                final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dish_item,parent,false);

                return new ViewHolderDailyOffer(view);
            }
        };
        recyclerView.setAdapter(mAdapter);
        findViewById(R.id.button2).setOnClickListener(w->{
            if (keys.size()==0){
                Toast.makeText(this, "Inserire un piatto.", Toast.LENGTH_LONG);
            }
            else{
                Intent intent = new Intent(this, Confirm.class);
                intent.putExtra("key", key);
                intent.putStringArrayListExtra("keys", (ArrayList<String>) keys);
                intent.putStringArrayListExtra("names", (ArrayList<String>) names);
                intent.putStringArrayListExtra("prices", (ArrayList<String>) prices);
                intent.putStringArrayListExtra("nums", (ArrayList<String>) nums);
                startActivityForResult(intent, 0);

                //keys.clear();
                //names.clear();
                //prices.clear();
                //nums.clear();
                //findViewById(R.id.button2).setBackgroundColor(Color.GRAY);
                //finish();
            }
        });
        getIncomingIntent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data != null && resultCode == 0){
            removed = data.getStringArrayListExtra("removed");

            for(String s : removed){
                keys.remove(Integer.parseInt(s));
                names.remove(Integer.parseInt(s));
                prices.remove(Integer.parseInt(s));
                nums.remove(Integer.parseInt(s));
            }
        }

        if(resultCode == 1){
            finish();
        }
    }

    private void getIncomingIntent(){

        String myName = getIntent().getStringExtra("name");
        String myAddr = getIntent().getStringExtra("addr");
        Long myCell = getIntent().getLongExtra("cell",0);
        String myDescription = getIntent().getStringExtra("description");
        String myEmail = getIntent().getStringExtra("email");
        String myOpening = getIntent().getStringExtra("opening");
        String myImg = getIntent().getStringExtra("img");
        this.key = getIntent().getStringExtra("key");
        setFields(myName, myAddr, myCell, myDescription, myEmail, myOpening, myImg);

    }

    private void setFields (String name, String addr, Long cell, String description, String email, String opening, String img){
        TextView mname = findViewById(R.id.textView5);
        TextView maddr = findViewById(R.id.textView);
        TextView mcell = findViewById(R.id.textView3);
        TextView mdescription = findViewById(R.id.textView6);
        TextView memail = findViewById(R.id.textView2);
        TextView mopening = findViewById(R.id.textView4);
        ImageView mimg = findViewById(R.id.imageView);

        mname.setText(name);
        maddr.setText(addr);
        mcell.setText(cell.toString());
        mdescription.setText(description);
        memail.setText(email);
        mopening.setText(opening);
        Picasso.get()
                .load(img)
                .resize(150, 150)
                .centerCrop()
                .into(mimg);


    }

    @Override
    public void onStart() {
        super.onStart();
        mAdapter.startListening();
    }
    @Override
    public void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
