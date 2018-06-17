package com.mostafa.android.uber;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DriverMapSettings extends AppCompatActivity {
    @BindView(R.id.profileDriver)
    ImageView imageView;
    @BindView(R.id.nameEditTextDriver)
    EditText nameEditText;
    @BindView(R.id.phoneEditTextDriver)
    EditText phoneEditText;
    @BindView(R.id.updateDriver)
    Button updateDriver;
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    String id = firebaseAuth.getCurrentUser().getUid();
    private final DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child("driver").child(id);
    private Uri imageuri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map_settings);
        ButterKnife.bind(this);

        final Map updatepost = new HashMap();

        getUserInfo();

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });
        updateDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(nameEditText.getText().toString().isEmpty() && phoneEditText.getText().toString().isEmpty()){
                    nameEditText.setError("Enter the missing data");
                    phoneEditText.setError("Enter the missing data");
                }else{
                    String name = nameEditText.getText().toString().trim();
                    String phone = phoneEditText.getText().toString().trim();
                    updatepost.put("name",name);
                    updatepost.put("phone",phone);
                    databaseReference.updateChildren(updatepost);
                    if (imageuri != null) {
                        String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userid);
                        Bitmap bitmap = null;
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), imageuri);
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
                                    databaseReference.updateChildren(newImage);

                                    onBackPressed();
                                    return;

                                } else {
                                    // Handle failures
                                    // ...
                                }
                            }
                        });
                    }else{
                        onBackPressed();
                    }
                }
            }
        });
    }

    private void getUserInfo() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String,Object> map = (Map<String,Object>)dataSnapshot.getValue();
                    if(map.get("name") != null){
                        nameEditText.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        phoneEditText.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl")!=null){
                        String mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(imageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode ==1 && resultCode == RESULT_OK){
            final Uri imageUri = data.getData();
            imageView.setImageURI(imageUri);
            this.imageuri = imageUri;
        }
    }
}
