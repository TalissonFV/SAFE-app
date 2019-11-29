package com.example.safeapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.safeapp.models.ClusterMarker;
import com.example.safeapp.models.Events;
import com.example.safeapp.util.MyClusterManagerRenderer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        NavigationView.OnNavigationItemSelectedListener, GoogleMap.OnInfoWindowClickListener{

    private GoogleMap mMap;
    private FusedLocationProviderClient client;
    private ClusterManager<ClusterMarker> mClusterManager;
    private MyClusterManagerRenderer mClusterManagerRenderer;
    private ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();
    private List<Events> events = new ArrayList<>();
    private DrawerLayout drawer;

    Button registerEvent;
    TextView userName;
    TextView userEmail;

    NavigationView navigationView;

    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermission();
        setContentView(R.layout.activity_maps);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        Toolbar toolbar = findViewById(R.id.toolbar);

        registerEvent = findViewById(R.id.btnRegisterEvent);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        userName = headerView.findViewById(R.id.tvUserName);
        userEmail = headerView.findViewById(R.id.tvUserEmail);
        userName.setText(firebaseUser.getDisplayName());
        userEmail.setText(firebaseUser.getEmail());
        navigationView.setNavigationItemSelectedListener(this);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        client = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        client.getLastLocation().addOnSuccessListener(MapsActivity.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    System.out.println(location.getLatitude());
                    System.out.println(location.getLongitude());
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(currentLocation).title("Usuário").snippet("Localização atual"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

                }
            }
        });
        registerEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, EventRegister.class);
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("Result OK");
        if (requestCode == 0) {
            loadEvents();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_logout:
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                break;
            case R.id.nav_emergencies:
                Intent emergency = new Intent(MapsActivity.this, EmergencyNumbersActivity.class);
                startActivity(emergency);

                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.drawer_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        loadEvents();
        mMap.setOnInfoWindowClickListener(this);
    }

    private void requestPermission() {
        System.out.println("Pedindo permissão para localização.");
        Dexter.withActivity(MapsActivity.this)
                .withPermission(ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        System.out.println("Permissão garantida.");
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                            builder.setTitle("Permissão negada!")
                                    .setMessage("Permissão a localização do dispositivo foi negada permanentemente!")
                                    .setNegativeButton("Cancelar", null)
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent();
                                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            intent.setData(Uri.fromParts("package", getPackageName(), null));
                                        }
                                    })
                                    .show();
                        }
                        else {
                            Toast.makeText(MapsActivity.this, "Permissão negada!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .check();
    }

    private void loadEvents () {
        new Thread(new Runnable() {
            public void run() {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                        .build();
                db.setFirestoreSettings(settings);
                final DocumentReference docRef = db.collection("events").document();
                db.collection("events").whereEqualTo("visible", true).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult() != null) {
                                List<Events> data = task.getResult().toObjects(Events.class);
                                events.addAll(data);
                                addMapMarkers();
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private void addMapMarkers(){
        if (mMap != null){
            if (mClusterManager == null) {
                mClusterManager = new ClusterManager<>(getApplicationContext(), mMap);
                mClusterManager.setOnClusterItemClickListener(mClusterItemClickListener);
                mMap.setOnMarkerClickListener(mClusterManager);
            }

            if (mClusterManagerRenderer == null) {
                mClusterManagerRenderer = new MyClusterManagerRenderer(
                        this,
                        mMap,
                        mClusterManager
                );
                mClusterManager.setRenderer(mClusterManagerRenderer);
            }
            int avatar;
            for (final Events event: events) {
                switch (event.getType()) {
                    case "Roubo":
                        avatar = R.drawable.ic_thief;
                        break;
                    case "Furto":
                        avatar = R.drawable.ic_thief;
                        break;
                    case "Alagamento":
                        avatar = R.drawable.ic_flood;
                        break;
                    case "Rua fechada":
                        avatar = R.drawable.ic_closed_street;
                        break;
                    case "Descarte irregular de lixo":
                        avatar = R.drawable.ic_trash;
                        break;
                    default:
                        avatar = R.drawable.ic_settings;
                        break;
                }
                try {
                    ClusterMarker newClusterMarker = new ClusterMarker(
                            new LatLng(event.getPosition().getLatitude(),event.getPosition().getLongitude()),
                            event.getType(),
                            event.getDescription(),
                            avatar,
                            event.getUser(),
                            event
                    );
                    mClusterManager.addItem(newClusterMarker);
                    mClusterMarkers.add(newClusterMarker);

                }catch (NullPointerException e) {

                }
            }
            mClusterManager.cluster();
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    public ClusterManager.OnClusterItemClickListener<ClusterMarker> mClusterItemClickListener = new ClusterManager.OnClusterItemClickListener<ClusterMarker>() {

        @Override
        public boolean onClusterItemClick(final ClusterMarker item) {
            final Events data = item.getTag();

            final AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
            View mView = getLayoutInflater().inflate(R.layout.dialog_event_info, null);

            final TextView type = mView.findViewById(R.id.tvType);
            final TextView description = mView.findViewById(R.id.tvDescription);
            final TextView date = mView.findViewById(R.id.tvDate);
            final TextView userName = mView.findViewById(R.id.tvUserName);
            final TextView score = mView.findViewById(R.id.tvScore);
            final Button btnDeletar = mView.findViewById(R.id.btnDeleteEvent);
            final ImageView btnCloseDialog = mView.findViewById(R.id.btnCloseDialog);
            final ImageView btnUpVote = mView.findViewById(R.id.btnUpVote);
            final ImageView btnDownVote = mView.findViewById(R.id.btnDownVote);

            type.setText(data.getType());
            description.setText(data.getDescription());
            SimpleDateFormat dateFormatprev = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
            date.setText(dateFormatprev.format(data.getEventDate()));
            userName.setText(data.getUserName());
            score.setText("1");

            mBuilder.setView(mView);
            final Map<String, String> addUserToArrayMap = new HashMap<>();
            final Map<String, String> votes = new HashMap<>();
            final AlertDialog dialog = mBuilder.create();
            final FirebaseFirestore db = FirebaseFirestore.getInstance();
            final DocumentReference docRef = db.collection("events").document(data.getId());
            docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> eventSnap = snapshot.getData();
                        score.setText(eventSnap.get("score").toString());
                        for (Map.Entry<String, Object> entry : eventSnap.entrySet()) {
                            if (entry.getKey().equals("votes")) {
                                List<HashMap> list = (List<HashMap>) entry.getValue();
                                for (HashMap item : list) {
                                    if (item.get(firebaseUser.getUid()).equals("up")) {
                                        btnUpVote.setEnabled(false);
                                        btnUpVote.setColorFilter(Color.GREEN);
                                        btnDownVote.setEnabled(false);
                                        btnDownVote.setColorFilter(Color.GRAY);
                                    }
                                    if (item.get(firebaseUser.getUid()).equals("down")) {
                                        btnUpVote.setEnabled(false);
                                        btnUpVote.setColorFilter(Color.GRAY);
                                        btnDownVote.setEnabled(false);
                                        btnDownVote.setColorFilter(Color.RED);
                                    }
                                }
                            }
                        }
                    }
                }
            });


            btnCloseDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            btnUpVote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DocumentReference shardRef = db.collection("events").document(data.getId());
                    shardRef.update("score", FieldValue.increment(1));
                    addUserToArrayMap.put(firebaseUser.getUid(), "up");
                    shardRef.update("votes", FieldValue.arrayUnion(addUserToArrayMap));
                }
            });

            btnDownVote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (score.getText().equals("-9")) {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                                .build();
                        db.setFirestoreSettings(settings);
                        db.collection("events").document(data.getId()).update("visible", false);
                        mClusterManager.removeItem(item);
                        mClusterManager.cluster();
                        dialog.dismiss();
                    } else {
                        DocumentReference shardRef = db.collection("events").document(data.getId());
                        shardRef.update("score", FieldValue.increment(-1));
                        addUserToArrayMap.put(firebaseUser.getUid(), "down");

                        shardRef.update("votes", FieldValue.arrayUnion(addUserToArrayMap));
                    }
                }
            });


            if (firebaseUser.getUid().equals(data.getUser())) {
                btnDeletar.setVisibility(View.VISIBLE);
                btnDeletar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                                .build();
                        db.setFirestoreSettings(settings);
                        db.collection("events").document(data.getId()).delete();
                        mClusterManager.removeItem(item);
                        mClusterManager.cluster();
                        dialog.dismiss();
                    }
                });
            }

            dialog.show();
            return true;
        }
    };

}
