package com.mostafa.android.uber;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
    private boolean isIN = false;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthStateListener;
    boolean isForRegister = false;
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
                if(user != null && !isIN && !isForRegister){
                    if(intent.getStringExtra("cat").equals("customer")){
                        startActivity(new Intent(Login.this,CustomerMapActivity.class));
                        isIN = true;
                        finish();
                        return;
                    }else if(intent.getStringExtra("cat").equals("Driver")){
                        startActivity(new Intent(Login.this,DriverMapsActivity.class));
                        isIN = true;
                        finish();
                        return;
                    }
                    //finish();
                    //return;
                }else if(user != null && !isIN && isForRegister)
                {
                    if(intent.getStringExtra("cat").equals("customer")) {
                        startActivity(new Intent(Login.this, RegisterActivity.class));
                    }else{
                        startActivity(new Intent(Login.this, RegisterActivity.class));

                    }
                }
            }
        };
        final String[] cat = new String[1];
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isForRegister = true;
                final String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString();
                if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password)){
                    editTextEmail.setError("Enter the text here");
                    editTextPassword.setError("Enter the text here");
                }else{
                    mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()){
                                Toast.makeText(Login.this, "error with register" +task.getException().getMessage().toString(), Toast.LENGTH_SHORT).show();
                            }else{
                                String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                if(intent.getStringExtra("cat").equals("customer")){
                                    cat[0] = "customer";

                                }else{
                                    cat[0] = "driver";

                                }
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(cat[0]).child(userID);

                                Map newPost = new HashMap();
                                newPost.put("email",email);

                                ref.setValue(newPost);
                            }
                        }
                    });
                }

            }
        });
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString();
                if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password)){
                    editTextEmail.setError("Enter the text here");
                    editTextPassword.setError("Enter the text here");
                }else {
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(Login.this, "Error with login " + task.getException().getMessage().toString(), Toast.LENGTH_SHORT).show();
                                if(intent.getStringExtra("cat").equals("customer")){
                                    cat[0] = "customer";

                                }else{
                                    cat[0] = "Driver";

                                }
                            }
                        }
                    });
                }
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
