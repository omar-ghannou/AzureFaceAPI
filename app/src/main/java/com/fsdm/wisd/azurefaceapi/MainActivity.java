package com.fsdm.wisd.azurefaceapi;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.Person;
import com.microsoft.projectoxford.face.contract.PersonGroup;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String API_ENDPOINT = "https://wisdsmartdoor.cognitiveservices.azure.com/face/v1.0/";
    private static final String API_GROUP_ENDPOINT = "https://francecentral.api.cognitive.microsoft.com/face/v1.0/";
    private static final String  API_KEY ="614038ff388d4b5c9fa42b57320b57c9";

    static int c = 0;

    ArrayList<Bitmap> identifiedBitmaps = new ArrayList<Bitmap>();

    Button Detect;
    Button Add;
    ImageView imageView;
    Bitmap imageBitmap;
    private static FaceServiceRestClient faceServiceClient = new FaceServiceRestClient(API_ENDPOINT, API_KEY);
    Face[] faces;
    Button Identify;

    static DetectTask detectTask;
    static IdentifyTask identifyTask;

    class DetectTask extends AsyncTask<InputStream, String, Face[]>{
        @Override
        protected Face[] doInBackground(InputStream... params) {
            try {
                Log.d("stat", "Detecting...");
                Face[] result = faceServiceClient.detect(
                        params[0],
                        true,         // returnFaceId
                        true,        // returnFaceLandmarks
                        // returnFaceAttributes:
                        new FaceServiceClient.FaceAttributeType[]{
                                FaceServiceClient.FaceAttributeType.Emotion,FaceServiceClient.FaceAttributeType.Age}
                );
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(Face[] result) {
            faces = result;
            Bitmap bitmap = drawFaceRectanglesOnBitmap(imageBitmap,result);
            imageView.setImageBitmap(bitmap);
            Toast.makeText(MainActivity.this," age : " + result[0].faceAttributes.age + " emo : " + result[0].faceAttributes.emotion.happiness,Toast.LENGTH_LONG).show();
        }


    }

    class IdentifyTask extends AsyncTask<Bitmap, String, Integer>{
        @Override
        protected Integer doInBackground(Bitmap... params) {
            Bitmap bitmap = params[0];
            try {
                MainActivity.IdentifyFace(MainActivity.this,"007",bitmap);
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }


    }

    class AddPersonGroup extends AsyncTask<PersonGroupData, String, Integer>{

        @Override
        protected Integer doInBackground(PersonGroupData... personGroupData) {
            PersonGroupData person = personGroupData[0];
            if(person == null) return -1;
            try {
                MainActivity.CreatePersonGroup(person.context,person.PersonGroupId,person.PersonGroupName);
                MainActivity.AddPersonToGroup(person.context,person.PersonGroupId,person.PersonName,person.bitmaps);
                MainActivity.TrainingAI(person.context,person.PersonGroupId);
                return 0;
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            } catch (ClientException e) {
                e.printStackTrace();
                return -1;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return -1;
            }
        }
    }

    class PersonGroupData {
        public Context context;
        public String PersonGroupId,PersonGroupName,PersonName;
        public List<Bitmap> bitmaps;

        PersonGroupData(Context _context,String _PersonGroupId, String _PersonGroupName, String _PersonName,List<Bitmap> _bitmaps){
            context = _context;
            PersonGroupId = _PersonGroupId;
            PersonGroupName = _PersonGroupName;
            PersonName = _PersonName;
            bitmaps = _bitmaps;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Detect = findViewById(R.id.Detect);
        Add = findViewById(R.id.Add);
        Identify = findViewById(R.id.b_identify);

        imageView = findViewById(R.id.imageView);

        detectTask = new DetectTask();
        identifyTask = new IdentifyTask();

        Detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 110);
                }
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 100);
            }
        });

        Add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 111);
                }
                getPickImageIntent();
            }
        });

        Identify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 102);
            }
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            imageBitmap = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            detectTask.execute(inputStream);
        }
        if(requestCode == 101 && resultCode == RESULT_OK){
            //if(c==2)
            //{
            //    PersonGroupData personGroupData = new PersonGroupData(MainActivity.this, "007", "Agent", "James", identifiedBitmaps);
            //    new AddPersonGroup().execute(personGroupData);
            //    c=0;
            //    return;
            //}
            //identifiedBitmaps.add((Bitmap) data.getExtras().get("data"));
            //getPickImageIntent();

            //if(c==55) {
                ArrayList<Uri> uris = new ArrayList<Uri>();
                ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
                if (data.getClipData() != null) {
                    ClipData mClipData = data.getClipData();
                    for (int i = 0; i < mClipData.getItemCount(); i++) {
                        ClipData.Item item = mClipData.getItemAt(i);
                        Uri uri = item.getUri();
                        Log.d("image", Integer.toString(i));
                        uris.add(uri);
                        try {
                            bitmaps.add(MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    Log.d("image", "one data");
                    uris.add(uri);
                    try {
                        bitmaps.add(MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                PersonGroupData personGroupData = new PersonGroupData(MainActivity.this, "008", "NewAgent", "Omar", bitmaps);
                new AddPersonGroup().execute(personGroupData);

            //}
        }
        if (requestCode == 102 && resultCode == RESULT_OK) {
            imageBitmap = (Bitmap) data.getExtras().get("data");
            //ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            //InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            identifyTask.execute(imageBitmap);
        }

    }

    public void getPickImageIntent(){
        //if(c<2) {
        //    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //    startActivityForResult(cameraIntent, 101);
        //    c++;
        //}

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, 101);
    }

    private synchronized static void CreatePersonGroup(Context context, String personGroupID, String personGroupName) throws IOException, ClientException {


        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.INTERNET}, 111);
            }
            faceServiceClient.createPersonGroup(personGroupID,personGroupName, null);
            Log.d("FaceAPI","the person group has been created successfully");
        } catch (ClientException | IOException  e) {
            e.printStackTrace();
            Log.d("FaceAPI",e.getMessage());
        }

    }

    private synchronized static void AddPersonToGroup(Context context, String personGroupID, String personName, List<Bitmap> bitmap){

        try {
            faceServiceClient.getPersonGroup(personGroupID);
            CreatePersonResult personResult = faceServiceClient.createPerson(personGroupID,personName,null);
            DetectFaceAndRegister(personGroupID,personResult,bitmap);
            Log.d("FaceAPI","the person has been added to the group successfully");
        } catch (ClientException | IOException  e) {
            e.printStackTrace();
        }

    }

    private synchronized static void DetectFaceAndRegister(String personGroupID, CreatePersonResult personResult, List<Bitmap> bitmaps) throws IOException, ClientException {

        for (Bitmap b : bitmaps){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            faceServiceClient.addPersonFace(personGroupID,personResult.personId,inputStream,null,null);
        }

    }

    private synchronized static void TrainingAI(Context context,String personGroupID) throws IOException, ClientException, InterruptedException {
        faceServiceClient.trainPersonGroup(personGroupID);
        TrainingStatus trainingStatus = null;
        while(true){
            trainingStatus = faceServiceClient.getPersonGroupTrainingStatus(personGroupID);
            if(trainingStatus.status != TrainingStatus.Status.Running){
                Log.d("FaceAPIStatus",trainingStatus.status.name());
                break;
            }
            Thread.sleep(1000);
        }

    }

    private synchronized static void IdentifyFace(Context context,String personGroupID,Bitmap bitmap) throws IOException, ClientException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        Face[] faces = faceServiceClient.detect(
                inputStream,
                true,         // returnFaceId
                true,        // returnFaceLandmarks
                // returnFaceAttributes:
                null);
        List<UUID> faceIds = new ArrayList<UUID>();
        for (Face face: faces) {
            faceIds.add(face.faceId);
        }
        UUID[] uuids = new UUID[faceIds.size()];
        IdentifyResult[] identifiedFaces = faceServiceClient.identity(personGroupID,faceIds.toArray(uuids),10); //range is [1-100], 10 is default

        for (IdentifyResult result: identifiedFaces) {
            if(result.candidates.size() == 0){
                Log.d("IdResult","No one is identified");
            }
            else {
                UUID personId = result.candidates.get(0).personId;
                Person person = faceServiceClient.getPerson(personGroupID,personId);
                Log.d("IdResult",person.name + " is identified");
            }

        }



    }

    private static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private static Bitmap drawFaceRectanglesOnBitmap(
            Bitmap originalBitmap, Face[] faces) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(1);
        if (faces != null) {
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
            }
        }
        return bitmap;
    }
}