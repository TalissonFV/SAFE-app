package com.example.safeapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText email;
    EditText password;
    EditText passwordConfirmation;
    EditText CPF;
    Button registrar;
    ProgressBar progressBar;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etPassword);
        passwordConfirmation = findViewById(R.id.etConfirmPassword);
        CPF = findViewById(R.id.etCPF);
        registrar = findViewById(R.id.btnRegisterAccount);
        progressBar = findViewById(R.id.progressBarRegister);

        firebaseAuth = FirebaseAuth.getInstance();

        registrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (verifyFields()) {
                    registerAccountOnFirebase();
                }
            }
        });
    }

    protected boolean verifyFields () {
        boolean passwordConfirm = false, emailConfirm = false, cpfConfirm = false;
        if (password.getText().toString().equals(passwordConfirmation.getText().toString())) {
            passwordConfirm = true;
        }
        if( !email.getText().toString().isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email.getText().toString()).matches()) {
            emailConfirm = true;
        }
        if (verifyCPF(CPF.getText().toString())) {
            cpfConfirm = true;
        }
        return passwordConfirm && emailConfirm && cpfConfirm;
    }

    private boolean verifyCPF (String cpf) {
        return true;
    }

    protected void registerAccountOnFirebase () {
        progressBar.setVisibility(View.VISIBLE);
        firebaseAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            storeUser();
                            Toast.makeText(RegisterActivity.this, "Usuário registrado com sucesso!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, MapsActivity.class));
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Erro ao registrar usuário", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    protected void storeUser() {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email.getText().toString());
        user.put("cpf", CPF.getText().toString());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        System.out.println("DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("Error adding document: " + e);
                    }
                });
    }


}
