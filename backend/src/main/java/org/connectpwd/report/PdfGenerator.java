package org.connectpwd.report;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.connectpwd.scoring.IsaaScore;
import org.connectpwd.scoring.SeverityLevel;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Slf4j
@Component
public class PdfGenerator {

    public byte[] generateReport(IsaaScore score, String clientName, String caregiverName, String language) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            boolean isHindi = "hi".equals(language);

            // Title
            document.add(new Paragraph(isHindi ? "ConnectPWD — ISAA मूल्यांकन रिपोर्ट" : "ConnectPWD — ISAA Assessment Report")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // Client info
            document.add(new Paragraph((isHindi ? "ग्राहक: " : "Client: ") + clientName).setFontSize(12));
            document.add(new Paragraph((isHindi ? "देखभालकर्ता: " : "Caregiver: ") + caregiverName).setFontSize(12));
            document.add(new Paragraph("Session ID: " + score.getSessionId()).setFontSize(10).setMarginBottom(15));

            // Severity pill
            String severityLabel = formatSeverity(score.getSeverity(), isHindi);
            DeviceRgb severityColor = getSeverityColor(score.getSeverity());
            document.add(new Paragraph(severityLabel)
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(severityColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            // Total score and disability
            document.add(new Paragraph(
                    (isHindi ? "कुल स्कोर: " : "Total Score: ") + score.getTotalScore() + " / 200")
                    .setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(
                    (isHindi ? "विकलांगता प्रतिशत: " : "Disability Percentage: ") + score.getDisabilityPct() + "%")
                    .setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            // Domain table
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1}));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(createHeaderCell(isHindi ? "डोमेन" : "Domain"));
            table.addHeaderCell(createHeaderCell(isHindi ? "स्कोर" : "Score"));
            table.addHeaderCell(createHeaderCell(isHindi ? "अधिकतम" : "Max"));

            addDomainRow(table, isHindi ? "सामाजिक संबंध और पारस्परिकता" : "Social Relationship & Reciprocity", score.getDomain1Social(), 45);
            addDomainRow(table, isHindi ? "भावनात्मक प्रतिक्रिया" : "Emotional Responsiveness", score.getDomain2Emotional(), 25);
            addDomainRow(table, isHindi ? "भाषण, भाषा और संचार" : "Speech, Language & Communication", score.getDomain3Speech(), 45);
            addDomainRow(table, isHindi ? "व्यवहार पैटर्न" : "Behaviour Patterns", score.getDomain4Behaviour(), 35);
            addDomainRow(table, isHindi ? "संवेदी पहलू" : "Sensory Aspects", score.getDomain5Sensory(), 30);
            addDomainRow(table, isHindi ? "संज्ञानात्मक घटक" : "Cognitive Component", score.getDomain6Cognitive(), 20);

            document.add(table);

            // Footer
            document.add(new Paragraph(isHindi
                    ? "यह रिपोर्ट NIMH/राष्ट्रीय न्यास, भारत सरकार द्वारा प्रकाशित ISAA ढांचे पर आधारित है।"
                    : "This report is based on the ISAA framework published by NIMH / The National Trust, Government of India.")
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                .setBackgroundColor(new DeviceRgb(41, 50, 65))
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private void addDomainRow(Table table, String domain, int score, int max) {
        table.addCell(new Cell().add(new Paragraph(domain)));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(score)).setTextAlignment(TextAlignment.CENTER)));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(max)).setTextAlignment(TextAlignment.CENTER)));
    }

    private String formatSeverity(SeverityLevel severity, boolean isHindi) {
        return switch (severity) {
            case NO_AUTISM -> isHindi ? "ऑटिज़्म नहीं" : "No Autism";
            case MILD -> isHindi ? "हल्का" : "Mild";
            case MODERATE -> isHindi ? "मध्यम" : "Moderate";
            case SEVERE -> isHindi ? "गंभीर" : "Severe";
        };
    }

    private DeviceRgb getSeverityColor(SeverityLevel severity) {
        return switch (severity) {
            case NO_AUTISM -> new DeviceRgb(34, 139, 34);
            case MILD -> new DeviceRgb(255, 165, 0);
            case MODERATE -> new DeviceRgb(255, 69, 0);
            case SEVERE -> new DeviceRgb(220, 20, 60);
        };
    }
}
