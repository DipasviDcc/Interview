package org.example.model;

/** An exact, contiguous excerpt of one retrieved article. */
public record Evidence(KbDocument document, String snippet, double retrievalScore) {}
