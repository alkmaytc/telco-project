package com.telco.backend.service;

import com.telco.backend.dto.AuthResponseDTO;
import com.telco.backend.dto.LoginRequestDTO;
import com.telco.backend.dto.RegisterRequestDTO;
import com.telco.backend.model.Customer;
import com.telco.backend.model.Role;
import com.telco.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // CBS SRID tanımı (WGS 84 - Dünya Standart Koordinat Sistemi)
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    // 1. KULLANICI KAYIT METODU (REGISTER)
    public AuthResponseDTO register(RegisterRequestDTO request) {

        // E-posta adresi sistemde zaten var mı kontrolü
        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Bu e-posta adresi zaten kullanımda!");
        }

        // React'tan gelen Enlem ve Boylamı PostGIS Point nesnesine dönüştürüyoruz (CBS JÜRİ ŞOVU)
        Point customerLocation = null;
        if (request.getLatitude() != null && request.getLongitude() != null) {
            customerLocation = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
        }

        // Yeni müşteriyi (veya admini) oluşturuyoruz
        Customer customer = new Customer();
        // 🎯 T.C. KİMLİK SETLEME SATIRI BURADAN TAMAMEN SİLİNDİ!
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        // JÜRE ŞOVU GÜVENLİK: Şifreyi BCrypt ile hash'leyip DB'ye öyle yazıyoruz
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setLocation(customerLocation);

        // İlk kayıt olan herkes varsayılan olarak CUSTOMER (Müşteri) rolünde başlar.
        // Veritabanından ilk kaydettiğin bir hesabı elle 'ADMIN' yaparak admin panelini test edebilirsin kanka.
        customer.setRole(Role.CUSTOMER);

        customerRepository.save(customer);

        // Kayıt olur olmaz adam otomatik giriş yapsın diye token üretip dönüyoruz
        String jwtToken = jwtService.generateToken(customer);

        return AuthResponseDTO.builder()
                .token(jwtToken)
                .role(customer.getRole().name())
                .fullName(customer.getFullName())
                .build();
    }

    // 2. KULLANICI GİRİŞ METODU (LOGIN)
    public AuthResponseDTO login(LoginRequestDTO request) {

        // Spring Security'nin bizim yazdığımız Custom Provider üzerinden şifre kontrolü yapmasını tetikliyoruz
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Şifre doğruysa kullanıcıyı DB'den çekiyoruz
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı!"));

        // Kullanıcıya özel yeni bir JWT Token üretiyoruz
        String jwtToken = jwtService.generateToken(customer);

        return AuthResponseDTO.builder()
                .token(jwtToken)
                .role(customer.getRole().name())
                .fullName(customer.getFullName())
                .build();
    }
}