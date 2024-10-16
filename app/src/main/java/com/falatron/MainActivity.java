package com.falatron;

import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;

import com.falatron.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private static final int PERMISSION_REQUEST_CODE = 200;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setTheme(R.style.Theme_Falatron);
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar.getRoot());

        solicitarPermissoes();

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this);
        binding.viewPager.setAdapter(viewPagerAdapter);

        binding.toolbar.btnTts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.viewPager.setCurrentItem(0);
            }
        });

        binding.toolbar.btnRvc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.viewPager.setCurrentItem(1);
            }
        });

        binding.toolbar.btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.viewPager.setCurrentItem(2);
            }
        });

        binding.cardTermos.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("Minhas preferências", Context.MODE_PRIVATE);
        boolean userAlreadyPressedOk = sharedPreferences.getBoolean("userPressedOk", false);

        if (userAlreadyPressedOk) {
            binding.cardTermos.setVisibility(View.GONE);
        } else {
            binding.btnOk.setEnabled(false);
            CountDownTimer timer = new CountDownTimer(10 * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsLeft = (int) (millisUntilFinished / 1000);
                    String message = "Aguarde " + secondsLeft + " segundos e pressione OK para utilizar o site.";
                    binding.txtPermissao.setText(message);
                }

                @Override
                public void onFinish() {
                    String message = "\n" + "Pronto, pressione OK.";
                    binding.txtPermissao.setText(message);
                    binding.btnOk.setEnabled(true);
                }
            };
            timer.start();
        }

        binding.btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("userPressedOk", true);
                editor.apply();

                binding.cardTermos.setVisibility(View.GONE);
            }
        });
    }

    private void solicitarPermissoes() {
        String[] permissions = {
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.VIBRATE,
                Manifest.permission.RECORD_AUDIO
        };

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

}