package com.rafhi.controller;

import com.rafhi.dto.DefineTemplateRequest;
import com.rafhi.dto.TemplateDetailDTO;
import com.rafhi.dto.TemplatePlaceholderDTO;
import com.rafhi.dto.TemplateStatusUpdateRequest;
import com.rafhi.dto.TemplateSummaryDTO;
import com.rafhi.entity.Template;
// import com.rafhi.entity.TemplatePlaceholder;
import com.rafhi.service.TemplateService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/admin/templates")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateAdminResource {

    @Inject
    TemplateService templateService;

    @ConfigProperty(name = "template.upload.path")
    String uploadPath;

    /**
     * Langkah 1 (Create): Menerima file, memindai placeholder, dan menyimpannya sementara.
     * Endpoint ini tidak perlu @Transactional karena hanya bekerja dengan file.
     */
    @POST
    @Path("/upload-and-scan")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadAndScan(MultipartFormDataInput input) {
        try {
            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
            List<InputPart> inputParts = uploadForm.get("file");
            if (inputParts == null || inputParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Bagian 'file' tidak ditemukan.\"}").build();
            }

            InputPart filePart = inputParts.get(0);
            String originalFileName = getFileName(filePart.getHeaders());
            InputStream inputStream = filePart.getBody(InputStream.class, null);

            java.nio.file.Path tempPath = Files.createTempFile("template-", ".docx");
            Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);

            Set<String> placeholders = templateService.scanPlaceholders(tempPath);

            Map<String, Object> response = new HashMap<>();
            response.put("tempFilePath", tempPath.toString());
            response.put("placeholders", placeholders);
            response.put("originalFileName", originalFileName);

            return Response.ok(response).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Gagal memproses file: " + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Langkah 2 (Create): Menerima metadata, lalu menyimpan template baru.
     * Mengembalikan DTO detail dari template yang baru dibuat.
     */
    @POST
    @Path("/define-and-save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response defineAndSave(DefineTemplateRequest request) {
        try {
            java.nio.file.Path tempPath = Paths.get(request.tempFilePath);
            if (!Files.exists(tempPath)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"File sementara tidak ditemukan.\"}").build();
            }

            String newFileName = UUID.randomUUID() + ".docx";
            java.nio.file.Path finalPath = Paths.get(uploadPath, newFileName);
            Files.createDirectories(finalPath.getParent());
            Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);

            Template template = templateService.createTemplateFromRequest(request, newFileName);
            
            return Response.status(Response.Status.CREATED).entity(mapToDetailDTO(template)).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\":\"Gagal menyimpan template: " + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Mengambil daftar semua template (info ringkas) untuk tabel admin.
     * Mengembalikan List<TemplateSummaryDTO>.
     */
    @GET
    @Consumes(MediaType.WILDCARD) // Izinkan GET tanpa body
    public Response getAllTemplatesForAdmin() {
        List<Template> templates = templateService.listAllForAdmin();
        List<TemplateSummaryDTO> dtos = templates.stream()
            .map(this::mapToSummaryDTO)
            .collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    /**
     * Mengambil detail satu template LENGKAP dengan placeholder-nya untuk form edit.
     * Mengembalikan TemplateDetailDTO.
     */
    @GET
    @Path("/{id}")
    @Consumes(MediaType.WILDCARD) // Izinkan GET tanpa body
    @Transactional
    public Response getTemplateById(@PathParam("id") Long id) {
        Template template = templateService.findById(id);
        if (template == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(mapToDetailDTO(template)).build();
    }

    /**
     * Endpoint efisien untuk mengubah status aktif/tidak aktif sebuah template.
     * Mengembalikan TemplateSummaryDTO.
     */
    @PATCH
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateTemplateStatus(@PathParam("id") Long id, TemplateStatusUpdateRequest request) {
        Template template = templateService.updateStatus(id, request.isActive);
        if (template == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(mapToSummaryDTO(template)).build();
    }

    /**
     * Menyimpan perubahan pada template yang sudah ada (form edit lengkap).
     * Mengembalikan TemplateDetailDTO.
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateTemplate(@PathParam("id") Long id, DefineTemplateRequest request) {
        try {
            Template template = templateService.updateTemplateFromRequest(id, request);
            if (template == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(mapToDetailDTO(template)).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\":\"Gagal memproses file saat update: " + e.getMessage() + "\"}")
                        .build();
        }
    }

    // /**
    //  * Menghapus sebuah template.
    //  */
    // @DELETE
    // @Path("/{id}")
    // @Consumes(MediaType.WILDCARD) // Izinkan DELETE tanpa body
    // public Response deleteTemplate(@PathParam("id") Long id) {
    //     try {
    //         templateService.delete(id);
    //         return Response.noContent().build();
    //     } catch (NotFoundException e) {
    //         return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
    //     } catch (IOException e) {
    //         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
    //                 .entity("{\"error\":\"Gagal menghapus file fisik: " + e.getMessage() + "\"}")
    //                 .build();
    //     }
    // }

    // =======================================================
    // === METHOD HELPER UNTUK MAPPING DAN LAINNYA ===
    // =======================================================
    
    private TemplateSummaryDTO mapToSummaryDTO(Template template) {
        TemplateSummaryDTO dto = new TemplateSummaryDTO();
        dto.id = template.id;
        dto.templateName = template.templateName;
        dto.description = template.description;
        dto.isActive = template.isActive;
        return dto;
    }

    private TemplateDetailDTO mapToDetailDTO(Template template) {
        TemplateDetailDTO dto = new TemplateDetailDTO();
        dto.id = template.id;
        dto.templateName = template.templateName;
        dto.description = template.description;
        dto.isActive = template.isActive;
        
        dto.placeholders = template.getPlaceholders().stream()
            .map(p -> {
                TemplatePlaceholderDTO pDto = new TemplatePlaceholderDTO();
                pDto.id = p.id;
                pDto.placeholderKey = p.placeholderKey;
                pDto.label = p.label;
                pDto.dataType = p.dataType;
                return pDto;
            })
            .collect(Collectors.toList());
            
        return dto;
    }

    private String getFileName(MultivaluedMap<String, String> headers) {
        String[] contentDisposition = headers.getFirst("Content-Disposition").split(";");
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                return filename.split("=")[1].trim().replaceAll("\"", "");
            }
        }
        return "unknown";
    }
}