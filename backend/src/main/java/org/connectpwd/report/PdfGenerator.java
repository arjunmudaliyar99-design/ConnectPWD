package org.connectpwd.report;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.ListNumberingType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.connectpwd.scoring.IsaaScore;
import org.connectpwd.scoring.SeverityLevel;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class PdfGenerator {

    // ── Brand palette ────────────────────────────────────────────────────────
    private static final DeviceRgb COL_HEADER_BG = new DeviceRgb(29, 37, 51);
    private static final DeviceRgb COL_SECTION_BG = new DeviceRgb(243, 244, 246);
    private static final DeviceRgb COL_TABLE_HEAD = new DeviceRgb(41, 50, 65);
    private static final DeviceRgb COL_BORDER = new DeviceRgb(209, 213, 219);
    private static final DeviceRgb COL_TEXT_MUTED = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb COL_BRAND_DARK = new DeviceRgb(17, 24, 39);
    private static final DeviceRgb COL_ACCENT = new DeviceRgb(249, 115, 22);
    private static final DeviceRgb COL_ACCENT_BG = new DeviceRgb(255, 247, 237);

    // Severity colours
    private static final DeviceRgb COL_NONE = new DeviceRgb(34, 139, 34);
    private static final DeviceRgb COL_MILD = new DeviceRgb(202, 138, 4);
    private static final DeviceRgb COL_MODERATE = new DeviceRgb(249, 115, 22);
    private static final DeviceRgb COL_SEVERE = new DeviceRgb(220, 38, 38);

    // Domain-bar colours
    private static final DeviceRgb BAR_LOW = new DeviceRgb(34, 197, 94);
    private static final DeviceRgb BAR_MED = new DeviceRgb(202, 138, 4);
    private static final DeviceRgb BAR_HIGH = new DeviceRgb(220, 38, 38);
    private static final DeviceRgb BAR_EMPTY = new DeviceRgb(229, 231, 235);

    // ── Public entry point ───────────────────────────────────────────────────

    public byte[] generateReport(IsaaScore score, String clientName,
            String caregiverName, String language) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(new PdfDocument(new PdfWriter(baos)));
            document.setMargins(40, 40, 40, 40);

            boolean hi = "hi".equals(language);

            renderHeader(document, hi);
            renderClientBlock(document, clientName, caregiverName,
                    score.getSessionId(), hi);
            renderExecutiveSummary(document, score, hi);
            renderDomainBreakdown(document, score, hi);
            renderRecommendations(document, score.getSeverity(), hi);
            renderFooter(document, hi);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    // ── Section 0: Header ────────────────────────────────────────────────────

    private void renderHeader(Document doc, boolean hi) {
        String dateStr = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        Table hdr = new Table(UnitValue.createPercentArray(new float[] { 65, 35 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(0);

        hdr.addCell(new Cell()
                .add(new Paragraph("ConnectPWD")
                        .setFontSize(22).setBold().setFontColor(ColorConstants.WHITE))
                .add(new Paragraph(hi ? "ISAA मूल्यांकन रिपोर्ट" : "ISAA Assessment Report")
                        .setFontSize(11).setFontColor(new DeviceRgb(209, 213, 219)))
                .setBackgroundColor(COL_HEADER_BG)
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(16).setPaddingTop(16).setPaddingBottom(16));

        hdr.addCell(new Cell()
                .add(new Paragraph(hi ? "तिथि" : "Report Date")
                        .setFontSize(8).setFontColor(new DeviceRgb(156, 163, 175)))
                .add(new Paragraph(dateStr)
                        .setFontSize(11).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(COL_HEADER_BG)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPaddingRight(16).setPaddingTop(16).setPaddingBottom(16));

        doc.add(hdr);
    }

    // ── Section 0b: Client block ─────────────────────────────────────────────

    private void renderClientBlock(Document doc, String client, String caregiver,
            String sessionId, boolean hi) {
        Table t = new Table(UnitValue.createPercentArray(new float[] { 1, 1, 1 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(COL_SECTION_BG)
                .setMarginBottom(14);

        t.addCell(infoCell(hi ? "ग्राहक" : "Client", client));
        t.addCell(infoCell(hi ? "देखभालकर्ता" : "Caregiver", caregiver));
        String sid = sessionId.length() > 14 ? sessionId.substring(0, 14) + "…" : sessionId;
        t.addCell(infoCell("Session ID", sid));
        doc.add(t);
    }

    private Cell infoCell(String label, String value) {
        return new Cell()
                .add(new Paragraph(label)
                        .setFontSize(8).setFontColor(COL_TEXT_MUTED).setMarginBottom(2))
                .add(new Paragraph(value)
                        .setFontSize(11).setBold().setFontColor(COL_BRAND_DARK))
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(14).setPaddingTop(10).setPaddingBottom(10);
    }

    // ── Section 1: Executive Summary ─────────────────────────────────────────

    private void renderExecutiveSummary(Document doc, IsaaScore score, boolean hi) {
        doc.add(sectionHeading(hi ? "1.  कार्यकारी सारांश" : "1.  Executive Summary"));

        DeviceRgb severityColor = severityColor(score.getSeverity());

        // Two-column: severity badge | score metrics
        Table row = new Table(UnitValue.createPercentArray(new float[] { 50, 50 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(12);

        // Left: badge
        row.addCell(new Cell()
                .add(new Paragraph(hi ? "स्क्रीनिंग परिणाम" : "Screening Result")
                        .setFontSize(8).setFontColor(COL_TEXT_MUTED).setMarginBottom(4))
                .add(new Paragraph(formatSeverity(score.getSeverity(), hi))
                        .setFontSize(17).setBold().setFontColor(severityColor))
                .setBorderTop(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(severityColor, 4f))
                .setBackgroundColor(lighten(severityColor))
                .setPadding(14));

        // Right: metrics grid
        Table metrics = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                .setWidth(UnitValue.createPercentValue(100));
        metrics.addCell(metricCell(hi ? "कुल स्कोर" : "Total Score",
                score.getTotalScore() + " / 200", COL_ACCENT));
        metrics.addCell(metricCell(hi ? "विकलांगता %" : "Disability %",
                score.getDisabilityPct() + "%", severityColor));

        row.addCell(new Cell().add(metrics)
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(12));

        doc.add(row);

        // Description
        doc.add(new Paragraph(severityDescription(score.getSeverity(), hi))
                .setFontSize(11).setFontColor(COL_BRAND_DARK)
                .setMarginBottom(14));
    }

    private Cell metricCell(String label, String value, DeviceRgb color) {
        return new Cell()
                .add(new Paragraph(label)
                        .setFontSize(8).setFontColor(COL_TEXT_MUTED).setMarginBottom(2))
                .add(new Paragraph(value)
                        .setFontSize(14).setBold().setFontColor(color))
                .setBorder(new SolidBorder(COL_BORDER, 0.5f))
                .setPadding(10).setMargin(4);
    }

    // ── Section 2: Domain Breakdown ──────────────────────────────────────────

    private void renderDomainBreakdown(Document doc, IsaaScore score, boolean hi) {
        doc.add(sectionHeading(hi ? "2.  डोमेन विश्लेषण" : "2.  Domain Breakdown"));

        Table table = new Table(UnitValue.createPercentArray(new float[] { 36, 10, 10, 44 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(6);

        table.addHeaderCell(th(hi ? "डोमेन" : "Domain"));
        table.addHeaderCell(th(hi ? "स्कोर" : "Score").setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(th(hi ? "अधिकतम" : "Max").setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(th(hi ? "स्तर" : "Level"));

        addDomainRow(table, hi ? "सामाजिक संबंध और पारस्परिकता" : "Social Relationship & Reciprocity",
                score.getDomain1Social(), 45);
        addDomainRow(table, hi ? "भावनात्मक प्रतिक्रिया" : "Emotional Responsiveness",
                score.getDomain2Emotional(), 25);
        addDomainRow(table, hi ? "भाषण, भाषा और संचार" : "Speech, Language & Communication",
                score.getDomain3Speech(), 45);
        addDomainRow(table, hi ? "व्यवहार पैटर्न" : "Behaviour Patterns",
                score.getDomain4Behaviour(), 35);
        addDomainRow(table, hi ? "संवेदी पहलू" : "Sensory Aspects",
                score.getDomain5Sensory(), 30);
        addDomainRow(table, hi ? "संज्ञानात्मक घटक" : "Cognitive Component",
                score.getDomain6Cognitive(), 20);

        doc.add(table);

        doc.add(new Paragraph(hi
                ? "रंग कुंजी:  हरा = कम चिंता  |  पीला = मध्यम  |  लाल = उच्च चिंता"
                : "Colour key:  Green = Low concern  |  Amber = Moderate  |  Red = High concern")
                .setFontSize(8).setFontColor(COL_TEXT_MUTED).setMarginBottom(14));
    }

    private void addDomainRow(Table table, String name, int score, int max) {
        float pct = max > 0 ? (float) score / max : 0f;
        int pctInt = Math.round(pct * 100);
        DeviceRgb barColor = pct < 0.40f ? BAR_LOW : pct < 0.70f ? BAR_MED : BAR_HIGH;

        // Domain name
        table.addCell(new Cell()
                .add(new Paragraph(name).setFontSize(10))
                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COL_BORDER, 0.5f))
                .setPadding(8));

        // Score (colored)
        table.addCell(new Cell()
                .add(new Paragraph(String.valueOf(score))
                        .setFontSize(11).setBold().setFontColor(barColor)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COL_BORDER, 0.5f))
                .setPadding(8).setTextAlignment(TextAlignment.CENTER));

        // Max
        table.addCell(new Cell()
                .add(new Paragraph(String.valueOf(max))
                        .setFontSize(10).setFontColor(COL_TEXT_MUTED)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COL_BORDER, 0.5f))
                .setPadding(8).setTextAlignment(TextAlignment.CENTER));

        // Visual bar + %
        Cell barCell = new Cell()
                .add(buildBar(pct, barColor))
                .add(new Paragraph(pctInt + "%").setFontSize(9).setFontColor(barColor))
                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COL_BORDER, 0.5f))
                .setPadding(8);
        table.addCell(barCell);
    }

    /** Builds a simple two-tone horizontal bar proportional to pct (0..1). */
    private Table buildBar(float pct, DeviceRgb fillColor) {
        pct = Math.max(0f, Math.min(1f, pct));
        Table bar;
        if (pct <= 0f) {
            bar = new Table(UnitValue.createPercentArray(new float[] { 100 }))
                    .setWidth(UnitValue.createPercentValue(100));
            bar.addCell(new Cell().setMinHeight(8f).setBackgroundColor(BAR_EMPTY)
                    .setBorder(Border.NO_BORDER));
        } else if (pct >= 1f) {
            bar = new Table(UnitValue.createPercentArray(new float[] { 100 }))
                    .setWidth(UnitValue.createPercentValue(100));
            bar.addCell(new Cell().setMinHeight(8f).setBackgroundColor(fillColor)
                    .setBorder(Border.NO_BORDER));
        } else {
            float f = pct * 100f;
            float e = 100f - f;
            bar = new Table(UnitValue.createPercentArray(new float[] { f, e }))
                    .setWidth(UnitValue.createPercentValue(100));
            bar.addCell(new Cell().setMinHeight(8f).setBackgroundColor(fillColor)
                    .setBorder(Border.NO_BORDER));
            bar.addCell(new Cell().setMinHeight(8f).setBackgroundColor(BAR_EMPTY)
                    .setBorder(Border.NO_BORDER));
        }
        bar.setMarginBottom(3f);
        return bar;
    }

    // ── Section 3: Recommendations ───────────────────────────────────────────

    private void renderRecommendations(Document doc, SeverityLevel severity, boolean hi) {
        doc.add(sectionHeading(hi ? "3.  अनुशंसाएँ और अगले कदम" : "3.  Recommendations & Next Steps"));

        List bullets = new List(ListNumberingType.DECIMAL)
                .setSymbolIndent(10).setMarginLeft(20).setMarginBottom(12).setFontSize(11);

        for (String item : recommendations(severity, hi)) {
            ListItem li = new ListItem(item);
            li.setMarginBottom(6);
            li.setFontColor(COL_BRAND_DARK);
            bullets.add(li);
        }
        doc.add(bullets);

        // Support contact box
        Table ctaTable = new Table(UnitValue.createPercentArray(new float[] { 100 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(16);
        ctaTable.addCell(new Cell()
                .add(new Paragraph(hi ? "सहायता के लिए संपर्क करें" : "Get Support")
                        .setFontSize(10).setBold().setFontColor(COL_ACCENT).setMarginBottom(4))
                .add(new Paragraph(
                        "ConnectPWD  |  1800-599-0019  |  www.connectpwd.org  |  helpdesk@connectpwd.org")
                        .setFontSize(10).setFontColor(COL_BRAND_DARK))
                .setBackgroundColor(COL_ACCENT_BG)
                .setBorderLeft(new SolidBorder(COL_ACCENT, 3f))
                .setBorderTop(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER)
                .setPadding(12));
        doc.add(ctaTable);
    }

    private java.util.List<String> recommendations(SeverityLevel severity, boolean hi) {
        if (severity == null)
            return java.util.Collections.emptyList();
        if (hi) {
            return switch (severity) {
                case NO_AUTISM -> java.util.List.of(
                        "वर्तमान मूल्यांकन में ऑटिज़्म स्पेक्ट्रम की विशेषताएँ नहीं पाई गईं।",
                        "यदि विकासात्मक चिंताएँ बनी रहें, तो बाल रोग विशेषज्ञ से परामर्श लें।",
                        "नियमित विकासात्मक मील-पत्थर की निगरानी जारी रखें।",
                        "ConnectPWD भविष्य में भी सहायता के लिए उपलब्ध है।");
                case MILD -> java.util.List.of(
                        "3 महीने के भीतर विकासात्मक बाल रोग विशेषज्ञ या बाल मनोचिकित्सक से परामर्श लें।",
                        "संचार कौशल मजबूत करने के लिए प्रारंभिक हस्तक्षेप भाषण चिकित्सा पर विचार करें।",
                        "छोटे समूहों में सामाजिक कौशल प्रशिक्षण से साथियों के साथ बातचीत में सुधार होता है।",
                        "चिंता कम करने के लिए नियमित दिनचर्या और दृश्य अनुसूचियाँ अपनाएँ।",
                        "घर पर संवेदी खेल (बनावट वाले खिलौने, जल खेल) भावनात्मक नियमन में सहायक है।");
                case MODERATE -> java.util.List.of(
                        "न्यूरोलॉजिस्ट, मनोवैज्ञानिक और भाषण चिकित्सक द्वारा तत्काल बहु-विषयक मूल्यांकन कराएँ।",
                        "ABA (व्यवहार विश्लेषण) चिकित्सा — प्रति सप्ताह न्यूनतम 10 घंटे — की दृढ़ता से अनुशंसा।",
                        "AAC (वैकल्पिक और संवर्धित संचार) उपकरण मूल्यांकन के लिए अनुरोध करें।",
                        "संवेदी एकीकरण पर केंद्रित व्यावसायिक चिकित्सा (OT) अनुशंसित है।",
                        "IEP/GIEP हकदारियों के लिए जिले के विशेष शिक्षा कार्यालय से संपर्क करें।",
                        "राष्ट्रीय न्यास, भारत सरकार के माध्यम से देखभालकर्ता सहायता समूहों से जुड़ें।");
                case SEVERE -> java.util.List.of(
                        "सरकार द्वारा मान्यता प्राप्त पुनर्वास केंद्र (NIMH/NIEPMD) में तत्काल मूल्यांकन कराएँ।",
                        "ABA चिकित्सा (20+ घंटे/सप्ताह) और संरचित शिक्षण (TEACCH) तत्काल आरंभ करें।",
                        "AAC उपकरण और PECS (पिक्चर एक्सचेंज कम्युनिकेशन) मूल्यांकन आवश्यक है।",
                        "मोटर कौशल विकास के लिए फिजियोथेरेपी (PT) मूल्यांकन।",
                        "सरकारी योजनाओं का लाभ लेने के लिए Unique Disability ID (UDID) के लिए आवेदन करें।",
                        "24/7 देखभालकर्ता सहायता — राष्ट्रीय न्यास: 1800-599-0019।");
            };
        }
        return switch (severity) {
            case NO_AUTISM -> java.util.List.of(
                    "Current screening does not indicate autism spectrum features at this time.",
                    "If developmental concerns persist, consult a paediatrician for a comprehensive evaluation.",
                    "Continue monitoring developmental milestones and schedule routine check-ups.",
                    "ConnectPWD is available for future support at www.connectpwd.org.");
            case MILD -> java.util.List.of(
                    "Schedule a consultation with a developmental paediatrician or child psychiatrist within 3 months.",
                    "Explore early intervention speech and language therapy to strengthen communication skills.",
                    "Social skills training in small groups can significantly improve peer interactions.",
                    "Establish predictable daily routines; use visual schedules to reduce anxiety.",
                    "Sensory play activities (textured toys, water play) support emotional self-regulation at home.");
            case MODERATE -> java.util.List.of(
                    "Urgent multidisciplinary evaluation (neurologist, psychologist, speech therapist) is recommended.",
                    "Applied Behaviour Analysis (ABA) therapy — minimum 10 hours/week — is strongly advised.",
                    "Request an Augmentative & Alternative Communication (AAC) device assessment.",
                    "Occupational therapy with a sensory integration focus is strongly recommended.",
                    "Contact your district's special education office for IEP / GIEP entitlements.",
                    "Caregiver support groups are available through The National Trust, Government of India.");
            case SEVERE -> java.util.List.of(
                    "Seek immediate assessment at a government-recognised rehabilitation centre (NIMH / NIEPMD).",
                    "ABA therapy (20+ hours/week) and structured teaching (TEACCH) are urgently recommended.",
                    "AAC device and PECS (Picture Exchange Communication System) assessment required without delay.",
                    "Physical therapy (PT) assessment for motor skill development.",
                    "Apply for a Unique Disability ID (UDID) to access government scheme benefits.",
                    "24/7 carer support and respite services — The National Trust: 1800-599-0019.");
        };
    }

    // ── Footer ───────────────────────────────────────────────────────────────

    private void renderFooter(Document doc, boolean hi) {
        SolidLine rule = new SolidLine(0.5f);
        rule.setColor(COL_BORDER);
        doc.add(new LineSeparator(rule).setMarginTop(14).setMarginBottom(8));

        String disclaimer = hi
                ? "यह रिपोर्ट NIMH / राष्ट्रीय न्यास, भारत सरकार द्वारा प्रकाशित ISAA ढांचे पर आधारित एक स्क्रीनिंग साधन है। "
                        + "यह नैदानिक निदान का विकल्प नहीं है। अंतिम निदान के लिए किसी योग्य मनोवैज्ञानिक या मनोचिकित्सक से परामर्श लें।"
                : "This report is a screening tool based on the ISAA framework published by NIMH / The National Trust, "
                        + "Government of India. It is not a substitute for a clinical diagnosis. "
                        + "For a definitive diagnosis, consult a qualified psychologist or psychiatrist.";

        doc.add(new Paragraph(disclaimer)
                .setFontSize(8).setFontColor(COL_TEXT_MUTED)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));

        doc.add(new Paragraph(
                "© " + LocalDate.now().getYear() + " ConnectPWD.org  —  ISAA v2.1")
                .setFontSize(7).setFontColor(COL_TEXT_MUTED)
                .setTextAlignment(TextAlignment.CENTER));
    }

    // ── Shared helpers ───────────────────────────────────────────────────────

    private Paragraph sectionHeading(String title) {
        return new Paragraph(title)
                .setFontSize(12).setBold().setFontColor(COL_BRAND_DARK)
                .setBackgroundColor(COL_SECTION_BG)
                .setPaddingLeft(10).setPaddingTop(7).setPaddingBottom(7)
                .setMarginTop(10).setMarginBottom(10)
                .setBorderLeft(new SolidBorder(COL_ACCENT, 3f))
                .setBorderTop(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER);
    }

    private Cell th(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(9).setBold())
                .setBackgroundColor(COL_TABLE_HEAD)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(8);
    }

    private String formatSeverity(SeverityLevel sev, boolean hi) {
        if (sev == null)
            return hi ? "अज्ञात" : "Unknown";
        return switch (sev) {
            case NO_AUTISM -> hi ? "ऑटिज़्म नहीं" : "No Autism Characteristics";
            case MILD -> hi ? "हल्का ऑटिज़्म स्पेक्ट्रम" : "Mild Autism Spectrum";
            case MODERATE -> hi ? "मध्यम ऑटिज़्म स्पेक्ट्रम" : "Moderate Autism Spectrum";
            case SEVERE -> hi ? "गंभीर ऑटिज़्म स्पेक्ट्रम" : "Severe Autism Spectrum";
        };
    }

    private String severityDescription(SeverityLevel sev, boolean hi) {
        if (sev == null)
            return hi ? "परिणाम उपलब्ध नहीं है।" : "Assessment result unavailable.";
        if (hi) {
            return switch (sev) {
                case NO_AUTISM -> "ISAA स्क्रीनिंग में ऑटिज़्म स्पेक्ट्रम विकार की विशेषताएँ नहीं पाई गईं। "
                        + "विकासात्मक मील-पत्थर की नियमित निगरानी जारी रखने की सलाह दी जाती है।";
                case MILD -> "मूल्यांकन में हल्के ऑटिज़्म स्पेक्ट्रम की विशेषताएँ पाई गईं। "
                        + "प्रारंभिक हस्तक्षेप और लक्षित सहायता के साथ महत्वपूर्ण सुधार संभव है।";
                case MODERATE -> "मूल्यांकन में मध्यम ऑटिज़्म स्पेक्ट्रम की विशेषताएँ पाई गईं। "
                        + "बहु-विषयक सहायता के साथ उल्लेखनीय प्रगति संभव है।";
                case SEVERE -> "मूल्यांकन में गंभीर ऑटिज़्म स्पेक्ट्रम की विशेषताएँ पाई गईं। "
                        + "तत्काल बहु-विषयक सहायता और विशेषज्ञ मार्गदर्शन आवश्यक है।";
            };
        }
        return switch (sev) {
            case NO_AUTISM -> "The ISAA screening did not identify characteristics of Autism Spectrum Disorder. "
                    + "Continued monitoring of developmental milestones is advised.";
            case MILD -> "The assessment identified mild autism spectrum characteristics. "
                    + "With early intervention and targeted support, significant progress is achievable.";
            case MODERATE -> "The assessment identified moderate autism spectrum characteristics. "
                    + "Substantial progress is possible with consistent multi-disciplinary support.";
            case SEVERE -> "The assessment identified severe autism spectrum characteristics. "
                    + "Immediate multi-disciplinary support and specialist guidance are required.";
        };
    }

    private DeviceRgb severityColor(SeverityLevel sev) {
        if (sev == null)
            return COL_MODERATE;
        return switch (sev) {
            case NO_AUTISM -> COL_NONE;
            case MILD -> COL_MILD;
            case MODERATE -> COL_MODERATE;
            case SEVERE -> COL_SEVERE;
        };
    }

    /** Creates a very pale tint of the given colour for badge backgrounds. */
    private DeviceRgb lighten(DeviceRgb color) {
        float[] c = color.getColorValue();
        return new DeviceRgb(blend(c[0]), blend(c[1]), blend(c[2]));
    }

    private float blend(float c) {
        return c + (1f - c) * 0.88f;
    }
}
