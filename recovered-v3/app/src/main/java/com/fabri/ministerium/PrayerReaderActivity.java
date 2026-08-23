package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class PrayerReaderActivity extends ThemedActivity {
    public static final String EXTRA_PRAYER_ID = "prayer_id";
    public static final String EXTRA_COLLECTION = "prayer_collection";

    private PrayerEntry entry;
    private TextView content;
    private Button favorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_reader);

        entry = PrayerRepository.find(getIntent().getStringExtra(EXTRA_PRAYER_ID));
        if (entry == null) {
            finish();
            return;
        }

        content = findViewById(R.id.txtContent);
        favorite = findViewById(R.id.btnFavorite);
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(entry.title);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText(entry.category);
        ((TextView) findViewById(R.id.txtSource)).setText(
                "Oración tradicional · disponible sin conexión");

        try {
            content.setText(PrayerRepository.read(this, entry));
        } catch (IOException error) {
            Toast.makeText(this, "No se pudo abrir la oración.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        favorite.setOnClickListener(v -> {
            FavoritesStore.toggleItem(this, "prayer:" + entry.id);
            updateFavorite();
        });
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        updateFavorite();
        configureNavigation();
    }

    private void configureNavigation() {
        List<PrayerEntry> entries = "additional".equals(
                getIntent().getStringExtra(EXTRA_COLLECTION))
                ? PrayerRepository.additional() : PrayerRepository.basic();
        int position = entries.indexOf(entry);
        ReaderContext context = new ReaderContext("Oraciones", "prayer:" + entry.id,
                entry.title, entry.category, "Oraciones", false);
        TextViewReaderChrome.attach(this, content, findViewById(R.id.readerScroll),
                findViewById(R.id.readerHeader), context, new ReaderChrome.Navigator() {
                    @Override public boolean canPrevious() { return position > 0; }
                    @Override public boolean canNext() { return position >= 0 && position < entries.size() - 1; }
                    @Override public void previous() { show(entries.get(position - 1)); }
                    @Override public void next() { show(entries.get(position + 1)); }
                });
        TextViewReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), content, context);
    }

    private void show(PrayerEntry next) {
        getIntent().putExtra(EXTRA_PRAYER_ID, next.id);
        recreate();
    }

    private void updateFavorite() {
        favorite.setText(FavoritesStore.containsItem(this, "prayer:" + entry.id) ? "★" : "☆");
    }
}
