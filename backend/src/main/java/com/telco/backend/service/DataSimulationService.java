package com.telco.backend.service;

import com.telco.backend.model.Building;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import net.datafaker.Faker;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class DataSimulationService {

    private final InfrastructureNodeRepository nodeRepository;
    private final BuildingRepository buildingRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final Faker faker = new Faker();
    private final Random random = new Random();

    // Eskişehir Pilot Bölgeler Tanımı
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
    public void seedData() {
        if (nodeRepository.count() > 0 || buildingRepository.count() > 0) {
            return;
        }

        // 4 Farklı Popüler Eskişehir Bölgesi Bölge Sınırları (Bounding Boxes)
        List<Region> regions = List.of(
                new Region("Odunpazarı", "Vişnelik", 39.7620, 39.7700, 30.5000, 30.5150),
                new Region("Odunpazarı", "Akarbaşı", 39.7550, 39.7630, 30.5100, 30.5250),
                new Region("Tepebaşı", "Yenibağlar", 39.7820, 39.7900, 30.4950, 30.5100),
                new Region("Tepebaşı", "Eskibağlar", 39.7750, 39.7830, 30.5050, 30.5200)
        );

        // 1. KISIM: Bölgelere Dağıtılmış 40 Adet Saha Dolabı Ekle (Her bölgeye 10 adet)
        int nodeCounter = 1000;
        for (Region region : regions) {
            for (int i = 1; i <= 10; i++) {
                InfrastructureNode node = new InfrastructureNode();
                node.setName("TKN-DOLAP-" + (++nodeCounter));
                node.setNodeType(i % 3 == 0 ? "VDSL" : "FIBER");
                node.setDistrict(region.district);
                node.setNeighborhood(region.neighborhood);

                // --- ENVENTER/PORT SİMÜLASYONU EKLEMESİ ---
                int capacity = random.nextBoolean() ? 32 : 64; // Dolap 32'lik mi 64'lük mü?
                node.setTotalPorts(capacity);

                // Simülasyon gereği bazı dolapları tamamen dolu (stres testi için), bazılarını yarı dolu yapıyoruz
                if (i == 5) {
                    node.setAllocatedPorts(capacity); // %100 Dolu Dolap (Müşteri port bulamayacak!)
                } else {
                    node.setAllocatedPorts(random.nextInt(capacity - 5)); // Boş yer olan dolap
                }

                // Bölge sınırları dahilinde koordinat üretimi
                double lat = region.minLat + (random.nextDouble() * (region.maxLat - region.minLat));
                double lon = region.minLon + (random.nextDouble() * (region.maxLon - region.minLon));

                Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                point.setSRID(4326);
                node.setLocation(point);
                nodeRepository.save(node);
            }
        }
        // 2. KISIM: Bölgelere Dağıtılmış 500 Adet Bina Ekle (Her bölgeye 125 adet)
        for (Region region : regions) {
            for (int i = 1; i <= 125; i++) {
                Building building = new Building();
                building.setBbk(String.valueOf(1000000000L + random.nextInt(900000000)));
                building.setBuildingNumber(String.valueOf(random.nextInt(150) + 1));
                building.setStreet(faker.address().streetName() + " Sokak");
                building.setDistrict(region.district);
                building.setNeighborhood(region.neighborhood);

                // Dolaplarla tam olarak aynı mahalle sınırlarında koordinat üretimi
                double lat = region.minLat + (random.nextDouble() * (region.maxLat - region.minLat));
                double lon = region.minLon + (random.nextDouble() * (region.maxLon - region.minLon));

                Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                point.setSRID(4326);

                building.setLocation(point);
                buildingRepository.save(building);
            }
        }
    }
}