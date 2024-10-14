package com.falatron.fragments;

import static android.content.Context.MODE_PRIVATE;
import static androidx.constraintlayout.widget.ConstraintLayoutStates.TAG;

import android.animation.ValueAnimator;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
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
import android.widget.ImageButton;
import android.widget.SeekBar;

import com.falatron.helpers.AlertMessage;
import com.falatron.R;
import com.falatron.helpers.VoiceList;
import com.falatron.api.ApiRequest;
import com.falatron.api.ApiRequestTask;
import com.falatron.databinding.FragmentTtsBinding;
import com.falatron.notification.AudioNotification;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.util.concurrent.ExecutionException;

public class TtsFragment extends Fragment {

    private FragmentTtsBinding binding;

    private VoiceList voiceList;

    private MediaPlayer mediaPlayer;
    private ValueAnimator progressAnimator;

    private boolean isPlaying = false;
    private boolean isMuted = false;
    private int currentProgress;

    private Handler handler = new Handler();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        voiceList = new VoiceList(
                this,
                requireContext(),
                "https://falatron.com/static/models.json",
                binding.spinnerCategoria,
                binding.spinnerVoz,
                binding.imageModel,
                binding.txtNome,
                binding.txtAutor,
                binding.txtDublador,
                binding.cardViewModel
        );

        voiceList.choiceVoice();

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

        binding.btnGerarAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gerarAudio();
            }
        });

        binding.btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isPlaying) {
                    mediaPlayer.pause();
                    binding.btnPlay.setBackgroundResource(R.drawable.bg_play);
                    if (progressAnimator != null && progressAnimator.isRunning()) {
                        progressAnimator.cancel();
                    }
                } else {
                    mediaPlayer.start();
                    binding.btnPlay.setBackgroundResource(R.drawable.bg_pause);
                    binding.seekBar.setMax(mediaPlayer.getDuration());

                    updateSeekBar();
                }
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        binding.btnPlay.setBackgroundResource(R.drawable.bg_play);
                        binding.seekBar.setProgress(0);
                        currentProgress = 0;
                        if (progressAnimator != null && progressAnimator.isRunning()) {
                            progressAnimator.cancel();
                        }
                        isPlaying = false;

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
                binding.btnPlay.setBackgroundResource(R.drawable.bg_pause);
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

        // Configurar os elementos do toolbar
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

    private void gerarAudio() {
        AlertMessage alertMessage = new AlertMessage(requireContext());

        if ("Selecione a categoria...".equals(binding.spinnerCategoria.getSelectedItem().toString())) {
            alertMessage.mostrarAlertaCategoria();
        } else if ("Selecione a voz...".equals(binding.spinnerVoz.getSelectedItem().toString())) {
            alertMessage.mostrarAlertaVoz();
        } else if (binding.edtInsiraTexto.getText().toString().length() < 5) {
            alertMessage.mostrarAlertaTexto();
        } else if (!voiceList.testInternet()) {
            alertMessage.mostrarAlertaInternet();
        } else {
            binding.btnGerarAudio.setVisibility(View.GONE);
            binding.loadingProgressBar.setVisibility(View.VISIBLE);
            binding.cardViewMP.setVisibility(View.GONE);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ApiRequest apiPostTask = new ApiRequest(requireContext(), "7cd85a78a9b1d0355f65005e03dbde36");

                    String responseString;
                    try {
                        responseString = apiPostTask.execute("https://falatron.com/api/app", convertJsonString(binding.spinnerVoz.getSelectedItem().toString(), binding.edtInsiraTexto.getText().toString())).get();
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    startPeriodicUpdate(getTaskId(responseString));
                }
            }).start();
        }
    }

    public String convertJsonString(String chave1, String chave2) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("voz", chave1);
            jsonObject.put("texto", chave2);

            String jsonString = jsonObject.toString();

            Log.d("JSON Criado", jsonString);

            return jsonString;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getTaskId(String responseString) {
        try {
            return new JSONObject(responseString).getString("task_id");
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
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void makeAudio(String voiceValue) {
        Animation animationCard = AnimationUtils.loadAnimation(requireActivity(), R.anim.animation_card);

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

                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.setDataSource(requireActivity().getFilesDir() + "/temp_audio.mp3");

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

                            binding.scrollCardView.post(new Runnable() {
                                @Override
                                public void run() {
                                    binding.scrollView.fullScroll(View.FOCUS_DOWN);
                                }
                            });

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
                    public void onAnimationUpdate(@NonNull ValueAnimator animation) {
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
                currentProgress = mediaPlayer.getCurrentPosition();
                binding.seekBar.setProgress(currentProgress);

            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}