package org.orgarchive.archive.document;

public record DocumentDto(Long id, String title, String content, String organizationUuid, String metadata) {}
