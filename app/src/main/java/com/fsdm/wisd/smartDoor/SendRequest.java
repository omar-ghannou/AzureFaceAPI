package com.fsdm.wisd.smartDoor;
/*
 **    *** AzureFaceAPI ***
 **   Created by EL KHARROUBI HASSAN
 **   At Sunday May 2021 23H 13MIN
 */


import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class SendRequest {

private Context mContext;
    public SendRequest(Context context){
mContext = context;
    }
    public void openDoor(String open) {
        // url to post our data
        String url = "https://readbeforespeak.000webhostapp.com/read.php";

        RequestQueue queue = Volley.newRequestQueue(mContext);

        StringRequest request = new StringRequest(Request.Method.POST, url, new com.android.volley.Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();

                params.put("open", open);

                return params;
            }
        };

        queue.add(request);
    }

}
