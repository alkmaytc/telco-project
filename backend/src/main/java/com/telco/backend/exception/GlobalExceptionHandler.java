package com.telco.backend.exception;

import com.telco.backend.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // 🎯 EKLENDİ: Spring Security 403 Kalkanı İçin
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 1. 🎯 BİZİM ÖZEL HATALARIMIZ: İş mantığı ve telekom operasyon hatalarını yakalar (Örn: Port kalmadı, fizibilite tıkandı)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("İş mantığı hatası yakalandı: {} | Rota: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, ex.getStatus());
    }

    /**
     * 2. 🎯 BİZİM ÖZEL HATALARIMIZ: PostGIS veritabanında aranan nesne (Bina, Dolap, Müşteri) bulunamadığında tetiklenir.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Kaynak bulunamadı hatası: {} | Rota: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * 3. VALIDASYON SÜZGECİ: DTO katmanındaki validasyon (@NotBlank, @Size vb.) hatalarını yakalar.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String mergedErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validasyon ihlali: {} | Rota: {}", mergedErrors, request.getRequestURI());

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Geçersiz veri girişi: " + mergedErrors,
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * 4. 🚨 MÜKERRER KAYIT SÜZGECİ (Duplicate Key)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Veritabanı Kısıtı İhlali (Mükerrer Kayıt Denemesi) | Rota: {}", request.getRequestURI());

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Bu T.C. Kimlik Numarası veya E-posta adresi ile zaten kayıtlı bir kullanıcı bulunmaktadır.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * 5. 🚨 İLLEGAL STATE SÜZGECİ (Çifte Sipariş)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {

        log.warn("Geçersiz İşlem Durumu: {} | Rota: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * 7. 🛡️ YETKİ VE IDOR SÜZGECİ (403 Forbidden) 🎯 YENİ EKLENDİ
     * Başkasının verisine erişmeye çalışan sinsi istekleri yakalar, 500 yerine zarif bir 403 döner.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("🚨 Güvenlik İhlali Girişimi (IDOR/Yetkisiz Erişim): {} | Rota: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage() != null && !ex.getMessage().equals("Erişim engellendi") ? ex.getMessage() : "Ağ Güvenlik Protokolü: Bu veriyi veya sayfayı görüntüleme yetkiniz bulunmamaktadır.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * 6. 🛡️ SİBER GÜVENLİK SÜZGECİ (Information Disclosure Koruması)
     * Geriye kalan tüm kritik 500 hatalarını yakalar. (En altta kalmalı)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneralException(
            Exception ex, HttpServletRequest request) {

        log.error("Sistemsel Kritik Hata! Rota: " + request.getRequestURI(), ex);

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Güvenlik politikaları gereği sistemsel detaylar gizlenmiştir. Lütfen sistem yöneticisi ile iletişime geçin.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}