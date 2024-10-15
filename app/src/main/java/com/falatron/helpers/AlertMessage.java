package com.falatron.helpers;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

public class AlertMessage {

    private Context context;

    public AlertMessage(Context context) {
        this.context = context;
    }

    public void mostrarAlertaCategoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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

    public void mostrarAlertaVoz() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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

    public void mostrarAlertaTexto() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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

    public void mostrarAlertaAudio() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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

    public void mostrarAlertaInternet() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Sem conexão com a Internet");
        builder.setMessage("Ative a conexão de internet para continuar.");

        builder.setPositiveButton("Ativar Internet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                context.startActivity(intent);
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

    public void mostrarAlertaDePermissao() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Permissão Negada");
        builder.setMessage("A permissão é necessária para realizar esta ação. Por favor, conceda a permissão nas configurações do aplicativo.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
}