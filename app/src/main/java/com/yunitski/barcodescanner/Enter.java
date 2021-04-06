package com.yunitski.barcodescanner;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Enter extends AppCompatActivity implements View.OnClickListener {

    Button saveButton, photo, save;
    TextView codeText;
    EditText etLoc, codeTextIfScanDisabled;
    private static  final int REQUEST_ACCESS_TYPE=1;
    static final String ACCESS_MESSAGE="ACCESS_MESSAGE";
    ImageView imageView, restart;
    static final String IMAGE = "image";
    Bitmap bitmap;
    public static final int REQUEST_CODE_PHOTO = 100;
    public static final int ACCESS_DOC = 112;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter);
        saveButton = findViewById(R.id.scanF);
        saveButton.setOnClickListener(this);
        codeText = findViewById(R.id.codeText);
        photo = findViewById(R.id.photo);
        photo.setOnClickListener(this);
        save = findViewById(R.id.save);
        ActivityCompat.requestPermissions(Enter.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        accessCam();
        save.setOnClickListener(this);
        restart = findViewById(R.id.restart);
        restart.setOnClickListener(this);
        etLoc = findViewById(R.id.etLocation);
        codeTextIfScanDisabled = findViewById(R.id.codeTextIfScanDisabled);
        imageView = findViewById(R.id.image);
        imageView.setOnClickListener(this);
        imageView.setClickable(false);
        if (ContextCompat.checkSelfPermission(Enter.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(Enter.this, new String[]{
                    Manifest.permission.CAMERA
            }, REQUEST_CODE_PHOTO);
        }

    }
    public void accessCam(){

        ActivityCompat.requestPermissions(Enter.this,new String[]{Manifest.permission.CAMERA},REQUEST_CODE_PHOTO);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scanF:
                if (codeTextIfScanDisabled.getText().toString().isEmpty()) {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivityForResult(intent, REQUEST_ACCESS_TYPE);
                }
                else {
                    saveButton.setEnabled(false);
                }
            break;
            case R.id.photo:
                if (ActivityCompat.checkSelfPermission(Enter.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent1, REQUEST_CODE_PHOTO);
                } else {
                    accessCam();
                }
                break;
            case R.id.image:
                if (imageView.isClickable()) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    Intent intent2 = new Intent(this, PhotoActivity.class);
                    intent2.putExtra(IMAGE, byteArray);
                    startActivity(intent2);
                }
                break;
            case R.id.save:
                if ((!codeText.getText().toString().isEmpty() || !codeTextIfScanDisabled.getText().toString().isEmpty()) && imageView.isClickable() && !etLoc.getText().toString().isEmpty()) {
                    imageSave();
                    if (codeTextIfScanDisabled.getText().toString().isEmpty() && !codeText.getText().toString().isEmpty()) {
                        String s = codeText.getText().toString() + ";" + etLoc.getText().toString() + ";" + "SCANNED" +  "\n";
                        writeToFile(s);
                    } else if (codeText.getText().toString().isEmpty() && !codeTextIfScanDisabled.getText().toString().isEmpty()) {
                        String s = codeTextIfScanDisabled.getText().toString() + ";" + etLoc.getText().toString() + ";" + "MANUAL" + "\n";
                        writeToFile(s);
                    }
                    //Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "No data to save", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.restart:
                resetAll();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCESS_TYPE:
            if (requestCode == REQUEST_ACCESS_TYPE) {
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    String accessMessage = data.getStringExtra(ACCESS_MESSAGE);
                    codeText.setText(accessMessage);
                    codeTextIfScanDisabled.setInputType(InputType.TYPE_NULL);
                } else {
                    codeTextIfScanDisabled.setInputType(InputType.TYPE_CLASS_TEXT);
                    Toast.makeText(this, "Make a scan", Toast.LENGTH_SHORT).show();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
            break;
            case REQUEST_CODE_PHOTO:
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    bitmap = (Bitmap) data.getExtras().get("data");
                    imageView.setImageBitmap(bitmap);
                    imageView.setClickable(true);
                } else {
                    Toast.makeText(this, "Take a photo", Toast.LENGTH_SHORT).show();
                }
            break;
            case ACCESS_DOC:
                if (requestCode == ACCESS_DOC){
                    if (resultCode == RESULT_OK){
                        String s = "";
                        if (codeTextIfScanDisabled.getText().toString().isEmpty() && !codeText.getText().toString().isEmpty()) {
                            s = codeText.getText().toString() + ";" + etLoc.getText().toString() + ";" + "SCANNED" + "\n";
                        } else if (codeText.getText().toString().isEmpty() && !codeTextIfScanDisabled.getText().toString().isEmpty()) {
                            s = codeTextIfScanDisabled.getText().toString() + ";" + etLoc.getText().toString() + ";" + "MANUAL" + "\n";
                        }
                        try {
                            Uri uri = data.getData();
                            OutputStream outputStream = getContentResolver().openOutputStream(uri);
                            outputStream.write(s.getBytes());
                            outputStream.close();
                            Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Toast.makeText(this, "not saved", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    private void imageSave(){
        ImageView img = findViewById(R.id.image);
        BitmapDrawable draw = (BitmapDrawable) img.getDrawable();
        Bitmap bitmap = draw.getBitmap();
        if (Build.VERSION.SDK_INT < 29) {
            FileOutputStream outStream = null;
            File sdCard = Environment.getExternalStorageDirectory();
            String apartFolder = etLoc.getText().toString();
            File dir = new File(sdCard.getAbsolutePath() + "/PhotoInvApp" + "/" + apartFolder);
            dir.mkdirs();
            String fileName;
            String barcode = codeText.getText().toString();
            String barcodeET = codeTextIfScanDisabled.getText().toString();
            if (!barcode.isEmpty() && barcodeET.isEmpty()) {
                fileName = String.format("%s.jpg", barcode);
            } else if (barcode.isEmpty() && !barcodeET.isEmpty()) {
                fileName = String.format("%s.jpg", barcodeET);
            } else {
                fileName = String.format("%d.jpg", System.currentTimeMillis());
            }
            File outFile = new File(dir, fileName);
            try {
                outStream = new FileOutputStream(outFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                outStream.flush();
                outStream.close();
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(outFile));
                sendBroadcast(intent);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            String apartFolder = etLoc.getText().toString();
            File filesDir = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS + "/" + apartFolder);
            assert filesDir != null;
            if (!filesDir.exists()){
                filesDir.mkdirs();
            }
            String fileName;
            String barcode = codeText.getText().toString();
            String barcodeET = codeTextIfScanDisabled.getText().toString();
            if (!barcode.isEmpty() && barcodeET.isEmpty()) {
                fileName = String.format("%s.jpg", barcode);
            } else if (barcode.isEmpty() && !barcodeET.isEmpty()) {
                fileName = String.format("%s.jpg", barcodeET);
            } else {
                fileName = String.format("%d.jpg", System.currentTimeMillis());
            }
            File outFile = new File(filesDir, fileName);
            try {
                OutputStream outStreamFile = new FileOutputStream(outFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStreamFile);
                outStreamFile.flush();
                outStreamFile.close();
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(outFile));
                sendBroadcast(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private void writeToFile(String data){
        String fileName = "PhotoInvText.txt";

        if (Build.VERSION.SDK_INT < 29) {

            String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PhotoInvApp";

            File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + "/PhotoInvApp", fileName);


            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                fileOutputStream.write(data.getBytes());
                fileOutputStream.close();
                Toast.makeText(this, "Saved in directory:" + dir, Toast.LENGTH_LONG).show();

            } catch (IOException e) {
                Toast.makeText(this, "IOException", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
        else {
            File filesDir = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            //assert filesDir != null;
            if (!filesDir.exists()){
                filesDir.mkdirs();
            }
            File file = new File(filesDir, fileName);
            try {
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        throw new IOException("Cant able to create file");
                    }
                }
                OutputStream os = new FileOutputStream(file, true);
                byte[] dataF = data.getBytes();
                os.write(dataF);
                os.close();
                Toast.makeText(this, "Saved in directory: " + filesDir, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show();
            }
        }

    }
    private void resetAll(){
        codeText.setText("");
        codeTextIfScanDisabled.setInputType(InputType.TYPE_CLASS_TEXT);
        codeTextIfScanDisabled.setText("");
        imageView.setImageResource(0);
        etLoc.setText("");
        saveButton.setEnabled(true);
    }
}