package com.rafhi.dto;

import java.util.List;

// DTO untuk detail template lengkap
public class TemplateDetailDTO extends TemplateSummaryDTO {
    public List<TemplatePlaceholderDTO> placeholders;
}