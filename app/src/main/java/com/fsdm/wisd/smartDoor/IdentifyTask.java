package com.fsdm.wisd.smartDoor;
/*
 **    *** AzureFaceAPI ***
 **   Created by EL KHARROUBI HASSAN
 **   At Tuesday April 2021 15H 51MIN
 */


import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.Person;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class IdentifyTask  extends AsyncTask<Bitmap, String, Integer> {


    private Context mContext;
    private String mPersonGroupID;
    private IdentifyListener mIdentifyListener;
    private static int mResult=0;

    private static FaceServiceRestClient mFaceServiceRestClient;

    IdentifyTask(Context context, FaceServiceRestClient faceServiceClient, String personGroupID){
        mContext=context;
        mPersonGroupID=personGroupID;

        mFaceServiceRestClient=faceServiceClient;

    }



    private synchronized static void IdentifyFace(String personGroupID,Bitmap bitmap) {



        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        Face[] faces = new Face[1];

        Log.d("Task","start identify");
        try {

            faces = mFaceServiceRestClient.detect(
                    inputStream,
                    true,         // returnFaceId
                    true,        // returnFaceLandmarks
                    // returnFaceAttributes:
                    null);
            Log.d("Task","faces............"+faces.length);
        } catch (ClientException e) {

            e.printStackTrace();
            Log.d("Task","client Exception.............");


        } catch (IOException e) {
            Log.d("Task","IOException............."+e.getMessage());
            e.printStackTrace();
        }

        //if we arrive here that is means the first test is passed
        mResult=1;

        List<UUID> faceIds = new ArrayList<UUID>();


        for (Face face: faces) {

            if(face==null){

                Log.d("Task","faces are null");
                mResult=0;
                return ;
            }

            faceIds.add(face.faceId);
        }

        UUID[] uuids = new UUID[faceIds.size()];
        IdentifyResult[] identifiedFaces = new IdentifyResult[1]; //range is [1-100], 10 is default
        try {

            identifiedFaces = mFaceServiceRestClient.identity(personGroupID,faceIds.toArray(uuids),1);

        } catch (ClientException e) {
            //if error occurs
            mResult=0;

            Log.d("Task","client Exception, identifiedFaces............"+e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            mResult=0;
            Log.d("Task","IOException, identifiedFaces............");
            e.printStackTrace();
        }


        Log.d("Task","length of identifiedfaces "+identifiedFaces.length);
        for (IdentifyResult result: identifiedFaces) {

            if (result.candidates==null)
            {
                Log.d("Task","No one is identified");
                mResult=0;
                return ;

            }

            if(   result.candidates.size() == 0){
                Log.d("Task","No one is identified");
                mResult=0;
                return ;

            }
            else {
                UUID personId = result.candidates.get(0).personId;
                Person person = null;
                try {

                    person = mFaceServiceRestClient.getPerson(personGroupID,personId);

                } catch (ClientException e) {

                    mResult=0;
                    e.printStackTrace();

                } catch (IOException e) {
                    e.printStackTrace();
                    mResult=0;
                }
                Log.d("Task",person.name + " is identified");
            }
        }

    }

    @Override
    protected Integer doInBackground(Bitmap... params) {
        Bitmap bitmap = params[0];

            IdentifyFace(mPersonGroupID,bitmap);
            return mResult;
    }
    @Override
    protected void onPostExecute(Integer integer) {

        if(mContext instanceof IdentifyListener){
            mIdentifyListener=(IdentifyListener)mContext;
            mIdentifyListener.isVerified(integer);
        }



    }
}
