package com.rafhi.dto;
import java.util.Map;

// DTO untuk request generasi dokumen dinamis
public class DynamicGenerateRequest {
    public Long templateId;
    public Map<String, String> data; // Key: "${placeholder}", Value: "isian pengguna"
}