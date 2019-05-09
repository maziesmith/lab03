package com.mad.appetit;

import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.net.Uri;
import android.support.design.widget.BottomNavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.view.View.VISIBLE;
import static android.view.View.INVISIBLE;
import static com.mad.lib.SharedClass.RESERVATION_PATH;

public class FragmentManager extends AppCompatActivity implements DailyOffer.OnFragmentInteractionListener,
        Reservation.OnFragmentInteractionListener, Home.OnFragmentInteractionListener,
        Profile.OnFragmentInteractionListener{

    private View notificationBadge;
    private BottomNavigationView navigation;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item ->  {
        switch (item.getItemId()) {
            case R.id.navigation_home:
                checkBadge();
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Home()).commit();
                return true;
            case R.id.navigation_profile:
                checkBadge();
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Profile()).commit();
                return true;
            case R.id.navigation_dailyoffer:
                checkBadge();
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new DailyOffer()).commit();
                return true;
            case R.id.navigation_reservation:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Reservation()).commit();
                hideBadgeView();
                return true;
        }
        return false;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_manager);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(RESERVATION_PATH);
        Map<String, Object> dishMap = new HashMap<>();
        ArrayList<String> order = new ArrayList<>();
        order.add("ciao ciao dio cane");
        ReservationItem reservationItem = new ReservationItem("Federico",
                "Via Vinadio 14", "3496998347", null, "19:00", order);
        dishMap.put(Objects.requireNonNull(myRef.push().getKey()), reservationItem);
        myRef.updateChildren(dishMap);

        checkBadge();

        navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new Home()).commit();

            addBadgeView();
            hideBadgeView();
        }
    }

    private void checkBadge(){
        Query query = FirebaseDatabase.getInstance().getReference(RESERVATION_PATH);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    long count = dataSnapshot.getChildrenCount();

                    ((TextView)notificationBadge.findViewById(R.id.count_badge)).setText(Long.toString(count));
                    refreshBadgeView();
                }
                else{
                    hideBadgeView();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addBadgeView() {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) navigation.getChildAt(0);
        BottomNavigationItemView itemView = (BottomNavigationItemView) menuView.getChildAt(3);

        notificationBadge = LayoutInflater.from(this).inflate(R.layout.notification_badge, menuView, false);

        itemView.addView(notificationBadge);
    }

    private void refreshBadgeView() {
        notificationBadge.setVisibility(VISIBLE);
    }

    private void hideBadgeView(){
        notificationBadge.setVisibility(INVISIBLE);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}