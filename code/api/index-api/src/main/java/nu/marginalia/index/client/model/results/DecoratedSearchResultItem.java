package nu.marginalia.index.client.model.results;

import lombok.Getter;
import lombok.ToString;
import nu.marginalia.model.EdgeUrl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

@Getter
@ToString
public class DecoratedSearchResultItem {
    public final SearchResultItem rawIndexResult;

    @NotNull
    public final EdgeUrl url;
    @NotNull
    public final String title;
    @NotNull
    public final String description;
    public final double urlQuality;
    @NotNull
    public final String format;

    /** Document features bitmask, see HtmlFeature */
    public final int features;

    @Nullable
    public final Integer pubYear;
    public final long dataHash;
    public final int wordsTotal;
    public final double rankingScore;

    public long documentId() {
        return rawIndexResult.getDocumentId();
    }
    public int domainId() {
        return rawIndexResult.getDomainId();
    }
    public int resultsFromDomain() {
        return rawIndexResult.getResultsFromDomain();
    }

    public List<SearchResultKeywordScore> keywordScores() {
        return rawIndexResult.getKeywordScores();
    }

    public long rankingId() {
        return rawIndexResult.getRanking();
    }

    public DecoratedSearchResultItem(SearchResultItem rawIndexResult,
                                     @NotNull
                                     EdgeUrl url,
                                     @NotNull
                                     String title,
                                     @NotNull
                                     String description,
                                     double urlQuality,
                                     @NotNull
                                     String format,
                                     int features,
                                     @Nullable
                                     Integer pubYear,
                                     long dataHash,
                                     int wordsTotal,
                                     double rankingScore)
    {
        this.rawIndexResult = rawIndexResult;
        this.url = url;
        this.title = title;
        this.description = description;
        this.urlQuality = urlQuality;
        this.format = format;
        this.features = features;
        this.pubYear = pubYear;
        this.dataHash = dataHash;
        this.wordsTotal = wordsTotal;
        this.rankingScore = rankingScore;
    }
}
