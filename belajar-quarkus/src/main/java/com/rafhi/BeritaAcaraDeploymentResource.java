package com.rafhi;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/berita-acara/deployment")
public class BeritaAcaraDeploymentResource {

    @GET
    @Produces("application/pdf")
    public Response generate(
        @QueryParam("nomor_uat") String nomorUAT,
        @QueryParam("tanggal_ba") String tanggalBA,
        @QueryParam("tanggal_permohonan") String tanggalPermohonan,
        @QueryParam("perihal") String perihal,
        @QueryParam("divisi") String divisi,
        @QueryParam("tanggal_deploy") String tanggalDeploy,
        @QueryParam("aktivitas") List<String> aktivitas,
        @QueryParam("status") List<String> status,
        @QueryParam("nomor") String nomor
    ) throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);

        // Judul
        Paragraph p1 = new Paragraph("BERITA ACARA PENYEBARAN (DEPLOYMENT)", title);
        p1.setAlignment(Element.ALIGN_CENTER);
        doc.add(p1);
        // subjudul
        Paragraph p2 = new Paragraph("PERUBAHAN",bold);
        p2.setAlignment(Element.ALIGN_CENTER);
        doc.add(p2);
        

        String nomorDok = nomor == null ? "____/____/____/2025" : nomor;
        Paragraph p3 = new Paragraph("No. " + nomorDok, normal);
        p3.setAlignment(Element.ALIGN_CENTER);
        doc.add(p3);
        // Isi
        doc.add(new Paragraph("Pada hari ini tanggal " + formatDate(tanggalBA) +
            " telah dibuat Berita Acara Penyebaran (Deployment) tahap I fitur tambahan berdasarkan BA UAT nomor "
            + nomorUAT + " tentang permohonan perubahan aplikasi merujuk pada change / job request tanggal " +
            formatMonthYear(tanggalPermohonan) + " perihal " + perihal + ", yang dikirimkan oleh " + divisi + ".", normal));

        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Pekerjaan penyebaran/deployment telah dilakukan pada hari " +
            formatDate(tanggalDeploy) + ". Adapun proses untuk penyebaran/deployment terdiri dari:", normal));
        doc.add(Chunk.NEWLINE);

        // Tabel Aktivitas
        PdfPTable table = new PdfPTable(new float[]{1, 5, 3});
        table.setWidthPercentage(100);
        table.addCell(headerCell("No."));
        table.addCell(headerCell("Aktifitas"));
        table.addCell(headerCell("Status"));

        for (int i = 0; i < 3; i++) {
            table.addCell(cell((i + 1) + ""));
            table.addCell(cell(i < aktivitas.size() ? aktivitas.get(i) : ""));
            table.addCell(cell(i < status.size() ? status.get(i) : ""));
        }

        doc.add(table);
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Demikian Berita Acara ini dibuat untuk dipergunakan sebagaimana mestinya.", normal));
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);

        // Tanda tangan
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);
        signTable.addCell(centerCell("PT PLN (Persero)"));
        signTable.addCell(centerCell("PT Indonesia Comnets Plus"));
        signTable.addCell(centerCell("\n\n\n\n(...............................)"));
        signTable.addCell(centerCell("\n\n\n\n(...............................)"));
        doc.add(signTable);

        doc.close();

        return Response.ok(out.toByteArray())
            .type("application/pdf")
            .header("Content-Disposition", "attachment; filename=\"berita-acara-deployment.pdf\"")
            .build();
    }

    private String formatDate(String raw) {
        LocalDate date = LocalDate.parse(raw);
        return date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
    }

    private String formatMonthYear(String raw) {
        LocalDate date = LocalDate.parse(raw);
        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell cell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        return cell;
    }

    private PdfPCell centerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
}
