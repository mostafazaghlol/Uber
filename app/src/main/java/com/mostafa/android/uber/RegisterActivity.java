package com.mostafa.android.uber;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RegisterActivity extends AppCompatActivity {
    @BindView(R.id.prog)
    ProgressBar progressBar;
    @BindView(R.id.nameEditTextReg)
    EditText EdName;
    @BindView(R.id.phoneEditTextReg)
    EditText EdPhone;
    @BindView(R.id.profileReg)
    ImageView profileImage;
    @BindView(R.id.updateReg)
    Button Update;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference firebaseDatabase;
    private Uri imageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_register);
        ButterKnife.bind(this);
        setTitle("Driver Information");

        firebaseAuth  = FirebaseAuth.getInstance();
        String userId = firebaseAuth.getCurrentUser().getUid();
        firebaseDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("customer").child(userId);

        Update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTheUserdata();
            }
        });

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode ==1 && resultCode == RESULT_OK){
            final Uri imageUri = data.getData();
            profileImage.setImageURI(imageUri);
            this.imageUri = imageUri;
        }
    }

    private void updateTheUserdata() {
        progressBar.setVisibility(View.VISIBLE);
        String name = EdName.getText().toString();
        String password = EdPhone.getText().toString();
        if (TextUtils.isEmpty(name)) {
            EdName.setError("Enter the name");
        } else if (TextUtils.isEmpty(password)) {
            EdPhone.setError("Enter the password");
        } else {
            Map updatePost = new HashMap();
            updatePost.put("name", name);
            updatePost.put("phone", password);
            if (firebaseDatabase.updateChildren(updatePost).isSuccessful()) {
                Toast.makeText(this, "Updated !", Toast.LENGTH_SHORT).show();
            }

            if (imageUri != null) {
                String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userid);
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
                byte[] data = baos.toByteArray();
                UploadTask uploadTask = filePath.putBytes(data);

                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        finish();
                        return;
                    }
                });

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            Map newImage = new HashMap();
                            newImage.put("profileImageUrl", downloadUri.toString());
                            firebaseDatabase.updateChildren(newImage);
                            progressBar.setVisibility(View.GONE);
                            startActivity(new Intent(RegisterActivity.this,DriverMapsActivity.class));
                            finish();
                            return;

                        } else {
                            // Handle failures
                            // ...
                        }
                    }
                });
            }
        }
    }
//    private void getUserInfo() {
//
//        firebaseDatabase.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if(dataSnapshot.exists()){
//                    Map<String,Object> map = (Map<String,Object>)dataSnapshot.getValue();
//                    if(map.get("name") != null){
//                        EdName.setText(map.get("name").toString());
//                    }
//                    if(map.get("phone")!=null){
//                        EdPhone.setText(map.get("phone").toString());
//                    }
//                    if(map.get("profileImageUrl")!=null){
//                        String mProfileImageUrl = map.get("profileImageUrl").toString();
//                        Glide.with(getApplication()).load(mProfileImageUrl).into(profileImage);
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });
//    }


}

