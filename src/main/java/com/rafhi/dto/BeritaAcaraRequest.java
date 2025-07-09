package com.rafhi.dto;

import java.util.List;

public class BeritaAcaraRequest {
    public String jenisBeritaAcara; // UAT / Deployment
    public String kategoriAplikasi; // Web / Portal
    public String tipeRequest;      // Change Request / Job Request
    public String nomorBA;
    public String tahun;
    public String nomorSuratRequest;
    public String tanggalSuratRequest;
    public String tanggalPelaksanaan;
    public String judulPekerjaan;

    public List<Fitur> fiturList;
    public String namaPenandatangan;
    public String jabatanPenandatangan;
}