package dev.diego.accommodationbookingservice.config;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.ReportingPolicy;

@org.mapstruct.MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        implementationPackage = "dev.diego.accommodationbookingservice.mapper.impl"
)
public class MapperConfig {
}
