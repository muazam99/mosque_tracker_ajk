package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single hadith, sourced live from the fawazahmed0/hadith-api open dataset — text is proxied verbatim, never generated.")
public record HadithResponse(
        @Schema(example = "bukhari") String collection,
        @Schema(example = "Sahih al Bukhari") String collectionName,
        @Schema(example = "eng-bukhari", description = "Edition identifier used, e.g. language/translator") String edition,
        @Schema(example = "1") int hadithNumber,
        String text,
        HadithReferenceDto reference) {}
