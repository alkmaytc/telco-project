package com.telco.backend.service;

import com.telco.backend.model.Building;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import net.datafaker.Faker;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSimulationService {

    private final InfrastructureNodeRepository nodeRepository;
    private final BuildingRepository buildingRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final Faker faker = new Faker();
    private final Random random = new Random();

    private static class Region {
        String district;
        String neighborhood;
        double minLat, maxLat, minLon, maxLon;

        Region(String district, String neighborhood, double minLat, double maxLat, double minLon, double maxLon) {
            this.district = district;
            this.neighborhood = neighborhood;
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }
    }

    @PostConstruct
    public void initializeSimulation() {
        cleanupOldBadData(); // 🎯 Önce eski çöpleri temizle!
        seedData();
        seedRemoteBuildings();
    }

    // 🎯 YENİ EKLENDİ: Eski merkezde kalan hatalı REMOTE-0 serisi binaları acımadan siler
    private void cleanupOldBadData() {
        log.info("🧹 Eski hatalı Sivrihisar test verileri aranıyor...");
        List<Building> badBuildings = buildingRepository.findAll().stream()
                .filter(b -> b.getBbk() != null && b.getBbk().startsWith("REMOTE-0"))
                .toList();

        if (!badBuildings.isEmpty()) {
            buildingRepository.deleteAll(badBuildings);
            log.info("🗑️ {} adet eski hatalı merkez kaydı veritabanından silindi!", badBuildings.size());
        }
    }

    private void seedData() {
        if (nodeRepository.count() > 0 || buildingRepository.count() > 0) {
            return;
        }

        log.info("Simülasyon başlatılıyor: Merkez binalar ve saha dolapları oluşturuluyor...");

        List<Region> regions = List.of(
                new Region("Odunpazarı", "Vişnelik", 39.7620, 39.7700, 30.5000, 30.5150),
                new Region("Odunpazarı", "Akarbaşı", 39.7550, 39.7630, 30.5100, 30.5250),
                new Region("Tepebaşı", "Yenibağlar", 39.7820, 39.7900, 30.4950, 30.5100),
                new Region("Tepebaşı", "Eskibağlar", 39.7750, 39.7830, 30.5050, 30.5200)
        );

        int nodeCounter = 1000;
        for (Region region : regions) {
            for (int i = 1; i <= 10; i++) {
                InfrastructureNode node = new InfrastructureNode();
                node.setName("TKN-DOLAP-" + (++nodeCounter));
                node.setNodeType(i % 3 == 0 ? "VDSL" : "FIBER");
                node.setDistrict(region.district);
                node.setNeighborhood(region.neighborhood);

                int capacity = random.nextBoolean() ? 32 : 64;
                node.setTotalPorts(capacity);

                if (i == 5) {
                    node.setAllocatedPorts(capacity);
                } else {
                    node.setAllocatedPorts(random.nextInt(capacity - 5));
                }

                double lat = region.minLat + (random.nextDouble() * (region.maxLat - region.minLat));
                double lon = region.minLon + (random.nextDouble() * (region.maxLon - region.minLon));

                Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                point.setSRID(4326);
                node.setLocation(point);
                nodeRepository.save(node);
            }
        }

        for (Region region : regions) {
            for (int i = 1; i <= 125; i++) {
                Building building = new Building();
                building.setBbk(String.valueOf(1000000000L + random.nextInt(900000000)));
                building.setBuildingNumber(String.valueOf(random.nextInt(150) + 1));
                building.setStreet(faker.address().streetName() + " Sokak");
                building.setDistrict(region.district);
                building.setNeighborhood(region.neighborhood);

                double lat = region.minLat + (random.nextDouble() * (region.maxLat - region.minLat));
                double lon = region.minLon + (random.nextDouble() * (region.maxLon - region.minLon));

                Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                point.setSRID(4326);

                building.setLocation(point);
                buildingRepository.save(building);
            }
        }
    }

    private void seedRemoteBuildings() {
        if (buildingRepository.findByBbk("REMOTE-101").isPresent()) {
            return;
        }

        log.info("🏗️ [ALTYAPI YOK TESTİ] Gerçek Sivrihisar Merkez'e 20 adet altyapısız bina inşa ediliyor...");

        double baseLat = 39.4494;
        double baseLon = 31.5375;

        for (int i = 1; i <= 20; i++) {
            Building building = new Building();
            building.setBbk(String.format("REMOTE-%d", 100 + i));
            building.setBuildingNumber(String.valueOf(i));
            building.setStreet("Altyapısızlar Sokak");
            building.setDistrict("Sivrihisar");
            building.setNeighborhood("Uzaklar Mahallesi");

            double lat = baseLat + (random.nextDouble() * 0.005);
            double lon = baseLon + (random.nextDouble() * 0.005);

            Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
            point.setSRID(4326);

            building.setLocation(point);
            buildingRepository.save(building);
        }

        log.info("✅ [ALTYAPI YOK TESTİ] Issız binalar başarıyla oluşturuldu! Test BBK: REMOTE-101");
    }
}