package com.falatron.api;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiRequest extends AsyncTask<String, Void, String> {
    private static final String TAG = "ApiRequest";

    private static final int CONNECTION_TIMEOUT = 100000;

    private OnPostExecuteListener listener;
    private ProgressBar progressBar;
    private Context context;
    private String apiKey;

    public interface OnPostExecuteListener {
        void onPostExecute(String responseString);
    }

    public void setOnPostExecuteListener(OnPostExecuteListener listener) {
        this.listener = listener;
    }

    public ApiRequest(Context context, ProgressBar progressBar, String apiKey) {
        this.context = context;
        this.progressBar = progressBar;
        this.apiKey = apiKey;
    }

    public ApiRequest(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
    }

    @Override
    protected String doInBackground(String... params) {
        if (!verificaConexaoDeRede()) {
            return null;
        }

        String url = params[0];
        String jsonString = params[1];
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String responseString = null;

        try {
            URL apiUrl = new URL(url);
            urlConnection = (HttpURLConnection) apiUrl.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("x-api-key", apiKey);
            urlConnection.setDoOutput(true);

            urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
            urlConnection.setReadTimeout(CONNECTION_TIMEOUT);

            OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
            writer.write(jsonString);
            writer.flush();

            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();

            if (inputStream == null) {
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                return null;
            }

            responseString = buffer.toString();

        } catch (IOException e) {
            if (e instanceof java.net.SocketTimeoutException) {
                Log.e(TAG, "Tempo limite excedido durante a requisição à API: " + e.getMessage());
            } else {
                Log.e(TAG, "Erro durante a requisição à API: " + e.getMessage());
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar o BufferedReader: " + e.getMessage());
                    Toast.makeText(context, "Erro de rede ou sem conexão de internet", Toast.LENGTH_SHORT).show();
                }
            }
        }

        return responseString;
    }

    @Override
    protected void onPostExecute(String responseString) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (responseString == null) {
            Toast.makeText(context, "Erro de rede ou sem conexão de internet", Toast.LENGTH_SHORT).show();
        } else if (listener != null) {
            listener.onPostExecute(responseString);
        }
    }

    private boolean verificaConexaoDeRede() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}