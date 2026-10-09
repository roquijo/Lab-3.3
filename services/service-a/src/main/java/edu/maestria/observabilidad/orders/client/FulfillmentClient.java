package edu.maestria.observabilidad.orders.client;

import edu.maestria.observabilidad.lab33.ChaosPolicy;
import edu.maestria.observabilidad.orders.domain.OrderView;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FulfillmentClient {

    private final RestClient restClient;

    public FulfillmentClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public OrderView fetch(String orderId) {
        String experiment = currentExperiment();
        RestClient.RequestHeadersSpec<?> request = restClient.get().uri("/api/fulfillment/{orderId}", orderId);
        if (experiment != null) {
            request = request.header(ChaosPolicy.EXPERIMENT_HEADER, experiment);
        }
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, (req, response) -> {
                    throw new FulfillmentException(response.getStatusCode().value());
                })
                .body(OrderView.class);
    }

    private String currentExperiment() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest().getHeader(ChaosPolicy.EXPERIMENT_HEADER);
        }
        return null;
    }
}
