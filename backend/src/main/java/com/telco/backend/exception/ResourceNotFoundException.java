package com.telco.backend.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    /**
     * 🎯 KESİN ÇÖZÜM: Üst sınıfa (super) hem mesajı hem de telekom kurumsal
     * standardı olan NOT_FOUND (404) statüsünü otomatik olarak paslıyoruz kanka. ✅
     */
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}