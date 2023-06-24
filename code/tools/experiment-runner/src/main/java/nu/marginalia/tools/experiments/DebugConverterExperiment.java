package nu.marginalia.tools.experiments;

import com.google.inject.Inject;
import nu.marginalia.converting.model.GeneratorType;
import nu.marginalia.converting.model.ProcessedDocument;
import nu.marginalia.converting.processor.DomainProcessor;
import nu.marginalia.converting.processor.logic.DocumentGeneratorExtractor;
import nu.marginalia.crawling.model.CrawledDomain;
import nu.marginalia.tools.Experiment;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import java.util.HashSet;
import java.util.Set;

public class DebugConverterExperiment extends Experiment {


    private final DomainProcessor domainProcessor;

    @Inject
    public DebugConverterExperiment(DomainProcessor domainProcessor) {
        this.domainProcessor = domainProcessor;

    }

    Set<String> seenGenerators = new HashSet<>();

    @Override
    public boolean process(CrawledDomain domain) {

        if (domain.doc == null) return true;

        var dge = new DocumentGeneratorExtractor();

        for (var doc : domain.doc) {
            if (doc.documentBody == null) continue;

            var parsed = Jsoup.parse(doc.documentBody.decode());
            parsed.getElementsByTag("head").comments()
                    .stream().filter(c -> {
                        String data = c.getData();
                        if (data.contains("<script"))
                            return false;
                        if (data.contains("[if"))
                            return false;
                        if (data.contains("shim"))
                            return false;
                        return data.contains("Generated by") || data.contains("generated by")
                                || data.contains("Powered by") || data.contains("powered by");
                    }).forEach(System.out::println);

            var generators = dge.generatorCleaned(parsed);
            for (var g : generators.keywords()) {
                if (seenGenerators.add(g)) {
                    System.out.println(g + "->" + generators.type());
                    if (generators.type() == GeneratorType.UNKNOWN) {
                        System.out.println(parsed.select("meta[name=generator]")
                                .attr("content"));
                        System.out.println(doc.url);
                    }
                }
            }

        }

//
//        var ret = domainProcessor.process(domain);
//
//
//        ret.documents.stream()
//                .filter(ProcessedDocument::isProcessedFully)
//                .peek(d -> System.out.println(d.url))
//                .map(d -> d.details.metadata)
//                .forEach(System.out::println);

        return true;
    }

    @Override
    public void onFinish() {
    }
}
