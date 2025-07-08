package com.rafhi;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

@Path("/berita-acara")
public class BeritaAcaraResource {

    @GET
    @Produces("application/pdf")
    public Response generate(
            @QueryParam("pihak1_nama") String pihak1Nama,
            @QueryParam("pihak1_jabatan") String pihak1Jabatan,
            @QueryParam("pihak2_nama") String pihak2Nama,
            @QueryParam("pihak2_jabatan") String pihak2Jabatan,
            @QueryParam("lokasi") String lokasi
    ) throws Exception {

        // Tanggal otomatis (contoh: Senin, 7 Juli 2025)
        LocalDate today = LocalDate.now();
        String hari = today.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("id", "ID"));
        String bulan = today.getMonth().getDisplayName(TextStyle.FULL, new Locale("id", "ID"));
        String tanggal = hari + ", " + today.getDayOfMonth() + " " + bulan + " " + today.getYear();

        // Nomor berita acara
        String nomor = "001/BA/VII/" + today.getYear();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        doc.add(new Paragraph("BERITA ACARA", titleFont));
        doc.add(new Paragraph("Nomor: " + nomor));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Pada hari " + tanggal + " bertempat di " + lokasi + ", telah dilakukan kegiatan oleh dua pihak sebagai berikut:"));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Pihak Pertama:", bold));
        doc.add(new Paragraph("Nama    : " + pihak1Nama));
        doc.add(new Paragraph("Jabatan : " + pihak1Jabatan));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Pihak Kedua:", bold));
        doc.add(new Paragraph("Nama    : " + pihak2Nama));
        doc.add(new Paragraph("Jabatan : " + pihak2Jabatan));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Demikian berita acara ini dibuat dan ditandatangani oleh kedua belah pihak untuk digunakan sebagaimana mestinya."));
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);

        // Tanda tangan manual (kosong)
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(getCell("Pihak Pertama", Element.ALIGN_CENTER));
        table.addCell(getCell("Pihak Kedua", Element.ALIGN_CENTER));
        table.addCell(getCell("\n\n\n\n( " + pihak1Nama + " )", Element.ALIGN_CENTER));
        table.addCell(getCell("\n\n\n\n( " + pihak2Nama + " )", Element.ALIGN_CENTER));
        doc.add(table);

        doc.close();

        return Response.ok(out.toByteArray())
                .type("application/pdf")
                .header("Content-Disposition", "attachment; filename=\"berita-acara.pdf\"")
                .build();
    }

    private PdfPCell getCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setPadding(10);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(PdfPCell.NO_BORDER);
        return cell;
    }
}
