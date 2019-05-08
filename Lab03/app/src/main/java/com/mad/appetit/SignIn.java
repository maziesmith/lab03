package com.mad.appetit;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mad.lib.Restaurateur;

import static com.mad.lib.SharedClass.CameraOpen;
import static com.mad.lib.SharedClass.Description;
import static com.mad.lib.SharedClass.Name;
import static com.mad.lib.SharedClass.PERMISSION_GALLERY_REQUEST;
import static com.mad.lib.SharedClass.Photo;
import static com.mad.lib.SharedClass.RESTAURATEUR_INFO;
import static com.mad.lib.SharedClass.ROOT_UID;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SignIn extends AppCompatActivity {
    private String mail, psw, name, addr, descr, phone;
    private String errMsg = " ";
    private String currentPhotoPath = null;

    private boolean camera_open = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        findViewById(R.id.plus).setOnClickListener(p -> editPhoto());
        findViewById(R.id.img_profile).setOnClickListener(e -> editPhoto());

        findViewById(R.id.button).setOnClickListener(e -> {
            if(checkFields()){
                auth.createUserWithEmailAndPassword(mail, psw).addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        ROOT_UID = auth.getUid();
                        storeDatabase();

                        Intent i = new Intent();
                        setResult(1, i);

                        finish();
                    }
                    else {
                        Toast.makeText(SignIn.this,"Registration failed. Try again", Toast.LENGTH_LONG).show();
                        Log.d("SIGN IN", "Error: createUserWithEmail:failure", task.getException());
                    }
                });
            }
            else{
                Toast.makeText(SignIn.this, errMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public boolean checkFields(){
        mail = ((EditText)findViewById(R.id.mail)).getText().toString();
        psw = ((EditText)findViewById(R.id.psw)).getText().toString();
        name = ((EditText)findViewById(R.id.name)).getText().toString();
        addr = ((EditText)findViewById(R.id.address)).getText().toString();
        descr = ((EditText)findViewById(R.id.description)).getText().toString();
        phone = ((EditText)findViewById(R.id.phone)).getText().toString();

        if(mail.trim().length() == 0 || !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()){
            errMsg = "Invalid Mail";
            return false;
        }

        if(psw.trim().length() == 0 || psw.length() < 6){
            errMsg = "Password should be at least 6 characters";
            return false;
        }

        if(name.trim().length() == 0){
            errMsg = "Fill name";
            return false;
        }

        if(addr.trim().length() == 0){
            errMsg = "Fill address";
            return false;
        }

        if(phone.trim().length() != 10){
            errMsg = "Invalid phone number";
            return false;
        }

        return true;
    }

    private void editPhoto(){
        AlertDialog alertDialog = new AlertDialog.Builder(SignIn.this, R.style.AlertDialogStyle).create();
        LayoutInflater factory = LayoutInflater.from(SignIn.this);
        final View view = factory.inflate(R.layout.custom_dialog, null);

        camera_open = true;

        alertDialog.setOnCancelListener(dialog -> {
            camera_open = false;
            alertDialog.dismiss();
        });

        view.findViewById(R.id.camera).setOnClickListener( c -> {
            cameraIntent();
            camera_open = false;
            alertDialog.dismiss();
        });
        view.findViewById(R.id.gallery).setOnClickListener( g -> {
            galleryIntent();
            camera_open = false;
            alertDialog.dismiss();
        });

        alertDialog.setView(view);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Camera", (dialog, which) -> {
            cameraIntent();
            camera_open = false;
            dialog.dismiss();
        });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Gallery", (dialog, which) -> {
            galleryIntent();
            camera_open = false;
            dialog.dismiss();
        });
        alertDialog.show();
    }

    private void cameraIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 2);
            }
        }
    }

    private void galleryIntent(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    PERMISSION_GALLERY_REQUEST);
        }
        else{
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, 1);
        }
    }

    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File( storageDir + File.separator +
                imageFileName + /* prefix */
                ".jpg"
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();

        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_GALLERY_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission Run Time: ", "Obtained");

                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, 1);
                } else {
                    Log.d("Permission Run Time: ", "Denied");

                    Toast.makeText(getApplicationContext(), "Access to media files denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if((requestCode == 1) && resultCode == RESULT_OK && null != data){
            Uri selectedImage = data.getData();

            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            currentPhotoPath = picturePath;
        }

        if((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK){
            Glide.with(getApplicationContext()).load(currentPhotoPath).into((ImageView)findViewById(R.id.img_profile));
        }
    }

    public void storeDatabase(){
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference(RESTAURATEUR_INFO);
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        Map<String, Object> restMap = new HashMap<>();

        if(currentPhotoPath != null) {
            Uri photoUri = Uri.fromFile(new File(currentPhotoPath));
            StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());

            ref.putFile(photoUri).continueWithTask(task -> {
                if (!task.isSuccessful()){
                    throw Objects.requireNonNull(task.getException());
                }
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    Uri downUri = task.getResult();

                    restMap.put(ROOT_UID, new Restaurateur(mail, name, addr, descr, phone, downUri.toString()));
                    myRef.updateChildren(restMap);
                }
            });
        }
        else{
            restMap.put(ROOT_UID, new Restaurateur(mail, name, addr, descr, phone, null));
            myRef.updateChildren(restMap);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString(Name, ((EditText)findViewById(R.id.name)).getText().toString());
        savedInstanceState.putString(Description, ((EditText)findViewById(R.id.description)).getText().toString());
        savedInstanceState.putString(Photo, currentPhotoPath);
        savedInstanceState.putBoolean(CameraOpen, camera_open);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        ((EditText)findViewById(R.id.name)).setText(savedInstanceState.getString(Name));
        ((EditText)findViewById(R.id.description)).setText(savedInstanceState.getString(Description));

        currentPhotoPath = savedInstanceState.getString(Photo);
        if(currentPhotoPath != null){
            Glide.with(getApplicationContext()).load(currentPhotoPath).into((ImageView) findViewById(R.id.img_profile));
        }

        if(savedInstanceState.getBoolean(CameraOpen))
            editPhoto();
    }
}