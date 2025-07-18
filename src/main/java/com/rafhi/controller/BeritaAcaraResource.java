package com.rafhi.controller;

import com.rafhi.dto.BeritaAcaraRequest;
import com.rafhi.dto.Fitur;
import com.rafhi.dto.Signatory;
import com.rafhi.helper.DateToWordsHelper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Impor Jsoup
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

// Impor Apache POI
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;

// Impor untuk histori
import com.rafhi.entity.BeritaAcaraHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import com.rafhi.dto.HistoryResponseDTO;
import java.util.stream.Collectors;
import jakarta.ws.rs.PathParam;

// Impor untuk konversi PDF
// import org.docx4j.Docx4J;
// import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

@Path("/berita-acara")
@ApplicationScoped
public class BeritaAcaraResource {

    @Inject
    Jsonb jsonb; // Suntikkan JSON-B untuk konversi ke JSON

    @POST
    @Path("/generate-docx")
    @Produces("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    @Consumes("application/json")
    @Transactional // Tambahkan anotasi ini untuk memastikan operasi database dilakukan dalam konteks transaksi
    public Response generateDocx(BeritaAcaraRequest request) throws Exception {
        String templateFileName;

        if ("Deployment".equalsIgnoreCase(request.jenisBeritaAcara)) {
            templateFileName = "template_deploy.docx";
        } else { // Asumsi jenisnya adalah UAT
            // Hitung jumlah penandatangan dengan tipe "utama"
            // long countUtama = request.signatoryList.stream()
            long countUtama = request.signatoryList.stream().filter(s -> s.tipe.startsWith("utama")).count();
                // .filter(s -> "utama".equals(s.tipe))
                // .count();
                
            if (countUtama == 3) {
                templateFileName = "template_uat_signatory4.docx"; 
            } else if (countUtama == 4) {
                templateFileName = "template_uat_signatory5.docx";
            } else { // default UAT
                templateFileName = "template_uat.docx";
            }
        }

        String templatePath = "/templates/" + templateFileName;
        InputStream templateInputStream = getClass().getResourceAsStream(templatePath);

        if (templateInputStream == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Template file not found at path: " + templatePath)
                           .build();
        }

        try (XWPFDocument document = new XWPFDocument(templateInputStream)) {
            // Langkah 1: Siapkan data pengganti
            Map<String, String> replacements = buildReplacementsMap(request);

            // Langkah 2: Ganti semua placeholder teks biasa
            replaceTextPlaceholders(document, replacements);

            // Langkah 3: Ganti placeholder deskripsi fitur dengan konten HTML secara khusus
            if ("UAT".equalsIgnoreCase(request.jenisBeritaAcara) && request.fiturList != null && !request.fiturList.isEmpty()) {
                Fitur fitur = request.fiturList.get(0); // Mengambil fitur pertama
                replacePlaceholderWithHtml(document, "${fitur.deskripsi}", fitur.deskripsi);
            }
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);

            byte[] docxBytes = out.toByteArray(); // Simpan hasil docx ke variabel

            // --- LOGIKA BARU: Simpan ke History ---
            BeritaAcaraHistory history = new BeritaAcaraHistory();
            history.nomorBA = request.nomorBA;
            history.jenisBeritaAcara = request.jenisBeritaAcara;
            history.judulPekerjaan = request.judulPekerjaan;
            history.generationTimestamp = LocalDateTime.now();
            history.requestJson = jsonb.toJson(request); // Ubah request menjadi string JSON
            history.fileContent = docxBytes; // Simpan file

            // Di dalam metode generateDocx di BeritaAcaraResource.java
            history.persistAndFlush(); // Gunakan persistAndFlush untuk mendapatkan ID segera

            ResponseBuilder response = Response.ok(new ByteArrayInputStream(docxBytes));
            response.header("Content-Disposition", "inline; filename=BA-" + request.nomorBA + ".docx");
            response.header("X-History-ID", history.id); // Tambahkan header ini
            response.header("Access-Control-Expose-Headers", "X-History-ID"); // Agar bisa dibaca frontend
            return response.build();
        }
    }

    @GET
    @Path("/history")
    @Produces("application/json")
    @Transactional // Pastikan ini juga dalam konteks transaksi
    public Response getHistory() {
        // Ambil semua data dari database
        List<BeritaAcaraHistory> historyList = BeritaAcaraHistory.listAll();

        // Ubah list entity menjadi list DTO
        List<HistoryResponseDTO> responseList = historyList.stream()
            .map(h -> new HistoryResponseDTO(h.id, h.nomorBA, h.jenisBeritaAcara, h.judulPekerjaan, h.generationTimestamp))
            .collect(Collectors.toList());

        return Response.ok(responseList).build();
    }

    // Tambahkan metode ini di dalam kelas BeritaAcaraResource.java

    @GET
    @Path("/history/{id}/file")
    @Produces("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    @Transactional
    public Response getHistoryFile(@PathParam("id") Long id) {
        // Cari histori berdasarkan ID
        BeritaAcaraHistory history = BeritaAcaraHistory.findById(id);

        if (history == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Buat respons dengan konten file .docx
        ResponseBuilder response = Response.ok(new ByteArrayInputStream(history.fileContent));
        response.header("Content-Disposition", "inline; filename=BA-" + history.nomorBA + ".docx");
        return response.build();
    }

    // @GET
    // @Path("/history/{id}/pdf")
    // @Produces("application/pdf")
    // @Transactional
    // public Response getHistoryAsPdf(@PathParam("id") Long id) throws Exception {
    //     BeritaAcaraHistory history = BeritaAcaraHistory.findById(id);
    //     if (history == null) {
    //         return Response.status(Response.Status.NOT_FOUND).build();
    //     }

    //     // Muat dokumen .docx dari database
    //     InputStream docxInputStream = new ByteArrayInputStream(history.fileContent);
    //     WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(docxInputStream);

    //     // Siapkan output stream untuk PDF
    //     ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();

    //     // Lakukan konversi
    //     Docx4J.toPDF(wordMLPackage, pdfOutputStream);

    //     // Kirim hasil PDF sebagai respons
    //     ResponseBuilder response = Response.ok(new ByteArrayInputStream(pdfOutputStream.toByteArray()));
    //     response.header("Content-Disposition", "inline; filename=BA-" + history.nomorBA + ".pdf");
    //     return response.build();
    // }
    
    private Map<String, String> buildReplacementsMap(BeritaAcaraRequest request) {
        Map<String, String> replacements = new HashMap<>();
        DateToWordsHelper baDate = new DateToWordsHelper(request.tanggalBA);
        DateToWordsHelper pengerjaanDate = new DateToWordsHelper(request.tanggalPengerjaan);

        Signatory utama1 = request.signatoryList.stream().filter(s -> "utama1".equals(s.tipe)).findFirst().orElse(new Signatory());
        Signatory utama2 = request.signatoryList.stream().filter(s -> "utama2".equals(s.tipe)).findFirst().orElse(new Signatory());
        Signatory utama3 = request.signatoryList.stream().filter(s -> "utama3".equals(s.tipe)).findFirst().orElse(new Signatory());
        Signatory utama4 = request.signatoryList.stream().filter(s -> "utama4".equals(s.tipe)).findFirst().orElse(new Signatory());
        Signatory mengetahui = request.signatoryList.stream().filter(s -> "mengetahui".equals(s.tipe)).findFirst().orElse(new Signatory());
        Fitur fitur = (request.fiturList != null && !request.fiturList.isEmpty()) ? request.fiturList.get(0) : new Fitur();

        replacements.put("${jenisRequest}", "Change Request".equalsIgnoreCase(request.jenisRequest) ? "PERUBAHAN" : "PENGEMBANGAN");
        replacements.put("${namaAplikasiSpesifik}", Objects.toString(request.namaAplikasiSpesifik, ""));
        replacements.put("${nomorBA}", Objects.toString(request.nomorBA, ""));
        replacements.put("${judulPekerjaan}", Objects.toString(request.judulPekerjaan, ""));
        replacements.put("${tahap}", Objects.toString(request.tahap, ""));
        replacements.put("${nomorSuratRequest}", Objects.toString(request.nomorSuratRequest, ""));
        replacements.put("${tanggalSuratRequest}", Objects.toString(request.tanggalSuratRequest, ""));
        replacements.put("${nomorBaUat}", Objects.toString(request.nomorBaUat, ""));

        replacements.put("${hariBATerbilang}", baDate.getDayOfWeek());
        replacements.put("${tanggalBATerbilang}", baDate.getDay());
        replacements.put("${bulanBATerbilang}", baDate.getMonth());
        replacements.put("${tahunBATerbilang}", baDate.getYear());
        replacements.put("${tanggalBA}", baDate.getFullDate());
            
        replacements.put("${hariPengerjaanTerbilang}", pengerjaanDate.getDayOfWeek());
        replacements.put("${tanggalPengerjaanTerbilang}", pengerjaanDate.getDay());
        replacements.put("${bulanPengerjaanTerbilang}", pengerjaanDate.getMonth());
        replacements.put("${tahunPengerjaanTerbilang}", pengerjaanDate.getYear());
        replacements.put("${tanggalPengerjaan}", pengerjaanDate.getFullDate());
        
        // Deskripsi fitur tidak diganti di sini, ditangani oleh replacePlaceholderWithHtml
        replacements.put("${fitur.status}", Objects.toString(fitur.status, ""));
        replacements.put("${fitur.keterangan}", Objects.toString(fitur.catatan, ""));

        replacements.put("${signatory.utama1.perusahaan}", Objects.toString(utama1.perusahaan, ""));
        replacements.put("${signatory.utama1.nama}", Objects.toString(utama1.nama, ""));
        replacements.put("${signatory.utama1.jabatan}", Objects.toString(utama1.jabatan, ""));
        replacements.put("${signatory.utama2.perusahaan}", Objects.toString(utama2.perusahaan, ""));
        replacements.put("${signatory.utama2.nama}", Objects.toString(utama2.nama, ""));
        replacements.put("${signatory.utama2.jabatan}", Objects.toString(utama2.jabatan, ""));
        replacements.put("${signatory.utama3.perusahaan}", Objects.toString(utama3.perusahaan, ""));
        replacements.put("${signatory.utama3.nama}", Objects.toString(utama3.nama, ""));
        replacements.put("${signatory.utama3.jabatan}", Objects.toString(utama3.jabatan, ""));
        replacements.put("${signatory.utama4.perusahaan}", Objects.toString(utama4.perusahaan, ""));
        replacements.put("${signatory.utama4.nama}", Objects.toString(utama4.nama, ""));
        replacements.put("${signatory.utama4.jabatan}", Objects.toString(utama4.jabatan, ""));
        replacements.put("${signatory.mengetahui.perusahaan}", Objects.toString(mengetahui.perusahaan, ""));
        replacements.put("${signatory.mengetahui.nama}", Objects.toString(mengetahui.nama, ""));
        replacements.put("${signatory.mengetahui.jabatan}", Objects.toString(mengetahui.jabatan, ""));
        
        return replacements;
    }
    
    /**
     * Metode pengganti placeholder yang menggabungkan semua teks dalam paragraf,
     * melakukan replace, lalu menulisnya kembali dengan menjaga style.
     */
    private void replaceTextPlaceholders(XWPFDocument document, Map<String, String> replacements) {
        for (XWPFParagraph p : document.getParagraphs()) {
            replaceInParagraph(p, replacements);
        }
        for (XWPFTable tbl : document.getTables()) {
            for (XWPFTableRow row : tbl.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        // Jangan proses placeholder fitur di sini
                        if (p.getText() != null && p.getText().contains("${fitur.deskripsi}")) {
                            continue;
                        }
                        replaceInParagraph(p, replacements);
                    }
                }
            }
        }
    }

    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> replacements) {
        String paragraphText = paragraph.getText();
        if (paragraphText == null || !paragraphText.contains("$")) {
            return;
        }

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String placeholder = entry.getKey();
            if (!paragraph.getText().contains(placeholder)) {
                continue;
            }

            List<XWPFRun> runs = paragraph.getRuns();
            int startRunIndex = -1, endRunIndex = -1;
            String accumulatedText = "";

            // Cari sekuens "run" yang berisi placeholder lengkap
            for (int i = 0; i < runs.size(); i++) {
                String runText = runs.get(i).getText(0);
                if (runText == null) continue;
                
                accumulatedText += runText;
                if(startRunIndex == -1) {
                    startRunIndex = i;
                }

                if (accumulatedText.contains(placeholder)) {
                    endRunIndex = i;
                    
                    // Lakukan penggantian
                    String newText = accumulatedText.replace(placeholder, entry.getValue());
                    runs.get(startRunIndex).setText(newText, 0);

                    // Kosongkan run lain yang terlibat
                    for (int j = startRunIndex + 1; j <= endRunIndex; j++) {
                        runs.get(j).setText("", 0);
                    }
                    
                    // Ulangi proses untuk placeholder yang sama di paragraf yang sama
                    replaceInParagraph(paragraph, replacements);
                    return;
                }

                if (!placeholder.startsWith(accumulatedText)) {
                    accumulatedText = "";
                    startRunIndex = -1;
                }
            }
        }
    }
    
    private void replacePlaceholderWithHtml(XWPFDocument document, String placeholder, String html) {
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (int p = 0; p < cell.getParagraphs().size(); p++) {
                        XWPFParagraph paragraph = cell.getParagraphs().get(p);
                        if (paragraph.getText() != null && paragraph.getText().contains(placeholder)) {
                            String fontFamily = null;
                            int fontSize = -1;
                            if (!paragraph.getRuns().isEmpty()) {
                                XWPFRun firstRun = paragraph.getRuns().get(0);
                                fontFamily = firstRun.getFontFamily();
                                fontSize = firstRun.getFontSize();
                            }

                            while (paragraph.getRuns().size() > 0) {
                                paragraph.removeRun(0);
                            }
                            // Hapus paragraf kosong yang menjadi placeholder
                            cell.removeParagraph(p);

                            Document htmlDoc = Jsoup.parse(html);
                            XWPFNumbering numbering = document.getNumbering();
                            if(numbering == null) numbering = document.createNumbering();
                            
                            for (Element element : htmlDoc.body().children()) {
                                if (element.tagName().equals("p")) {
                                    XWPFParagraph targetParagraph = cell.addParagraph();
                                    applyRuns(targetParagraph, element, fontFamily, fontSize);
                                } else if (element.tagName().equals("ul") || element.tagName().equals("ol")) {
                                    BigInteger numId = createNumbering(numbering, element.tagName());
                                    for (Element li : element.select("li")) {
                                        XWPFParagraph listParagraph = cell.addParagraph();
                                        listParagraph.setNumID(numId);
                                        // **PERBAIKAN: Deteksi dan terapkan level indentasi**
                                        int indentLevel = 0;
                                        if (li.hasClass("ql-indent-1")) indentLevel = 1;
                                        if (li.hasClass("ql-indent-2")) indentLevel = 2;
                                        if (li.hasClass("ql-indent-3")) indentLevel = 3;
                                        // Tambahkan jika perlu level lebih dalam
                                        
                                        listParagraph.setNumILvl(BigInteger.valueOf(indentLevel));
                                        // Terapkan style font
                                        applyRuns(listParagraph, li, fontFamily, fontSize);
                                    }
                                }
                            }
                            return; 
                        }
                    }
                }
            }
        }
    }
    
    // 2. Modifikasi applyRuns untuk menerima properti style
    private void applyRuns(XWPFParagraph paragraph, Element element, String fontFamily, int fontSize) {
        for (Node node : element.childNodes()) {
            XWPFRun run = paragraph.createRun();
            // Terapkan style dasar
            if (fontFamily != null) run.setFontFamily(fontFamily);
            if (fontSize != -1) run.setFontSize(fontSize);

            if (node instanceof TextNode) {
                run.setText(((TextNode) node).text());
            } else if (node instanceof Element) {
                Element childElement = (Element) node;
                run.setText(childElement.text());
                if (childElement.tagName().equals("strong") || childElement.tagName().equals("b")) {
                    run.setBold(true);
                }
                if (childElement.tagName().equals("em") || childElement.tagName().equals("i")) {
                    run.setItalic(true);
                }
                if (childElement.tagName().equals("u")) {
                    run.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
                }
            }
        }
    }
    
    // **PERBAIKAN: Membuat definisi multi-level numbering**
    private BigInteger createNumbering(XWPFNumbering numbering, String listType) {
        CTAbstractNum cTAbstractNum = CTAbstractNum.Factory.newInstance();
        // Beri ID unik untuk setiap definisi numbering baru
        cTAbstractNum.setAbstractNumId(BigInteger.valueOf(System.currentTimeMillis() % 100000));

        if ("ul".equals(listType)) {
            // Definisi untuk bullet list multi-level
            addNumberingLevel(cTAbstractNum, 0, STNumberFormat.BULLET, "-");
            addNumberingLevel(cTAbstractNum, 1, STNumberFormat.BULLET, "-");
            addNumberingLevel(cTAbstractNum, 2, STNumberFormat.BULLET, "-");
        } else { // "ol"
            // Definisi untuk ordered list multi-level
            addNumberingLevel(cTAbstractNum, 0, STNumberFormat.DECIMAL, "%1.");
            addNumberingLevel(cTAbstractNum, 1, STNumberFormat.LOWER_LETTER, "%2.");
            addNumberingLevel(cTAbstractNum, 2, STNumberFormat.LOWER_ROMAN, "%3.");
        }

        XWPFAbstractNum abstractNum = new XWPFAbstractNum(cTAbstractNum);
        BigInteger abstractNumId = numbering.addAbstractNum(abstractNum);
        return numbering.addNum(abstractNumId);
    }

    // Helper baru untuk menambahkan level ke definisi numbering
    private void addNumberingLevel(CTAbstractNum abstractNum, int level, STNumberFormat.Enum format, String lvlText) {
        CTLvl cTLvl = abstractNum.addNewLvl();
        cTLvl.setIlvl(BigInteger.valueOf(level));
        cTLvl.addNewNumFmt().setVal(format);
        cTLvl.addNewLvlText().setVal(lvlText);
        cTLvl.addNewStart().setVal(BigInteger.valueOf(1));
        cTLvl.addNewPPr().addNewInd().setLeft(BigInteger.valueOf(310L * (level + 1)));
    }
}