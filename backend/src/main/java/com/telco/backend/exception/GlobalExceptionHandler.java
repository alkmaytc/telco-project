package com.telco.backend.exception;

import com.telco.backend.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j; // 🎯 Madde 5 loglama altyapısı için hazırlandı kanka
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
     * Tüm validation hatalarını virgülle birleştirip frontend'e tertemiz bir mesaj halinde sunar kanka. ✅
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
     * 4. 🛡️ SİBER GÜVENLİK SÜZGECİ (Information Disclosure Koruması):
     * Geriye kalan tüm öngörülemeyen sistemsel çalışma zamanı hatalarını (NullPointerException, Veritabanı çökmesi vb.) yakalar.
     * Dış dünyaya ham Java kod satırlarını sızdırmaz, zafiyetleri engeller!
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneralException(
            Exception ex, HttpServletRequest request) {

        // Hatayı console ve log dosyasına detaylıca basıyoruz ki biz arka planda görebilelim kanka
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