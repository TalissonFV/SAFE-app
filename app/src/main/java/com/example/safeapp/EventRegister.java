package com.example.safeapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventRegister extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private Spinner dropdownEvents;
    private TextView date;
    private EditText description;
    private Button btnSaveEvent;
    private ImageView btnReturn;
    private GeoPoint location;
    private @ServerTimestamp Date eventDate;

    PlacesClient placesClient;
    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;

    String[] itens = new String[]{"Roubo","Furto","Descarte irregular de lixo","Alagamento","Rua fechada","Obras"};
    int day, month, year, hour, minute;
    int eventDay, eventMonth, eventYear, eventHour, eventMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_register);
        dropdownEvents = findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, itens);
        dropdownEvents.setAdapter(adapter);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_api_key));
        placesClient = Places.createClient(this);

        final AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                final LatLng latLng = place.getLatLng();
                location = new GeoPoint(latLng.latitude, latLng.longitude);
                Toast.makeText(EventRegister.this, latLng.toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });

        btnSaveEvent = findViewById(R.id.btnSaveEvent);
        description = findViewById(R.id.etDescription);
        btnReturn = findViewById(R.id.btnReturn);
        date = findViewById(R.id.etDate);

        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDateTime();
            }
        });

        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        btnSaveEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> event = new HashMap<>();
                event.put("type", dropdownEvents.getSelectedItem().toString());
                event.put("position", location);
                event.put("description", description.getText().toString());
                event.put("date", Timestamp.now());
                event.put("user", firebaseUser.getUid());
                event.put("eventDate", eventDate);

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("events")
                        .add(event)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                documentReference.update("id",documentReference.getId());
                                System.out.println("DocumentSnapshot added with ID: " + documentReference.getId());
                                Toast.makeText(EventRegister.this, "Evento registrado com sucesso!", Toast.LENGTH_SHORT).show();
                                setResult(0);
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                System.out.println("Error adding document: " + e);
                            }
                        });
            }
        });
    }

    public void pickDateTime () {

        Calendar cal = Calendar.getInstance();


        day = cal.get(Calendar.DAY_OF_MONTH);
        month = cal.get(Calendar.MONTH);
        year = cal.get(Calendar.YEAR);

        DatePickerDialog datePickerDialog = new DatePickerDialog(EventRegister.this, EventRegister.this,
                year, month, day);
        datePickerDialog.show();

    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        eventYear = year;
        eventMonth = month;
        eventDay = dayOfMonth;

        Calendar cal = Calendar.getInstance();

        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(EventRegister.this, EventRegister.this,
                hour, minute, DateFormat.is24HourFormat(this));
        timePickerDialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        eventHour = hourOfDay;
        eventMinute = minute;

        eventDate = new GregorianCalendar(eventYear,eventMonth-1,eventDay,eventHour,eventMinute).getTime();
        System.out.println(eventDate);
        SimpleDateFormat dateFormatprev = new SimpleDateFormat("dd/MM/yyyy - HH:mm");

        date.setText(dateFormatprev.format(eventDate));
    }
}