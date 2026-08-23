package com.fabri.ministerium;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TtsPlaybackService extends Service implements TextToSpeech.OnInitListener {
    public static final String ACTION_START = "ministerium.tts.START";
    public static final String ACTION_PLAY_PAUSE = "ministerium.tts.PLAY_PAUSE";
    public static final String ACTION_STOP = "ministerium.tts.STOP";
    public static final String ACTION_NEXT = "ministerium.tts.NEXT";
    public static final String ACTION_PREVIOUS = "ministerium.tts.PREVIOUS";
    public static final String EXTRA_SESSION = "tts_session";
    public static final String EXTRA_SPEED = "tts_speed";
    public static final String EXTRA_PITCH = "tts_pitch";
    private static final String CHANNEL = "ministerium_tts";
    private static final int NOTIFICATION = 3800;
    private final List<String> blocks = new ArrayList<>();
    private TextToSpeech tts;
    private int index;
    private boolean ready;
    private boolean paused;
    private boolean startWhenReady;
    private float speed = 1f;
    private float pitch = 1f;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        tts = new TextToSpeech(this, this);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) { updateNotification(); }
            @Override public void onError(String utteranceId) { advance(); }
            @Override public void onDone(String utteranceId) { advance(); }
        });
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            load(intent.getStringExtra(EXTRA_SESSION));
            speed = intent.getFloatExtra(EXTRA_SPEED, 1f);
            pitch = intent.getFloatExtra(EXTRA_PITCH, 1f);
            index = 0;
            paused = false;
            startForeground(NOTIFICATION, notification());
            if (ready) speak(); else startWhenReady = true;
        } else if (ACTION_PLAY_PAUSE.equals(action)) {
            if (paused) { paused = false; speak(); }
            else { paused = true; if (tts != null) tts.stop(); updateNotification(); }
        } else if (ACTION_NEXT.equals(action)) {
            if (index + 1 < blocks.size()) index++;
            paused = false;
            speak();
        } else if (ACTION_PREVIOUS.equals(action)) {
            if (index > 0) index--;
            paused = false;
            speak();
        } else if (ACTION_STOP.equals(action)) {
            stopPlayback();
        }
        return START_NOT_STICKY;
    }

    @Override public void onInit(int status) {
        ready = status == TextToSpeech.SUCCESS;
        if (!ready) { stopPlayback(); return; }
        int language = tts.setLanguage(new Locale("es", "EC"));
        if (language == TextToSpeech.LANG_MISSING_DATA
                || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(new Locale("es"));
        }
        if (startWhenReady) { startWhenReady = false; speak(); }
    }

    private void speak() {
        if (!ready || paused || blocks.isEmpty() || index >= blocks.size()) return;
        tts.stop();
        tts.setSpeechRate(speed);
        tts.setPitch(pitch);
        String text = blocks.get(index).replaceAll(
                "(?m)(^|\\s)\\d{1,3}(?=\\s+\\p{L})", "$1");
        Bundle parameters = new Bundle();
        parameters.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, parameters, "block-" + index);
        updateNotification();
        sendBroadcast(new Intent("com.fabri.ministerium.TTS_BLOCK")
                .putExtra("index", index));
    }

    private void advance() {
        if (paused) return;
        index++;
        if (index >= blocks.size()) stopPlayback();
        else speak();
    }

    private void load(String path) {
        blocks.clear();
        if (path == null || path.isEmpty()) return;
        try (FileInputStream input = new FileInputStream(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            JSONArray values = new JSONArray(new String(output.toByteArray(),
                    StandardCharsets.UTF_8));
            for (int i = 0; i < values.length(); i++) {
                String value = values.optString(i).trim();
                if (!value.isEmpty()) blocks.add(value);
            }
        } catch (Exception ignored) {}
    }

    private Notification notification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this, 3801, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher).setContentTitle("Ministerium · Lectura")
                .setContentText(blocks.isEmpty() ? "Preparando…"
                        : "Bloque " + Math.min(index + 1, blocks.size()) + " de " + blocks.size())
                .setContentIntent(content).setOngoing(!paused)
                .addAction(R.drawable.ic_launcher, "Anterior", action(ACTION_PREVIOUS, 3802))
                .addAction(R.drawable.ic_launcher, paused ? "Continuar" : "Pausa",
                        action(ACTION_PLAY_PAUSE, 3803))
                .addAction(R.drawable.ic_launcher, "Siguiente", action(ACTION_NEXT, 3804))
                .addAction(R.drawable.ic_launcher, "Detener", action(ACTION_STOP, 3805));
        return builder.build();
    }

    private PendingIntent action(String action, int request) {
        return PendingIntent.getService(this, request,
                new Intent(this, TtsPlaybackService.class).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION, notification());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "Lectura en voz alta", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Controles de la lectura en voz alta de Ministerium");
        manager.createNotificationChannel(channel);
    }

    private void stopPlayback() {
        if (tts != null) tts.stop();
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
