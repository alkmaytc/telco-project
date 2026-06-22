package com.telco.backend.service;

import com.telco.backend.model.Customer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // Kriptolama işlemleri için bellekte güvenli rastgele bir gizli anahtar üretiyoruz (En az 256-bit)
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Üretilen token'ın geçerlilik süresi: 24 Saat (Milisaniye cinsinden)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    // Token içerisinden e-posta adresini (kullanıcı adını) söken metot
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Giriş başarılı olduğunda Customer objesini alıp ona özel şifreli JWT üreten ana metot
    public String generateToken(Customer customer) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", customer.getRole().name());
        extraClaims.put("fullName", customer.getFullName());

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(customer.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // Gelen token geçerli mi ve bu e-postaya mı ait kontrolü
    public boolean isTokenValid(String token, String email) {
        final String username = extractUsername(token);
        return (username.equals(email)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Token'ın şifresini çözüp içindeki tüm verileri (Claims) ayıran metot
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}