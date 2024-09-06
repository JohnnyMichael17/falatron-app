package com.falatron.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.falatron.R;
import com.falatron.databinding.FragmentMenuBinding;

public class MenuFragment extends Fragment {

    private FragmentMenuBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnDiscord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://discord.com/invite/4npUee2XMk");
            }
        });

        binding.btnTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://twitter.com/falatronoficial");
            }
        });

        binding.btnYouTube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://www.youtube.com/channel/UCfbOooC1RDGxY1s4hTx0geg");
            }
        });

        binding.btnTikTok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://www.tiktok.com/@falatronoficial");
            }
        });

        binding.btnReddit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://www.reddit.com/r/FALATRON/?rdt=59768");
            }
        });

        binding.btnFalatron.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLinkInBrowser("https://falatron.com/");
            }
        });

        binding.btnCompartilhe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                compartilharApp();
            }
        });

        binding.btnAvalie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                avaliarApp(requireActivity());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ImageButton imageButton = requireActivity().findViewById(R.id.btnMenu);
        Drawable iconDrawable = imageButton.getDrawable();
        iconDrawable.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue), PorterDuff.Mode.SRC_IN);
        imageButton.setImageDrawable(iconDrawable);

        // Configurar as views do toolbar
        View view = requireActivity().findViewById(R.id.view3);
        view.setBackgroundColor(getResources().getColor(R.color.blue, null));
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = 8;
        view.setLayoutParams(params);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Configurar os elementos do toolbar
        ImageButton imageButton = requireActivity().findViewById(R.id.btnMenu);
        Drawable iconDrawable = imageButton.getDrawable();
        iconDrawable.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white), PorterDuff.Mode.SRC_IN);
        imageButton.setImageDrawable(iconDrawable);

        // Configurar as views do toolbar
        View view = requireActivity().findViewById(R.id.view3);
        view.setBackgroundColor(getResources().getColor(R.color.backgroundColor, null));
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = 0;
        view.setLayoutParams(params);
    }

    //------ Método para abrir um Link Externo ------//
    private void openLinkInBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void avaliarApp(Context context) {
        Uri uri = Uri.parse("http://play.google.com/store/apps/details?id=" + "com.falatron");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        context.startActivity(intent);
    }

    private void compartilharApp() {
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "\"Envie áudios personalizados com vozes de personagens para seus amigos!\n\n" +
                    "Baixe o Falatron na Play Store: https://play.google.com/store/apps/details?id=" + getActivity().getPackageName());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Compartilhar via"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}