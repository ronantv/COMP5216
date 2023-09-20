package comp5216.sydney.edu.au.greenmysterybox;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;

public class MysteryBoxList extends AppCompatActivity {

    private static final int REQUEST_CODE_FILTER = 1;

    private RecyclerView recyclerView;
    private MysteryBoxAdapter adapter;
    private List<MysteryBox> mysteryBoxList;
    private CollectionReference mysteryBoxesRef;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double userLatitude, userLongitude;
    private ArrayList<String> selectedCategories;
    private int maxPriceValue = 9999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mystery_box_list);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mysteryBoxList = new ArrayList<>();
        adapter = new MysteryBoxAdapter(this, mysteryBoxList, userLatitude, userLongitude);
        recyclerView.setAdapter(adapter);

        mysteryBoxesRef = FirebaseFirestore.getInstance().collection("mystery_boxes");
        fetchMysteryBoxes();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                userLatitude = location.getLatitude();
                userLongitude = location.getLongitude();
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_list) {
                return true;
            } else if (itemId == R.id.nav_map) {
                // Start the Map activity
                return true;
            } else if (itemId == R.id.nav_orders) {
                // Start the Orders activity
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Start the Profile activity
                return true;
            } else {
                return false;
            }
        });

        FloatingActionButton fabFilter = findViewById(R.id.fab_filter);
        fabFilter.setOnClickListener(v -> {
            Intent intent = new Intent(MysteryBoxList.this, FilterActivity.class);
            startActivityForResult(intent, REQUEST_CODE_FILTER);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FILTER && resultCode == RESULT_OK) {
            selectedCategories = data.getStringArrayListExtra("selectedCategories");
            maxPriceValue = data.getIntExtra("maxPrice", 9999);
            fetchMysteryBoxes();
        }
    }


    private void fetchMysteryBoxes() {
        Query query = mysteryBoxesRef;
        if (selectedCategories != null && !selectedCategories.isEmpty() && !selectedCategories.contains("All")) {
            query = query.whereIn("category", selectedCategories);
        }
        query = query.whereLessThanOrEqualTo("currentPrice", maxPriceValue);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                mysteryBoxList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    MysteryBox box = document.toObject(MysteryBox.class);
                    mysteryBoxList.add(box);
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(MysteryBoxList.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }
}
