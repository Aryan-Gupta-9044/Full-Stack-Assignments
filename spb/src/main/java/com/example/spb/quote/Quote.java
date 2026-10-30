package com.example.spb.quote;

/** source = "dummyjson.com" for live quotes, "offline" for the built-in fallback list. */
public record Quote(String text, String author, String source) {
}
