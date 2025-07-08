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

@Path("/berita-acara/uat")
public class BeritaAcaraUAT {
    @GET
    @Produces("application/pdf")
    public Response generate(
        @QueryParam("nomor_uat") String nomorUAT,
        @QueryParam("tanggal_ba") String tanggalBA,
        @QueryParam("tanggal_permohonan") String tanggalPermohonan,
        @QueryParam("perihal") String perihal,
        @QueryParam("divisi") String divisi,
        @QueryParam("tanggal_deploy") String tanggalDeploy,
        @QueryParam("keterangan") List<String> keterangan,
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

        Paragraph p1 = new Paragraph("BERITA ACARA USER ACCEPTANCE TEST (UAT)", title);
        p1.setAlignment(Element.ALIGN_CENTER);
        doc.add(p1);
        Paragraph p2 = new Paragraph("PERUBAHAN",title);
        p2.setAlignment(Element.ALIGN_CENTER);
        doc.add(p2);
        String nomorDok = nomor == null ? "____/____/____/2025" : nomor;
        Paragraph p3 = new Paragraph("No. " + nomorDok, title);
        p3.setAlignment(Element.ALIGN_CENTER);
        doc.add(p3);
        
        doc.add(new Paragraph("Pada hari ini tanggal " + formatDate(tanggalBA) +
            " telah dibuat Berita Acara User Acceptance Test (UAT) tahap I terhadap permohonan perubahan aplikasi merujuk pada change / job request dengan nomor surat"
            + nomorUAT + " tanggal " + formatMonthYear(tanggalPermohonan) + " perihal " + perihal + ", yang dikirimkan oleh " + divisi + ".", normal));

        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Pekerjaan pengujian telah dilakukan pada hari " +
            formatDate(tanggalDeploy) + ". Dari hasil pengujian yang dilakukan secara terpadu antara PT PLN (persero) dan PT Indonesia Comnets Plus didapatkan:", normal));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(new float[]{1, 5, 3, 4});
        table.setWidthPercentage(100);
        table.addCell(headerCell("No."));
        table.addCell(headerCell("Kegiatan"));
        table.addCell(headerCell("Status"));
        table.addCell(headerCell("Keterangan"));

        for (int i = 0; i < 1; i++) {
            table.addCell(cell((i + 1) + ""));
            table.addCell(cell(i < aktivitas.size() ? aktivitas.get(i) : ""));
            table.addCell(cell(i < status.size() ? status.get(i) : ""));
            table.addCell(cell(i < keterangan.size() ? keterangan.get(i) : ""));
        }
            table.addCell(cell("2"));
            table.addCell(cell("Cacat fitur < 5% "));
            table.addCell(cell("OK"));
            table.addCell(cell("-"));

            table.addCell(cell("3"));
            table.addCell(cell("Penyebaran Aplikasi"));
            table.addCell(cell("OK"));
            table.addCell(cell("-"));

        doc.add(table);
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Demikian Berita Acara ini dibuat untuk dipergunakan sebagaimana mestinya.", normal));
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);

        // Tanda tangan
        PdfPTable ttd = new PdfPTable(3);
            ttd.setWidthPercentage(100f);
            ttd.setSpacingBefore(30f);

            ttd.addCell(noBorderCell("PT PLN (Persero)"));
            ttd.addCell(noBorderCell("Tim Aplikasi / Pihak Terkait"));
            ttd.addCell(noBorderCell("PT Indonesia Comnets Plus"));

            ttd.addCell(noBorderCell("\n\n\n\n(...............................)"));
            ttd.addCell(noBorderCell("\n\n\n\n(...............................)"));
            ttd.addCell(noBorderCell("\n\n\n\n(...............................)"));

        doc.add(ttd);
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

    private PdfPCell noBorderCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 11);
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

}
