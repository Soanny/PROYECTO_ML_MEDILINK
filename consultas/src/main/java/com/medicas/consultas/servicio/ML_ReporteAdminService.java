/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.servicio;

import jakarta.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ML_ReporteAdminService {

    private final JdbcTemplate ML_jdbcTemplate;

    public ML_ReporteAdminService(JdbcTemplate ML_jdbcTemplate) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
    }

    public int ML_contarTotalCitas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas");
    }

    public int ML_contarCitasConfirmadas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Confirmada'");
    }

    public int ML_contarCitasPendientes() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Pendiente'");
    }

    public int ML_contarCitasCanceladas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Cancelada'");
    }

    public int ML_contarCitasAtendidas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Atendida'");
    }

    public String ML_obtenerIngresosTotales() {
        double ML_total = ML_obtenerDecimal(ML_sqlIngresosPagadosUnicos());
        DecimalFormat ML_formato = new DecimalFormat("#,##0.00");
        return "S/ " + ML_formato.format(ML_total);
    }

    private String ML_sqlIngresosPagadosUnicos() {
        return """
            SELECT COALESCE(SUM(monto), 0)
            FROM (
                SELECT p1.id_cita, p1.monto
                FROM pagos p1
                INNER JOIN (
                    SELECT id_cita, MAX(id_pago) AS id_pago
                    FROM pagos
                    WHERE estado_pago = 'Pagado'
                    GROUP BY id_cita
                ) ult ON ult.id_pago = p1.id_pago
                INNER JOIN citas c ON c.id_cita = p1.id_cita
                WHERE c.estado <> 'Cancelada'
            ) pagos_unicos
        """;
    }

    public List<Map<String, Object>> ML_obtenerCitasPorEstado() {

        String ML_sql = """
            SELECT 
                estado,
                COUNT(*) AS total
            FROM citas
            GROUP BY estado
            ORDER BY total DESC
        """;

        return ML_consultarLista(ML_sql);
    }

    public List<Map<String, Object>> ML_obtenerCitasPorDiaSemana() {

        String ML_sql = """
            SELECT 
                CASE DAYOFWEEK(fecha_cita)
                    WHEN 1 THEN 'Dom'
                    WHEN 2 THEN 'Lun'
                    WHEN 3 THEN 'Mar'
                    WHEN 4 THEN 'Mié'
                    WHEN 5 THEN 'Jue'
                    WHEN 6 THEN 'Vie'
                    WHEN 7 THEN 'Sáb'
                END AS dia,
                COUNT(*) AS total
            FROM citas
            GROUP BY DAYOFWEEK(fecha_cita)
            ORDER BY DAYOFWEEK(fecha_cita)
        """;

        return ML_consultarLista(ML_sql);
    }

    public List<Map<String, Object>> ML_obtenerRendimientoPorEspecialidad() {

        String ML_sql = """
            SELECT 
                e.nombre_especialidad AS especialidad,
                COUNT(c.id_cita) AS total_citas,
                SUM(CASE WHEN c.estado = 'Confirmada' THEN 1 ELSE 0 END) AS confirmadas,
                SUM(CASE WHEN c.estado = 'Pendiente' THEN 1 ELSE 0 END) AS pendientes,
                SUM(CASE WHEN c.estado = 'Cancelada' THEN 1 ELSE 0 END) AS canceladas,
                SUM(CASE WHEN c.estado = 'Atendida' THEN 1 ELSE 0 END) AS atendidas,
                CASE 
                    WHEN COUNT(c.id_cita) = 0 THEN 0
                    ELSE ROUND((SUM(CASE WHEN c.estado IN ('Confirmada', 'Atendida') THEN 1 ELSE 0 END) * 100) / COUNT(c.id_cita), 1)
                END AS rendimiento
            FROM especialidades e
            LEFT JOIN doctores d ON e.id_especialidad = d.id_especialidad
            LEFT JOIN citas c ON d.id_doctor = c.id_doctor
            GROUP BY e.id_especialidad, e.nombre_especialidad
            ORDER BY total_citas DESC
        """;

        return ML_consultarLista(ML_sql);
    }

    public List<Map<String, Object>> ML_obtenerRendimientoPorDoctor() {

        String ML_sql = """
            SELECT 
                d.id_doctor AS id_doctor,
                CONCAT('Dr. ', u.nombres, ' ', u.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COUNT(c.id_cita) AS total_citas,
                SUM(CASE WHEN c.estado = 'Confirmada' THEN 1 ELSE 0 END) AS confirmadas,
                SUM(CASE WHEN c.estado = 'Pendiente' THEN 1 ELSE 0 END) AS pendientes,
                SUM(CASE WHEN c.estado = 'Cancelada' THEN 1 ELSE 0 END) AS canceladas,
                SUM(CASE WHEN c.estado = 'Atendida' THEN 1 ELSE 0 END) AS atendidas,
                CASE 
                    WHEN COUNT(c.id_cita) = 0 THEN 0
                    ELSE ROUND((SUM(CASE WHEN c.estado IN ('Confirmada', 'Atendida') THEN 1 ELSE 0 END) * 100) / COUNT(c.id_cita), 1)
                END AS rendimiento
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN citas c ON d.id_doctor = c.id_doctor
            GROUP BY d.id_doctor, u.nombres, u.apellidos, e.nombre_especialidad
            ORDER BY total_citas DESC, rendimiento DESC
        """;

        return ML_consultarLista(ML_sql);
    }


    public List<Map<String, Object>> ML_obtenerDoctoresMasAtenciones() {
        String ML_sql = """
            SELECT
                CONCAT('Dr. ', u.nombres, ' ', u.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COUNT(c.id_cita) AS total_atenciones,
                COALESCE(SUM(pg.monto_pagado), 0) AS monto_generado
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN citas c ON d.id_doctor = c.id_doctor AND c.estado = 'Atendida'
            LEFT JOIN (
                SELECT p1.id_cita, p1.monto AS monto_pagado
                FROM pagos p1
                INNER JOIN (
                    SELECT id_cita, MAX(id_pago) AS id_pago
                    FROM pagos
                    WHERE estado_pago = 'Pagado'
                    GROUP BY id_cita
                ) ult ON ult.id_pago = p1.id_pago
            ) pg ON pg.id_cita = c.id_cita
            GROUP BY d.id_doctor, u.nombres, u.apellidos, e.nombre_especialidad
            ORDER BY total_atenciones DESC, monto_generado DESC, doctor ASC
            LIMIT 10
        """;
        return ML_consultarLista(ML_sql);
    }

    public List<Map<String, Object>> ML_obtenerMetodosPago() {

        String ML_sql = """
            SELECT 
                metodo_pago,
                COUNT(*) AS total,
                COALESCE(SUM(monto), 0) AS monto_total
            FROM (
                SELECT p1.id_cita, p1.metodo_pago, p1.monto
                FROM pagos p1
                INNER JOIN (
                    SELECT id_cita, MAX(id_pago) AS id_pago
                    FROM pagos
                    WHERE estado_pago = 'Pagado'
                    GROUP BY id_cita
                ) ult ON ult.id_pago = p1.id_pago
                INNER JOIN citas c ON c.id_cita = p1.id_cita
                WHERE c.estado <> 'Cancelada'
            ) pagos_unicos
            GROUP BY metodo_pago
            ORDER BY monto_total DESC
        """;

        return ML_consultarLista(ML_sql);
    }

    private List<Map<String, Object>> ML_consultarLista(String ML_sql) {
        try {
            return ML_jdbcTemplate.queryForList(ML_sql);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private int ML_obtenerEntero(String ML_sql) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class);
            return ML_resultado != null ? ML_resultado.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double ML_obtenerDecimal(String ML_sql) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class);
            return ML_resultado != null ? ML_resultado.doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public void ML_exportarReportesPDF(HttpServletResponse ML_response) throws Exception {
        ML_response.setContentType("application/pdf");
        ML_response.setHeader("Content-Disposition", "attachment; filename=reporte_admin.pdf");

        List<Map<String, Object>> ML_citasEstado = ML_obtenerCitasPorEstado();
        List<Map<String, Object>> ML_citasDia = ML_obtenerCitasPorDiaSemana();
        List<Map<String, Object>> ML_rendimientoDoc = ML_obtenerRendimientoPorDoctor();
        List<Map<String, Object>> ML_metodosPago = ML_obtenerMetodosPago();

        com.lowagie.text.Document ML_documento = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate(), 42, 42, 38, 38);
        com.lowagie.text.pdf.PdfWriter.getInstance(ML_documento, ML_response.getOutputStream());
        ML_documento.open();

        java.awt.Color ML_azulColor = new java.awt.Color(11, 99, 229);
        java.awt.Color ML_celesteColor = new java.awt.Color(18, 181, 203);
        java.awt.Color ML_grisColor = new java.awt.Color(51, 65, 85);
        java.awt.Color ML_bordeColor = new java.awt.Color(219, 226, 238);

        com.lowagie.text.Font ML_tituloFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 21, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
        com.lowagie.text.Font ML_subFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 13, com.lowagie.text.Font.BOLD, ML_grisColor);
        com.lowagie.text.Font ML_descripcionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, new java.awt.Color(100, 116, 139));
        com.lowagie.text.Font ML_celdaFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, new java.awt.Color(15, 23, 42));
        com.lowagie.text.Font ML_celdaBold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);

        com.lowagie.text.pdf.PdfPTable ML_encabezado = new com.lowagie.text.pdf.PdfPTable(1);
        ML_encabezado.setWidthPercentage(100);
        com.lowagie.text.pdf.PdfPCell ML_celdaEnc = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("MediLink - Reporte Administrativo", ML_tituloFont));
        ML_celdaEnc.setBackgroundColor(ML_azulColor);
        ML_celdaEnc.setPadding(18);
        ML_celdaEnc.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        ML_celdaEnc.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        ML_encabezado.addCell(ML_celdaEnc);
        ML_documento.add(ML_encabezado);

        com.lowagie.text.Paragraph ML_intro = new com.lowagie.text.Paragraph("Indicadores generales de citas, atención médica y pagos registrados en CONSULTAS - MediLink.", ML_descripcionFont);
        ML_intro.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        ML_intro.setSpacingBefore(8);
        ML_intro.setSpacingAfter(18);
        ML_documento.add(ML_intro);

        ML_agregarTituloReportePdf(ML_documento, "Citas por estado", "Relación de citas agrupadas por estado del proceso de atención.", ML_subFont, ML_descripcionFont);
        com.lowagie.text.pdf.PdfPTable ML_t1 = new com.lowagie.text.pdf.PdfPTable(2);
        ML_t1.setWidthPercentage(60);
        ML_t1.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
        ML_t1.setWidths(new float[]{70, 30});
        ML_t1.setSpacingAfter(18);
        ML_agregarHeaderReportePdf(ML_t1, ML_azulColor, ML_celdaBold, "Estado", "Total");
        for (Map<String, Object> ML_fila : ML_citasEstado) {
            ML_agregarCeldaReportePdf(ML_t1, String.valueOf(ML_fila.get("estado")), ML_celdaFont, ML_bordeColor);
            ML_agregarCeldaReportePdf(ML_t1, String.valueOf(ML_fila.get("total")), ML_celdaFont, ML_bordeColor);
        }
        ML_documento.add(ML_t1);

        ML_agregarTituloReportePdf(ML_documento, "Citas por día de semana", "Cantidad de citas programadas según el día de atención.", ML_subFont, ML_descripcionFont);
        com.lowagie.text.pdf.PdfPTable ML_t2 = new com.lowagie.text.pdf.PdfPTable(2);
        ML_t2.setWidthPercentage(60);
        ML_t2.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
        ML_t2.setWidths(new float[]{70, 30});
        ML_t2.setSpacingAfter(18);
        ML_agregarHeaderReportePdf(ML_t2, ML_celesteColor, ML_celdaBold, "Día", "Total");
        for (Map<String, Object> ML_fila : ML_citasDia) {
            ML_agregarCeldaReportePdf(ML_t2, String.valueOf(ML_fila.get("dia")), ML_celdaFont, ML_bordeColor);
            ML_agregarCeldaReportePdf(ML_t2, String.valueOf(ML_fila.get("total")), ML_celdaFont, ML_bordeColor);
        }
        ML_documento.add(ML_t2);

        ML_agregarTituloReportePdf(ML_documento, "Rendimiento por doctor", "Comparativo de citas asignadas, atendidas, canceladas y porcentaje de rendimiento.", ML_subFont, ML_descripcionFont);
        com.lowagie.text.pdf.PdfPTable ML_t3 = new com.lowagie.text.pdf.PdfPTable(6);
        ML_t3.setWidthPercentage(100);
        ML_t3.setWidths(new float[]{26, 22, 12, 12, 12, 16});
        ML_t3.setSpacingAfter(18);
        ML_agregarHeaderReportePdf(ML_t3, ML_azulColor, ML_celdaBold, "Doctor", "Especialidad", "Total", "Atendidas", "Canceladas", "Rendimiento");
        for (Map<String, Object> ML_fila : ML_rendimientoDoc) {
            String[] ML_vals = {String.valueOf(ML_fila.get("doctor")), String.valueOf(ML_fila.get("especialidad")),
                String.valueOf(ML_fila.get("total_citas")), String.valueOf(ML_fila.get("atendidas")),
                String.valueOf(ML_fila.get("canceladas")), String.valueOf(ML_fila.get("rendimiento")) + "%"};
            for (String ML_valor : ML_vals) ML_agregarCeldaReportePdf(ML_t3, ML_valor, ML_celdaFont, ML_bordeColor);
        }
        ML_documento.add(ML_t3);

        ML_agregarTituloReportePdf(ML_documento, "Métodos de pago", "Montos pagados agrupados por método, considerando únicamente pagos vigentes.", ML_subFont, ML_descripcionFont);
        com.lowagie.text.pdf.PdfPTable ML_t4 = new com.lowagie.text.pdf.PdfPTable(3);
        ML_t4.setWidthPercentage(72);
        ML_t4.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
        ML_t4.setWidths(new float[]{45, 20, 35});
        ML_t4.setSpacingAfter(10);
        ML_agregarHeaderReportePdf(ML_t4, ML_celesteColor, ML_celdaBold, "Método", "Total", "Monto total");
        for (Map<String, Object> ML_fila : ML_metodosPago) {
            String[] ML_vals = {String.valueOf(ML_fila.get("metodo_pago")), String.valueOf(ML_fila.get("total")), "S/ " + ML_fila.get("monto_total")};
            for (String ML_valor : ML_vals) ML_agregarCeldaReportePdf(ML_t4, ML_valor, ML_celdaFont, ML_bordeColor);
        }
        ML_documento.add(ML_t4);

        com.lowagie.text.Paragraph ML_nota = new com.lowagie.text.Paragraph("Reporte generado con información registrada en bd_citas_medicas.", ML_descripcionFont);
        ML_nota.setSpacingBefore(8);
        ML_documento.add(ML_nota);
        ML_documento.close();
    }

    private void ML_agregarTituloReportePdf(com.lowagie.text.Document ML_documento, String ML_titulo, String ML_descripcion, com.lowagie.text.Font ML_tituloFont, com.lowagie.text.Font ML_descripcionFont) throws Exception {
        com.lowagie.text.Paragraph ML_t = new com.lowagie.text.Paragraph(ML_titulo, ML_tituloFont);
        ML_t.setSpacingBefore(8);
        ML_t.setSpacingAfter(4);
        ML_documento.add(ML_t);
        com.lowagie.text.Paragraph ML_d = new com.lowagie.text.Paragraph(ML_descripcion, ML_descripcionFont);
        ML_d.setSpacingAfter(10);
        ML_documento.add(ML_d);
    }

    private void ML_agregarHeaderReportePdf(com.lowagie.text.pdf.PdfPTable ML_tabla, java.awt.Color ML_color, com.lowagie.text.Font ML_font, String... ML_headers) {
        for (String ML_h : ML_headers) {
            com.lowagie.text.pdf.PdfPCell ML_celda = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(ML_h, ML_font));
            ML_celda.setBackgroundColor(ML_color);
            ML_celda.setPadding(9);
            ML_celda.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            ML_celda.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            ML_tabla.addCell(ML_celda);
        }
    }

    private void ML_agregarCeldaReportePdf(com.lowagie.text.pdf.PdfPTable ML_tabla, String ML_valor, com.lowagie.text.Font ML_font, java.awt.Color ML_borde) {
        com.lowagie.text.pdf.PdfPCell ML_celda = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(ML_valor != null ? ML_valor : "-", ML_font));
        ML_celda.setPadding(8);
        ML_celda.setBorderColor(ML_borde);
        ML_celda.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        ML_tabla.addCell(ML_celda);
    }

    public void ML_exportarReportesExcel(HttpServletResponse ML_response) throws Exception {
        ML_response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ML_response.setHeader("Content-Disposition", "attachment; filename=reporte_admin.xlsx");

        List<Map<String, Object>> ML_citasEstado = ML_obtenerCitasPorEstado();
        List<Map<String, Object>> ML_citasDia = ML_obtenerCitasPorDiaSemana();
        List<Map<String, Object>> ML_rendimientoDoc = ML_obtenerRendimientoPorDoctor();
        List<Map<String, Object>> ML_metodosPago = ML_obtenerMetodosPago();

        org.apache.poi.xssf.usermodel.XSSFWorkbook ML_wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.CellStyle ML_hs = ML_wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font ML_hf = ML_wb.createFont();
        ML_hf.setBold(true);
        ML_hf.setFontHeightInPoints((short) 12);
        ML_hs.setFont(ML_hf);

        org.apache.poi.xssf.usermodel.XSSFSheet ML_s1 = ML_wb.createSheet("Citas por Estado");
        ML_s1.createRow(0).createCell(0).setCellValue("Estado");
        ML_s1.getRow(0).createCell(1).setCellValue("Total");
        ML_s1.getRow(0).getCell(0).setCellStyle(ML_hs);
        ML_s1.getRow(0).getCell(1).setCellStyle(ML_hs);
        int r = 1;
        for (Map<String, Object> f : ML_citasEstado) {
            ML_s1.createRow(r).createCell(0).setCellValue(String.valueOf(f.get("estado")));
            ML_s1.getRow(r).createCell(1).setCellValue(Double.parseDouble(String.valueOf(f.get("total"))));
            r++;
        }

        org.apache.poi.xssf.usermodel.XSSFSheet ML_s2 = ML_wb.createSheet("Citas por Dia");
        ML_s2.createRow(0).createCell(0).setCellValue("Dia");
        ML_s2.getRow(0).createCell(1).setCellValue("Total");
        ML_s2.getRow(0).getCell(0).setCellStyle(ML_hs);
        ML_s2.getRow(0).getCell(1).setCellStyle(ML_hs);
        r = 1;
        for (Map<String, Object> f : ML_citasDia) {
            ML_s2.createRow(r).createCell(0).setCellValue(String.valueOf(f.get("dia")));
            ML_s2.getRow(r).createCell(1).setCellValue(Double.parseDouble(String.valueOf(f.get("total"))));
            r++;
        }

        org.apache.poi.xssf.usermodel.XSSFSheet ML_s3 = ML_wb.createSheet("Rendimiento Doctores");
        String[] h3 = {"Doctor", "Especialidad", "Total", "Confirmadas", "Pendientes", "Atendidas", "Canceladas", "Rendimiento"};
        ML_s3.createRow(0);
        for (int i = 0; i < h3.length; i++) {
            ML_s3.getRow(0).createCell(i).setCellValue(h3[i]);
            ML_s3.getRow(0).getCell(i).setCellStyle(ML_hs);
        }
        r = 1;
        for (Map<String, Object> f : ML_rendimientoDoc) {
            ML_s3.createRow(r).createCell(0).setCellValue(String.valueOf(f.get("doctor")));
            ML_s3.getRow(r).createCell(1).setCellValue(String.valueOf(f.get("especialidad")));
            ML_s3.getRow(r).createCell(2).setCellValue(Double.parseDouble(String.valueOf(f.get("total_citas"))));
            ML_s3.getRow(r).createCell(3).setCellValue(Double.parseDouble(String.valueOf(f.get("confirmadas"))));
            ML_s3.getRow(r).createCell(4).setCellValue(Double.parseDouble(String.valueOf(f.get("pendientes"))));
            ML_s3.getRow(r).createCell(5).setCellValue(Double.parseDouble(String.valueOf(f.get("atendidas"))));
            ML_s3.getRow(r).createCell(6).setCellValue(Double.parseDouble(String.valueOf(f.get("canceladas"))));
            ML_s3.getRow(r).createCell(7).setCellValue(String.valueOf(f.get("rendimiento")) + "%");
            r++;
        }

        org.apache.poi.xssf.usermodel.XSSFSheet ML_s4 = ML_wb.createSheet("Metodos de Pago");
        ML_s4.createRow(0).createCell(0).setCellValue("Metodo");
        ML_s4.getRow(0).createCell(1).setCellValue("Total");
        ML_s4.getRow(0).createCell(2).setCellValue("Monto Total");
        ML_s4.getRow(0).getCell(0).setCellStyle(ML_hs);
        ML_s4.getRow(0).getCell(1).setCellStyle(ML_hs);
        ML_s4.getRow(0).getCell(2).setCellStyle(ML_hs);
        r = 1;
        for (Map<String, Object> f : ML_metodosPago) {
            ML_s4.createRow(r).createCell(0).setCellValue(String.valueOf(f.get("metodo_pago")));
            ML_s4.getRow(r).createCell(1).setCellValue(Double.parseDouble(String.valueOf(f.get("total"))));
            ML_s4.getRow(r).createCell(2).setCellValue("S/ " + f.get("monto_total"));
            r++;
        }

        for (int i = 0; i < ML_wb.getNumberOfSheets(); i++) ML_wb.getSheetAt(i).autoSizeColumn(0);
        ML_wb.write(ML_response.getOutputStream());
        ML_wb.close();
    }
}