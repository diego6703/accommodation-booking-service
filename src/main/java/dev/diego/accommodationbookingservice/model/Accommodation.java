package dev.diego.accommodationbookingservice.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

@Entity
@Table(name = "accommodations")
@Getter
@Setter
@SoftDelete(columnName = "is_deleted")
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccommodationType type;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String size;

    @ElementCollection
    @CollectionTable(name = "accommodation_amenities",
            joinColumns = @JoinColumn(name = "accommodation_id"))
    @Column(name = "amenity")
    private List<String> amenities;

    @Column(name = "daily_rate", nullable = false)
    private BigDecimal dailyRate;

    @Column(nullable = false)
    private Integer availability;
}
