package com.falatron.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.falatron.R;
import com.falatron.json.JsonDownloader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VoiceList {
    private Context context;
    private Spinner spinnerCategoria, spinnerVoz;
    private ImageView imageModel, imageIcLogo;
    private TextView nameModel, authorModel, dubladorModel;
    private String selectedCategory, selectedName;
    private CardView cardVoz;
    private JSONObject jsonObject;

    public VoiceList(Context context, Spinner spinnerCategoria, Spinner spinnerVoz,
                     ImageView imageModel, ImageView imageIcLogo, TextView nameModel, TextView authorModel, TextView dubladorModel, CardView cardVoz) {

        this.context = context;
        this.spinnerCategoria = spinnerCategoria;
        this.spinnerVoz = spinnerVoz;
        this.imageModel = imageModel;
        this.imageIcLogo = imageIcLogo;
        this.nameModel = nameModel;
        this.authorModel = authorModel;
        this.dubladorModel = dubladorModel;
        this.cardVoz = cardVoz;
    }

    public void choiceVoice(String url) {
        try {
            if (testInternet()) {
                jsonObject = new JSONObject(downloadJson(url));
                updateJsonInAssets(context);
            } else {
                jsonObject = new JSONObject(loadJSONFromAsset());
            }

            JSONArray modelsArray = jsonObject.getJSONArray("models");

            List<String> categoriesList = new ArrayList<>();
            List<String> namesList = new ArrayList<>();

            categoriesList.add("Selecione a categoria...");
            categoriesList.add("Todas as vozes");

            for (int i = 0; i < modelsArray.length(); i++) {
                JSONObject modelObject = modelsArray.getJSONObject(i);
                String category = modelObject.getString("category");
                String name = modelObject.getString("name");

                if (!categoriesList.contains(category)) {
                    categoriesList.add(category);
                }
                namesList.add(name);
            }

            Collections.sort(categoriesList.subList(2, categoriesList.size()));

            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context, R.layout.spinner_categoria_text, categoriesList);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategoria.setAdapter(categoryAdapter);

            spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedCategory = categoriesList.get(position);
                    updateVoiceSpinner(modelsArray, selectedCategory);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateVoiceSpinner(JSONArray modelsArray, String selectedCategory) {
        List<String> filteredNamesList = new ArrayList<>();
        filteredNamesList.add("Selecione a voz...");

        try {
            for (int i = 0; i < modelsArray.length(); i++) {
                JSONObject modelObject = modelsArray.getJSONObject(i);
                String category = modelObject.getString("category");
                String name = modelObject.getString("name");

                if (selectedCategory.equals("Todas as vozes") || category.equals(selectedCategory)) {
                    filteredNamesList.add(name);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Collections.sort(filteredNamesList.subList(1, filteredNamesList.size()));

        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(context, R.layout.spinner_voz_text, filteredNamesList);
        nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVoz.setAdapter(nameAdapter);

        spinnerVoz.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedName = (String) parent.getItemAtPosition(position);
                updateModelDetails(modelsArray);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateModelDetails(JSONArray modelsArray) {
        try {
            for (int i = 0; i < modelsArray.length(); i++) {
                JSONObject modelObject = modelsArray.getJSONObject(i);
                String name = modelObject.getString("name");

                if (name.equals(selectedName)) {
                    String image = modelObject.optString("image");
                    String author = modelObject.optString("author");
                    String dublador = modelObject.optString("dublador");

                    nameModel.setText(selectedName);
                    authorModel.setText(author);

                    if (!dublador.isEmpty()) {
                        dubladorModel.setText(dublador);
                        dubladorModel.setVisibility(View.VISIBLE);
                    } else {
                        dubladorModel.setVisibility(View.GONE);
                    }

                    if (!image.isEmpty()) {
                        imageIcLogo.setVisibility(View.GONE);

                        Glide.with(context)
                                .load(image)
                                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                .into(imageModel);
                    } else {
                        imageIcLogo.setVisibility(View.VISIBLE);
                    }

                    cardVoz.setVisibility(View.VISIBLE);

                    return;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean testInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private String downloadJson(String url) {
        try {
            return new JsonDownloader().execute(url).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String loadJSONFromAsset() {
        String json;

        try {
            InputStream inputStream = context.getAssets().open("models.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }


    private void updateJsonInAssets(Context context) {
        try {
            InputStream inputStream = context.getAssets().open("models");
            OutputStream outputStream = context.openFileOutput("temp.json", Context.MODE_PRIVATE);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            InputStream updatedInputStream = context.openFileInput("temp.json");
            InputStreamReader inputStreamReader = new InputStreamReader(updatedInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String originalJson = stringBuilder.toString();

            JSONObject originalJsonObject = new JSONObject(originalJson);

            OutputStream assetOutputStream = context.getAssets().openFd("models").createOutputStream();
            assetOutputStream.write(originalJsonObject.toString().getBytes());
            assetOutputStream.close();

            context.deleteFile("temp.json");

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

}