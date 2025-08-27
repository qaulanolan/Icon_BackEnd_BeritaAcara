package com.rafhi.service;

import com.rafhi.dto.DefineTemplateRequest;
import com.rafhi.entity.Template;
import com.rafhi.entity.TemplatePlaceholder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
// import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.apache.poi.xwpf.usermodel.*;
import org.hibernate.Hibernate; 

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class TemplateService {

    @ConfigProperty(name = "template.upload.path")
    String uploadPath;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public List<Template> listAllActive() {
        return Template.list("isActive", true);
    }

    public List<Template> listAllForAdmin() {
        return Template.listAll();
    }

    public Template findById(Long id) {
        return Template.findById(id);
    }

    @Transactional
    public Template findByIdWithPlaceholders(Long id) {
        Template template = findById(id);
        if (template != null) {
            // Perintah ini memaksa Hibernate untuk memuat list placeholders
            // dari database sebelum sesi transaksi berakhir.
            Hibernate.initialize(template.getPlaceholders());
        }
        return template;
    }

    public Path getTemplatePath(Template template) {
        return Paths.get(uploadPath, template.fileNameStored);
    }

    public Set<String> scanPlaceholders(Path filePath) throws IOException {
        Set<String> placeholders = new HashSet<>();
        try (InputStream is = Files.newInputStream(filePath); XWPFDocument document = new XWPFDocument(is)) {
            for (XWPFParagraph p : document.getParagraphs()) {
                findPlaceholdersInText(p.getText(), placeholders);
            }
            for (XWPFTable tbl : document.getTables()) {
                for (XWPFTableRow row : tbl.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            findPlaceholdersInText(p.getText(), placeholders);
                        }
                    }
                }
            }
        }
        return placeholders;
    }

    private void findPlaceholdersInText(String text, Set<String> placeholders) {
        if (text == null || text.isEmpty()) return;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            placeholders.add(matcher.group(0));
        }
    }

    // @Transactional
    // public void delete(Long id) throws IOException {
    //     Template template = findById(id);
    //     if (template == null) throw new NotFoundException("Template dengan ID " + id + " tidak ditemukan.");
    //     Files.deleteIfExists(getTemplatePath(template));
    //     template.delete();
    // }

    @Transactional
    public Template createTemplateFromRequest(DefineTemplateRequest request, String newFileName) {
        Template template = new Template();
        template.templateName = request.templateName;
        template.description = request.description;
        template.originalFileName = request.originalFileName;
        template.fileNameStored = newFileName;
        template.setPlaceholders(new ArrayList<>());
        template.persist();

        for (TemplatePlaceholder ph : request.placeholders) {
            ph.template = template;
            ph.persist();
            template.getPlaceholders().add(ph);
        }
        return template;
    }

    @Transactional
    public Template updateTemplateFromRequest(Long id, DefineTemplateRequest request) throws IOException {
        Template template = findById(id);
        if (template == null) return null;

        // Cek apakah ada file baru yang diunggah
        if (request.newFileUploaded) {
            // Hapus file fisik yang lama
            java.nio.file.Path oldFilePath = Paths.get(uploadPath, template.fileNameStored);
            Files.deleteIfExists(oldFilePath);
            
            // Pindahkan file baru dari path sementara ke path permanen
            java.nio.file.Path tempPath = Paths.get(request.tempFilePath);
            if (!Files.exists(tempPath)) {
                throw new IOException("File sementara tidak ditemukan untuk diupdate.");
            }
            String newFileName = UUID.randomUUID() + ".docx";
            java.nio.file.Path finalPath = Paths.get(uploadPath, newFileName);
            Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Update nama file di database
            template.fileNameStored = newFileName;
            template.originalFileName = request.originalFileName;
        }

        // Update metadata teks
        template.templateName = request.templateName;
        template.description = request.description;
        template.isActive = request.isActive;
        
        // Hapus placeholder lama dan tambahkan yang baru
        template.getPlaceholders().clear();
        for (TemplatePlaceholder phFromRequest : request.placeholders) {
            TemplatePlaceholder newPh = new TemplatePlaceholder();
            newPh.template = template;
            newPh.placeholderKey = phFromRequest.placeholderKey;
            newPh.label = phFromRequest.label;
            newPh.dataType = phFromRequest.dataType;
            newPh.isRequired = phFromRequest.isRequired;
            template.getPlaceholders().add(newPh);
        }
        
        return template;
    }

    @Transactional
    public Template updateStatus(Long id, boolean isActive) {
        Template template = findById(id);
        if (template == null) return null;
        template.isActive = isActive;
        return template;
    }
}