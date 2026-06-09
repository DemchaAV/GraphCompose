package com.demcha.compose.engine.core;

import com.demcha.compose.engine.core.SystemECS;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Stream;

/**
 * A registry for managing and retrieving Entity Component System (ECS) systems.
 * Provides methods to add, retrieve, and stream registered systems.
 */
@Slf4j
@Getter
@Accessors(fluent = true)
public class SystemRegistry {

    /**
     * A map storing registered systems, keyed by their class type.
     * Uses a LinkedHashMap to maintain the order of insertion.
     */
    private final Map<Class<? extends SystemECS>, SystemECS> systems = new LinkedHashMap<>();

    /**
     * The text-measurement service provider, held separately from the
     * process()-driven {@link #systems} map. Measurement exposes font metrics to
     * builders and layout helpers on demand; it is not a {@link SystemECS} and
     * never participates in the {@code processSystems()} loop, so it is registered
     * out-of-band via {@link #registerTextMeasurement(TextMeasurementSystem)}
     * rather than {@link #addSystem(SystemECS)}.
     */
    @Getter(AccessLevel.NONE)
    private TextMeasurementSystem textMeasurement;

    /**
     * Registers the text-measurement service exposed to the legacy engine's text
     * components and layout alignment. Unlike {@link #addSystem(SystemECS)} this
     * does not enroll the measurement system in the {@code process()} loop — it is
     * a service provider, not an ECS system.
     *
     * @param textMeasurement the measurement service to expose
     * @since 1.7.1
     */
    public void registerTextMeasurement(TextMeasurementSystem textMeasurement) {
        this.textMeasurement = Objects.requireNonNull(textMeasurement, "textMeasurement");
    }

    /**
     * Retrieves the registered text-measurement service, if any.
     *
     * @return an Optional containing the measurement service, or empty if none has
     *     been registered
     * @since 1.7.1
     */
    public Optional<TextMeasurementSystem> textMeasurement() {
        return Optional.ofNullable(textMeasurement);
    }

    /**
     * Adds a single system to the registry.
     *
     * @param system the system to add
     * @param <T>    the type of the system, extending SystemECS
     */
    public <T extends SystemECS> void addSystem(T system) {
        log.info("Adding SystemECS {}", system.getClass().getName());
        systems.put(system.getClass(), system);
    }

    /**
     * Returns a stream of all registered systems.
     *
     * @return a stream of SystemECS instances
     */
    public Stream<SystemECS> getStream() {
        return systems.values().stream();
    }

    /**
     * Retrieves a system from the registry by its class type.
     * If an exact match is not found, it searches for a subclass or implementation.
     *
     * @param systemClass the class of the system to retrieve
     * @param <T>         the expected type of the system
     * @return an Optional containing the system if found, or an empty Optional otherwise
     */
    public <T extends SystemECS> Optional<T> getSystem(Class<T> systemClass) {
        SystemECS system = systems.get(systemClass);
        if (systemClass.isInstance(system)) {
            return Optional.of(systemClass.cast(system));
        }
        for (SystemECS candidate : systems.values()) {
            if (systemClass.isInstance(candidate)) {
                return Optional.of(systemClass.cast(candidate));
            }
        }
        return Optional.empty();
    }

    /**
     * Adds a list of systems to the registry.
     *
     * @param systems the list of systems to add
     * @param <T>     the type of the systems, extending SystemECS
     */
    public <T extends SystemECS> void addAllSystems(List<T> systems) {
        systems.forEach(this::addSystem);
    }

}
