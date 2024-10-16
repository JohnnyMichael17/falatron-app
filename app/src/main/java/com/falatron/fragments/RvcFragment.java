package com.falatron.fragments;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static androidx.constraintlayout.widget.ConstraintLayoutStates.TAG;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.falatron.R;
import com.falatron.api.ApiRequestRvc;
import com.falatron.api.ApiRequestTask;
import com.falatron.databinding.FragmentRvcBinding;
import com.falatron.helpers.AlertMessage;
import com.falatron.helpers.VoiceList;
import com.falatron.notification.AudioNotification;
import com.falatron.notification.DownloadNotification;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

public class RvcFragment extends Fragment {

    private FragmentRvcBinding binding;

    private static final int NOTIFICATION_ID = 1;
    private static final int REQUEST_AUDIO_PERMISSIONS = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 200;

    private Boolean verificarFile = false;

    private String base64Audio;
    private MediaPlayer selectedAudio;
    private MediaPlayer generatedAudio;

    private int selectedValue;
    private File upload;

    private ValueAnimator progressAnimator;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = true;

    private boolean isPlaying = false;
    private boolean isMuted = false;
    private int currentProgress;

    private String audioFilePath;

    private final Handler handler = new Handler();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRvcBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
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

        VoiceList voiceList = new VoiceList(
                requireContext(),
                binding.spinnerCategoria,
                binding.spinnerVoz,
                binding.imageModel,
                binding.imageIcLogo,
                binding.txtNome,
                binding.txtAutor,
                binding.txtDublador,
                binding.cardViewModel
        );

        voiceList.choiceVoice("https://falatron.com/static/sovits.json");

        audioCompartilhado();

        /*binding.btnGravarAudio.setOnTouchListener(new View.OnTouchListener() {
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
        });*/

        binding.btnGravarAudio.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_STORAGE_PERMISSION);
                } else {
                    if (isRecording) {
                        binding.btnGravarAudio.setIcon(getResources().getDrawable(R.drawable.baseline_square_24, null));
                        binding.btnGravarAudio.setIconTint(ContextCompat.getColorStateList(requireContext(), R.color.red));
                        binding.btnGravarAudio.setText(R.string.parar_gravacao);
                        startRecording();
                    } else {
                        binding.btnGravarAudio.setIcon(getResources().getDrawable(R.drawable.baseline_mic_24, null));
                        binding.btnGravarAudio.setIconTint(ContextCompat.getColorStateList(requireContext(), R.color.white));
                        binding.btnGravarAudio.setText(R.string.gravar_audio);
                        stopRecording();
                    }
                    isRecording = !isRecording;
                }
            }
        });

        binding.btnEnviarAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String[] storagePermissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), storagePermissions, REQUEST_STORAGE_PERMISSION);
                } else {
                    binding.btnEnviarAudio.setClickable(false);
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                    startActivityForResult(Intent.createChooser(intent, "Selecione um áudio"), 1);
                }
            }
        });

        binding.seekBarPitch.setProgress(13);
        selectedValue = 0;

        binding.seekBarPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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
                gerarAudio(voiceList);
            }
        });

        binding.btnUserPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isPlaying) {
                    selectedAudio.pause();
                    binding.btnUserPlay.setBackgroundResource(R.drawable.bg_play);
                    if (progressAnimator != null && progressAnimator.isRunning()) {
                        progressAnimator.cancel();
                    }
                } else {
                    selectedAudio.start();
                    binding.btnUserPlay.setBackgroundResource(R.drawable.bg_pause);
                    binding.userSeekBar.setMax(selectedAudio.getDuration());

                    updateUserSeekBar();
                }
                selectedAudio.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        binding.btnUserPlay.setBackgroundResource(R.drawable.bg_play);
                        binding.userSeekBar.setProgress(0);
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
                    binding.btnPlay.setBackgroundResource(R.drawable.bg_play);
                    if (progressAnimator != null && progressAnimator.isRunning()) {
                        progressAnimator.cancel();
                    }
                } else {
                    generatedAudio.start();
                    binding.btnPlay.setBackgroundResource(R.drawable.bg_pause);
                    binding.seekBar.setMax(generatedAudio.getDuration());

                    updateSeekBar();
                }
                generatedAudio.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        binding.btnPlay.setBackgroundResource(R.drawable.bg_play);
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

        binding.userSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                binding.btnPlay.setBackgroundResource(R.drawable.bg_pause);
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

        binding.btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verificarFile = true;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        downloadAudio();
                    } else {
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                    }
                } else {
                    downloadAudio();
                }
            }
        });

        binding.btnCompartilhar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareAudio();
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

    private void gerarAudio(VoiceList voiceList) {
        AlertMessage alertMessage = new AlertMessage(requireContext());

        if ("Selecione a categoria...".equals(binding.spinnerCategoria.getSelectedItem().toString())) {
            alertMessage.mostrarAlertaCategoria();
        } else if ("Selecione a voz...".equals(binding.spinnerVoz.getSelectedItem().toString())) {
            alertMessage.mostrarAlertaVoz();
        } else if (binding.cardViewUserAudio.getVisibility() == View.GONE) {
            alertMessage.mostrarAlertaAudio();
        } else if (!voiceList.testInternet()) {
            alertMessage.mostrarAlertaInternet();
        } else {
            binding.btnGerarAudioRvc.setVisibility(View.GONE);
            binding.loadingProgressBar.setVisibility(View.VISIBLE);
            binding.cardViewMP.setVisibility(View.GONE);

            new Thread(new Runnable() {
                @Override
                public void run() {

                    ApiRequestRvc apiPostTask = new ApiRequestRvc(requireActivity(), "7cd85a78a9b1d0355f65005e03dbde36");

                    String responseString;
                    try {
                        responseString = apiPostTask.execute("https://falatron.com/api/app/rvc/", binding.spinnerVoz.getSelectedItem().toString(), selectedValue, upload).get();

                        startPeriodicUpdate(responseString);
                    } catch (ExecutionException | InterruptedException | JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 1 && data != null && data.getData() != null) {
            Uri userAudio = data.getData();

            selectedAudio = new MediaPlayer();

            try {
                selectedAudio.setDataSource(requireContext(), userAudio);
                selectedAudio.prepare();

                visibilidadeTomDeVoz();
                binding.cardViewUserAudio.setVisibility(View.VISIBLE);
                binding.btnEnviarAudio.setClickable(true);

                upload = saveAudioToCache(userAudio);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            binding.btnEnviarAudio.setClickable(true);
        }
    }

    private File saveAudioToCache(Uri audioUri) throws IOException {
        File cacheFile = new File(requireContext().getCacheDir(), "upload.mp3");

        InputStream inputStream = requireContext().getContentResolver().openInputStream(audioUri);
        OutputStream outputStream = new FileOutputStream(cacheFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        inputStream.close();
        outputStream.close();

        return cacheFile;
    }

    private void audioCompartilhado() {
        Intent intent = requireActivity().getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("audio/")) {

                Toast.makeText(requireContext(), "Áudio recebido", Toast.LENGTH_SHORT).show();

                Uri audioUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                selectedAudio = new MediaPlayer();
                try {
                    selectedAudio.setDataSource(requireContext(), audioUri);
                    selectedAudio.prepare();

                    visibilidadeTomDeVoz();
                    binding.cardViewUserAudio.setVisibility(View.VISIBLE);
                    upload = saveAudioToCache(audioUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startRecording() {
        audioFilePath = requireContext().getExternalCacheDir().getAbsolutePath() + "/upload.mp3";

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

            selectedAudio = new MediaPlayer();

            try {
                selectedAudio.setDataSource(audioFilePath);
                selectedAudio.prepare();

                visibilidadeTomDeVoz();
                binding.cardViewUserAudio.setVisibility(View.VISIBLE);

                Uri audioUri = Uri.fromFile(new File(audioFilePath));
                upload = saveAudioToCache(audioUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Toast.makeText(requireContext(), "Gravação finalizada", Toast.LENGTH_SHORT).show();
        }
    }

    private void visibilidadeTomDeVoz() {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.btnGerarAudioRvc.getLayoutParams();
        params.topMargin = 50;
        binding.btnGerarAudioRvc.setLayoutParams(params);

        if (binding.txtTom.getVisibility() == View.GONE) {
            Toast.makeText(requireActivity().getApplicationContext(), "Áudio selecionado", Toast.LENGTH_SHORT).show();

            binding.txtTom.setVisibility(View.VISIBLE);
            binding.seekBarPitch.setVisibility(View.VISIBLE);
            binding.txtValor.setVisibility(View.VISIBLE);
        }
    }

    private void downloadAudio() {
        try {
            byte[] audioData = Base64.decode(base64Audio, Base64.DEFAULT);

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            //String fileName = text_box.getText().toString().toLowerCase().replace(" ", "_") + ".mp3";
            File file = new File(path, "download.mp3");

            final NotificationManager notificationManager = (NotificationManager) requireActivity().getSystemService(Context.NOTIFICATION_SERVICE);

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
                            Toast.makeText(requireActivity().getApplicationContext(), "Áudio baixado com sucesso!", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireActivity().getApplicationContext(), "Áudio baixado com sucesso!", Toast.LENGTH_SHORT).show();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shareAudio() {
        File tempAudioFile = new File(requireActivity().getFilesDir(), "temp_audio.mp3");
        Uri audioUri = FileProvider.getUriForFile(requireContext(), requireActivity().getPackageName() + ".fileprovider", tempAudioFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Compartilhar via"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSIONS:
                boolean permissionToRecordAccepted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (permissionToRecordAccepted) {
                    startRecording();
                } else {
                    //alertMessage.mostrarAlertaDePermissao();
                }
                break;

            case REQUEST_STORAGE_PERMISSION:
                boolean permissionToReadAccepted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean permissionToWriteAccepted = grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (permissionToReadAccepted && permissionToWriteAccepted) {
                    if (verificarFile) {
                        downloadAudio();
                    } else {
                        binding.btnEnviarAudio.setClickable(true);
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("audio/*");
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                        startActivityForResult(Intent.createChooser(intent, "Selecione um áudio"), 1);
                    }
                } else {
                    //alertMessage.mostrarAlertaDePermissao();
                }
                break;
        }
    }

    private void startPeriodicUpdate(String responseString) throws JSONException {
        String taskId = new JSONObject(responseString).getString("task_id");

        try {
            getQueue(taskId);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    getQueue(taskId);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                handler.postDelayed(this, 5000);
            }
        }, 5000);
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

        requireActivity().runOnUiThread(new Runnable() {
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

    private void updateUserSeekBar() {
        if (selectedAudio != null) {
            final int maxProgress = selectedAudio.getDuration();

            if (selectedAudio.isPlaying()) {
                final int currentProgress = selectedAudio.getCurrentPosition();
                binding.userSeekBar.setProgress(currentProgress);

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
                        binding.userSeekBar.setProgress(progress);
                    }
                });
                progressAnimator.start();

                binding.userSeekBar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        binding.userSeekBar.setProgress(currentProgress);
                    }
                }, 500);

            } else {
                if (progressAnimator != null && progressAnimator.isRunning())
                    progressAnimator.cancel();

                currentProgress = selectedAudio.getCurrentPosition();
                binding.userSeekBar.setProgress(currentProgress);
            }
        }
    }

    private void updateSeekBar() {
        if (generatedAudio != null) {
            final int maxProgress = generatedAudio.getDuration();

            if (generatedAudio.isPlaying()) {
                final int currentProgress = generatedAudio.getCurrentPosition();
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
                if (progressAnimator != null && progressAnimator.isRunning())
                    progressAnimator.cancel();

                currentProgress = generatedAudio.getCurrentPosition();
                binding.seekBar.setProgress(currentProgress);
            }
        }
    }

    private void makeAudio(String voiceValue) {
        Animation animationCard = AnimationUtils.loadAnimation(requireActivity(), R.anim.animation_card);
        base64Audio = voiceValue;

        new Thread(new Runnable() {
            @Override
            public void run() {
                final byte[] audioBytes = Base64.decode(voiceValue, Base64.DEFAULT);

                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.cardViewMP.startAnimation(animationCard);
                        binding.cardViewMP.setVisibility(View.VISIBLE);

                        try {
                            FileOutputStream fos = requireActivity().openFileOutput("temp_audio.mp3", MODE_PRIVATE);
                            fos.write(audioBytes);
                            fos.close();

                            generatedAudio = new MediaPlayer();
                            generatedAudio.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            generatedAudio.setDataSource(requireActivity().getFilesDir() + "/temp_audio.mp3");

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

                            binding.teste.post(new Runnable() {
                                @Override
                                public void run() {
                                    binding.scrollView.fullScroll(View.FOCUS_DOWN);
                                }
                            });

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

        binding = null;
    }

}