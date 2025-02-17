package nu.marginalia.control.app.svc;

import com.google.inject.Inject;
import nu.marginalia.client.Context;
import nu.marginalia.index.client.model.query.SearchSetIdentifier;
import nu.marginalia.index.query.limit.QueryLimits;
import nu.marginalia.model.EdgeUrl;
import nu.marginalia.query.client.QueryClient;
import nu.marginalia.query.model.QueryParams;
import nu.marginalia.renderer.RendererFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class SearchToBanService {
    private final ControlBlacklistService blacklistService;
    private final RendererFactory rendererFactory;
    private final QueryClient queryClient;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public SearchToBanService(ControlBlacklistService blacklistService,
                              RendererFactory rendererFactory,
                              QueryClient queryClient)
    {

        this.blacklistService = blacklistService;
        this.rendererFactory = rendererFactory;
        this.queryClient = queryClient;
    }

    public void register() throws IOException {
        var searchToBanRenderer = rendererFactory.renderer("control/app/search-to-ban");

        Spark.get("/public/search-to-ban", this::handle, searchToBanRenderer::render);
        Spark.post("/public/search-to-ban", this::handle, searchToBanRenderer::render);
    }

    public Object handle(Request request, Response response) {
        if (Objects.equals(request.requestMethod(), "POST")) {
            executeBlacklisting(request);

            return findResults(Context.fromRequest(request), request.queryParams("query"));
        }

        return findResults(Context.fromRequest(request), request.queryParams("q"));
    }

    private Object findResults(Context ctx, String q) {
        if (q == null || q.isBlank()) {
            return Map.of();
        } else {
            return executeQuery(ctx, q);
        }
    }

    private void executeBlacklisting(Request request) {
        String query = request.queryParams("query");
        for (var param : request.queryParams()) {
            logger.info(param + ": " + request.queryParams(param));
            if ("query".equals(param)) {
                continue;
            }
            EdgeUrl.parse(param).ifPresent(url ->
                    blacklistService.addToBlacklist(url.domain, query)
            );
        }
    }

    private Object executeQuery(Context ctx, String query) {
        return queryClient.search(ctx, new QueryParams(
                query, new QueryLimits(2, 200, 250, 8192),
                SearchSetIdentifier.NONE
        ));
    }
}
