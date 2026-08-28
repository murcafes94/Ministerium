package com.fabri.ministerium;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MagisteriumActivity extends ThemedActivity {
    private static final String EXTRA_SECTION = "magisterium_section";
    private static final String SECTION_COUNCIL = "council";
    private static final String SECTION_CATECHESIS = "catechesis";
    private static final String SECTION_SOCIAL = "social";
    private static final String SECTION_LAW = "law";
    private static final String SECTION_LITURGY = "liturgy";

    private static final String OGLH_URL =
            "https://liturgiapapal.org/attachments/article/602/OGLH.pdf";
    private static final String OLM_URL =
            "https://liturgiapapal.org/attachments/article/731/Ordenacion%20Lecturas%20Misa.pdf";

    private String section;
    private final List<Entry> entries = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);

        section = getIntent().getStringExtra(EXTRA_SECTION);
        bindHeader();
        buildEntries();

        List<Map<String, String>> rows = new ArrayList<>();
        for (Entry entry : entries) rows.add(Rows.row(entry.title, entry.subtitle));

        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) ->
                open(entries.get(position)));
        findViewById(R.id.btnBack).setOnClickListener(v -> goBack());
    }

    private void bindHeader() {
        TextView title = findViewById(R.id.txtTitle);
        TextView subtitle = findViewById(R.id.txtSubtitle);
        TextView intro = findViewById(R.id.txtIntro);
        if (section == null) {
            title.setText("Magisterio");
            subtitle.setText("Doctrina, Concilio, derecho y normas litúrgicas");
            intro.setText("Biblioteca ordenada por materias. Los documentos EPUB y el "
                    + "Código están disponibles sin conexión; la búsqueda consulta el texto "
                    + "completo y abre el fragmento encontrado.");
            return;
        }
        title.setText(sectionTitle(section));
        subtitle.setText("Magisterio · biblioteca temática");
        intro.setText(SECTION_LITURGY.equals(section)
                ? "Normas generales para celebrar y ordenar la liturgia. Los PDF requieren "
                        + "conexión; la aplicación usa la Ordenación de las Lecturas de la Misa "
                        + "como fuente de sus reglas del Leccionario."
                : "Selecciona un documento para consultar su índice y leerlo sin conexión.");
    }

    private void buildEntries() {
        entries.clear();
        if (section == null) {
            entries.add(Entry.search());
            entries.add(Entry.section(SECTION_COUNCIL, "Concilio y enseñanza",
                    "Concilio Vaticano II · constituciones, decretos y declaraciones"));
            entries.add(Entry.section(SECTION_CATECHESIS, "Catequesis de la fe",
                    "Catecismo de la Iglesia Católica y Compendio"));
            entries.add(Entry.section(SECTION_SOCIAL, "Doctrina social",
                    "Principios, vida económica, política y bien común"));
            entries.add(Entry.section(SECTION_LAW, "Derecho de la Iglesia",
                    "Código de Derecho Canónico y comentarios por canon"));
            entries.add(Entry.section(SECTION_LITURGY, "Liturgia y sacramentos",
                    "Ordenación de las Horas y de las Lecturas de la Misa"));
            return;
        }
        if (SECTION_COUNCIL.equals(section)) {
            entries.add(Entry.epub(HoursRepository.VATICAN_II));
        } else if (SECTION_CATECHESIS.equals(section)) {
            entries.add(Entry.epub(HoursRepository.CATECHISM));
            entries.add(Entry.epub(HoursRepository.CATECHISM_COMPENDIUM));
        } else if (SECTION_SOCIAL.equals(section)) {
            entries.add(Entry.epub(HoursRepository.SOCIAL_DOCTRINE));
        } else if (SECTION_LAW.equals(section)) {
            entries.add(Entry.canon("Código de Derecho Canónico",
                    "1.752 cánones · español y latín · texto local consolidado"));
            entries.add(Entry.canon("Comentarios al Código de Derecho Canónico",
                    "Comentarios enlazados por canon · texto sin conexión"));
        } else if (SECTION_LITURGY.equals(section)) {
            entries.add(Entry.web("Ordenación General de la Liturgia de las Horas",
                    "Instrucción general · PDF web", OGLH_URL));
            entries.add(Entry.web("Ordenación de las Lecturas de la Misa",
                    "Normas del Leccionario · PDF web · fuente de reglas 4.0", OLM_URL));
        }
    }

    private void open(Entry entry) {
        if (entry.kind == Entry.SEARCH) {
            startActivity(new Intent(this, SearchActivity.class)
                    .putExtra(SearchActivity.EXTRA_SCOPE, SearchActivity.SCOPE_MAGISTERIUM));
        } else if (entry.kind == Entry.SECTION) {
            startActivity(new Intent(this, MagisteriumActivity.class)
                    .putExtra(EXTRA_SECTION, entry.value));
        } else if (entry.kind == Entry.EPUB) {
            HoursVolume volume = HoursRepository.find(entry.value);
            if (volume != null) openEpub(volume);
        } else if (entry.kind == Entry.CANON) {
            startActivity(new Intent(this, CanonLawActivity.class));
        } else if (entry.kind == Entry.WEB) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(entry.value)));
            } catch (Exception error) {
                Toast.makeText(this, "No hay una aplicación disponible para abrir el PDF.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openEpub(HoursVolume volume) {
        Intent intent = new Intent(this, HoursTocActivity.class);
        intent.putExtra(HoursTocActivity.EXTRA_VOLUME_ID, volume.id);
        startActivity(intent);
    }

    private String sectionTitle(String value) {
        if (SECTION_COUNCIL.equals(value)) return "Concilio y enseñanza";
        if (SECTION_CATECHESIS.equals(value)) return "Catequesis de la fe";
        if (SECTION_SOCIAL.equals(value)) return "Doctrina social";
        if (SECTION_LAW.equals(value)) return "Derecho de la Iglesia";
        if (SECTION_LITURGY.equals(value)) return "Liturgia y sacramentos";
        return "Magisterio";
    }

    private void goBack() {
        if (section != null) {
            finish();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override public void onBackPressed() { goBack(); }

    private static final class Entry {
        static final int SEARCH = 0;
        static final int SECTION = 1;
        static final int EPUB = 2;
        static final int CANON = 3;
        static final int WEB = 4;

        final int kind;
        final String title;
        final String subtitle;
        final String value;

        private Entry(int kind, String title, String subtitle, String value) {
            this.kind = kind;
            this.title = title;
            this.subtitle = subtitle;
            this.value = value;
        }

        static Entry search() {
            return new Entry(SEARCH, "Buscar en todo el Magisterio",
                    "Texto completo · resultados por relevancia · sin conexión", "");
        }

        static Entry section(String value, String title, String subtitle) {
            return new Entry(SECTION, title, subtitle, value);
        }

        static Entry epub(HoursVolume volume) {
            return new Entry(EPUB, volume.title, volume.subtitle + " · sin conexión", volume.id);
        }

        static Entry canon(String title, String subtitle) {
            return new Entry(CANON, title, subtitle, "");
        }

        static Entry web(String title, String subtitle, String url) {
            return new Entry(WEB, title, subtitle, url);
        }
    }
}
