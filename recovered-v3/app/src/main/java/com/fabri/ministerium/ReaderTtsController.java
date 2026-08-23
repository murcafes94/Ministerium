package com.fabri.ministerium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public final class ReaderTtsController {
    private static final String PREFS = "reader_tts";
    private ReaderTtsController() {}

    public static void start(Activity activity, WebView webView, boolean omitRubrics) {
        String script = "(function(omit){var nodes=document.querySelectorAll('h1,h2,h3,h4,p,li,blockquote');"
                + "var out=[];for(var i=0;i<nodes.length;i++){var e=nodes[i];"
                + "if(e.offsetParent===null||e.closest('nav,button,.source,.reading-reference,.lectionary-label'))continue;"
                + "var c=(e.className||'').toString().toLowerCase();"
                + "if(omit&&(c.indexOf('rubric')>=0||c.indexOf('rubrica')>=0||c.indexOf('rojo')>=0))continue;"
                + "var x=e.cloneNode(true),drop=x.querySelectorAll('sup,.verse-number,button,a[aria-hidden=true]');"
                + "for(var d=0;d<drop.length;d++)drop[d].remove();var t=(x.textContent||'')"
                + ".replace(/\\s+/g,' ').trim();if(t.length>1&&out.indexOf(t)<0)out.push(t);}"
                + "return JSON.stringify(out);})(" + (omitRubrics ? "true" : "false") + ")";
        webView.evaluateJavascript(script, value -> {
            JSONArray blocks = decode(value);
            if (blocks.length() == 0) {
                Toast.makeText(activity, "No se encontró texto para leer.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            showOptions(activity, blocks);
        });
    }

    public static void speakSelection(Activity activity, String selected) {
        JSONArray blocks = new JSONArray();
        blocks.put(selected.replaceAll("(?m)(^|\\s)\\d{1,3}(?=\\s+\\p{L})", "$1"));
        showOptions(activity, blocks);
    }

    private static void showOptions(Activity activity, JSONArray blocks) {
        int pad = Math.round(18 * activity.getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);
        TextView speedLabel = label(activity, "Velocidad");
        SeekBar speed = new SeekBar(activity);
        speed.setMax(150);
        speed.setProgress(Math.round((speed(activity) - .5f) * 100));
        TextView pitchLabel = label(activity, "Tono");
        SeekBar pitch = new SeekBar(activity);
        pitch.setMax(100);
        pitch.setProgress(Math.round((pitch(activity) - .5f) * 100));
        TextView volumeLabel = label(activity, "Volumen");
        SeekBar volume = new SeekBar(activity);
        AudioManager audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        int max = audio == null ? 15 : audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume.setMax(max);
        volume.setProgress(audio == null ? max / 2
                : audio.getStreamVolume(AudioManager.STREAM_MUSIC));
        volume.setOnSeekBarChangeListener(new SimpleSeekListener(progress -> {
            if (audio != null) audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
        }));
        box.addView(speedLabel); box.addView(speed);
        box.addView(pitchLabel); box.addView(pitch);
        box.addView(volumeLabel); box.addView(volume);
        TextView note = label(activity, "Se omiten controles y números de versículo. "
                + "La notificación permite pausar, detener y cambiar de bloque.");
        note.setTextSize(12);
        note.setPadding(0, pad / 2, 0, 0);
        box.addView(note);

        new AlertDialog.Builder(activity).setTitle("Leer en voz alta · Español")
                .setView(box).setNegativeButton("Cancelar", null)
                .setPositiveButton("Reproducir", (dialog, which) -> {
                    float selectedSpeed = .5f + speed.getProgress() / 100f;
                    float selectedPitch = .5f + pitch.getProgress() / 100f;
                    activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putFloat("speed", selectedSpeed)
                            .putFloat("pitch", selectedPitch).apply();
                    startService(activity, blocks, selectedSpeed, selectedPitch);
                }).show();
    }

    private static void startService(Activity activity, JSONArray blocks,
                                     float speed, float pitch) {
        try {
            File session = new File(activity.getCacheDir(), "ministerium-tts-session.json");
            try (FileOutputStream output = new FileOutputStream(session)) {
                output.write(blocks.toString().getBytes(StandardCharsets.UTF_8));
            }
            Intent intent = new Intent(activity, TtsPlaybackService.class)
                    .setAction(TtsPlaybackService.ACTION_START)
                    .putExtra(TtsPlaybackService.EXTRA_SESSION, session.getAbsolutePath())
                    .putExtra(TtsPlaybackService.EXTRA_SPEED, speed)
                    .putExtra(TtsPlaybackService.EXTRA_PITCH, pitch);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent);
            } else {
                activity.startService(intent);
            }
        } catch (Exception error) {
            Toast.makeText(activity, "No se pudo iniciar la lectura en voz alta.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private static JSONArray decode(String value) {
        try {
            String json = new JSONTokener(value).nextValue().toString();
            return new JSONArray(json);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static float speed(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat("speed", 1f);
    }

    private static float pitch(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat("pitch", 1f);
    }

    private static TextView label(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(15);
        return view;
    }

    private interface ProgressCallback { void changed(int progress); }

    private static final class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        private final ProgressCallback callback;
        SimpleSeekListener(ProgressCallback callback) { this.callback = callback; }
        @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) callback.changed(progress);
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
