package com.falatron.api;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiRequestRvc extends AsyncTask<Object, Void, String> {

    private Context context;
    private String apiKey;

    public ApiRequestRvc(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
    }

    @Override
    protected String doInBackground(Object... params) {
        String url = (String) params[0];
        String selectedName = (String) params[1];
        int pitch = (int) params[2];
        File upload = (File) params[3];

        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/octet-stream");
        RequestBody fileBody = RequestBody.create(mediaType, upload);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("voz", selectedName)
                .addFormDataPart("pitch", String.valueOf(pitch))
                .addFormDataPart("upload", upload.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("x-api-key", apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                return "Request failed: " + response.code();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Request failed: " + e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        // Handle the result if necessary
    }
}