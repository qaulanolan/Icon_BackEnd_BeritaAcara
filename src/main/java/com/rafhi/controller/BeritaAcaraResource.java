package com.rafhi.controller;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.rafhi.dto.BeritaAcaraRequest;
import com.rafhi.dto.Fitur;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Path("/berita-acara")
@Produces("application/pdf")
@Consumes("application/json")
public class BeritaAcaraResource {

    @POST
    @Path("/generate")
    public Response generate(BeritaAcaraRequest request) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        // === HEADER LOGO ===
        PdfPTable logoTable = new PdfPTable(2);
        logoTable.setWidthPercentage(100);
        Image plnLogo = Image.getInstance("src/main/resources/logo-pln.png");
        Image iconLogo = Image.getInstance("src/main/resources/logo-iconplus.png");
        plnLogo.scaleToFit(60, 60);
        iconLogo.scaleToFit(60, 60);

<<<<<<< Updated upstream
        PdfPCell leftLogo = new PdfPCell(plnLogo);
        PdfPCell rightLogo = new PdfPCell(iconLogo);
        leftLogo.setBorder(Rectangle.NO_BORDER);
        rightLogo.setBorder(Rectangle.NO_BORDER);
        rightLogo.setHorizontalAlignment(Element.ALIGN_RIGHT);
=======
        // 2. Tambahkan Logo Header
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        
        // Coba muat logo dari resources
        try (InputStream plnIs = getClass().getResourceAsStream("/images/iconplus_logo.png");
             InputStream iconIs = getClass().getResourceAsStream("/images/pln_logo.png")) {
            
            if (iconIs != null) {
                Image iconLogo = Image.getInstance(iconIs.readAllBytes());
                iconLogo.scaleToFit(100, 50);
                PdfPCell iconCell = new PdfPCell(iconLogo);
                iconCell.setBorder(Rectangle.NO_BORDER);
                iconCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                headerTable.addCell(iconCell);
            }
>>>>>>> Stashed changes

        logoTable.addCell(leftLogo);
        logoTable.addCell(rightLogo);
        doc.add(logoTable);
        doc.add(Chunk.NEWLINE);

        // === JUDUL ===
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        Paragraph title = new Paragraph("BERITA ACARA " + request.jenisBeritaAcara.toUpperCase(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph("PERUBAHAN APLIKASI " + request.kategoriAplikasi.toUpperCase(), titleFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        doc.add(sub);

        Paragraph nomor = new Paragraph("No. " + request.nomorBA + "/" + request.tahun, titleFont);
        nomor.setAlignment(Element.ALIGN_CENTER);
        doc.add(nomor);
        doc.add(Chunk.NEWLINE);

        // === PARAGRAF PEMBUKA ===
        String tanggalPelaksanaan = getTanggalFormal(request.tanggalPelaksanaan);
        String tanggalSurat = getTanggalFormal(request.tanggalSuratRequest);
        Paragraph pembuka = new Paragraph();

        if (request.jenisBeritaAcara.equalsIgnoreCase("Deployment")) {
            pembuka.setFont(textFont);
            pembuka.add("Pada hari ini " + tanggalPelaksanaan + ", telah dibuat Berita Acara Penyebaran (Deployment) tahap I fitur tambahan berdasarkan BA UAT nomor "
                    + request.nomorBA + " tentang permohonan perubahan aplikasi merujuk pada "
                    + request.tipeRequest + " dengan nomor surat " + request.nomorSuratRequest + " tanggal " + tanggalSurat
                    + " perihal \"" + request.judulPekerjaan + "\".\n\n"
                    + "Pekerjaan penyebaran telah dilakukan pada hari " + tanggalPelaksanaan + ". Adapun proses penyebaran terdiri dari:");
        } else {
            pembuka.setFont(textFont);
            pembuka.add("Pada hari ini " + tanggalPelaksanaan + ", telah dibuat Berita Acara User Acceptance Test (UAT) terhadap permohonan perubahan aplikasi merujuk pada "
                    + request.tipeRequest + " dengan nomor surat " + request.nomorSuratRequest + " tanggal " + tanggalSurat
                    + " perihal \"" + request.judulPekerjaan + "\".");
        }

        doc.add(pembuka);
        doc.add(Chunk.NEWLINE);

        // === TABEL KEGIATAN ===
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{1f, 5f, 2f, 3f});
        table.setHeaderRows(1);

        table.addCell(getHeaderCell("No"));
        table.addCell(getHeaderCell("Aktivitas"));
        table.addCell(getHeaderCell("Status"));
        table.addCell(getHeaderCell("Keterangan"));

        List<Fitur> fiturList = request.fiturList;
        int index = 1;
        for (Fitur f : fiturList) {
            table.addCell(getBodyCell(String.valueOf(index++)));
            table.addCell(getBodyCell(f.deskripsi));
            table.addCell(getBodyCell(f.status));
            table.addCell(getBodyCell(f.catatan != null ? f.catatan : "-"));
        }

        doc.add(table);
        doc.add(Chunk.NEWLINE);

        // === PENUTUP PARAGRAF ===
        Paragraph penutup = new Paragraph("Demikian Berita Acara ini dibuat untuk dipergunakan sebagaimana mestinya.", textFont);
        doc.add(penutup);
        doc.add(Chunk.NEWLINE);

        // === TANDA TANGAN ===
        PdfPTable sign = new PdfPTable(2);
        sign.setWidthPercentage(100);
        sign.setWidths(new float[]{1f, 1f});

        sign.addCell(getCenterCell("PT PLN (Persero)", false));
        sign.addCell(getCenterCell("PT Indonesia Comnets Plus", false));

        sign.addCell(getCenterCell("\n\n\n" + request.namaPenandatangan, false));
        sign.addCell(getCenterCell("(...............................)", false));

        sign.addCell(getCenterCell(request.jabatanPenandatangan, false));
        sign.addCell(getCenterCell("", false));

        doc.add(sign);
        doc.close();

        return Response.ok(out.toByteArray())
                .header("Content-Disposition", "attachment; filename=berita-acara.pdf")
                .build();
    }

    private PdfPCell getHeaderCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        return cell;
    }

    private PdfPCell getBodyCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell getCenterCell(String text, boolean bold) {
        Font font = bold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11) : FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private String getTanggalFormal(String dateStr) {
        try {
            DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date = LocalDate.parse(dateStr, inputFormat);
            DayOfWeek day = date.getDayOfWeek();
            String hari = day.getDisplayName(TextStyle.FULL, new Locale("id", "ID"));
            DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));
            return hari + ", " + date.format(outputFormat);
        } catch (Exception e) {
            return dateStr;
        }
    }
}
