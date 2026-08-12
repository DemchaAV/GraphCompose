package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * An Arabic article laid out the way a real one would be — not a feature list.
 *
 * <p>The point of rendering a whole document rather than a row of samples is that the
 * interesting failures only appear at length: a paragraph has to wrap across many lines
 * with every line reordered, the flow has to break across a page boundary and keep its
 * direction on the other side, and a heading, a list and a two-column row all have to sit
 * at the right edge without anyone positioning them by hand.</p>
 *
 * <p>Every paragraph here declares {@link TextDirection#RTL} and none declares an
 * alignment: a right-to-left paragraph starts at the right edge on its own. The Latin
 * words and the digits inside the Arabic keep running forwards, which is the bidirectional
 * algorithm rather than anything this example does.</p>
 *
 * <p>Set in {@code FontName.AMIRI}, bundled since {@code graph-compose-fonts} 1.1.0. The
 * engine shapes the letters into their contextual forms before measuring them, so the
 * family has to carry the Arabic presentation forms — which is why this one was chosen.</p>
 */
public final class ArabicArticleExample {

    private static final DocumentColor INK = DocumentColor.rgb(28, 32, 44);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 126, 140);
    private static final DocumentColor ACCENT = DocumentColor.rgb(21, 101, 192);
    private static final DocumentColor RULE = DocumentColor.rgb(222, 226, 234);
    private static final DocumentColor PANEL = DocumentColor.rgb(246, 248, 252);

    /**
     * A4 width less the two 52pt side margins. A divider is a drawn shape rather than a
     * flow rule, so it carries its own width — and the page's own arithmetic rounds, so
     * this stays a whole point inside the column rather than exactly on its edge.
     */
    private static final double COLUMN = 491;

    /** "Typography of the Arabic script" — the article's title. */
    private static final String TITLE = "طباعة الحروف العربية";
    private static final String SUBTITLE = "كيف يرسم المحرك الحروف المتصلة داخل ملف PDF";

    private static final String LEAD =
            "تتغير أشكال الحروف العربية حسب موضعها في الكلمة، فلكل حرف صورة في أول الكلمة "
            + "وأخرى في وسطها وثالثة في آخرها ورابعة حين يقف وحده. هذه القاعدة هي ما يجعل "
            + "النص العربي متصلاً، وهي أيضاً ما يجعل رسمه داخل ملف PDF مسألة تحتاج إلى عمل "
            + "إضافي من المحرك نفسه.";

    private static final String BODY_ONE =
            "ملف PDF لا ينفذ قواعد الخط المكتوبة داخل الملف نفسه، بل يرسم ما يُعطى له مباشرة. "
            + "لذلك يحوّل المحرك كل حرف إلى صورته المناسبة قبل قياس عرض السطر، ثم يرسم الصور "
            + "نفسها التي قاسها. لو جرى القياس على الحروف المجردة والرسم على الحروف المتصلة "
            + "لاختلف عرض السطر عن الشكل الظاهر، ولانكسرت الأسطر في مواضع خاطئة.";

    private static final String BODY_TWO =
            "الأرقام والكلمات اللاتينية داخل النص العربي تبقى متجهة من اليسار إلى اليمين. "
            + "في السطر التالي مثال: صدرت النسخة GraphCompose 2.2.0 في عام 2026، ويظل ترتيب "
            + "الحروف والأرقام فيها كما هو رغم أن الفقرة كلها تُقرأ من اليمين.";

    private static final String BODY_THREE =
            "العلامات المصاحبة للحروف، مثل الحركات، لا تقطع الاتصال بين الحرفين حولها. "
            + "أما محرف منع الوصل فيقطعه عمداً، وهو الوسيلة الوحيدة التي يملكها الكاتب ليقول "
            + "إن هذين الحرفين يجب ألا يتصلا. المحرك يقرأ الاثنين ويتصرف بموجبهما.";

    private static final String BODY_FOUR =
            "يبدأ السطر العربي من الحافة اليمنى دون أن يطلب الكاتب ذلك، لأن اتجاه الفقرة هو "
            + "الذي يحدد الحافة التي تبدأ منها أسطرها. ومن أراد غير ذلك يذكر المحاذاة صراحة، "
            + "فتغلب على القاعدة. أما إذا لم يذكر الاتجاه أصلاً فيمكن للمحرك أن يقرأه من أول "
            + "حرف قوي في الفقرة، وهو ما تفعله القيمة AUTO، وتتخطى ما بين محارف العزل كما "
            + "يقول معيار يونيكود.";

    private static final String BODY_FIVE =
            "تنعكس الأقواس وعلامات التنصيص داخل السطر العربي حتى يواجه القوس ما يحيط به "
            + "(مثل هذا القوس هنا)، وهي قاعدة من معيار الاتجاه نفسه لا من الخط. ولأن النص "
            + "يمر على المحرك قبل أن يصل إلى الملف، فإن ما يظهر في الصفحة هو نفسه ما جرى "
            + "قياسه، ولا يتغير عرض السطر بين القياس والرسم.";

    private static final String CLOSING =
            "ما سبق يصف الفقرة وحدها. النص داخل خلية الجدول يمر بتخطيط الجدول الذي لا يحمل "
            + "اتجاهاً، فيُرسم بالترتيب الذي كُتب به. أما الصفوف والقوائم فتتبع اتجاه الفقرة "
            + "كما في هذه الصفحة.";

    private ArabicArticleExample() {
    }

    /**
     * Renders the article.
     *
     * @return the written file
     * @throws Exception if the document cannot be written
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "arabic-article.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(56, 52, 56, 52)
                .create()) {

            document.pageFlow()
                    .name("ArabicArticle")
                    .spacing(11)

                    .addParagraph(p -> p.text(TITLE).direction(TextDirection.RTL).textStyle(title()))
                    .addParagraph(p -> p.text(SUBTITLE).direction(TextDirection.RTL).textStyle(subtitle()))
                    .addDivider(d -> d.width(COLUMN).color(RULE).thickness(1)
                            .margin(DocumentInsets.symmetric(8, 0)))

                    .addParagraph(p -> p.text(LEAD).direction(TextDirection.RTL).textStyle(lead()))
                    .addParagraph(p -> p.text(BODY_ONE).direction(TextDirection.RTL).textStyle(body()))
                    .addParagraph(p -> p.text(BODY_TWO).direction(TextDirection.RTL).textStyle(body()))

                    // A panel of facts: two columns, each a right-to-left paragraph, so the
                    // label sits at the right edge of its own column.
                    .addRow(row -> row
                            .fillColor(PANEL)
                            .cornerRadius(6)
                            .padding(DocumentInsets.of(14))
                            .gap(18)
                            .weights(1.0, 1.0, 1.0)
                            .addParagraph(p -> p.text("النسخة  2.2.0")
                                    .direction(TextDirection.RTL).textStyle(fact()))
                            .addParagraph(p -> p.text("الخط  Amiri")
                                    .direction(TextDirection.RTL).textStyle(fact()))
                            .addParagraph(p -> p.text("الحروف  ٢٢٥ صورة")
                                    .direction(TextDirection.RTL).textStyle(fact())))

                    .addParagraph(p -> p.text("ثلاث قواعد يطبقها المحرك")
                            .direction(TextDirection.RTL).textStyle(heading()))
                    // A list carries no writing direction of its own — the marker is placed
                    // by the list's alignment, so an Arabic list has to be told to sit right
                    // or its bullets end up on the wrong side of the text.
                    .addList(list -> list
                            .align(TextAlign.RIGHT)
                            .items("يُشكَّل الحرف قبل القياس، فما يُقاس هو ما يُرسم.",
                                    "تنعكس علامات الترقيم المزدوجة داخل السطر العربي.",
                                    "يُقرأ اتجاه الفقرة من أول حرف قوي فيها عند اختيار AUTO.")
                            .textStyle(body()))

                    .addParagraph(p -> p.text(BODY_FOUR).direction(TextDirection.RTL).textStyle(body()))
                    .addParagraph(p -> p.text(BODY_FIVE).direction(TextDirection.RTL).textStyle(body()))

                    // No page break here on purpose: the article is long enough to overflow,
                    // so the second page is the paginator's work rather than a hand-placed cut.
                    .addParagraph(p -> p.text("الحركات ومنع الوصل")
                            .direction(TextDirection.RTL).textStyle(heading()))
                    .addParagraph(p -> p.text(BODY_THREE).direction(TextDirection.RTL).textStyle(body()))
                    .addParagraph(p -> p.text(CLOSING).direction(TextDirection.RTL).textStyle(body()))
                    .addDivider(d -> d.width(COLUMN).color(RULE).thickness(1)
                            .margin(DocumentInsets.symmetric(10, 0)))
                    .addParagraph(p -> p.text("GraphCompose · features/text/arabic-article")
                            .align(TextAlign.LEFT).textStyle(footer()))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    /**
     * Renders the article to the example output directory.
     *
     * @param args ignored
     * @throws Exception if the document cannot be written
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static DocumentTextStyle title() {
        return DocumentTextStyle.builder().fontName(FontName.AMIRI).size(30).color(INK).build();
    }

    private static DocumentTextStyle subtitle() {
        return DocumentTextStyle.builder().fontName(FontName.AMIRI).size(15).color(MUTED).build();
    }

    private static DocumentTextStyle heading() {
        return DocumentTextStyle.builder().fontName(FontName.AMIRI).size(19).color(ACCENT).build();
    }

    private static DocumentTextStyle lead() {
        return DocumentTextStyle.builder().fontName(FontName.AMIRI).size(15).color(INK).build();
    }

    private static DocumentTextStyle body() {
        return DocumentTextStyle.builder().fontName(FontName.AMIRI).size(13).color(INK).build();
    }

    private static DocumentTextStyle fact() {
        return DocumentTextStyle.builder().fontName(FontName.AMIRI).size(13).color(ACCENT).build();
    }

    private static DocumentTextStyle footer() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(9).color(MUTED).build();
    }
}
