package com.mostafa.android.uber;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Login extends AppCompatActivity {
    @BindView(R.id.email)
    EditText editTextEmail;
    @BindView(R.id.password)
    EditText editTextPassword;
    @BindView(R.id.login)
    Button buttonLogin;
    @BindView(R.id.register)
    Button buttonRegister;

    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthStateListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);
        ButterKnife.bind(this);
        final Intent intent=getIntent();
        mAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null){
                    if(intent.getStringExtra("cat").equals("customer")){
                        startActivity(new Intent(Login.this,CustomerMapActivity.class));
                    }else{
                        startActivity(new Intent(Login.this,DriverMapsActivity.class));                    }
                    finish();
                    return;
                }
            }
        };
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString();
                mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(Login.this, "error with register" +task.getException().getMessage().toString(), Toast.LENGTH_SHORT).show();
                        }else{
                            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            String cat;
                            if(intent.getStringExtra("cat").equals("customer")){
                                cat = "customer";
                            }else{
                                cat = "driver";
                            }
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(cat).child(userID);

                            Map newPost = new HashMap();
                            newPost.put("email",email);

                            ref.setValue(newPost);
                        }
                    }
                });
            }
        });
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString();
                mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(Login.this, "Error with login "+task.getException().getMessage().toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.addAuthStateListener(mAuthStateListener);
    }
}
