package com.mad.appetit;

import static com.mad.lib.SharedClass.*;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Reservation.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the  factory method to
 * create an instance of this fragment.
 */

class ViewHolderReservation extends RecyclerView.ViewHolder{
    private TextView name, addr, cell, time;
    private ImageView img;
    private int position;

    public ViewHolderReservation(View itemView){
        super(itemView);

        name = itemView.findViewById(R.id.listview_name);
        addr = itemView.findViewById(R.id.listview_address);
        cell = itemView.findViewById(R.id.listview_cellphone);
        img = itemView.findViewById(R.id.profile_image);
        time = itemView.findViewById(R.id.textView_time);
    }

    void setData(ReservationItem current, int position){
        this.name.setText(current.getName());
        this.addr.setText(current.getAddr());
        this.cell.setText(current.getCell());
        this.time.setText(current.getTime());
        if(current.getImg() != null) {
            Glide.with(itemView.getContext()).load(current.getImg()).into(img);
        }
        this.position = position;
    }
}

public class Reservation extends Fragment {
    private RecyclerView recyclerView,recyclerView_accepted;
    private FirebaseRecyclerAdapter<ReservationItem, ViewHolderReservation> mAdapter;
    private FirebaseRecyclerAdapter<ReservationItem, ViewHolderReservation> mAdapter_accepted;
    private RecyclerAdapterOrdered mAdapter_ordered;
    private RecyclerView.LayoutManager layoutManager;

    private static FirebaseRecyclerOptions<ReservationItem> options =
            new FirebaseRecyclerOptions.Builder<ReservationItem>()
                    .setQuery(FirebaseDatabase.getInstance().getReference(RESERVATION_PATH),
                            ReservationItem.class).build();

    private static FirebaseRecyclerOptions<ReservationItem> options2 =
            new FirebaseRecyclerOptions.Builder<ReservationItem>()
                    .setQuery(FirebaseDatabase.getInstance().getReference(ACCEPTED_ORDER_PATH),
                            ReservationItem.class).build();

    private Reservation.OnFragmentInteractionListener mListener;
    private RecyclerView recyclerView_ordered;

    public Reservation() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reservation, container, false);

        recyclerView = view.findViewById(R.id.ordered_list);
        mAdapter = new FirebaseRecyclerAdapter<ReservationItem, ViewHolderReservation>(options) {
            @NonNull
            @Override
            public ViewHolderReservation onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.reservation_listview, viewGroup, false);

                view.findViewById(R.id.confirm_reservation).setOnClickListener(e -> {
                    String id = ((TextView)view.findViewById(R.id.listview_name)).getText().toString();
                    acceptOrder(id);
                });

                view.findViewById(R.id.delete_reservation).setOnClickListener(h -> {
                    String id = ((TextView)view.findViewById(R.id.listview_name)).getText().toString();
                    removeOrder(id);
                });

                view.findViewById(R.id.open_reservation).setOnClickListener(k -> {
                    String id = ((TextView)view.findViewById(R.id.listview_name)).getText().toString();
                    viewOrder(id, false);
                });

                return new ViewHolderReservation(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ViewHolderReservation holder, int position, @NonNull ReservationItem model) {
                holder.setData(model, position);
            }
        };

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView_accepted = view.findViewById(R.id.reservation_list_accepted);
        mAdapter_accepted = new FirebaseRecyclerAdapter<ReservationItem, ViewHolderReservation>(options2) {
            @Override
            protected void onBindViewHolder(@NonNull ViewHolderReservation holder, int position, @NonNull ReservationItem model) {
                holder.setData(model, position);
            }

            @NonNull
            @Override
            public ViewHolderReservation onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.reservation_listview, parent, false);

                view.findViewById(R.id.confirm_reservation).setVisibility(View.INVISIBLE);
                view.findViewById(R.id.delete_reservation).setVisibility(View.INVISIBLE);

                view.findViewById(R.id.open_reservation).setOnClickListener(k -> {
                    String id = ((TextView)view.findViewById(R.id.listview_name)).getText().toString();
                    viewOrder(id, true);
                });

                return new ViewHolderReservation(view);
            }
        };

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView_accepted.setAdapter(mAdapter_accepted);
        recyclerView_accepted.setLayoutManager(layoutManager);

        return view;
    }

    public void acceptOrder(String id){
        AlertDialog reservationDialog = new AlertDialog.Builder(this.getContext()).create();
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        final View view = inflater.inflate(R.layout.reservation_dialog, null);

        view.findViewById(R.id.button_confirm).setOnClickListener(e -> {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            Query queryDel = database.getReference().child(RESERVATION_PATH).orderByChild("name").equalTo(id);

            queryDel.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        DatabaseReference acceptOrder = database.getReference(ACCEPTED_ORDER_PATH);
                        Map<String, Object> orderMap = new HashMap<>();

                        for (DataSnapshot d : dataSnapshot.getChildren()) {
                            ReservationItem reservationItem = d.getValue(ReservationItem.class);
                            orderMap.put(Objects.requireNonNull(acceptOrder.push().getKey()), reservationItem);
                            d.getRef().removeValue();
                        }

                        acceptOrder.updateChildren(orderMap);

                        // choosing the first available rider which assign the order
                        Query queryRider = database.getReference(RIDERS_PATH);
                        queryRider.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()) {
                                    String keyRider = "", name = "";

                                    for(DataSnapshot d : dataSnapshot.getChildren()){
                                        if((boolean)d.child("available").getValue()){
                                            keyRider = d.getKey();
                                            name = d.child("rider_info").child("name").getValue(String.class);
                                            break;
                                        }
                                    }

                                    DatabaseReference addOrderToRider = database.getReference(RIDERS_PATH + "/" + keyRider + RIDERS_ORDER);
                                    addOrderToRider.updateChildren(orderMap);

                                    Toast.makeText(getContext(), "Order assigned to rider " + name, Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.w("RESERVATION", "Failed to read value.", error.toException());
                }
            });

            mAdapter.notifyDataSetChanged();

            reservationDialog.dismiss();
        });

        view.findViewById(R.id.button_cancel).setOnClickListener(e -> reservationDialog.dismiss());

        reservationDialog.setView(view);
        reservationDialog.setTitle("Confirm Reservation?");

        reservationDialog.show();
    }

    public void removeOrder(String id){
        AlertDialog reservationDialog = new AlertDialog.Builder(this.getContext()).create();
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        final View view = inflater.inflate(R.layout.reservation_dialog, null);

        view.findViewById(R.id.button_confirm).setOnClickListener(e -> {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            Query queryDel = database.getReference().child(RESERVATION_PATH).orderByChild("name").equalTo(id);

            queryDel.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        for(DataSnapshot d : dataSnapshot.getChildren())
                            d.getRef().removeValue();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.w("RESERVATION", "Failed to read value.", error.toException());
                }
            });

            mAdapter.notifyDataSetChanged();

            reservationDialog.dismiss();
        });

        view.findViewById(R.id.button_cancel).setOnClickListener(e -> reservationDialog.dismiss());

        reservationDialog.setView(view);
        reservationDialog.setTitle("Delete Reservation?");

        reservationDialog.show();
    }

    public void viewOrder(String id, boolean order){
        AlertDialog reservationDialog = new AlertDialog.Builder(this.getContext()).create();
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        final View view = inflater.inflate(R.layout.dishes_list_dialog, null);
        final ReservationItem[] i = {new ReservationItem()};
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        Query query;

        if(!order)
            query = database.getReference().child(RESERVATION_PATH).orderByChild("name").equalTo(id);
        else
            query = database.getReference().child(ACCEPTED_ORDER_PATH).orderByChild("name").equalTo(id);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot d : dataSnapshot.getChildren()){
                        i[0] = d.getValue(ReservationItem.class);
                    }

                    recyclerView_ordered = view.findViewById(R.id.ordered_list);
                    mAdapter_ordered = new RecyclerAdapterOrdered(reservationDialog.getContext(), i[0].getOrder());
                    layoutManager = new LinearLayoutManager(reservationDialog.getContext());
                    recyclerView_ordered.setAdapter(mAdapter_ordered);
                    recyclerView_ordered.setLayoutManager(layoutManager);

                    view.findViewById(R.id.back).setOnClickListener(e -> reservationDialog.dismiss());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w("RESERVATION", "Failed to read value.", error.toException());
            }
        });

        reservationDialog.setView(view);
        reservationDialog.setTitle("Order");

        reservationDialog.show();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdapter.startListening();
        mAdapter_accepted.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.stopListening();
        mAdapter_accepted.stopListening();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}