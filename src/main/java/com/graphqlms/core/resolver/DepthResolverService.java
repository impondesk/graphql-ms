package com.graphqlms.core.resolver;

import com.graphqlms.core.annotation.RelationField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generic depth-based relation resolver for MongoDB documents.
 *
 * <p>Inspired by <a href="https://payloadcms.com/docs/queries/depth">Payload CMS depth option</a>,
 * this service traverses an entity's fields annotated with {@link RelationField} and resolves
 * referenced documents up to the requested depth level:</p>
 * <ul>
 *   <li>{@code depth = 0} – no resolution; relation fields remain {@code null}</li>
 *   <li>{@code depth = 1} – resolve direct relations</li>
 *   <li>{@code depth = 2} – resolve relations of relations, and so on</li>
 * </ul>
 *
 * <p>This class is a reusable accelerator component: any domain entity that annotates
 * its relation fields with {@link RelationField} automatically gains depth-aware
 * resolution without any additional configuration.</p>
 */
@Service
public class DepthResolverService {

    private static final Logger log = LoggerFactory.getLogger(DepthResolverService.class);

    /**
     * Hard cap on the depth that can be requested via the GraphQL argument.
     * Prevents accidental runaway recursion on highly connected graphs.
     */
    public static final int MAX_ALLOWED_DEPTH = 10;

    private final MongoTemplate mongoTemplate;

    public DepthResolverService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Resolve all {@link RelationField}-annotated fields in {@code entity} up to {@code depth} levels.
     *
     * @param entity the root document (may be {@code null})
     * @param depth  0 = no resolution, &gt;0 = resolve that many levels deep
     * @param <T>    entity type
     * @return the same entity instance (fields mutated in-place), or {@code null} if input is {@code null}
     */
    public <T> T resolve(T entity, int depth) {
        if (entity == null || depth <= 0) {
            return entity;
        }
        int cappedDepth = Math.min(depth, MAX_ALLOWED_DEPTH);
        processFields(entity, cappedDepth);
        return entity;
    }

    /**
     * Convenience method to resolve a list of entities.
     *
     * @param entities list of root documents
     * @param depth    depth level
     * @param <T>      entity type
     * @return the same list (each element mutated in-place)
     */
    public <T> List<T> resolveAll(List<T> entities, int depth) {
        if (entities == null || entities.isEmpty() || depth <= 0) {
            return entities;
        }
        entities.forEach(e -> resolve(e, depth));
        return entities;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void processFields(Object entity, int depth) {
        if (entity == null) {
            return;
        }

        for (Field field : getAllFields(entity.getClass())) {
            RelationField relation = field.getAnnotation(RelationField.class);
            if (relation == null) {
                continue;
            }

            field.setAccessible(true);

            try {
                if (relation.many()) {
                    resolveManyRelation(entity, field, relation, depth);
                } else {
                    resolveSingleRelation(entity, field, relation, depth);
                }
            } catch (Exception ex) {
                log.warn("Could not resolve relation '{}' on '{}': {}",
                        field.getName(), entity.getClass().getSimpleName(), ex.getMessage());
            }
        }
    }

    private void resolveSingleRelation(Object entity, Field targetField,
                                       RelationField relation, int depth) throws Exception {
        Field idField = findField(entity.getClass(), relation.idField());
        idField.setAccessible(true);

        String id = (String) idField.get(entity);
        if (id == null || id.isBlank()) {
            return;
        }

        Object resolved = mongoTemplate.findById(id, relation.targetType());
        if (resolved == null) {
            log.debug("No document found for id='{}' in collection for type '{}'",
                    id, relation.targetType().getSimpleName());
            return;
        }

        if (depth > 1) {
            processFields(resolved, depth - 1);
        }

        targetField.set(entity, resolved);
    }

    @SuppressWarnings("unchecked")
    private void resolveManyRelation(Object entity, Field targetField,
                                     RelationField relation, int depth) throws Exception {
        Field idsField = findField(entity.getClass(), relation.idField());
        idsField.setAccessible(true);

        List<String> ids = (List<String>) idsField.get(entity);
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Object> resolved = ids.stream()
                .map(id -> mongoTemplate.findById(id, relation.targetType()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (depth > 1) {
            resolved.forEach(r -> processFields(r, depth - 1));
        }

        targetField.set(entity, resolved);
    }

    /**
     * Returns all declared fields from the class hierarchy (excluding {@link Object}).
     */
    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                fields.add(f);
            }
        }
        return fields;
    }

    /**
     * Walks the class hierarchy to find a field by name.
     */
    private Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                // continue up the hierarchy
            }
        }
        throw new NoSuchFieldException(
                "Field '" + fieldName + "' not found in class hierarchy of " + type.getName());
    }
}
