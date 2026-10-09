package edu.maestria.observabilidad.fulfillment.client;

import edu.maestria.observabilidad.lab33.ChaosPolicy;
import edu.maestria.observabilidad.fulfillment.domain.FulfillmentView;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DataClient {

    private final RestClient restClient;

    public DataClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public FulfillmentView load(String orderId, String experiment) {
        RestClient.RequestHeadersSpec<?> request = restClient.get().uri("/api/records/orders/{orderId}", orderId);
        if (experiment != null && !experiment.isBlank()) {
            request = request.header(ChaosPolicy.EXPERIMENT_HEADER, experiment);
        }
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, (req, response) -> {
                    throw new DataClientException(response.getStatusCode().value());
                })
                .body(FulfillmentView.class);
    }
}
