package com.falatron.fragments;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static androidx.constraintlayout.widget.ConstraintLayoutStates.TAG;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.falatron.R;
import com.falatron.api.ApiRequestRvc;
import com.falatron.api.ApiRequestTask;
import com.falatron.databinding.FragmentRvcBinding;
import com.falatron.json.JsonDownloader;
import com.falatron.notification.AudioNotification;
import com.falatron.notification.DownloadNotification;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
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

public class RvcFragment extends Fragment {

    private FragmentRvcBinding binding;

    private static final int NOTIFICATION_ID = 1;

    private JSONObject jsonObject;

    private String selectedName;
    private String selectedCategory;
    private String base64Audio;
    private MediaPlayer selectedAudio;
    private MediaPlayer generatedAudio;
    private Uri userAudio;

    private int selectedValue;
    private File upload;

    private ValueAnimator progressAnimator;

    private MediaRecorder mediaRecorder;
    private String outputFile;

    private boolean isPlaying = false;
    private boolean isMuted = false;
    private int currentProgress;

    private Handler handler = new Handler();

    private String audioFilePath;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRvcBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
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
        audioCompartilhado();
        clearCache();

        outputFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/recording.mp3";

        binding.btnGravarAudio.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        binding.btnGravarAudio.setIcon(getResources().getDrawable(R.drawable.baseline_square_24, null));
                        binding.btnGravarAudio.setText("Parar de gravar");
                        startRecording();
                        break;
                    case MotionEvent.ACTION_UP:
                        binding.btnGravarAudio.setIcon(getResources().getDrawable(R.drawable.baseline_mic_24, null));
                        binding.btnGravarAudio.setText("Gravar áudio");
                        stopRecording();
                        break;
                }
                return true;
            }
        });

        binding.btnEnviarAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(Intent.createChooser(intent, "Selecione um áudio"), 1);
            }
        });

        binding.seekBarPitch.setProgress(0);
        selectedValue = 0;

        binding.seekBarPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Converte o progresso para o intervalo desejado de -12 a 12
                selectedValue = progress - 12;
                binding.txtValor.setText("Valor: " + selectedValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Pode ser implementado se necessário
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Pode ser implementado se necessário
            }
        });

        binding.btnGerarAudioRvc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("Selecione a categoria...".equals(selectedCategory)) {
                    mostrarAlertaCategoria();
                } else if ("Selecione a voz...".equals(selectedName)) {
                    mostrarAlertaVoz();
                } else if (!isCacheEmpty()) {
                    mostrarAlertaAudio();
                } else if (!testInternet()) {
                    mostrarAlertaInternet();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            ApiRequestRvc apiPostTask = new ApiRequestRvc(requireActivity(), "7cd85a78a9b1d0355f65005e03dbde36");

                            String responseString;
                            try {
                                responseString = apiPostTask.execute("https://falatron.com/api/app/rvc/", selectedName, selectedValue, upload).get();

                            } catch (ExecutionException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            startPeriodicUpdate(getTaskId(responseString));
                        }
                    }).start();
                }
            }
        });

        binding.userPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isPlaying) {
                    selectedAudio.pause();
                    if (progressAnimator != null && progressAnimator.isRunning()) {
                        progressAnimator.cancel();
                    }
                } else {
                    selectedAudio.start();
                    binding.seekBar.setMax(selectedAudio.getDuration());

                    updateSeekBar();
                }
                selectedAudio.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        binding.seekBar.setProgress(0);
                        currentProgress = 0;
                        if (progressAnimator != null && progressAnimator.isRunning()) {
                            progressAnimator.cancel();
                        }
                        isPlaying = false;

                        selectedAudio.seekTo(0);
                    }
                });

                isPlaying = !isPlaying;
            }
        });

        binding.btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isPlaying) {
                    generatedAudio.pause();
                    binding.btnPlay.setBackgroundResource(R.drawable.bg_play_button);
                    if (progressAnimator != null && progressAnimator.isRunning()) {
                        progressAnimator.cancel();
                    }
                } else {
                    generatedAudio.start();
                    binding.btnPlay.setBackgroundResource(R.drawable.bg_pause_button);
                    binding.seekBar.setMax(generatedAudio.getDuration());

                    updateSeekBar();
                }
                generatedAudio.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        binding.btnPlay.setBackgroundResource(R.drawable.bg_play_button);
                        binding.seekBar.setProgress(0);
                        currentProgress = 0;
                        if (progressAnimator != null && progressAnimator.isRunning()) {
                            progressAnimator.cancel();
                        }
                        isPlaying = false;

                        generatedAudio.seekTo(0);
                    }
                });

                isPlaying = !isPlaying;
            }
        });

        binding.userAudioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    selectedAudio.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                selectedAudio.pause();
                progressAnimator.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                selectedAudio.start();
                updateSeekBar();
                binding.btnPlay.setBackgroundResource(R.drawable.bg_pause_button);
            }
        });

        binding.btnVolumeOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (generatedAudio != null) {
                    if (isMuted) {
                        generatedAudio.setVolume(1.0f, 1.0f);
                        isMuted = false;
                        binding.btnVolumeOn.setBackgroundResource(R.drawable.baseline_volume_up_24);
                    } else {
                        generatedAudio.setVolume(0.0f, 0.0f);
                        isMuted = true;
                        binding.btnVolumeOn.setBackgroundResource(R.drawable.baseline_volume_off_24);
                    }
                }
            }
        });

        //------ Implementação do botão de Downloado do áudio, chama o método downloadAudio, responsável por baixar o áudio no dispositivo ------//
        binding.btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadAudio();
            }
        });

        //------ Implementação do botão de Compartilhar áudio, chama o método shareAudio, responsável por compartilhar o áudio em outros Aplicativos ------//
        binding.btnCompartilhar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareAudio(base64Audio);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Configurar os elementos do toolbar
        ImageButton imageButton = requireActivity().findViewById(R.id.btnRvc);
        Drawable iconDrawable = imageButton.getDrawable();
        iconDrawable.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue), PorterDuff.Mode.SRC_IN);
        imageButton.setImageDrawable(iconDrawable);

        // Configurar as views do toolbar
        View view = requireActivity().findViewById(R.id.view2);
        view.setBackgroundColor(getResources().getColor(R.color.blue, null));
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = 8;
        view.setLayoutParams(params);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Configurar os elementos do toolbar
        ImageButton imageButton = requireActivity().findViewById(R.id.btnRvc);
        Drawable iconDrawable = imageButton.getDrawable();
        iconDrawable.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white), PorterDuff.Mode.SRC_IN);
        imageButton.setImageDrawable(iconDrawable);

        // Configurar as views do toolbar
        View view = requireActivity().findViewById(R.id.view2);
        view.setBackgroundColor(getResources().getColor(R.color.backgroundColor, null));
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = 0;
        view.setLayoutParams(params);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                userAudio = data.getData();
            }

            selectedAudio = new MediaPlayer();

            if (userAudio != null) {
                try {
                    selectedAudio.setDataSource(requireContext(), userAudio);
                    selectedAudio.prepare();
                    upload = createTempAudio(userAudio);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void audioCompartilhado() {
        Intent intent = getActivity().getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("audio/")) {

                Toast.makeText(requireContext(), "Áudio recebido", Toast.LENGTH_SHORT).show();

                Uri audioUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                MediaPlayer audioCompartilhado = new MediaPlayer();
                try {
                    audioCompartilhado.setDataSource(requireContext(), audioUri);
                    audioCompartilhado.prepare();
                    audioCompartilhado.start();

                    upload = createTempAudio(audioUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startRecording() {
        audioFilePath = getActivity().getCacheDir() + "/upload.mp3";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Toast.makeText(requireContext(), "Gravação iniciada", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;

            try {
                Uri audioUri = Uri.fromFile(new File(audioFilePath));
                upload = createTempAudio(audioUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Toast.makeText(requireContext(), "Gravação finalizada", Toast.LENGTH_SHORT).show();
        }
    }

    private File createTempAudio(Uri uri) throws IOException {
        InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Não foi possível abrir InputStream do URI");
        }

        File tempFile = File.createTempFile("audio", ".mp3", getActivity().getCacheDir());
        FileOutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.close();
        inputStream.close();

        return new File(tempFile.getParent(), "upload.mp3");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (selectedAudio != null) {
            selectedAudio.release();
            selectedAudio = null;
        }
        if (generatedAudio != null) {
            generatedAudio.release();
            generatedAudio = null;
        }
    }

    //------ Método para baixar o áudio no Dispositivo do Usuário ------//
    private void downloadAudio() {
        try {
            byte[] audioData = Base64.decode(base64Audio, Base64.DEFAULT);

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            //String fileName = text_box.getText().toString().toLowerCase().replace(" ", "_") + ".mp3";
            File file = new File(path, "download.mp3");

            final NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

            if (file.exists()) {

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
                dialogBuilder.setTitle("Arquivo já existe");
                dialogBuilder.setMessage("Deseja substituir o arquivo existente?");
                dialogBuilder.setPositiveButton("Substituir", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            FileOutputStream outputStream = new FileOutputStream(file);
                            outputStream.write(audioData);
                            outputStream.close();
                            DownloadNotification.showDownloadNotification(requireContext(), file.getAbsolutePath());
                            Toast.makeText(getActivity().getApplicationContext(), "Áudio baixado com sucesso!", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                dialogBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        notificationManager.cancel(NOTIFICATION_ID);
                    }
                });

                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();

            } else {

                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(audioData);
                outputStream.close();
                DownloadNotification.showDownloadNotification(requireContext(), file.getAbsolutePath());
                Toast.makeText(getActivity().getApplicationContext(), "Áudio baixado com sucesso!", Toast.LENGTH_SHORT).show();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //------ Método para compartilhar o áudio com outros Aplicativos ------//
    public void shareAudio(String base64Audio) {
        File tempAudioFile = new File(getActivity().getFilesDir(), "temp_audio.mp3");
        Uri audioUri = FileProvider.getUriForFile(requireContext(), getActivity().getPackageName() + ".fileprovider", tempAudioFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Compartilhar via"));

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

        final String apiUrl = "https://falatron.com/api/app/rvc/" + task_id;

        ApiRequestTask apiRequest = new ApiRequestTask("7cd85a78a9b1d0355f65005e03dbde36");

        try {
            JSONObject jsonResponse = apiRequest.execute(apiUrl).get();

            if (jsonResponse.has("voice")) {
                voiceValue = jsonResponse.getString("voice");
            } else {
                queueGet = jsonResponse.getString("queue");
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao obter resposta da API: " + e.getMessage());
        }

        String finalQueueGet = queueGet;

        /*getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                binding.queue.setText("Seu lugar na fila é: " + finalQueueGet);
                binding.queue.setVisibility(View.VISIBLE);
            }
        });*/

        if (!voiceValue.isEmpty()) {
            apiRequest.cancelRequest();
            makeAudio(voiceValue);
        }
    }

    private void startPeriodicUpdate(String task_id) {
        try {
            getQueue(task_id);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    getQueue(task_id);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                handler.postDelayed(this, 10000);
            }
        }, 10000);
    }

    private void updateSeekBar() {
        if (generatedAudio != null) {
            final int maxProgress = generatedAudio.getDuration();

            if (generatedAudio.isPlaying()) {
                final int currentProgress = generatedAudio.getCurrentPosition();
                binding.userAudioSeekBar.setProgress(currentProgress);

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
                        binding.userAudioSeekBar.setProgress(progress);
                    }
                });

                progressAnimator.start();

                binding.userAudioSeekBar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        binding.userAudioSeekBar.setProgress(currentProgress);
                    }
                }, 500);

            } else {

                if (progressAnimator != null && progressAnimator.isRunning())
                    progressAnimator.cancel();

                currentProgress = generatedAudio.getCurrentPosition();
                binding.userAudioSeekBar.setProgress(currentProgress);

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

    //------ Método para verificar se tem áudio na memório cache ------//
    private boolean isCacheEmpty() {
        File cacheDir = getActivity().getCacheDir();

        File[] files = cacheDir.listFiles();

        return files == null || files.length == 0;
    }

    private void clearCache() {
        File cacheDir = getActivity().getCacheDir();
        deleteDir(cacheDir);
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    //------ Caixa de Dialogo caso o usuário não escolher uma Voz ------//
    private void mostrarAlertaAudio() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ops!");
        builder.setMessage("Por favor envie ou grave um áudio.");
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

    private void makeAudio(String voiceValue) {

        Animation animationCard = AnimationUtils.loadAnimation(requireActivity(), R.anim.animation_card);
        base64Audio = voiceValue;

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

                            generatedAudio = new MediaPlayer();
                            generatedAudio.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            generatedAudio.setDataSource(getActivity().getFilesDir() + "/temp_audio.mp3");

                            generatedAudio.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    binding.loadingProgressBar.setVisibility(View.GONE);
                                    binding.txtQueue.setVisibility(View.GONE);
                                    binding.btnGerarAudioRvc.startAnimation(animationCard);
                                    binding.btnGerarAudioRvc.setVisibility(View.VISIBLE);
                                    binding.seekBar.setMax(generatedAudio.getDuration());

                                    AudioNotification.showAudioNotification(requireActivity());
                                }
                            });

                            /*scrollView.post(new Runnable() {
                                @Override
                                public void run() {
                                    scrollView.fullScroll(View.FOCUS_DOWN);
                                }
                            });*/

                            generatedAudio.prepareAsync();
                            handler.removeCallbacksAndMessages(null);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();
    }

    private void choiceVoice() {
        try {
            if (testInternet()) {
                jsonObject = new JSONObject(downloadJson("https://falatron.com/static/sovits.json"));
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

                    ImageView imageModel = binding.imageModel;
                    TextView txtNome = binding.txtNome;
                    TextView txtAutor = binding.txtAutor;
                    TextView txtDublador = binding.txtDublador;

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

                                txtNome.setText(selectedName);
                                txtAutor.setText(author);

                                if (!dublador.isEmpty()) {
                                    txtDublador.setText(dublador);
                                    binding.lblDublador.setVisibility(View.VISIBLE);
                                    binding.txtDublador.setVisibility(View.VISIBLE);
                                } else {
                                    binding.lblDublador.setVisibility(View.GONE);
                                    binding.txtDublador.setVisibility(View.GONE);
                                }

                                binding.cardModel.setVisibility(View.VISIBLE);

                                if (image.isEmpty()) {
                                    binding.imgIcLogo.setVisibility(View.VISIBLE);
                                } else {
                                    binding.imgIcLogo.setVisibility(View.GONE);

                                    Glide.with(RvcFragment.this)
                                            .load(image)
                                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                            .into(imageModel);
                                }
                            } else {
                                binding.cardModel.setVisibility(View.GONE);
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

}