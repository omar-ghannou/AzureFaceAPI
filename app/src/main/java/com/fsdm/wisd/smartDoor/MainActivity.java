package com.fsdm.wisd.smartDoor;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceRestClient;

public class MainActivity extends AppCompatActivity implements IdentifyListener{

    private static final String API_ENDPOINT = "https://wisdsmartdoor.cognitiveservices.azure.com/face/v1.0/";
    private static final String API_GROUP_ENDPOINT = "https://francecentral.api.cognitive.microsoft.com/face/v1.0/";
    private static final String  API_KEY ="614038ff388d4b5c9fa42b57320b57c9";




    ImageView imageView;
    Bitmap imageBitmap;
    private static final FaceServiceRestClient faceServiceClient = new FaceServiceRestClient(API_ENDPOINT, API_KEY);
    Button Identify,mOpenBtn,mCloseBtn;

    static IdentifyTask identifyTask;

    private ProgressDialog mProgressDialog;

    private SendRequest mSendRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressDialog=new ProgressDialog(this);

        mSendRequest=new SendRequest(this);

        Identify = findViewById(R.id.b_identify);

        mOpenBtn=findViewById(R.id.open);
        mOpenBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "open", Toast.LENGTH_SHORT).show();
                mSendRequest.openDoor("open");
            }
        });

        mCloseBtn=findViewById(R.id.close);
        mCloseBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "close", Toast.LENGTH_SHORT).show();
                mSendRequest.openDoor("close");
            }
        });

        imageView = findViewById(R.id.imageView);
        Identify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 100);

                }
                else{

                    capture();

                }

            }
        });

    }

    private void capture() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, 102);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == 102 && resultCode == RESULT_OK) {
            imageBitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(imageBitmap);
            //ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            //InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            mProgressDialog.setMessage("Please be patient... we are trying to match your face!");
            mProgressDialog.show();
            identifyTask = new IdentifyTask(this,faceServiceClient,"007");
            identifyTask.execute(imageBitmap);
        }


        if (requestCode == 100 && resultCode == RESULT_OK) {
            capture();

        }

    }

    @Override
    public void isVerified(int result) {

        Log.d("Main",result+" ");
        mProgressDialog.dismiss();
        Toast.makeText(this, result==0? "mal9inax wjah bhad had camara":"l9ina camartak", Toast.LENGTH_SHORT).show();
        new SendRequest(this).openDoor(result==0?"close":"open");

    }
}