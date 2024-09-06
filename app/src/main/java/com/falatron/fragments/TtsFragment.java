package com.falatron.fragments;

import static android.content.Context.MODE_PRIVATE;
import static androidx.constraintlayout.widget.ConstraintLayoutStates.TAG;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.falatron.R;
import com.falatron.api.ApiRequest;
import com.falatron.api.ApiRequestTask;
import com.falatron.databinding.FragmentTtsBinding;
import com.falatron.json.JsonDownloader;
import com.falatron.notification.AudioNotification;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TtsFragment extends Fragment {

    private JSONObject jsonObject;

    private String selectedName;
    private String selectedCategory;
    private MediaPlayer mediaPlayer;
    private ValueAnimator progressAnimator;

    private boolean isPlaying = false;
    private boolean isMuted = false;
    private int currentProgress;

    private Handler handler = new Handler();

    private FragmentTtsBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTtsBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //------ Colocando anúncio/AD ------//
        AdView adView1 = new AdView(requireContext());
        adView1.setAdSize(AdSize.BANNER);
        adView1.setAdUnitId("ca-app-pub-7015560586203687/3126600134");

        AdView adView2 = new AdView(requireContext());
        adView2.setAdSize(AdSize.BANNER);
        adView2.setAdUnitId("ca-app-pub-7015560586203687/9237600961");

        binding.anuncio01.addView(adView1);
        binding.anuncio02.addView(adView2);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView1.loadAd(adRequest);

        AdRequest adRequest2 = new AdRequest.Builder().build();
        adView2.loadAd(adRequest2);

        choiceVoice();

        binding.edtInsiraTexto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int charCount = s.length();
                binding.txtContadorDeCaracteres.setText(charCount + " / 300");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        //------ Implementação do botão de Gerar àudio ------//
        //------ Esse botão é responsável por acionar a Api de Gerar a voz, ele usa o método performApiRequest para fazer a requisição ------//
        //------ Esse botão também é responsável por mostrar alguns Alertas para o usuário ------//
        binding.btnGerarAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if ("Selecione a categoria...".equals(selectedCategory)) {
                    mostrarAlertaCategoria();
                } else if ("Selecione a voz...".equals(selectedName)) {
                    mostrarAlertaVoz();
                } else if (binding.edtInsiraTexto.getText().toString().length() < 5) {
                    mostrarAlertaTexto();
                } else if (!testInternet()) {
                    mostrarAlertaInternet();
                } else {
                    binding.btnGerarAudio.setVisibility(View.GONE);
                    //loadingProgressBar.setVisibility(View.VISIBLE);
                    binding.cardMediaPlayer.setVisibility(View.GONE);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ApiRequest apiPostTask = new ApiRequest(requireContext(), "7cd85a78a9b1d0355f65005e03dbde36");

                            String responseString;
                            try {
                                responseString = apiPostTask.execute("https://falatron.com/api/app", convertJsonString(selectedName, binding.edtInsiraTexto.getText().toString())).get();
                            } catch (ExecutionException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            startPeriodicUpdate(getTaskId(responseString));
                        }
                    }).start();
                }
            }
        });

        binding.btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isPlaying) {
                    // Pausar a reprodução
                    mediaPlayer.pause();
                    binding.btnPlay.setBackgroundResource(R.drawable.bg_play_button);
                    if (progressAnimator != null && progressAnimator.isRunning()) {
                        progressAnimator.cancel();
                    }
                } else {
                    // Iniciar a reprodução
                    mediaPlayer.start();
                    binding.btnPlay.setBackgroundResource(R.drawable.bg_pause_button);
                    binding.seekBar.setMax(mediaPlayer.getDuration());

                    updateSeekBar();
                }
                // Faz o botão de play/pause e a Seek Bar reiniciar após o término do áudio
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        binding.btnPlay.setBackgroundResource(R.drawable.bg_play_button);
                        binding.seekBar.setProgress(0);
                        currentProgress = 0;
                        if (progressAnimator != null && progressAnimator.isRunning()) {
                            progressAnimator.cancel();
                        }
                        isPlaying = false;

                        // Reinicie a reprodução do áudio
                        mediaPlayer.seekTo(0);
                    }
                });

                isPlaying = !isPlaying;
            }
        });

        // Atualiza a barra de progressão do áudio (seekBar)
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Se o usuário moveu a SeekBar, atualize a posição do áudio
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Callback quando o usuário toca na SeekBar
                mediaPlayer.pause();
                progressAnimator.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Callback quando o usuário solta a SeekBar
                mediaPlayer.start();
                updateSeekBar();
                binding.btnPlay.setBackgroundResource(R.drawable.bg_pause_button);
            }
        });

        binding.btnVolumeOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (isMuted) {
                        mediaPlayer.setVolume(1.0f, 1.0f);
                        isMuted = false;
                        binding.btnVolumeOn.setBackgroundResource(R.drawable.baseline_volume_up_24);
                    } else {
                        mediaPlayer.setVolume(0.0f, 0.0f);
                        isMuted = true;
                        binding.btnVolumeOn.setBackgroundResource(R.drawable.baseline_volume_off_24);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ImageButton imageButton = requireActivity().findViewById(R.id.btnTts);
        Drawable iconDrawable = imageButton.getDrawable();
        iconDrawable.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue), PorterDuff.Mode.SRC_IN);
        imageButton.setImageDrawable(iconDrawable);

        // Configurar as views do toolbar
        View view = requireActivity().findViewById(R.id.view);
        view.setBackgroundColor(getResources().getColor(R.color.blue, null));
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = 8;
        view.setLayoutParams(params);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Configurar os elementos do toolbar
        ImageButton imageButton = requireActivity().findViewById(R.id.btnTts);
        Drawable iconDrawable = imageButton.getDrawable();
        iconDrawable.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white), PorterDuff.Mode.SRC_IN);
        imageButton.setImageDrawable(iconDrawable);

        // Configurar as views do toolbar
        View view = requireActivity().findViewById(R.id.view);
        view.setBackgroundColor(getResources().getColor(R.color.backgroundColor, null));
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = 0;
        view.setLayoutParams(params);
    }

    private void choiceVoice() {
        try {
            if (testInternet()) {
                jsonObject = new JSONObject(downloadJson("https://falatron.com/static/models.json"));
                updateJsonInAssets(requireContext(), "models", jsonObject);
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

            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireActivity(), R.layout.spinner_categoria_text, categoriesList);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerCategoria.setAdapter(categoryAdapter);

            binding.spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                    selectedCategory = categoriesList.get(position);
                    List<String> filteredNamesList = new ArrayList<>();
                    filteredNamesList.add("Selecione a voz...");

                    if (selectedCategory != "Todas as vozes") {

                        for (int i = 0; i < modelsArray.length(); i++) {
                            try {
                                JSONObject modelObject = modelsArray.getJSONObject(i);
                                String category = modelObject.getString("category");
                                String name = modelObject.getString("name");

                                if (category.equals(selectedCategory)) {
                                    filteredNamesList.add(name);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    } else {
                        for (int i = 0; i < modelsArray.length(); i++) {
                            try {
                                JSONObject modelObject = modelsArray.getJSONObject(i);
                                String name = modelObject.getString("name");

                                filteredNamesList.add(name);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    Collections.sort(filteredNamesList.subList(1, filteredNamesList.size()));

                    ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(requireActivity(), R.layout.spinner_voz_text, filteredNamesList);
                    nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.spinnerVoz.setAdapter(nameAdapter);

                    ImageView imageModel = binding.imgModel;
                    TextView nameModel = binding.txtNome;
                    TextView authorModel = binding.txtAutor;
                    TextView dubladorModel = binding.txtDublador;

                    binding.spinnerVoz.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                            selectedName = (String) parent.getItemAtPosition(position);

                            JSONObject selectedModel = null;

                            for (int i = 0; i < modelsArray.length(); i++) {
                                try {
                                    JSONObject modelObject = modelsArray.getJSONObject(i);
                                    String name = modelObject.getString("name");
                                    if (name.equals(selectedName)) {
                                        selectedModel = modelObject;
                                        break;
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (selectedModel != null) {

                                String image = selectedModel.optString("image");
                                String author = selectedModel.optString("author");
                                String dublador = selectedModel.optString("dublador");

                                nameModel.setText(selectedName);
                                authorModel.setText(author);

                                if (!dublador.isEmpty()) {
                                    dubladorModel.setText(dublador);
                                    binding.txtDublador.setVisibility(View.VISIBLE);
                                    binding.lblDublador.setVisibility(View.VISIBLE);
                                } else {
                                    binding.txtDublador.setVisibility(View.GONE);
                                    binding.lblAutor.setVisibility(View.GONE);
                                }

                                binding.cardVoz.setVisibility(View.VISIBLE);

                                if (image.isEmpty()) {
                                    binding.imgIcLogo.setVisibility(View.VISIBLE);
                                } else {
                                    binding.imgIcLogo.setVisibility(View.GONE);

                                    Glide.with(TtsFragment.this)
                                            .load(image)
                                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                            .into(imageModel);
                                }
                            } else {
                                binding.cardVoz.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String downloadJson(String url) {
        try {
            // Execute a tarefa para baixar o JSON
            return new JsonDownloader().execute(url).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject updateJsonInAssets(Context context, String assetFileName, JSONObject updatedJsonObject) {
        try {
            // Abra um InputStream para o arquivo JSON original no diretório "assets"
            InputStream inputStream = context.getAssets().open(assetFileName);

            // Crie um OutputStream para a nova cópia do arquivo JSON
            OutputStream outputStream = context.openFileOutput("temp.json", Context.MODE_PRIVATE);

            // Faça a cópia do arquivo original para o novo arquivo
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // Feche os streams
            inputStream.close();
            outputStream.close();

            // Leia o JSON do arquivo recém-criado
            InputStream updatedInputStream = context.openFileInput("temp.json");
            InputStreamReader inputStreamReader = new InputStreamReader(updatedInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String originalJson = stringBuilder.toString();

            // Atualize o JSON conforme necessário (por exemplo, usando bibliotecas JSON)
            JSONObject originalJsonObject = new JSONObject(originalJson);
            // Atualize o originalJsonObject com os dados de updatedJsonObject

            // Salve o JSON atualizado no arquivo original na pasta "assets"
            OutputStream assetOutputStream = context.getAssets().openFd(assetFileName).createOutputStream();
            assetOutputStream.write(originalJsonObject.toString().getBytes());
            assetOutputStream.close();

            // Exclua o arquivo temporário
            context.deleteFile("temp.json");

            return originalJsonObject;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String loadJSONFromAsset() {
        String json;

        try {
            InputStream inputStream = requireActivity().getAssets().open("models.json");
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

    public String convertJsonString(String chave1, String chave2) {
        try {
            // Criar um objeto JSON com as chaves e valores desejados
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("voz", chave1);
            jsonObject.put("texto", chave2);

            // Obter a representação do objeto JSON como uma string

            String jsonString = jsonObject.toString();

            // Aqui você pode usar o jsonString conforme necessário
            Log.d("JSON Criado", jsonString);

            return jsonString;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getTaskId(String responseString) {
        try {
            JSONObject jsonResponse = new JSONObject(responseString);

            String queue = jsonResponse.getString("queue");
            String task_id = jsonResponse.getString("task_id");

            return task_id;
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao fazer parsing do JSON ou executar a segunda solicitação: " + e.getMessage());
        }
        return "Erro";
    }

    private void getQueue(final String task_id) throws ExecutionException, InterruptedException {
        String voiceValue = "";
        String queueGet = "0";

        final String apiUrl = "https://falatron.com/api/app/" + task_id;

        ApiRequestTask apiRequest = new ApiRequestTask("7cd85a78a9b1d0355f65005e03dbde36");

        try {
            JSONObject jsonResponse = apiRequest.execute(apiUrl).get();
            // Trate a resposta String aqui

            if (jsonResponse.has("voice")) {
                voiceValue = jsonResponse.getString("voice");
            } else {
                queueGet = jsonResponse.getString("queue");
            }
            Log.d(TAG, "Resposta da API: " + jsonResponse);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao obter resposta da API: " + e.getMessage());
        }

        String finalQueueGet = queueGet;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                binding.txtQueue.setText("Seu lugar na fila é: " + finalQueueGet);

                binding.txtQueue.setVisibility(View.VISIBLE);
            }
        });

        if (!voiceValue.isEmpty()) {
            apiRequest.cancelRequest();
            makeAudio(voiceValue);
        }
    }

    private void startPeriodicUpdate(String task_id) {
        // Executa a primeira solicitação imediatamente
        try {
            getQueue(task_id);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Agenda a execução periódica a cada 10 segundos
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    getQueue(task_id);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Repete a atualização a cada 10 segundos
                handler.postDelayed(this, 10000);
            }
        }, 10000);
    }

    private void makeAudio(String voiceValue) {

        Animation animationCard = AnimationUtils.loadAnimation(requireActivity(), R.anim.animation_card);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final byte[] audioBytes = Base64.decode(voiceValue, Base64.DEFAULT);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.cardMP.startAnimation(animationCard);
                        binding.cardMP.setVisibility(View.VISIBLE);

                        try {
                            FileOutputStream fos = getActivity().openFileOutput("temp_audio.mp3", MODE_PRIVATE);
                            fos.write(audioBytes);
                            fos.close();

                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.setDataSource(getActivity().getFilesDir() + "/temp_audio.mp3");

                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    binding.loadingProgressBar.setVisibility(View.GONE);
                                    binding.txtQueue.setVisibility(View.GONE);
                                    binding.btnGerarAudio.startAnimation(animationCard);
                                    binding.btnGerarAudio.setVisibility(View.VISIBLE);
                                    binding.seekBar.setMax(mediaPlayer.getDuration());

                                    AudioNotification.showAudioNotification(requireActivity());
                                }
                            });

                            /*scrollView.post(new Runnable() {
                                @Override
                                public void run() {
                                    scrollView.fullScroll(View.FOCUS_DOWN);
                                }
                            });*/

                            mediaPlayer.prepareAsync();
                            handler.removeCallbacksAndMessages(null);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();
    }

    private void updateSeekBar() {
        if (mediaPlayer != null) {
            final int maxProgress = mediaPlayer.getDuration();

            if (mediaPlayer.isPlaying()) {
                final int currentProgress = mediaPlayer.getCurrentPosition();
                binding.seekBar.setProgress(currentProgress);

                if (progressAnimator != null && progressAnimator.isRunning()) {
                    progressAnimator.cancel();
                }

                progressAnimator = ValueAnimator.ofInt(currentProgress, maxProgress);
                progressAnimator.setDuration(maxProgress - currentProgress);
                progressAnimator.setInterpolator(new LinearInterpolator());

                progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int progress = (int) animation.getAnimatedValue();
                        binding.seekBar.setProgress(progress);
                    }
                });

                progressAnimator.start();

                binding.seekBar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        binding.seekBar.setProgress(currentProgress);
                    }
                }, 500);
            } else {
                if (progressAnimator != null && progressAnimator.isRunning()) {
                    progressAnimator.cancel();
                }
                // Armazene a posição atual do áudio quando for pausado
                currentProgress = mediaPlayer.getCurrentPosition();
                binding.seekBar.setProgress(currentProgress);

            }
        }
    }

    //------ Caixa de Dialogo caso o usuário não escolher uma Categoria ------//
    private void mostrarAlertaCategoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ops!");
        builder.setMessage("Por favor selecione uma categoria.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //------ Caixa de Dialogo caso o usuário não escolher uma Voz ------//
    private void mostrarAlertaVoz() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ops!");
        builder.setMessage("Por favor selecione uma voz.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //------ Caixa de Dialogo caso o usuário não digitar uma Texto ------//
    private void mostrarAlertaTexto() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ops!");
        builder.setMessage("Por favor digite um texto entre 5 e 300 caracteres.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //------ Método para verificar a conectividade de internet ------//
    private boolean testInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    //------ Caixa de Dialogo caso o usuário não esteja conectado à internet ------//
    private void mostrarAlertaInternet() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sem conexão com a Internet");
        builder.setMessage("Ative a conexão de internet para continuar.");

        builder.setPositiveButton("Ativar Internet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Abra as configurações de rede para que o usuário possa ativar a internet
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

}