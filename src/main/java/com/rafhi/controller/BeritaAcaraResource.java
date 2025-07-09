package com.rafhi.controller;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rafhi.dto.BeritaAcaraRequest;
import com.rafhi.dto.Fitur;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("/berita-acara")
@Produces("application/pdf")
@Consumes("application/json")
public class BeritaAcaraResource {

    @POST
    @Path("/generate")
    public Response generateBeritaAcara(BeritaAcaraRequest request) throws Exception {
        System.out.println("== DEBUG REQUEST ==");
        System.out.println("Judul: " + request.judulPekerjaan);
        System.out.println("Jumlah fitur: " + (request.fiturList != null ? request.fiturList.size() : "NULL"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // Tambahkan judul & header
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph title = new Paragraph("BERITA ACARA " + request.jenisBeritaAcara.toUpperCase(), bold);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph("PERUBAHAN " + request.kategoriAplikasi.toUpperCase(), bold);
        sub.setAlignment(Element.ALIGN_CENTER);
        doc.add(sub);

        Paragraph nomor = new Paragraph("No. " + request.nomorBA + "/" + request.tahun);
        nomor.setAlignment(Element.ALIGN_CENTER);
        doc.add(nomor);

        doc.add(Chunk.NEWLINE);

        // Paragraf utama
        String kalimatPembuka = "Pada hari ini " + getTanggalFormal(request.tanggalPelaksanaan) + 
            ", telah dibuat Berita Acara " + request.jenisBeritaAcara + 
            " terhadap permohonan perubahan aplikasi merujuk pada " + 
            request.tipeRequest + " dengan nomor surat " + request.nomorSuratRequest + 
            " tanggal " + getTanggalFormal(request.tanggalSuratRequest) + 
            " perihal \"" + request.judulPekerjaan + "\".";

        doc.add(new Paragraph(kalimatPembuka));
        doc.add(Chunk.NEWLINE);

        // Tabel aktivitas
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(10);
        table.addCell(getCell("No", Element.ALIGN_CENTER, true));
        table.addCell(getCell("Kegiatan", Element.ALIGN_CENTER, true));
        table.addCell(getCell("Status", Element.ALIGN_CENTER, true));

        int i = 1;
        for (Fitur f : request.fiturList) {
            table.addCell(getCell(String.valueOf(i++), Element.ALIGN_CENTER, false));
            table.addCell(getCell(f.deskripsi, Element.ALIGN_LEFT, false));
            table.addCell(getCell(f.status + (f.catatan != null ? " (" + f.catatan + ")" : ""), Element.ALIGN_CENTER, false));
        }

        doc.add(table);
        doc.add(Chunk.NEWLINE);

        // Tanda tangan
        doc.add(new Paragraph("Demikian Berita Acara ini dibuat untuk dipergunakan sebagaimana mestinya."));
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);

        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100f);
        signTable.addCell(getCell("PT PLN (Persero)", Element.ALIGN_CENTER, false));
        signTable.addCell(getCell("PT Indonesia Comnets Plus", Element.ALIGN_CENTER, false));
        signTable.addCell(getCell("\n\n\n\n" + request.namaPenandatangan, Element.ALIGN_CENTER, false));
        signTable.addCell(getCell("\n\n\n\n(..............................)", Element.ALIGN_CENTER, false));
        signTable.addCell(getCell(request.jabatanPenandatangan, Element.ALIGN_CENTER, false));
        signTable.addCell(getCell("", Element.ALIGN_CENTER, false));
        doc.add(signTable);

        doc.close();
        return Response.ok(out.toByteArray())
                .header("Content-Disposition", "attachment; filename=berita-acara.pdf")
                .build();
    }

    private PdfPCell getCell(String text, int alignment, boolean bold) {
            Font font = bold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD) : FontFactory.getFont(FontFactory.HELVETICA);
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setHorizontalAlignment(alignment);
            cell.setBorder(Rectangle.NO_BORDER);  // Tambahan ini
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
