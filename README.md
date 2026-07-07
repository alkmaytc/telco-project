
# 🏗️ Telco Infrastructure Management System — Backend

> Bir telekomünikasyon operatörünün saha altyapısını yönetmek ve son kullanıcılara internet hizmet siparişi sunmak amacıyla geliştirilmiş **Spring Boot 3** tabanlı RESTful API.

---

## 📋 İçindekiler

- [Proje Hakkında](#-proje-hakkında)
- [Kullanılan Teknolojiler](#-kullanılan-teknolojiler)
- [Mimari ve Katmanlar](#-mimari-ve-katmanlar)
- [Temel Özellikler](#-temel-özellikler)
- [Güvenlik](#-güvenlik)
- [Veritabanı Yapısı](#-veritabanı-yapısı)
- [API Grupları](#-api-grupları)
- [Kurulum](#-kurulum)
- [Testler](#-testler)

---

## 🎯 Proje Hakkında

Bir müşteri belirli bir adrese internet hizmeti talep ettiğinde sistem şu adımları izler:

1. Müşterinin adresine karşılık gelen binayı veritabanında bulur
2. O binaya en yakın saha dolabını **PostGIS** ile coğrafi olarak tespit eder
3. Altyapı tipine (Fiber/VDSL) göre teknik uygunluğu hesaplar
4. VDSL durumunda mesafeye bağlı sinyal kalitesini **gerçek telekomünikasyon formülleriyle** simüle eder
5. Uygun paketleri kullanıcıya sunar
6. Siparişi anında onaylar ya da kapasite yetersizse **RabbitMQ kuyruğuna** alıp kapasite açıldığında otomatik işler
7. Altyapının bulunmadığı bölgelerde kullanıcı talebini kaydeder, admin takip eder

Bu sistem gerçek bir **BSS (Business Support System)** mimarisinin teknik olarak derinlikli bir uygulamasıdır.

---

## 🛠️ Kullanılan Teknolojiler

| Kategori | Teknoloji | Versiyon |
|----------|-----------|----------|
| Dil & Framework | Java + Spring Boot | 21 / 3.x |
| Güvenlik | Spring Security + JWT (jjwt) | 6 / 0.11.5 |
| Veritabanı | PostgreSQL + PostGIS | - |
| ORM | Spring Data JPA + Hibernate | - |
| Mesajlaşma | RabbitMQ | - |
| Önbellekleme | Redis + Spring Cache | - |
| API Dokümantasyonu | Springdoc OpenAPI (Swagger UI) | 2.8.9 |
| Doğrulama | Jakarta Bean Validation | - |
| Loglama | Logback | - |
| Test Verisi | Java DataFaker | 2.1.0 |
| Altyapı | Docker Compose | - |
| Test | JUnit 5 + Mockito | - |

---

## 🏛️ Mimari ve Katmanlar

```
com.telco.backend
├── config/          → Spring Security, JWT Filter, RabbitMQ, Redis, Swagger, CORS
├── controller/      → HTTP endpoint'leri (AddressController, OrderController, AdminController...)
├── service/         → İş mantığı katmanı (FeasibilityService, OrderService, AuthService...)
├── repository/      → Spring Data JPA ile veritabanı erişimi
├── model/           → Entity sınıfları (Building, Customer, Order, Port...)
├── dto/             → Veri transfer nesneleri (istek/yanıt modelleri)
├── consumer/        → RabbitMQ mesaj dinleyicisi (OrderConsumer)
└── exception/       → Merkezi hata yönetimi (GlobalExceptionHandler)
```

### Katman Sorumlulukları

- **Controller** → İstekleri karşılar, doğrular, servise yönlendirir. Swagger ile belgelenmiştir.
- **Service** → Tüm iş mantığı burada yaşar. Fizibilite, sipariş, cache, mesajlaşma.
- **Repository** → Parametrik sorgular. SQL injection riski yoktur.
- **DTO** → Entity yerine DTO kullanımı ile hassas veri sızıntısı önlenir.
- **Exception** → `BusinessException`, `ResourceNotFoundException` ve `GlobalExceptionHandler` ile tutarlı hata yanıtları.

---

## ⚙️ Temel Özellikler

### 📍 Adres Hiyerarşisi ve Redis Cache
İlçe → Mahalle → Sokak → Bina verileri `@Cacheable` ile Redis'te önbelleklenir. Her seviye ayrı cache key kullanır. İlk istekte veritabanından çekilir, sonrasında Redis'ten milisaniyeler içinde dönülür.

---

### 🗺️ Fizibilite Analizi (PostGIS + Haversine)
`FeasibilityService` şu adımları izler:

1. BBK kodu ile binayı bulur
2. **PostGIS KNN** algoritmasıyla 500m yarıçapındaki en yakın saha dolabını bulur
3. **Haversine formülüyle** küresel mesafeyi hesaplar
4. VDSL altyapısında gerçek telekomünikasyon formülleriyle hesaplar:
   - `attenuationDb` → Hat zayıflama değeri
   - `snrMarginDb` → Sinyal gürültü oranı marjini
   - `lineQualityPercent` → Hat kalite yüzdesi

---

### 🤖 Crowdsourced Healing Algoritması
500m içinde dolap bulunamazsa **üç kademeli fallback** devreye girer:

```
1. Müşterinin kayıtlı GPS koordinatından yakın dolap ara
       ↓ bulunamazsa
2. Sistemdeki tüm binaların koordinatlarını tara, en mantıklı alternatifi bul
       ↓ bulunamazsa
3. infrastructureAvailable = false döndür → Kullanıcıya talep oluşturma seçeneği sun
```

---

### ⚡ Asenkron Sipariş İşleme (RabbitMQ)

```
Sipariş geldi
    → Duplicate kontrol (aynı müşteri + aynı BBK + aktif sipariş)
    → En yakın dolap bulundu
        ├─ Boş port VAR  → Port tahsis et → ONAYLANDI
        └─ Boş port YOK → PORT_BEKLENIYOR → RabbitMQ kuyruğuna gönder
                              ↓
                    Admin kapasite ekler
                              ↓
                    OrderConsumer devreye girer
                              ↓
                    Sipariş otomatik ONAYLANDI
```

Her durum değişikliği `OrderStatusHistory` tablosuna zaman damgasıyla kaydedilir.

---

### 🔧 Akıllı Port Tahsis Algoritması
- Mevcut port numaraları sorgulanır
- Boşluk varsa en küçük boşluk doldurulur
- Yoksa `max + 1` ile sıradaki numara atanır
- Fragmentation oluşmadan tutarlı port tahsisi sağlanır

---

### ❌ Sipariş İptali ve Port İadesi
İki senaryo ele alınır:

| Durum | Yapılan İşlem |
|-------|---------------|
| Kuyrukta / Beklemede | Sadece status güncellenir |
| Onaylanmış (Port tahsisli) | `allocatedPorts--`, müşteri-port bağı kopar, Port kaydı silinir, Redis cache temizlenir |

IDOR koruması uygulanır: kullanıcı yalnızca **kendi** siparişini iptal edebilir.

---

### 📝 Altyapı Talep Yönetimi
- Giriş yapmadan da talep oluşturulabilir (anonim destek)
- Aynı BBK için tek talep alınır (duplicate engelleme)
- Admin talebi listeleyebilir ve statüsünü güncelleyebilir (`BEKLEMEDE` → `BILDIRIM_GONDERILDI`)
- Bölge bazlı istatistik desteği

---

### 🌱 Otomatik Test Verisi
`DataSimulationService` ile uygulama ilk çalıştığında:
- **40 saha dolabı** gerçekçi koordinatlarla oluşturulur
- **500 bina** BBK kodlarıyla oluşturulur
- **REMOTE-xxx** prefix'li binalar Doğu Anadolu koordinatlarına yerleştirilir (altyapısız bölge testi için)

---

## 🔐 Güvenlik

| Katman | Uygulama |
|--------|----------|
| Kimlik Doğrulama | Stateless JWT (HMAC-SHA256) |
| Şifre | BCrypt hashleme |
| Yetkilendirme | Rol bazlı erişim kontrolü (ADMIN / CUSTOMER) |
| IDOR Koruması | SecurityContextHolder üzerinden sahiplik doğrulaması |
| Input Validation | Jakarta Bean Validation (`@NotBlank`, `@Email`, `@Pattern`, `@Positive`) |
| SQL Injection | Tüm sorgular parametrik, string concat yok |
| Veri Sızıntısı | API yanıtlarında entity yerine DTO |
| CORS | Merkezi olarak SecurityConfig'te yönetilir |
| Secret Yönetimi | Tüm hassas değerler environment variable |

---

## 🗄️ Veritabanı Yapısı

| Tablo | Açıklama |
|-------|----------|
| `buildings` | BBK kodu, adres bilgileri ve PostGIS koordinatı |
| `customers` | Kullanıcılar, rol, hashli şifre, GPS koordinatı, port ilişkisi |
| `infrastructure_nodes` | Saha dolapları, altyapı tipi (FIBER/VDSL), koordinat, kapasite |
| `orders` | Siparişler, paket bilgisi, fiyat, hız, statü |
| `order_status_history` | Her durum değişikliğinin zaman damgalı kaydı |
| `ports` | Fiziksel portlar, dolap-müşteri bağlantısı |
| `service_requests` | Altyapı talepleri, BBK, adres, email, statü |

---

## 📡 API Grupları

Swagger UI'da endpoint'ler erişim rolüne göre gruplandırılmıştır:

| Grup | Endpoint Prefixi | Erişim |
|------|-----------------|--------|
| 🌐 Herkese Açık | `/api/v1/addresses/**`, `/api/v1/feasibility/**` | Token gerekmez |
| 🔑 Kimlik Doğrulama | `/api/v1/auth/**` | Token gerekmez |
| 👤 Müşteri İşlemleri | `/api/v1/orders/**`, `/api/v1/users/**`, `/api/v1/service-requests` (POST) | CUSTOMER veya ADMIN |
| 🔧 Yönetim Paneli | `/api/v1/admin/**`, `/api/v1/orders/nodes/**`, `/api/v1/service-requests` (GET/PUT) | Yalnızca ADMIN |

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 🚀 Kurulum

### Gereksinimler
- Java 21
- Maven
- Docker & Docker Compose

### Ortam Değişkenleri
```env
DB_PASSWORD=your_db_password
JWT_SECRET=your_256bit_secret_key
RABBITMQ_PASSWORD=guest
```

### Adımlar

```bash
# 1. Repoyu klonla
git clone https://github.com/alkmaytc/telco-project.git
cd telco-project

# 2. Altyapı servislerini başlat (PostgreSQL + PostGIS, Redis, RabbitMQ)
docker-compose up -d

# 3. Uygulamayı derle ve çalıştır
cd backend
mvn spring-boot:run
```

Uygulama ilk başladığında veritabanı **otomatik olarak** örnek verilerle doldurulur.

---

## 🧪 Testler

```bash
mvn test
```

### Test Senaryoları

| Senaryo | Servis |
|---------|--------|
| Fiber altyapı için doğru metrik hesaplaması | FeasibilityServiceTest |
| VDSL için mesafeye bağlı sinyal simülasyonu | FeasibilityServiceTest |
| Alternatif dolap algoritmasının çalışması | FeasibilityServiceTest |
| Port olmadığında siparişin kuyruğa alınması | OrderServiceTest |
| Başarılı sipariş oluşturma ve RabbitMQ gönderimi | OrderServiceTest |

---

## 🔗 İlgili Repo

Frontend reposu: [telco-frontend](https://github.com/alkmaytc/telco-frontend)
