// src/main/java/com/rafhi/controller/BeritaAcaraResource.java
package com.rafhi.controller;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
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
import com.rafhi.dto.Signatory;

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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // --- Judul Umum ---
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph title = new Paragraph("BERITA ACARA " + request.jenisBeritaAcara.toUpperCase(), bold);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph("PERUBAHAN " + request.kategoriAplikasi.toUpperCase(), bold);
        sub.setAlignment(Element.ALIGN_CENTER);
        doc.add(sub);
        
        if (request.namaAplikasiSpesifik != null && !request.namaAplikasiSpesifik.isEmpty()) {
            Paragraph appName = new Paragraph(request.namaAplikasiSpesifik.toUpperCase(), bold);
            appName.setAlignment(Element.ALIGN_CENTER);
            doc.add(appName);
        }

        Paragraph nomor = new Paragraph("No. " + request.nomorBA);
        nomor.setAlignment(Element.ALIGN_CENTER);
        doc.add(nomor);

        doc.add(Chunk.NEWLINE);

        // --- Logika Kondisional untuk Konten ---
        String jenisBA = request.jenisBeritaAcara;

        if ("UAT".equalsIgnoreCase(jenisBA)) {
            // --- KONTEN UNTUK UAT ---
            String kalimatPembuka = "Pada hari ini " + getTanggalFormal(request.tanggalPelaksanaan) +
                    ", telah dibuat Berita Acara " + request.jenisBeritaAcara +
                    (request.tahap != null ? " " + request.tahap : "") +
                    " terhadap permohonan perubahan aplikasi merujuk pada " +
                    request.tipeRequest + " dengan nomor surat " + request.nomorSuratRequest +
                    " tanggal " + getTanggalFormal(request.tanggalSuratRequest) +
                    " perihal \"" + request.judulPekerjaan + "\".";
            doc.add(new Paragraph(kalimatPembuka));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100f);
            table.setSpacingBefore(10);
            table.addCell(getCell("No", Element.ALIGN_CENTER, true, true));
            table.addCell(getCell("Kegiatan", Element.ALIGN_CENTER, true, true));
            table.addCell(getCell("Status", Element.ALIGN_CENTER, true, true));
            table.addCell(getCell("Keterangan", Element.ALIGN_CENTER, true, true));

            int i = 1;
            for (Fitur f : request.fiturList) {
                table.addCell(getCell(String.valueOf(i++), Element.ALIGN_CENTER, false, true));
                table.addCell(getCell(f.deskripsi, Element.ALIGN_LEFT, false, true));
                table.addCell(getCell(f.status, Element.ALIGN_CENTER, false, true));
                table.addCell(getCell(f.catatan != null ? f.catatan : "-", Element.ALIGN_CENTER, false, true));
            }
            doc.add(table);

        } else if ("Deployment".equalsIgnoreCase(jenisBA)) {
            // --- KONTEN UNTUK DEPLOYMENT ---
            String kalimatPembuka = "Pada hari ini " + getTanggalFormal(request.tanggalPelaksanaan) +
                    ", telah dibuat Berita Acara Penyebaran (" + request.jenisBeritaAcara + ")" +
                    (request.tahap != null ? " " + request.tahap : "") +
                    " fitur tambahan berdasarkan BA UAT nomor " + request.nomorBaUat +
                    " tentang permohonan perubahan aplikasi merujuk pada " +
                    request.tipeRequest + " dengan nomor surat " + request.nomorSuratRequest +
                    " tanggal " + getTanggalFormal(request.tanggalSuratRequest) +
                    " perihal \"" + request.judulPekerjaan + "\".";
            doc.add(new Paragraph(kalimatPembuka));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100f);
            table.setSpacingBefore(10);
            table.addCell(getCell("No.", Element.ALIGN_CENTER, true, true));
            table.addCell(getCell("Aktifitas", Element.ALIGN_CENTER, true, true));
            table.addCell(getCell("Status", Element.ALIGN_CENTER, true, true));

            table.addCell(getCell("1.", Element.ALIGN_CENTER, false, true));
            table.addCell(getCell("Pengecekan validasi sesuai dengan UAT", Element.ALIGN_LEFT, false, true));
            table.addCell(getCell("OK", Element.ALIGN_CENTER, false, true));

            table.addCell(getCell("2.", Element.ALIGN_CENTER, false, true));
            table.addCell(getCell("Penyebaran / deployment fitur baru", Element.ALIGN_LEFT, false, true));
            table.addCell(getCell("OK", Element.ALIGN_CENTER, false, true));
            
            table.addCell(getCell("3.", Element.ALIGN_CENTER, false, true));
            table.addCell(getCell("Pengujian hasil proses Penyebaran/ deployment", Element.ALIGN_LEFT, false, true));
            table.addCell(getCell("OK", Element.ALIGN_CENTER, false, true));
            
            doc.add(table);

        } else {
            doc.add(new Paragraph("Jenis Berita Acara tidak dikenali: " + jenisBA));
        }

        doc.add(Chunk.NEWLINE);

        // --- Tanda Tangan DINAMIS ---
        doc.add(new Paragraph("Demikian Berita Acara ini dibuat untuk dipergunakan sebagaimana mestinya."));
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);

        List<Signatory> mengetahuiList = request.signatoryList.stream()
                .filter(s -> "mengetahui".equalsIgnoreCase(s.tipe))
                .toList();

        List<Signatory> utamaList = request.signatoryList.stream()
                .filter(s -> "utama".equalsIgnoreCase(s.tipe))
                .toList();

        if (!mengetahuiList.isEmpty()) {
            doc.add(new Paragraph("Mengetahui,"));
            PdfPTable mengetahuiTable = new PdfPTable(mengetahuiList.size());
            mengetahuiTable.setWidthPercentage(100f);

            for (Signatory s : mengetahuiList) {
                mengetahuiTable.addCell(getCell(s.perusahaan, Element.ALIGN_CENTER, false, false));
            }
            for (Signatory s : mengetahuiList) {
                mengetahuiTable.addCell(getCell("\n\n\n\n" + s.nama, Element.ALIGN_CENTER, false, false));
            }
            for (Signatory s : mengetahuiList) {
                mengetahuiTable.addCell(getCell(s.jabatan, Element.ALIGN_CENTER, false, false));
            }
            doc.add(mengetahuiTable);
            doc.add(Chunk.NEWLINE);
        }

        PdfPTable utamaTable = new PdfPTable(utamaList.size());
        utamaTable.setWidthPercentage(100f);

        for (Signatory s : utamaList) {
            utamaTable.addCell(getCell(s.perusahaan, Element.ALIGN_CENTER, false, false));
        }
        for (Signatory s : utamaList) {
            utamaTable.addCell(getCell("\n\n\n\n" + s.nama, Element.ALIGN_CENTER, false, false));
        }
        for (Signatory s : utamaList) {
            utamaTable.addCell(getCell(s.jabatan, Element.ALIGN_CENTER, false, false));
        }
        doc.add(utamaTable);

        // --- Selesai ---
        doc.close();
        return Response.ok(out.toByteArray())
                .header("Content-Disposition", "attachment; filename=berita-acara.pdf")
                .build();
    }

    private PdfPCell getCell(String text, int alignment, boolean bold, boolean border) {
        Font font = bold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD) : FontFactory.getFont(FontFactory.HELVETICA);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        if (!border) {
            cell.setBorder(Rectangle.NO_BORDER);
        }
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