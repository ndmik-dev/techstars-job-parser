package io.ndmik.tsparser.service;

import io.ndmik.tsparser.dto.ScrapedJob;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class TechstarsJobsParser {

    private static final String JOB_CARD_SELECTOR = "[data-testid=job-list-item]";
    private static final String JOB_TITLE_SELECTOR = "[data-testid=job-title-link]";
    private static final String COMPANY_LINK_SELECTOR = "[itemprop=hiringOrganization] [data-testid=link]";
    private static final String LOCATION_SELECTOR = "[itemprop=addressLocality]";
    private static final String TAG_SELECTOR = "[data-testid=tag]";

    public List<ScrapedJob> parse(String html, String baseUrl) {
        return parse(org.jsoup.Jsoup.parse(html, baseUrl), baseUrl);
    }

    public List<ScrapedJob> parse(Document document, String baseUrl) {
        return document.select(JOB_CARD_SELECTOR).stream()
                .map(card -> parseCard(card, baseUrl))
                .filter(Objects::nonNull)
                .toList();
    }

    private ScrapedJob parseCard(Element card, String baseUrl) {
        Element titleLink = card.selectFirst(JOB_TITLE_SELECTOR);
        if (titleLink == null) {
            return null;
        }

        String sourceUrl = absoluteUrl(titleLink.attr("href"), baseUrl);
        String title = text(titleLink);
        String companyName = text(card.selectFirst(COMPANY_LINK_SELECTOR));
        String companyUrl = absoluteUrl(attr(card.selectFirst(COMPANY_LINK_SELECTOR), "href"), baseUrl);
        String location = attr(card.selectFirst(LOCATION_SELECTOR), "content");
        String description = attr(card.selectFirst("meta[itemprop=description]"), "content");
        String salaryText = text(card.selectFirst("[class*=ejsdCL] p"));
        String postedAtText = text(card.selectFirst(".added div"));
        List<String> tags = uniqueTexts(card.select(TAG_SELECTOR).eachText());
        String seniority = firstMatchingTag(tags, "internship", "entry level", "associate", "mid-senior level", "director", "executive");

        return new ScrapedJob(
                externalId(sourceUrl),
                title,
                companyName,
                companyUrl,
                location,
                description,
                sourceUrl,
                null,
                seniority,
                salaryText,
                postedAtText,
                tags
        );
    }

    private static String absoluteUrl(String url, String baseUrl) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return URI.create(baseUrl).resolve(url).toString();
    }

    private static String externalId(String sourceUrl) {
        String path = URI.create(sourceUrl).getPath();
        String lastPathSegment = path.substring(path.lastIndexOf('/') + 1);
        if (!lastPathSegment.isBlank()) {
            return lastPathSegment;
        }
        return sha256(sourceUrl);
    }

    private static String firstMatchingTag(List<String> tags, String... values) {
        for (String tag : tags) {
            String normalizedTag = tag.toLowerCase(Locale.ROOT);
            for (String value : values) {
                if (normalizedTag.equals(value)) {
                    return tag;
                }
            }
        }
        return null;
    }

    private static List<String> uniqueTexts(List<String> values) {
        return new LinkedHashSet<>(values.stream()
                .map(TechstarsJobsParser::normalize)
                .filter(value -> !value.isBlank())
                .toList())
                .stream()
                .toList();
    }

    private static String text(Element element) {
        return element == null
                ? null
                : normalize(element.text());
    }

    private static String attr(Element element, String attribute) {
        return element == null
                ? null
                : normalize(element.attr(attribute));
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.replace('\u00a0', ' ')
                  .trim()
                  .replaceAll("\\s+", " ");
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
