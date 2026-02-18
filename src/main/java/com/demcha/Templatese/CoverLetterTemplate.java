package com.demcha.Templatese;
import com.demcha.Templatese.data.EmailYaml;
import com.demcha.Templatese.data.Header;
import com.demcha.Templatese.data.LinkYml;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.loyaut_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.components_builders.ModuleBuilder;
import com.demcha.compose.loyaut_core.components.content.link.Email;
import com.demcha.compose.loyaut_core.components.content.link.LinkUrl;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.core.PdfComposer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.Arrays;
import java.util.List;

public class CoverLetterTemplate  {
    public static void main(String[] args) {
        CoverLetterTemplate coverLetterTemplate = new CoverLetterTemplate();

        Header header = new Header();
        header.setName("Artem Demchyshyn");
        header.setAddress("Kyiv, Ukraine");
        header.setPhoneNumber("+380991234567");

        EmailYaml email = new EmailYaml();
        email.setTo("artem@example.com");
        email.setSubject("Job Application");
        email.setBody("Hello");
        email.setDisplayText("artem@example.com");
        header.setEmail(email);

        LinkYml linkedIn = new LinkYml();
        linkedIn.setLinkUrl(new LinkUrl( "https://linkedin.com/in/artem"));
        linkedIn.setDisplayText("LinkedIn");
        header.setLinkedIn(linkedIn);

        LinkYml gitHub = new LinkYml();
        gitHub.setLinkUrl(new LinkUrl("https://github.com/artem"));
        gitHub.setDisplayText("GitHub");
        header.setGitHub(gitHub);

        String wroteLetter = "Dear ${companyName},\n" +
                             "\n" +
                             "I am writing to express my strong interest in the **Junior Java Developer** position at **${companyName}**. While I am currently transitioning into my first commercial role, I bring a robust technical foundation in **Java 17/21** and **Spring Boot 3+**, alongside a proven track record of resilience and rapid self-directed learning.\n" +
                             "\n" +
                             "## An Unconventional Path Built on Logic\n" +
                             "\n" +
                             "My journey into software development is rooted in a lifelong fascination with technology. Long before my formal transition, I spent my time designing media player skins for **AIMP** and writing scripts for **Photoshop**. Although I initially trained as a **Hydraulic Engineer** in Ukraine, the core of my background has always been about solving complex structural problems — a mindset I now apply to backend architecture.\n" +
                             "\n" +
                             "## From Resilience to Technical Proficiency\n" +
                             "\n" +
                             "Relocating to London from an occupied region near Crimea presented a significant challenge. I arrived with limited English and took a role in hospitality to support myself. Within seven months, I was promoted for my leadership and consistency, all while teaching myself English, Java, and Spring Boot in my limited free time.\n" +
                             "\n" +
                             "My recent work demonstrates a shift from basic learning to building production-ready systems:\n" +
                             "\n" +
                             "- **GraphCompose:** I developed a declarative PDF generation library using an **Entity Component System (ECS)** architecture, implementing complex two-pass layout algorithms and smart pagination.\n" +
                             "- **CVRewriter:** I built a full-stack AI-powered application that integrates **Gemini AI**, utilizes **Playwright** for web scraping, and provides real-time updates via **Server-Sent Events (SSE)**.\n" +
                             "- **Infrastructure & Security:** My projects consistently utilize **JWT authentication**, **Docker**, **Flyway** for database migrations, and **Spring Data JPA** with **MySQL**.\n" +
                             "\n" +
                             "## Why I Am a Fit for Your Team\n" +
                             "\n" +
                             "What sets me apart is the combination of engineering logic and real-world discipline. I don’t just write code; I focus on clean architecture, modularity, and reliable services. My experience leading a team of eight in my previous engineering career has equipped me with the communication skills and accountability necessary for a high-performing development team.\n" +
                             "\n" +
                             "Thank you for your time and consideration. I am eager to discuss how my technical skills and unique background can contribute to **${companyName}**. I am available for an interview at your earliest convenience.\n";

        JobDetails jobDetails = new JobDetails(
                "https://linkedin.com/jobs/view/123456",
                "Software Engineer",
                "Tech Corp",
                "Remote",
                "Job Description...",
                "Senior",
                "Full-time"
        );
        wroteLetter = wroteLetter.replace("${companyName}", jobDetails.company());


        try  {
            PDDocument document = coverLetterTemplate.render(header, wroteLetter, jobDetails);
            document.save("cover_letter.pdf");
            System.out.println("Cover letter saved to cover_letter.pdf");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Unique identifier for this template.
     * Used to select template via API (e.g., "modern-professional", "classic",
     * "minimal").


    /**
     * Renders a PDF document using this template.
     *
     * @param header
     * @param wroteLetter
     * @param jobDetails
     * @return A PDDocument that can be saved or streamed
     */
    public PDDocument render(Header header, String wroteLetter, JobDetails jobDetails) {
        return render(header, wroteLetter, jobDetails, true);
    }

    public PDDocument render(Header header, String wroteLetter, JobDetails jobDetails, boolean guideLines) {
        try {
            // Do NOT use try-with-resources here!
            // The PDDocument must remain open for the caller to stream/save it.
            // The caller (StreamingResponseBody) is responsible for closing it.
            PdfComposer composer = GraphCompose.pdf()
                    .pageSize(PDRectangle.A4)
                    .margin(15, 10, 15, 15)
                    .markdown(true)
                    .guideLines(guideLines)
                    .create();

            Canvas canvas = composer.canvas();
            String whitespace = "";
            BlockIndentStrategy indentStrategy = BlockIndentStrategy.FIRST_LINE;

            TemplateBuilder cv = composer.componentBuilder().template(CvTheme.defaultTheme());

            float textBlockWidth = (float) canvas.innerWidth();

            Entity moduleHeader = createHeader(cv, header, canvas);


            Entity coverLetter = letterSection(cv, wroteLetter, textBlockWidth, whitespace, indentStrategy);
            Entity regards = cv.blockText("King Regards,\nArtem Demchyshyn");
            regards.addComponent(Margin.top(10));
            regards.addComponent(Anchor.topRight());


            cv.moduleBuilder(canvas)
                    .entityName("MainVBoxContainer")
                    .addChild(moduleHeader)
                    .addChild(coverLetter)
                    .addChild(regards)
                    .build();

            return composer.toPDDocument();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CV", e);
        }
    }

    private Entity createHeader(TemplateBuilder cv, Header header, Canvas canvas) {
        var number = header.getPhoneNumber();
        var address = header.getAddress();
        var email = header.getEmail();

        var linkedIn = header.getLinkedIn();
        var gitHub = header.getGitHub();

        Entity artemDemchyshyn = cv.name(header.getName());

        Entity infoPanel = cv.infoPanel(List.of(cv.info(address), cv.info(number)), null, null);

        var linksPanel = cv.infoPanel(List.of(
                        cv.link(
                                new Email(email.getTo(),
                                        email.getSubject(),
                                        email.getBody()),
                                email.getDisplayText()),
                        cv.link(new LinkUrl(linkedIn.getLinkUrl().getUrl()), linkedIn.getDisplayText()),
                        cv.link(new LinkUrl(gitHub.getLinkUrl().getUrl()), gitHub.getDisplayText())), null,
                null);

        return new ModuleBuilder(cv.entityManager(), Align.middle(5), canvas)
                .entityName("ModuleHeader")
                .margin(new Margin(0, 10, 10, 10))
                .anchor(Anchor.topRight())
                .addChild(artemDemchyshyn)
                .addChild(infoPanel)
                .addChild(linksPanel)
                .build();
    }

    private Entity letterSection(TemplateBuilder cv,
                                 List<String> content, float width, String bullet, BlockIndentStrategy strategy) {
        return cv.blockText(content, width, bullet, strategy);
    }
    private Entity letterSection(TemplateBuilder cv,
                                 String content, float width, String bullet, BlockIndentStrategy strategy) {

        List<String> contentList = Arrays.asList(content.split("\\R", -1)); // -1 keeps empty lines

        return cv.blockText(contentList, width, bullet, strategy);
    }
}
